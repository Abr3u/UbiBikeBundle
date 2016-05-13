package pt.ulisboa.tecnico.cmov.ubibike;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
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

//import pt.ulisboa.tecnico.cmov.ubibike.commons.UserJsonKey;
//import pt.ulisboa.tecnico.cmov.ubibike.commons.UserReplyType;
//import pt.ulisboa.tecnico.cmov.ubibike.commons.UserRequestType;

public final class LoginActivity extends NetworkHandlerActivity {

    UbiBikeApplication mUbiBikeApplication;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LogEnter(LoginActivity.class.getSimpleName(), "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mUbiBikeApplication = (UbiBikeApplication) getApplicationContext();

        //Add previously defined fields
        Intent intent = getIntent();
        if(intent.hasExtra(IntentKey.USERNAME.toString()) && intent.hasExtra(IntentKey.PASSWORD.toString())) {
            ((EditText) findViewById(R.id.username)).setText(intent.getStringExtra(IntentKey.USERNAME.toString()));
            ((EditText) findViewById(R.id.password)).setText(intent.getStringExtra(IntentKey.PASSWORD.toString()));
        } else {
            SharedPreferences sharedPreferences = getSharedPreferences(SharedPreferenceKey.NAME.toString(), Context.MODE_PRIVATE);
            String username = sharedPreferences.getString(SharedPreferenceKey.USERNAME.toString(), Constant.INVALID_VALUE),
                   password = sharedPreferences.getString(SharedPreferenceKey.PASSWORD.toString(), Constant.INVALID_VALUE);

            if(username != null && password != null && !username.equals(Constant.INVALID_VALUE) && !password.equals(Constant.INVALID_VALUE)) {
                ((EditText) findViewById(R.id.username)).setText(username);
                ((EditText) findViewById(R.id.password)).setText(password);
            }
        }

        //Get error message if exists
        if(intent.hasExtra(IntentKey.LOGIN_MESSAGE.toString())) {
            //Print error
            TextView errorView = ((TextView) findViewById(R.id.errorView));
            errorView.setText(intent.getStringExtra(IntentKey.LOGIN_MESSAGE.toString()));
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
                switch(networkMessageCode) {
                    case LOGIN_USER:
                        finishLoginUser(message);
                        break;
                    case RECONNECT_NETWORK:
                        onDataConnected();
                        return;
                    case EXIT:
                        exitApp(LoginActivity.this, (String) message.obj);
                        return;
                    default:
                        exitApp(LoginActivity.this, "Invalid network message code " + networkMessageCode.toString());
                        return;
                }

                //Enable buttons
                enableButtons(true);
            }
        };
        LogExit(LoginActivity.class.getSimpleName(), "onCreate");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        LogEnter(LoginActivity.class.getSimpleName(), "onActivityResult");
        // Check which request we're responding to
        ActivityMessageCode activityMessageCode = ActivityMessageCode.values()[requestCode];
        switch(activityMessageCode) {
            case SIGN_UP:
                // Make sure the request was successful
                if (resultCode == RESULT_OK) {
                    if(data.hasExtra(IntentKey.USERNAME.toString()) && data.hasExtra(IntentKey.PASSWORD.toString())) {
                        ((EditText) findViewById(R.id.username)).setText(data.getStringExtra(IntentKey.USERNAME.toString()));
                        ((EditText) findViewById(R.id.password)).setText(data.getStringExtra(IntentKey.PASSWORD.toString()));
                    }
                }
                break;
            default:
                exitApp(LoginActivity.this, "Invalid activity message code " + activityMessageCode.toString());
                break;
        }
        LogExit(LoginActivity.class.getSimpleName(), "onActivityResult");
    }

    @Override
    protected void onResume() {
        LogEnter(LoginActivity.class.getSimpleName(), "onResume");
        super.onResume();

        //Change activity layout if offline;
        onNetworkChange();
        LogExit(LoginActivity.class.getSimpleName(), "onResume");
    }

    private void finishLoginUser(Message message) {
        LogEnter(LoginActivity.class.getSimpleName(), "finishLoginUser");
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
                        exitApp(LoginActivity.this, "Shared preference has no user info");
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

                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                    break;
                case INVALID_PASSWORD:
                    //Print error
                    errorView.setText(R.string.invalidPasswordError);
                    break;
                case INVALID_USERNAME:
                    //Print error
                    errorView.setText(R.string.invalidUsernameError);
                    break;
                case TECHNICAL_FAILURE:
                    exitApp(LoginActivity.this, "The server is down.");
                    return;
                default:
                    exitApp(LoginActivity.this, "Invalid user reply type");
            }
        } catch (JSONException | InvalidReplyException exception) {
            exitApp(LoginActivity.this, exception.getMessage() + ".");
        }
        LogExit(LoginActivity.class.getSimpleName(), "finishLoginUser");
    }

    public void loginClicked(View view) {
        LogEnter(LoginActivity.class.getSimpleName(), "loginClicked");
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

        //Change UI: Connecting...
        usernameEditText.setEnabled(false);
        passwordEditText.setEnabled(false);
        loginButton.setEnabled(false);
        signUpButton.setEnabled(false);
        TextView errorView = (TextView) findViewById(R.id.errorView);
        errorView.setText(R.string.connectionLogin);

        try {
            //Creating request message
            UserRequestType userRequestType = UserRequestType.LOGIN_USER;
            JSONObject request = new JSONObject();
            request.put(UserJsonKey.REQUEST_TYPE.toString(), userRequestType.ordinal());
            request.put(UserJsonKey.USERNAME.toString(), usernameValue);
            request.put(UserJsonKey.PASSWORD.toString(), passwordValue);

            //Start login thread
            ClientThread clientThreadLogin = new ClientThread(
                    userRequestType,
                    mNetworkHandler,
                    NetworkMessageCode.LOGIN_USER,
                    request);
            new Thread(clientThreadLogin).start();
        } catch (JSONException exception) {
            exitApp(LoginActivity.this, exception.getMessage() + ".");
            return;
        }

        //Save login fields
        SharedPreferences sharedPreferences = getSharedPreferences(SharedPreferenceKey.NAME.toString(), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString(SharedPreferenceKey.USERNAME.toString(), usernameValue);
        editor.putString(SharedPreferenceKey.PASSWORD.toString(), passwordValue);

        editor.apply();
        LogExit(LoginActivity.class.getSimpleName(), "loginClicked");
    }

    public void signUpClicked(View view) {
        LogEnter(LoginActivity.class.getSimpleName(), "signUpClicked");
        //Disable buttons
        enableButtons(false);

        //Get login values
        EditText usernameEditText = (EditText) findViewById(R.id.username),
                passwordEditText = (EditText) findViewById(R.id.password);
        String usernameValue = usernameEditText.getText().toString(),
                passwordValue = passwordEditText.getText().toString();

        Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
        intent.putExtra(IntentKey.USERNAME.toString(), usernameValue);
        intent.putExtra(IntentKey.PASSWORD.toString(), passwordValue);
        startActivityForResult(intent, ActivityMessageCode.SIGN_UP.ordinal());
        LogExit(LoginActivity.class.getSimpleName(), "signUpClicked");
    }

    @Override
    protected void enableButtons(boolean option) {
        findViewById(R.id.login).setEnabled(option);
    }

    @Override
    protected void exitApp(Context context, String message) {
        LogEnter(LoginActivity.class.getSimpleName(), "exitApp");
        //Print error
        ((TextView) findViewById(R.id.errorView))
                .setText(String.format("%s", message + " Apologies for inconvenience."));
        LogExit(LoginActivity.class.getSimpleName(), "exitApp");
    }
}
