package linh.chatbluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by sev_user on 8/5/2016.
 */
public class BluetoothConnection {
    private BluetoothAdapter mAdapter;
    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private Handler mHandler;
    private BluetoothSocket mSocket;

    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    private int state = STATE_NONE;

    public static final int RECEIVE_MESSAGE = 1;
    public static final int SOCKET_LISTEN_FAILED = 2;
    public static final int SOCKET_ACCEPT_FAILED = 3;
    public static final int SOCKET_CAN_NOT_CLOSE = 4;
    public static final int SOCKET_CONNECT_NOT_CREATE = 5;
    public static final int UNABLE_TO_CONNECT = 6;
    public static final int DISCONNECTED = 7;
    public static final int CONNECTED = 8;
    public static final int OTHER_DEVICE_CONNECTED = 9;
    public static final int WAITING_FOR_ACCEPT = 10;
    public static final int CONNECT_REQUEST_ACCEPTED = 11;
    public static final int CONVERSATION_START = 12;

    public BluetoothConnection(BluetoothAdapter adapter, Handler handler) {
        mAdapter = adapter;
        mHandler = handler;
    }

    public boolean BTIsOn(){
        return mAdapter.isEnabled();
    }

    public synchronized void setState(int state){
        if(state==STATE_CONNECTED) {
            mHandler.obtainMessage(CONNECTED).sendToTarget();
            connected();
        }
        Log.i("BluetoothConnection", "This state is: " + state);
        this.state = state;
    }

    public int getState(){
        return state;
    }

    public void stop(){
        if(mConnectedThread!=null){
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        if(mConnectThread!=null){
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if(mAcceptThread!=null){
            mAcceptThread.cancel();
            mAcceptThread = null;
        }
        setState(STATE_NONE);
    }

    public synchronized void listen(){
        if(!BTIsOn() || getState()==STATE_LISTEN) return;
        if(mConnectThread!=null){
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if(mConnectedThread!=null){
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        setState(STATE_LISTEN);
        if(mAcceptThread==null){
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
        else if(!mAcceptThread.isAlive()) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
    }

    public synchronized void connect(BluetoothDevice device){
        Log.i("BlueConnection.connect", "Called");
        if(!BTIsOn()) return;
        if(getState() == STATE_CONNECTING){
            if(mConnectThread!=null){
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        if(mConnectedThread!=null){
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_CONNECTING);
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        Log.i("BlueConnection.connect", "Connecting");
    }

    public synchronized void connected(){
        if(mAcceptThread!=null){
            mAcceptThread.cancel();
        }
    }

    public BluetoothDevice getBluetoothDevice(){
        if(getState()==STATE_CONNECTED){
            return mSocket.getRemoteDevice();
        }
        return null;
    }

    public void sendMessage(String msg){
        if(state==STATE_CONNECTED)
            mConnectedThread.write(msg.getBytes());
    }

    private class AcceptThread extends Thread{
        private final BluetoothServerSocket mServerSocket;

        public AcceptThread(){
            BluetoothServerSocket tmp = null;
            try{
                tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord("Insecure", UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66"));
            } catch (IOException e) {
                mHandler.obtainMessage(SOCKET_LISTEN_FAILED).sendToTarget();
                e.printStackTrace();
            }
            mServerSocket = tmp;
        }

        public void run(){
            BluetoothSocket socket = null;
            Log.i("AcceptThread", "Waiting for connection ...");
            while (state != STATE_CONNECTED) {
                try {
                    socket = mServerSocket.accept();
                } catch (IOException e) {
                    mHandler.obtainMessage(SOCKET_ACCEPT_FAILED).sendToTarget();
                    e.printStackTrace();
                    break;
                }

                if(socket!= null){
                    Log.i("AcceptThread", "A device has been connected");
                    try {

                        mServerSocket.close();
                    } catch (IOException e) {
                        mHandler.obtainMessage(SOCKET_CAN_NOT_CLOSE).sendToTarget();
                        e.printStackTrace();
                    }

                    setState(STATE_CONNECTED);
                    mConnectedThread = new ConnectedThread(socket);
                    mConnectedThread.start();
                    mHandler.obtainMessage(OTHER_DEVICE_CONNECTED, socket.getRemoteDevice().getName()).sendToTarget();
                }
            }
            Log.i("AcceptThread", "End");
        }

        public void cancel() {
            try {
                mServerSocket.close();
            } catch (IOException e) {
                mHandler.obtainMessage(SOCKET_CAN_NOT_CLOSE).sendToTarget();
                e.printStackTrace();
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createInsecureRfcommSocketToServiceRecord(UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66"));
            } catch (IOException e) {
                mHandler.obtainMessage(SOCKET_CONNECT_NOT_CREATE).sendToTarget();
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i("ConnectThread", "Running");
            // Cancel discovery because it will slow down the connection
            mAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                mHandler.obtainMessage(UNABLE_TO_CONNECT).sendToTarget();
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    mHandler.obtainMessage(SOCKET_CAN_NOT_CLOSE).sendToTarget();
                }
                return;
            }

            // Do work to manage the connection (in a separate thread)
            setState(STATE_CONNECTED);
            mConnectedThread = new ConnectedThread(mmSocket);
            mConnectedThread.start();
            mHandler.obtainMessage(WAITING_FOR_ACCEPT).sendToTarget();
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class ConnectedThread extends Thread{
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket){
            mmSocket = socket;
            mSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run(){
            Log.i("ConnectedThread", "Run is running ...");
            byte[] buffer = new byte[1024];
            int bytes;
            while (state == STATE_CONNECTED){
                try {
                    bytes = mmInStream.read(buffer);
                    //receiveMessage.onReceive(new String(buffer, 0, bytes));
                    mHandler.obtainMessage(RECEIVE_MESSAGE, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
            mSocket = null;
            mHandler.obtainMessage(DISCONNECTED).sendToTarget();
            Log.i("ConnectedThread", "Listening ended");
        }

        public void write(byte[] buffer){
            try {
                mmOutStream.write(buffer);
                Log.i("Out: ", new String(buffer));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void cancel(){
            try {
                mmSocket.close();
            } catch (IOException e) {
                mHandler.obtainMessage(SOCKET_CAN_NOT_CLOSE).sendToTarget();
                e.printStackTrace();
            }
        }
    }
}