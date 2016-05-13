package pt.ulisboa.tecnico.cmov.ubibike;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

//import pt.ulisboa.tecnico.cmov.ubibike.commons.UserJsonKey;
//import pt.ulisboa.tecnico.cmov.ubibike.commons.UserReplyType;
//import pt.ulisboa.tecnico.cmov.ubibike.commons.UserRequestType;

public final class SignUpActivity extends NetworkHandlerActivity {

    UbiBikeApplication mUbiBikeApplication;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mUbiBikeApplication = (UbiBikeApplication) getApplicationContext();

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Intent intent = getIntent();
        if (intent.hasExtra(IntentKey.USERNAME.toString()) && intent.hasExtra(IntentKey.PASSWORD.toString())) {
            ((EditText) findViewById(R.id.username)).setText(intent.getStringExtra(IntentKey.USERNAME.toString()));
            ((EditText) findViewById(R.id.password)).setText(intent.getStringExtra(IntentKey.PASSWORD.toString()));
        }

        mNetworkHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                //Get login values
                EditText usernameEditText = (EditText) findViewById(R.id.username),
                        passwordEditText = (EditText) findViewById(R.id.password);
                Button loginButton = (Button) findViewById(R.id.login),
                        signUpButton = (Button) findViewById(R.id.signUp);

                //Enable all layout elements
                usernameEditText.setEnabled(true);
                passwordEditText.setEnabled(true);
                loginButton.setEnabled(true);
                signUpButton.setEnabled(true);

                NetworkMessageCode networkMessageCode = NetworkMessageCode.values()[message.what];
                switch (networkMessageCode) {
                    case SIGN_UP_USER:
                        finishSignUpUser(message);
                        break;
                    case RECONNECT_NETWORK:
                        onDataConnected();
                        return;
                    case EXIT:
                        exitApp(SignUpActivity.this, (String) message.obj);
                        return;
                    default:
                        exitApp(SignUpActivity.this, "Invalid network message code " + networkMessageCode.toString());
                        return;
                }

                //Enable buttons
                enableButtons(true);
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();

