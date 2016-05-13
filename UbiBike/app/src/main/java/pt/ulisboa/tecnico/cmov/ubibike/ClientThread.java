package pt.ulisboa.tecnico.cmov.ubibike;

import android.os.Handler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

//import pt.ulisboa.tecnico.cmov.ubibike.commons.UserJsonKey;
//import pt.ulisboa.tecnico.cmov.ubibike.commons.UserRequestType;

final class ClientThread implements Runnable {

    private static final int SERVER_PORT = 9999;
    private static final String SERVER_IP = "10.0.2.2";
    private Handler _mHandler;
    private NetworkMessageCode _networkMessageCode;
    private UserRequestType _userRequestType;
    private JSONObject _request;

    public ClientThread(UserRequestType userRequestType, Handler mHandler, NetworkMessageCode networkMessageCode, JSONObject request) {
        super();
        _userRequestType = userRequestType;
        _mHandler = mHandler;
        _networkMessageCode = networkMessageCode;
        _request = request;
    }

    @Override
    public void run() {
        try {
            //Connect socket to server
            SocketAddress socketAddress = new InetSocketAddress(SERVER_IP, SERVER_PORT);
            Socket socket = new Socket();
            socket.connect(socketAddress, Constant.SOCKET_TIMEOUT);

            //Send request
            byte[] message = _request.toString().getBytes();

            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataOutputStream.writeInt(message.length); // write length of the message
            dataOutputStream.write(message);

            //Receive response
            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

            //Read length of incoming message
            int length = dataInputStream.readInt();
            if (length <= 0) {
                throw new IOException();
            }

            //Read message
            byte[] encodedReply = new byte[length];
            dataInputStream.readFully(encodedReply, 0, encodedReply.length);

            JSONObject reply = new JSONObject(new String(encodedReply));

            //Verify if user request type key exists
            String userJsonRequest = UserJsonKey.REQUEST_TYPE.toString();
            if (!reply.has(userJsonRequest)) {
                throw new JSONException("JSON object doesn't have parameter " + userJsonRequest);
            }

            //Verify if user request type key is correct
            UserRequestType userRequestType = UserRequestType.values()[reply.getInt(userJsonRequest)];
            if (userRequestType != _userRequestType) {
                throw new JSONException("JSON parameter " + userJsonRequest + " doesn't have valid value.");
            }

            _mHandler.obtainMessage(_networkMessageCode.ordinal(), reply).sendToTarget();
        } catch (IOException | JSONException exception) {
            _mHandler.obtainMessage(NetworkMessageCode.EXIT.ordinal(), exception.getMessage() + ".").sendToTarget();
        }

    }
}

