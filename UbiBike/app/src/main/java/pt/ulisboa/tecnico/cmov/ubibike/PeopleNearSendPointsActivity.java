package pt.ulisboa.tecnico.cmov.ubibike;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

//import pt.ulisboa.tecnico.cmov.ubibike.commons.UserJsonKey;
//import pt.ulisboa.tecnico.cmov.ubibike.commons.UserRequestType;

public final class PeopleNearSendPointsActivity extends PeopleNearActivity {

    private String _password;
    private String _currentPoints;
    private String SECRETPATH ="";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myWifiService.setNearPointsActivity(this);

        Intent i = getIntent();
        SECRETPATH = getApplicationInfo().dataDir+"/"+username+".txt";
        username = i.getStringExtra(IntentKey.USERNAME.toString());
        _password = i.getStringExtra(IntentKey.PASSWORD.toString());

        TextView current = (TextView) findViewById(R.id.PointsEditText);
        _currentPoints = current.getText().toString();

        findViewById(R.id.newMessageButtonSend).setOnClickListener(listenerSendButtonNew);
        mToSend.setHint("how many points to send?");

    }

    protected View.OnClickListener listenerSendButtonNew = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            findViewById(R.id.newMessageButtonSend).setEnabled(false);

            new SendPointsTask().executeOnExecutor(
                    AsyncTask.THREAD_POOL_EXECUTOR,
                    mToSend.getText().toString());
        }
    };


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    public class SendPointsTask extends AsyncTask<String, Void, Void> {

        private String pointsToSend = "";
        private String updatedPoints = _currentPoints;
        private JSONObject tradedPointsJSON;
        boolean sent;

        @Override
        protected Void doInBackground(String... msg) {
            try {
                pointsToSend = msg[0];
                if (Integer.parseInt(pointsToSend) > Integer.parseInt(_currentPoints)
                        || Integer.parseInt(pointsToSend) <= 0) {
                    pointsToSend = "";
                    sent = false;
                } else {
                    //generate JSON object of this trade
                    try {
                        //Get current date
                        String date = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date());

                        //Creating request signature
                        SecretSignature secretSignature = new SecretSignature().invoke(
                                username + currentPeerUname + pointsToSend + date);

                        tradedPointsJSON = new JSONObject()
                                .put(UserJsonKey.REQUEST_TYPE.toString(), UserRequestType.SEND_POINTS.ordinal())
                                .put(UserJsonKey.USERNAME.toString(), username)
                                .put(UserJsonKey.RECEIVER.toString(), currentPeerUname)
                                .put(UserJsonKey.TRADED_POINTS.toString(), pointsToSend)
                                .put(UserJsonKey.DATE.toString(), date)
                                .put(UserJsonKey.SIGNATURE.toString(), secretSignature.getEncodedSignature());
                    } catch (NoSuchAlgorithmException |
                            InvalidKeySpecException |
                            NoSuchPaddingException |
                            IllegalBlockSizeException |
                            BadPaddingException |
                            InvalidParameterSpecException |
                            InvalidKeyException |
                            UnsupportedEncodingException |
                            JSONException exception) {
                        exitApp(PeopleNearSendPointsActivity.this, exception.getMessage() + ".");
                    }

                    //Send Points to peer
                    String aux = "Received " + pointsToSend + " points from " + username + "\n";
                    mCliSocket.getOutputStream().write(aux.getBytes());
                    updatedPoints = ""+(Integer.parseInt(_currentPoints) - Integer.parseInt(pointsToSend));
                    sent = true;
                }
            } catch (IOException exception) {
                exitApp(PeopleNearSendPointsActivity.this, exception.getMessage() + ".");
            }
            mCliSocket = null;
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {

            guiUpdateDisconnectedState();

            Intent i = null;
            i = getIntent();
            if (sent) {
                i.putExtra(IntentKey.TRADED_POINT_JSON.toString(), tradedPointsJSON.toString());
                i.putExtra(IntentKey.CURRENT_POINTS.toString(), updatedPoints);
                setResult(RESULT_OK, i);
            } else {
                setResult(RESULT_CANCELED, i);
            }
            finish();
        }
    }

    private class SecretSignature {
        private byte[] encodedSignature;

        public String getEncodedSignature() {
            return Base64.encodeToString(encodedSignature, Base64.DEFAULT);
        }

        private SecretKey getSecretFromFile() {

            String KeyStr = readFromFile(SECRETPATH);
            byte[] KeyBytes = Base64.decode(KeyStr, Base64.DEFAULT);
            SecretKey Key = new SecretKeySpec(KeyBytes, "AES");
            return Key;
        }

        public String readFromFile(String path) {
            BufferedReader br = null;
            String everything = null;
            try {
                br = new BufferedReader(new FileReader(path));
                StringBuilder sb = new StringBuilder();
                String line = br.readLine();

                while (line != null) {
                    sb.append(line);
                    line = br.readLine();
                }
                everything = sb.toString();
                br.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return everything;
        }

        public SecretSignature invoke(String toCipher)
                throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidParameterSpecException, InvalidKeyException, UnsupportedEncodingException {

            SecretKey secret = getSecretFromFile();
            //ciphering important data (cipher the hash)
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secret);
            encodedSignature = cipher.doFinal(getHash(toCipher.getBytes("UTF-8")));

            return this;
        }

        private byte[] getHash(byte[] data)
                throws NoSuchAlgorithmException {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(data);
            return md.digest();
        }
    }
}