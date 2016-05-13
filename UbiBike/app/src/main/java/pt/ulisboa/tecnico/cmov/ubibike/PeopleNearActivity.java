package pt.ulisboa.tecnico.cmov.ubibike;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocket;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

abstract class PeopleNearActivity extends ErrorHandlerActivity {

    public static final String TAG = "msgsender";
    protected String username;
    protected String currentPeer = "";
    protected String currentPeerUname = "";
    private HashMap<String, String> ips_username = new HashMap<String, String>();

    protected SimWifiP2pSocket mCliSocket = null;
    protected TextView mTextInput;
    protected TextView mTextOutput;
    protected TextView mToSend;


    protected UbiBikeService myWifiService;
    private boolean wifiBound = false;
    private ArrayList<String> msgHistory = new ArrayList<String>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // initialize the UI
        setContentView(R.layout.activity_people_near);
        mTextInput = (TextView) findViewById(R.id.receiverEditText);
        mTextOutput = (TextView) findViewById(R.id.HistoryMessages);
        mToSend = (EditText) findViewById(R.id.newMessageBody);

        username = getIntent().getStringExtra(IntentKey.USERNAME.toString());
        TextView current = (TextView) findViewById(R.id.PointsEditText);
        current.setText(getIntent().getStringExtra(IntentKey.CURRENT_POINTS.toString()));

        UbiBikeService.WifiBinder binder = (UbiBikeService.WifiBinder) getIntent().getSerializableExtra(IntentKey.SERVICE.toString());
        myWifiService = binder.getService();

        wifiBound = true;

        if (getIntent().getExtras().containsKey(IntentKey.MSG_HISTORY.toString())) {
            msgHistory = (ArrayList<String>) getIntent().getSerializableExtra(IntentKey.MSG_HISTORY.toString());
        }
        updateHistoryList(msgHistory);