        //Change activity layout if offline;
        onNetworkChange();
    }

    private void finishSignUpUser(Message message) {
        TextView errorView = (TextView) findViewById(R.id.errorView);
        try {
            //Get and verify JSON elements
            JSONObject reply = (JSONObject) message.obj;
            UserReplyType userReplyType = UserReplyType.values()[getIntFromJson(reply, UserJsonKey.REPLY_TYPE)];


            switch (userReplyType) {
                case SUCCESS:
                    //Success
                    SharedPreferences sharedPreferences = getSharedPreferences(SharedPreferenceKey.NAME.toString(), Context.MODE_PRIVATE);
                    mUbiBikeApplication.setUsername(sharedPreferences.getString(SharedPreferenceKey.USERNAME.toString(), Constant.INVALID_VALUE));
                    mUbiBikeApplication.setPassword(sharedPreferences.getString(SharedPreferenceKey.PASSWORD.toString(), Constant.INVALID_VALUE));

                    if (mUbiBikeApplication.getUsername() == null ||
                            mUbiBikeApplication.getPassword() == null ||
                            mUbiBikeApplication.getUsername().equals(Constant.INVALID_VALUE) ||
                            mUbiBikeApplication.getPassword().equals(Constant.INVALID_VALUE)) {
                        exitApp(SignUpActivity.this, "Shared preference has no user info");
                        return;
                    }


                    String secretPath = getApplicationInfo().dataDir + "/" + mUbiBikeApplication.getUsername() + ".txt";
                    try {
                        File yourFile = new File(secretPath);
                        if (!yourFile.exists()) {
                            yourFile.createNewFile();

                            //write to key file
                            String keyStr = getStringFromJson(reply, UserJsonKey.SECRET_KEY);
                            Writer writer = new OutputStreamWriter(new FileOutputStream(secretPath), "utf-8");
                            writer = new BufferedWriter(writer);
                            writer.write(keyStr);
                            writer.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    //get rest of info
                    mUbiBikeApplication.setCurrentScore(getIntFromJson(
                            getObjectFromJson(reply, UserJsonKey.USER),
                            UserJsonKey.USER_CURRENT_SCORE));


                    if (reply.has(UserJsonKey.TRAJECTORY.toString())) {
                        mUbiBikeApplication.setLastTrajectory(getObjectFromJson(
                                getObjectFromJson(reply, UserJsonKey.TRAJECTORY),
                                UserJsonKey.USER_MOST_RECENT_TRAJECTORY));
                    }
                    mUbiBikeApplication.setStations(getArrayFromJson(reply, UserJsonKey.STATIONS));

                    if (reply.has(UserJsonKey.BIKE.toString())) {
                        mUbiBikeApplication.setBeaconId(getStringFromJson(
                                getObjectFromJson(reply, UserJsonKey.BIKE),
                                UserJsonKey.BIKE_BEACON_ID));

                        mUbiBikeApplication.setLastStationStation(getStringFromJson(
                                getObjectFromJson(reply, UserJsonKey.BIKE),
                                UserJsonKey.BIKE_LAST_STATION_NAME));
                    }

                    startActivity(new Intent(SignUpActivity.this, MainActivity.class));
                    finish();
                    break;
                case ALREADY_EXISTS:
                    //Print error
                    errorView.setText(R.string.userAlreadyExistsError);
                    break;
                case TECHNICAL_FAILURE:
                    exitApp(SignUpActivity.this, "The server is down");
                    return;
                default:
                    exitApp(SignUpActivity.this, "Invalid user reply type");
            }
        } catch (JSONException | InvalidReplyException exception) {
            exitApp(SignUpActivity.this, exception.getMessage() + ".");
        }
    }

    public void signUpClicked(View view) {
        //Disable buttons
        enableButtons(false);

        InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);

        //Get login values
        EditText usernameEditText = (EditText) findViewById(R.id.username),
                 passwordEditText = (EditText) findViewById(R.id.password);
        Button loginButton = (Button) findViewById(R.id.login),
               signUpButton = (Button) findViewById(R.id.signUp);
        String usernameValue = usernameEditText.getText().toString(),
               passwordValue = passwordEditText.getText().toString();

        //Check if login values exist
        if(usernameValue.equals("")) {
            //Print error
            TextView errorView = (TextView) findViewById(R.id.errorView);
            errorView.setText(R.string.usernameLoginError);
            return;
        }
        if(passwordValue.equals("")) {
            //Print error
            TextView errorView = (TextView) findViewById(R.id.errorView);
            errorView.setText(R.string.passwordLoginError);
            return;
        }

        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();

        //Check if user is online
        if (activeInfo == null || !activeInfo.isConnected()) {
            //Print error
            TextView errorView = (TextView) findViewById(R.id.errorView);
            errorView.setText(R.string.onDataDisconnected);
            return;
        }

        //Change UI: Connecting...
        usernameEditText.setEnabled(false);
        passwordEditText.setEnabled(false);
        loginButton.setEnabled(false);
        signUpButton.setEnabled(false);
        TextView errorView = (TextView) findViewById(R.id.errorView);
        errorView.setText(R.string.connectionLogin);

        try {
            //Creating request message
            UserRequestType userRequestType = UserRequestType.SIGN_UP_USER;
            JSONObject request = new JSONObject();
            request.put(UserJsonKey.REQUEST_TYPE.toString(), userRequestType.ordinal());
            request.put(UserJsonKey.USERNAME.toString(), usernameValue);
            request.put(UserJsonKey.PASSWORD.toString(), passwordValue);

            //Start sign up thread
            ClientThread clientThreadLogin = new ClientThread(
                    userRequestType,
                    mNetworkHandler,
                    NetworkMessageCode.SIGN_UP_USER,
                    request);
            new Thread(clientThreadLogin).start();
        } catch (JSONException exception) {
            //Print error
            errorView.setText(R.string.invalidState);
        }

        //Save sign up fields
        SharedPreferences sharedPreferences = getSharedPreferences(SharedPreferenceKey.NAME.toString(), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString(SharedPreferenceKey.USERNAME.toString(), usernameValue);
        editor.putString(SharedPreferenceKey.PASSWORD.toString(), passwordValue);

        editor.apply();
    }

    public void loginClicked(View view) {
        returnToParentActivity();
    }

    @Override
    protected void returnToParentActivity() {
        //Disable buttons
        enableButtons(false);

        //Send results to login activity
        setResult(RESULT_OK,
                new Intent(SignUpActivity.this, LoginActivity.class)
                        .putExtra(IntentKey.USERNAME.toString(),
                                ((EditText) findViewById(R.id.username)).getText().toString())
                        .putExtra(IntentKey.PASSWORD.toString(),
                                ((EditText) findViewById(R.id.password)).getText().toString()));
        finish();
    }

    @Override
    protected void enableButtons(boolean option) {
        findViewById(R.id.signUp).setEnabled(option);
    }

    @Override
    protected void exitApp(Context context, String message) {
        //Print error
        ((TextView) findViewById(R.id.errorView))
                .setText(String.format("%s", message + " Apologies for inconvenience."));
    }
}
