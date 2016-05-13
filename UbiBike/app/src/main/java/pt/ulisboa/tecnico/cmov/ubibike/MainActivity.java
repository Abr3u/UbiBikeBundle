package pt.ulisboa.tecnico.cmov.ubibike;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocketServer;

public final class MainActivity extends NetworkHandlerActivity {

    UbiBikeApplication mUbiBikeApplication;
    boolean pastUpdated, historyClicked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LogEnter(MainActivity.class.getSimpleName(), "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        enableButtons(false);

        historyClicked = false;
        mUbiBikeApplication = (UbiBikeApplication) getApplicationContext();
        pastUpdated = false;
        //Set handler
        mNetworkHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                try {
                    NetworkMessageCode networkMessageCode = NetworkMessageCode.values()[message.what];
                    switch (networkMessageCode) {
                        case GET_USER_PAST_TRAJECTORIES:
                            finishGetUserPastTrajectories(message);
                            return;
                        case RECONNECT_NETWORK:
                            onDataConnected();
                            return;
                        case SEND_POINTS:
                            finishSendPoints(message);
                            return;
                        case CHECK_POINTS:
                            finishCheckPoints(message);
                            break;
                        case ERROR:
                            //Print error
                            TextView errorView = (TextView) findViewById(R.id.errorView);
                            errorView.setText(R.string.technicalFailure);
                            break;
                        case EXIT:
                            exitApp(MainActivity.this, (String) message.obj);
                            return;
                        default:
                            exitApp(MainActivity.this, "Invalid network message code " + networkMessageCode.toString() + ".");
                            break;
                    }

                    //Enable buttons
                    enableButtons(true);
                } catch (JSONException | InvalidReplyException exception) {
                    exitApp(MainActivity.this, exception.getMessage() + ".");
                }
            }
        };

        //Change activity layout if offline;
        onNetworkChange();

        ((TextView) findViewById(R.id.editPoints))
                .setText(String.format("%d", mUbiBikeApplication.getCurrentScore()));
        enableButtons(true);

        //start pick up and drop off services
        bindService(
                new Intent(MainActivity.this, UbiBikeService.class),
                wifiServiceConn,
                Context.BIND_AUTO_CREATE);

        LogExit(MainActivity.class.getSimpleName(), "onCreate");
    }

    @Override
    protected void onResume() {
        LogEnter(MainActivity.class.getSimpleName(), "onResume");
        super.onResume();
        LogExit(MainActivity.class.getSimpleName(), "onResume");
    }

    @Override
    protected void onStop() {
        LogEnter(MainActivity.class.getSimpleName(), "onStop");
        super.onStop();
        LogExit(MainActivity.class.getSimpleName(), "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(wifiServiceConn);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        LogEnter(MainActivity.class.getSimpleName(), "onActivityResult");
        // Check which request we're responding to
        ActivityMessageCode activityMessageCode = ActivityMessageCode.values()[requestCode];
        switch (activityMessageCode) {
            case BOOK_BIKE:
                // Make sure the request was successful
                if (resultCode == RESULT_OK) {
                    mUbiBikeApplication.setBeaconId(data.getStringExtra(IntentKey.BEACON_ID.toString()));
                    mUbiBikeApplication.setLastStationStation(data.getStringExtra(IntentKey.STATION_NAME.toString()));
                }
                break;
            case SEND_POINTS_CODE:
                if (resultCode == RESULT_OK) {
                    try {
                        String jsonString = data.getStringExtra(IntentKey.TRADED_POINT_JSON.toString());
                        JSONObject json = new JSONObject(jsonString);

                        if (networkIsAvailable()) {
                            new Thread(new ClientThread(
                                    UserRequestType.SEND_POINTS,
                                    mNetworkHandler,
                                    NetworkMessageCode.SEND_POINTS,
                                    json)).start();
                        } else {
                            wifiService.addNewTradedPoints(json);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                break;
            default:
                exitApp(MainActivity.this, "Invalid activity message code " + activityMessageCode.toString());
                break;
        }
        LogExit(MainActivity.class.getSimpleName(), "onActivityResult");
    }

    private void finishSendPoints(Message message) throws JSONException, InvalidReplyException {
        JSONObject reply = (JSONObject) message.obj;
        UserReplyType userReplyType = UserReplyType.values()[getIntFromJson(reply, UserJsonKey.REPLY_TYPE)];

        switch (userReplyType) {
            case SUCCESS:
                int newPoints = reply.getInt(UserJsonKey.USER_CURRENT_SCORE.toString());
                updatePoints(newPoints);
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Points Sent!!")
                        .setMessage("You now have " + newPoints + " points")
                        .setNeutralButton("Dismiss", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .show();
                break;
            case TECHNICAL_FAILURE:
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Send Points")
                        .setMessage("Couldn't send points successfully")
                        .setNeutralButton("Dismiss", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .show();
                //exitApp(MainActivity.this, "Technical Failure.");
                break;
            case INVALID_ENUM:
                exitApp(MainActivity.this, "Invalid request.");
                break;
            default:
                exitApp(MainActivity.this, "Invalid user reply type.");
                break;
        }

    }

    private void finishCheckPoints(Message message) throws InvalidReplyException, JSONException {
        JSONObject reply = (JSONObject) message.obj;
        UserReplyType userReplyType = UserReplyType.values()[getIntFromJson(reply, UserJsonKey.REPLY_TYPE)];
        pointsToCheck = false;

        switch (userReplyType) {
            case SUCCESS:
                TextView current = (TextView) findViewById(R.id.editPoints);
                current.setBackgroundColor(Color.parseColor("white"));
                break;
            case POINTS_MISSMATCH:
                Toast.makeText(MainActivity.this, "Your points are not yet consistent with the Server", Toast.LENGTH_LONG).show();
                break;
            case TECHNICAL_FAILURE:
                exitApp(MainActivity.this, "Technical Failure.");
                break;
            case INVALID_ENUM:
                exitApp(MainActivity.this, "Invalid request.");
                break;
            default:
                exitApp(MainActivity.this, "Invalid user reply type.");
                break;
        }
    }

    private void finishGetUserPastTrajectories(Message message)
            throws JSONException, InvalidReplyException {
        LogEnter(MainActivity.class.getSimpleName(), "finishGetUserPastTrajectories");
        //Get JSON elements
        JSONObject reply = (JSONObject) message.obj;
        UserReplyType userReplyType = UserReplyType.values()[getIntFromJson(reply, UserJsonKey.REPLY_TYPE)];

        switch (userReplyType) {
            case SUCCESS:
                //Verify JSON elements
                checkJsonKeys(
                        reply,
                        UserJsonKey.REPLY_TYPE,
                        UserJsonKey.USER);

                JSONObject user = getObjectFromJson(reply, UserJsonKey.USER);


                //Add past trajectories into intent if exists
                if (user.has(UserJsonKey.USER_PAST_TRAJECTORIES.toString())) {
                    wifiService.addToPastTrajectories(getArrayFromJson(user, UserJsonKey.USER_PAST_TRAJECTORIES));
                }
                pastUpdated = true;

                if(historyClicked) {
                    startActivity(new Intent(MainActivity.this, HistoryActivity.class)
                            .putExtra(
                                    IntentKey.MOST_RECENT_TRAJECTORY.toString(),
                                    wifiService.getMostRecentTrajectory().toString())
                            .putExtra(
                                    IntentKey.PAST_TRAJECTORIES.toString(),
                                    wifiService.getPastTrajectories().toString()));
                } else {
                    startActivity(
                            new Intent(MainActivity.this, BookBikeActivity.class)
                                    .putExtra(IntentKey.USERNAME.toString(), mUbiBikeApplication.getUsername())
                                    .putExtra(IntentKey.STATION_NAME.toString(), mUbiBikeApplication.getLastStationStation())
                                    .putExtra(IntentKey.STATIONS.toString(), mUbiBikeApplication.getStations().toString()));
                }

                break;
            case INVALID_USERNAME:
                exitApp(MainActivity.this, "Invalid Username.");
                break;
            case TECHNICAL_FAILURE:
                exitApp(MainActivity.this, "Technical Failure.");
                break;
            case INVALID_ENUM:
                exitApp(MainActivity.this, "Invalid request.");
                break;
            default:
                exitApp(MainActivity.this, "Invalid user reply type.");
                break;
        }
        LogExit(MainActivity.class.getSimpleName(), "finishGetUserPastTrajectories");
    }

    public void bookBikeClicked(View view) {
        LogEnter(MainActivity.class.getSimpleName(), "bookBikeClicked");
        enableButtons(false);

        historyClicked = false;

        if(pastUpdated) {
            startActivity(
                    new Intent(MainActivity.this, BookBikeActivity.class)
                            .putExtra(IntentKey.USERNAME.toString(), mUbiBikeApplication.getUsername())
                            .putExtra(IntentKey.STATION_NAME.toString(), mUbiBikeApplication.getLastStationStation())
                            .putExtra(IntentKey.STATIONS.toString(), mUbiBikeApplication.getStations().toString()));
        } else {
            updatePastTrajectories();
        }
        LogExit(MainActivity.class.getSimpleName(), "bookBikeClicked");
    }

    private void updatePastTrajectories() {
        UserRequestType userRequestType = UserRequestType.GET_USER_PAST_TRAJECTORIES;;
        NetworkMessageCode networkMessageCode = NetworkMessageCode.GET_USER_PAST_TRAJECTORIES;

        try {
            //Creating request message
            JSONObject request = new JSONObject()
                    .put(UserJsonKey.REQUEST_TYPE.toString(), userRequestType.ordinal())
                    .put(UserJsonKey.USERNAME.toString(), mUbiBikeApplication.getUsername());

            //Start get stations thread
            new Thread(new ClientThread(
                    userRequestType,
                    mNetworkHandler,
                    networkMessageCode,
                    request)).start();
        } catch (JSONException exception) {
            exitApp(MainActivity.this, exception.getMessage() + ".");
        }
    }

    public void messagesClicked(View view) {
        LogEnter(MainActivity.class.getSimpleName(), "messagesClicked");
        enableButtons(false);
        TextView points = (TextView) findViewById(R.id.editPoints);

        Intent i = new Intent(MainActivity.this, PeopleNearNewMessageActivity.class);
        i.putExtra(IntentKey.USERNAME.toString(), mUbiBikeApplication.getUsername());
        i.putExtra(IntentKey.CURRENT_POINTS.toString(), points.getText().toString());
        i.putExtra(IntentKey.SERVICE.toString(), wifiBinder);

        if (wifiService.getMsgHistory().size() > 0) {
            i.putExtra(IntentKey.MSG_HISTORY.toString(), wifiService.getMsgHistory());
        }
        startActivity(i);
        LogExit(MainActivity.class.getSimpleName(), "messagesClicked");
    }

    public void historyClicked(View view) {
        LogEnter(MainActivity.class.getSimpleName(), "historyClicked");
        enableButtons(false);

        if(!wifiService.hasMostRecentTrajectory()){
            Toast.makeText(MainActivity.this, "You don't have any trajectories", Toast.LENGTH_SHORT).show();
        }

        historyClicked = true;

        if(pastUpdated) {
            startActivity(new Intent(MainActivity.this, HistoryActivity.class)
                    .putExtra(
                            IntentKey.MOST_RECENT_TRAJECTORY.toString(),
                            wifiService.getMostRecentTrajectory().toString())
                    .putExtra(
                            IntentKey.PAST_TRAJECTORIES.toString(),
                            wifiService.getPastTrajectories().toString()));
        } else {
            updatePastTrajectories();
        }

        LogExit(MainActivity.class.getSimpleName(), "historyClicked");
    }

    public void sendPointsClicked(View view) {
        LogEnter(MainActivity.class.getSimpleName(), "sendPointsClicked");

        enableButtons(false);

        TextView points = (TextView) findViewById(R.id.editPoints);

        Intent i = new Intent(MainActivity.this, PeopleNearSendPointsActivity.class);
        i.putExtra(IntentKey.CURRENT_POINTS.toString(), points.getText().toString());
        i.putExtra(IntentKey.USERNAME.toString(), mUbiBikeApplication.getUsername());
        i.putExtra(IntentKey.PASSWORD.toString(), mUbiBikeApplication.getPassword());
        i.putExtra(IntentKey.SERVICE.toString(), wifiBinder);
        startActivityForResult(i, ActivityMessageCode.SEND_POINTS_CODE.ordinal());
        LogExit(MainActivity.class.getSimpleName(), "sendPointsClicked");
    }

    public void showMostRecentTrajectoryButtonClicked(View view) {
        LogEnter(MainActivity.class.getSimpleName(), "showMostRecentTrajectoryButtonClicked");

        if (wifiService.getMostRecentTrajectory() == null) {
            Toast.makeText(MainActivity.this, "You have no Past Trajectories", Toast.LENGTH_LONG).show();
        } else {

            startActivity(new Intent(MainActivity.this, TrajectoryInformationActivity.class)
                    .putExtra(IntentKey.TRAJECTORY.toString(),
                            wifiService.getMostRecentTrajectory().toString()));
        }
        LogExit(MainActivity.class.getSimpleName(), "showMostRecentTrajectoryButtonClicked");
    }

    public void logoutClicked(View view) {
        LogEnter(MainActivity.class.getSimpleName(), "logoutClicked");
        enableButtons(false);

        //Show logout alert dialog
        new AlertDialog.Builder(view.getContext())
                .setTitle("Logout")
                .setMessage("Do you want to logout?")
                .setPositiveButton("Yes",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                //Return to login
                                startActivity(
                                        new Intent(MainActivity.this, LoginActivity.class)
                                                .putExtra(IntentKey.LOGIN_MESSAGE.toString(), "Thank you. Come again."));
                                finish();
                            }
                        })
                .setNegativeButton("No",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                .show();

        enableButtons(true);
        LogExit(MainActivity.class.getSimpleName(), "logoutClicked");
    }

    @Override
    protected void enableButtons(boolean option) {
        findViewById(R.id.BookBikeButton).setEnabled(option);
        findViewById(R.id.HistoryButton).setEnabled(option);
    }

    /*
    *
    * WIFI direct stuff
    *
     */

    private boolean pointsToCheck = false;

    public boolean getPointsToCheck() {
        return pointsToCheck;
    }

    private UbiBikeService wifiService = null;
    private UbiBikeService.WifiBinder wifiBinder = null;
    private ServiceConnection wifiServiceConn = new ServiceConnection() {
        // callbacks for service binding, passed to bindService()

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            LogEnter(ServiceConnection.class.getSimpleName(), "onServiceConnected");
            wifiBinder = (UbiBikeService.WifiBinder) service;
            wifiService = wifiBinder.getService();

            wifiService.setMainActivity(MainActivity.this);

            if (mUbiBikeApplication.getLastTrajectory() != null) {
                wifiService.setMostRecentTrajectory(mUbiBikeApplication.getLastTrajectory());
            }

            SimWifiP2pSocketServer mSrvSocket = null;
            try {
                mSrvSocket = new SimWifiP2pSocketServer(
                        Integer.parseInt(getString(R.string.port)));
            } catch (IOException e) {
                e.printStackTrace();
            }
            wifiService.setSrvSocket(mSrvSocket);
            wifiService.setUsername(mUbiBikeApplication.getUsername());

            wifiService.startIncomingTask();

            LogExit(ServiceConnection.class.getSimpleName(), "onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            LogEnter(ServiceConnection.class.getSimpleName(), "onServiceDisconnected");
            wifiService.stopWifiDirectService();
            wifiService = null;
            wifiBinder = null;
            LogExit(ServiceConnection.class.getSimpleName(), "onServiceDisconnected");
        }
    };

    public void updatePointsInc(int received, boolean fromOtherPeer) {
        TextView current = (TextView) findViewById(R.id.editPoints);
        Integer newpoints = Integer.parseInt(current.getText().toString()) + (received);
        mUbiBikeApplication.setCurrentScore(newpoints);
        current.setText("" + newpoints);
        if (fromOtherPeer) {
            current.setBackgroundColor(Color.parseColor("red"));
            pointsToCheck = true;
            if (networkIsAvailable()) {
                try {
                    Thread.sleep(5000);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
                startCheckPointsThread();
            }
        }
    }

    public void updatePoints(int newpoints) {
        TextView current = (TextView) findViewById(R.id.editPoints);
        current.setText("" + newpoints);
    }

    public void showAlertPointsReceived() {
        TextView current = (TextView) findViewById(R.id.editPoints);
        String points = current.getText().toString();
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Points Received!!")
                .setMessage("You now have "+points+" points.\nKeep in mind that the points you receive may be later retrived if some technical difficulties arise\n")
                .setNeutralButton("Dismiss", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    public void showAlertMsgReceived(String msg) {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Message Received!!")
                .setMessage(msg)
                .setNeutralButton("Dismiss", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    public void startCheckPointsThread() {
        //Creating request message
        UserRequestType userRequestType = UserRequestType.CHECK_POINTS;
        JSONObject request = null;
        try {
            request = new JSONObject()
                    .put(UserJsonKey.REQUEST_TYPE.toString(), userRequestType.ordinal())
                    .put(UserJsonKey.USERNAME.toString(), mUbiBikeApplication.getUsername())
                    .put(UserJsonKey.USER_CURRENT_SCORE.toString(), mUbiBikeApplication.getCurrentScore());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ClientThread clientThread = new ClientThread(
                userRequestType,
                mNetworkHandler,
                NetworkMessageCode.CHECK_POINTS,
                request);
        new Thread(clientThread).start();

    }
}