        guiSetButtonListeners();
        guiUpdateDisconnectedState();
    }

    /*
     * Listeners associated to buttons
	 */

    private OnClickListener listenerUpdateButton = new OnClickListener() {
        public void onClick(View v) {
            if (wifiBound) {
                myWifiService.WDinNetwork();
            }

        }
    };

    protected OnClickListener listenerSendButton = new OnClickListener() {
        @Override
        public void onClick(View v) {
            findViewById(R.id.newMessageButtonSend).setEnabled(false);

            new SendCommTask().executeOnExecutor(
                    AsyncTask.THREAD_POOL_EXECUTOR,
                    mToSend.getText().toString());
        }
    };

    private OnClickListener listenerDisconnectButton = new OnClickListener() {
        @Override
        public void onClick(View v) {
            findViewById(R.id.idDisconnectButton).setEnabled(false);
            if (mCliSocket != null) {
                try {
                    mCliSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            mCliSocket = null;
            guiUpdateDisconnectedState();
        }
    };

    public void updateHistoryList(ArrayList<String> h) {
        msgHistory = new ArrayList<String>();
        msgHistory.addAll(h);

        mTextOutput.setText("");
        for (String s : msgHistory) {
            mTextOutput.append(s + "\n");
        }
    }

    public void updatePoints(String v) {
        //received 9 points from abreu
        String received = v.substring(9, 10);

        TextView current = (TextView) findViewById(R.id.PointsEditText);
        Integer newpoints = Integer.parseInt(current.getText().toString()) + Integer.parseInt(received);
        current.setText("" + newpoints);
    }

    public class OutgoingCommTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            mToSend.setText("Connecting...");
        }

        @Override
        protected String doInBackground(String... params) {
            try {
                mCliSocket = new SimWifiP2pSocket(params[0],
                        Integer.parseInt(getString(R.string.port)));
                currentPeer = params[0];
                currentPeerUname = ips_username.get(currentPeer);
            } catch (UnknownHostException e) {
                return "Unknown Host:" + e.getMessage();
            } catch (IOException e) {
                return "IO error:" + e.getMessage();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                guiUpdateDisconnectedState();
            } else {
                findViewById(R.id.idDisconnectButton).setEnabled(true);
                findViewById(R.id.newMessageButtonSend).setEnabled(true);
                findViewById(R.id.newMessageButtonSend).setEnabled(true);

                mTextInput.setText("Connected to " + currentPeerUname + ", send msg below");
                mTextInput.setEnabled(true);
                mToSend.setText("");
                mToSend.setEnabled(true);
            }
        }
    }

    public class SendCommTask extends AsyncTask<String, String, Void> {

        private String msgSent = "";

        @Override
        protected Void doInBackground(String... msg) {
            try {
                String aux = "From " + username + ": " + msg[0] + "\n";
                mCliSocket.getOutputStream().write(aux.getBytes());
                mCliSocket.close();
                msgSent = msg[0];
            } catch (IOException e) {
                e.printStackTrace();
            }
            mCliSocket = null;
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mTextInput.setText("");
            myWifiService.addMsgToHistory("To " + currentPeerUname + ": " + msgSent);
            updateHistoryList(myWifiService.getMsgHistory());
            guiUpdateDisconnectedState();
        }
    }

	/*
     * Helper methods for updating the interface
	 */

    private void guiSetButtonListeners() {

        findViewById(R.id.idDisconnectButton).setOnClickListener(listenerDisconnectButton);
        findViewById(R.id.newMessageButtonSend).setOnClickListener(listenerSendButton);
        findViewById(R.id.idUpdateButton).setOnClickListener(listenerUpdateButton);

        ListView listView = (ListView) findViewById(R.id.peopleNearList);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View view, int position, long arg3) {
                String peerUsername = (String) arg0.getItemAtPosition(position);
                connectToPeerByUsername(peerUsername);
            }
        });
    }

    private void connectToPeerByUsername(String peer) {
        for(String ipSaved : ips_username.keySet()){
            if(ips_username.get(ipSaved).equals(peer)){
                new OutgoingCommTask().executeOnExecutor(
                        AsyncTask.THREAD_POOL_EXECUTOR,
                        ipSaved);
                break;
            }
        }
    }


    protected void guiUpdateDisconnectedState() {

        mTextInput.setEnabled(true);
        mTextInput.setText("update and select Receiver from list");

        mToSend.setText("");
        mToSend.setEnabled(false);

        findViewById(R.id.idDisconnectButton).setEnabled(false);
        findViewById(R.id.newMessageButtonSend).setEnabled(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void inRangeChanged(String peersInRange) {
        updateIPList(peersInRange);
        updateListView();
    }

    public void inNetworkChanged(String peersInRange) {
        updateIPList(peersInRange);

        new updateUsernamesThread().executeOnExecutor(
                AsyncTask.THREAD_POOL_EXECUTOR);

        updateListView();
    }

    public void updateIPList(String peersInRange) {
        if (!peersInRange.isEmpty()) {
            String[] peers = peersInRange.split("\\r?\\n");

            for (String s : peers) {
                String[] aux = s.split("\\(");//name(98765)
                String vIP = aux[1].substring(0, aux[1].length() - 1);//remove ) from the end

                if (!vIP.startsWith("?") && !ips_username.containsKey(vIP)) {
                    ips_username.put(vIP, "username unavailable");
                }
            }
        }
    }

    public void updateListView() {
        ArrayList<String> aux = new ArrayList<String>();

        for (String s : ips_username.keySet()) {
            aux.add(ips_username.get(s));
        }

        //display username-ip in list
        ArrayAdapter<String> itemsAdapter =
                new ArrayAdapter<String>(PeopleNearActivity.this, android.R.layout.simple_list_item_1, aux);

        final ListView listView = (ListView) findViewById(R.id.peopleNearList);

        listView.setAdapter(itemsAdapter);
    }

    class updateUsernamesThread extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... msg) {
            for (String ip : ips_username.keySet()) {
                try {
                    mCliSocket = new SimWifiP2pSocket(ip,
                            Integer.parseInt(getString(R.string.port)));


                    mCliSocket.getOutputStream().write("#\n".getBytes());

                    BufferedReader sockIn = new BufferedReader(
                            new InputStreamReader(mCliSocket.getInputStream()));
                    String usernameAux = sockIn.readLine();

                    mCliSocket.close();
                    mCliSocket = null;

                    if (!ips_username.get(ip).equals(usernameAux)) {
                        ips_username.put(ip, usernameAux);
                    }
                } catch (UnknownHostException e) {
                    Log.d("Unknown Host: ", e.getMessage());
                } catch (IOException e) {
                    Log.d("IO error: ", e.getMessage());
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            updateListView();
        }
    }
}