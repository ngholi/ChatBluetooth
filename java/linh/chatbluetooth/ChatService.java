package linh.chatbluetooth;

import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.text.format.Time;
import android.util.Log;

/**
 * Created by sev_user on 8/10/2016.
 */
public class ChatService extends Service {
    public static final String ACTION_START = "action.i.define.start";
    public static final String ACTION_STOP = "action.i.define.stop";
    public static final String ACTION_CONNECT = "action.i.define.connect";
    public static final String ACTION_SEND_MESSAGE = "action.i.define.send.message";
    public static final String ACTION_REFUSE_CONNECT = "action.i.define.refuse.connect";
    public static final String ACTION_SEND_ACCEPT_STRING = "action.i.define.send.accept";
    public static final String ACTION_GET_DEVICE_INFO = "action.i.define.device.info";
    public static final String ACTION_DISCONNECT = "action.i.define.disconnect";
    public static final String ACTION_OPENED_CHAT = "action.i.define.opened.chat";

    public static final String BROADCAST_RECEIVE_MESSAGE = "broadcast.i.define.receive.message";
    public static final String BROADCAST_DISCONNECTED = "broadcast.i.define.disconnected";
    public static final String BROADCAST_UNABLE_TO_CONNECT = "broadcast.i.define.unable.to.connect";
    public static final String BROADCAST_WAITING_FOR_ACCEPT = "broadcast.i.define.waiting.for.accept";
    public static final String BROADCAST_OTHER_DEVICE_CONNECT = "broadcast.i.define.other.device.connect";
    public static final String BROADCAST_CONNECT_REQUEST_ACCEPT = "broadcast.i.define.connect.request.accept";
    public static final String BROADCAST_CONNECTED = "broadcast.i.define.connected";
    public static final String BROADCAST_CONVERSATION_START = "broadcast.i.define.conversation.start";
    public static final String BROADCAST_GET_DEVICE_INFO = "broadcast.i.define.device.info";
    public static final String BROADCAST_STATE = "broadcast.i.define.state";

    public static final String EXTRA_MESSAGE = "incoming message";
    public static final String EXTRA_ADDRESS = "address";
    public static final String EXTRA_DEVICE_NAME = "device_name";
    public static final String EXTRA_STATE = "state";

    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_WAITING_ACCEPT = 3;
    public static final int STATE_REQUEST_CONNECT = 4;
    public static final int STATE_IN_CONVERSATION = 5;

    private final String ACCEPT_STRING = "S9Ajgja3Kgn!*#KNA)";
    private final String MAINTAIN_STRING = "gjdGi_gu-e-#)%Gu_(UG";

    private BluetoothConnection mBluetoothConnect;
    private Handler mHandler;
    private BluetoothAdapter mBluetoothAdapter;
    private NotificationManager mManager;
    private int notifyID;
    private NotificationCompat.Builder mBuider;
    private boolean sendBroadcast;
    private MessageDatabase db;
    private boolean started;
    private int state;
    private BroadcastReceiver receiver;
    private long lastMessageTime = 0;
    private long lastMaintain;
    private boolean maintain;
    private MainTainConnection mainTainConnection;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onCreate() {
        super.onCreate();

        final Uri alarm = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        mBluetoothAdapter = ((BluetoothManager)getSystemService(BLUETOOTH_SERVICE)).getAdapter();
        sendBroadcast = true;
        mHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what){
                    case BluetoothConnection.RECEIVE_MESSAGE:
                        byte[] readBuf = (byte[]) msg.obj;
                        String message = new String(readBuf, 0, msg.arg1);
                        if(message.equals(ACCEPT_STRING)){
                            mHandler.obtainMessage(BluetoothConnection.CONNECT_REQUEST_ACCEPTED).sendToTarget();
                        }
                        else if(message.equals(MAINTAIN_STRING)){
                            lastMaintain = System.currentTimeMillis();
                        }
                        else {
                            if(sendBroadcast){
                                Intent intent = new Intent(BROADCAST_RECEIVE_MESSAGE);
                                intent.putExtra(EXTRA_MESSAGE, message);
                                intent.putExtra(EXTRA_DEVICE_NAME, mBluetoothConnect.getBluetoothDevice().getName());
                                intent.putExtra(EXTRA_ADDRESS, mBluetoothConnect.getBluetoothDevice().getAddress());
                                sendBroadcast(intent);
                            }
                            BluetoothDevice device = mBluetoothConnect.getBluetoothDevice();
                            db.insert(new linh.chatbluetooth.Message(message, device.getAddress(), device.getName(), Time.getCurrentTimezone(), false));
                            long now = System.currentTimeMillis();
                            if(now - lastMessageTime > 1000 * 20){
                                mBuider.setSound(alarm);
                            }
                            lastMessageTime = now;
                            if(mBuider!=null) {
                                mBuider.setContentText(message).setContentTitle(mBluetoothConnect.getBluetoothDevice().getName());
                                mManager.notify(notifyID, mBuider.build());
                                mBuider.setSound(null);
                            }
                        }
                        break;
                    case BluetoothConnection.CONNECTED:
                        maintain = true;
                        if(mainTainConnection==null || !mainTainConnection.isAlive()){
                            mainTainConnection = new MainTainConnection();
                            mainTainConnection.start();
                        }
                        if(sendBroadcast){
                            Intent intent = new Intent(BROADCAST_CONNECTED);
                            sendBroadcast(intent);
                        }
                        break;
                    case BluetoothConnection.DISCONNECTED:
                        maintain = false;
                        state = STATE_LISTEN;
                        Intent openMain = new Intent(ChatService.this, HomeActivity.class);
                        openMain.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        PendingIntent pendingIntent = PendingIntent.getActivity(ChatService.this, 0, openMain, PendingIntent.FLAG_UPDATE_CURRENT);
                        if(mBuider!=null) {
                            mBuider.setContentIntent(pendingIntent);
                            mBuider.setContentText(getResources().getString(R.string.waiting_connect)).setContentTitle(getResources().getString(R.string.app_name));
                            mManager.notify(notifyID, mBuider.build());
                        }
                        if(sendBroadcast){
                            Intent intent = new Intent(BROADCAST_DISCONNECTED);
                            sendBroadcast(intent);
                        }
                        mBluetoothConnect.listen();
                        break;
                    case BluetoothConnection.UNABLE_TO_CONNECT:
                        state = STATE_LISTEN;
                        if(sendBroadcast){
                            Intent intent = new Intent(BROADCAST_UNABLE_TO_CONNECT);
                            sendBroadcast(intent);
                        }
                        break;
                    case BluetoothConnection.WAITING_FOR_ACCEPT:
                        state = STATE_WAITING_ACCEPT;
                        if (sendBroadcast) {
                            Intent intent = new Intent(BROADCAST_WAITING_FOR_ACCEPT);
                            sendBroadcast(intent);
                        }
                        if(mBuider!=null) {
                            mBuider.setContentText(getResources().getString(R.string.connecting));
                            mManager.notify(notifyID, mBuider.build());
                        }
                        break;
                    case BluetoothConnection.OTHER_DEVICE_CONNECTED:
                        state = STATE_REQUEST_CONNECT;
                        if(sendBroadcast){
                            Intent intent = new Intent(BROADCAST_OTHER_DEVICE_CONNECT);
                            intent.putExtra(EXTRA_DEVICE_NAME, (String) msg.obj);
                            sendBroadcast(intent);
                        }
                        if(mBuider!=null) {
                            mBuider.setSound(alarm);
                            mBuider.setContentText(getResources().getString(R.string.friend_connect_question));
                            mManager.notify(notifyID, mBuider.build());
                            mBuider.setSound(null);
                        }
                        break;
                    case BluetoothConnection.CONNECT_REQUEST_ACCEPTED:
                        mHandler.obtainMessage(BluetoothConnection.CONVERSATION_START).sendToTarget();
                        if(sendBroadcast){
                            Intent intent = new Intent(BROADCAST_CONNECT_REQUEST_ACCEPT);
                            sendBroadcast(intent);
                        }
                        break;
                    case BluetoothConnection.CONVERSATION_START:
                        if(state!=STATE_LISTEN && mBluetoothConnect.getState()==BluetoothConnection.STATE_CONNECTED) {
                            state = STATE_IN_CONVERSATION;
                            Intent intent1 = new Intent(ChatService.this, HomeActivity.class);
                            intent1.setAction(HomeActivity.ACTION_OPEN_CHAT);
                            intent1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            PendingIntent pendingIntent1 = PendingIntent.getActivity(ChatService.this, 0, intent1, 0);
                            if(mBuider!=null) {
                                mBuider.setContentIntent(pendingIntent1).setContentText(getResources().getString(R.string.open_to_chat));
                                mManager.notify(notifyID, mBuider.build());
                            }
                            if (sendBroadcast) {
                                Intent intent = new Intent(BROADCAST_CONVERSATION_START);
                                intent.putExtra(EXTRA_ADDRESS, mBluetoothConnect.getBluetoothDevice().getAddress());
                                intent.putExtra(EXTRA_DEVICE_NAME, mBluetoothConnect.getBluetoothDevice().getName());
                                sendBroadcast(intent);
                            }
                        }
                        break;
                }
            }
        };
        mBluetoothConnect = new BluetoothConnection(mBluetoothAdapter, mHandler);
        mManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notifyID = 1;
        db = new MessageDatabase(getBaseContext());
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)){
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                    if(state == BluetoothAdapter.STATE_ON) {
                        serviceConstruct();
                    }
                    else if(state == BluetoothAdapter.STATE_OFF) {
                        started = false;
                        stopSelf();
                    }
                }
            }
        };
        registerReceiver(receiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    public void serviceConstruct(){
        started = true;
        Intent openMain = new Intent(this, HomeActivity.class);
        openMain.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, openMain, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuider = new NotificationCompat.Builder(this);
        mBuider.setSmallIcon(R.drawable.chat).setContentTitle(getResources().getString(R.string.app_name)).setContentText(getResources().getString(R.string.your_app_running));
        mBuider.setContentIntent(pendingIntent);
        mBluetoothConnect.listen();
        startForeground(notifyID, mBuider.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent==null){
            stopSelf();
            return super.onStartCommand(intent, flags, startId);
        }
        String action = intent.getAction();
        if(action==null) {
            if(!started)
                serviceConstruct();
            return START_STICKY;
        }
        if(action.equals(ACTION_START)){
            if(!started) {
                serviceConstruct();
            }
            else{
                Intent intent1 = new Intent(BROADCAST_STATE);
                intent1.putExtra(EXTRA_STATE, state);
                if(state == STATE_REQUEST_CONNECT){
                    intent1.putExtra(EXTRA_DEVICE_NAME, mBluetoothConnect.getBluetoothDevice().getName());
                }
                sendBroadcast(intent1);
            }
        }
        else if(action.equals(ACTION_STOP)){
            mBluetoothConnect.stop();
            stopSelf();
        }
        else if(action.equals(ACTION_CONNECT)){
            String addr = intent.getStringExtra(EXTRA_ADDRESS);
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(addr);
            state = STATE_CONNECTING;
            mBluetoothConnect.connect(device);
        }
        else if(action.equals(ACTION_SEND_MESSAGE)){
            String msg = intent.getStringExtra(EXTRA_MESSAGE);
            mBluetoothConnect.sendMessage(msg);
            BluetoothDevice device = mBluetoothConnect.getBluetoothDevice();
            if(device!=null)
                db.insert(new linh.chatbluetooth.Message(msg, device.getAddress(), device.getName(), Time.getCurrentTimezone(), true));
        }
        else if(action.equals(ACTION_REFUSE_CONNECT)){
            mBluetoothConnect.listen();
        }
        else if(action.equals(ACTION_SEND_ACCEPT_STRING)){
            mBluetoothConnect.sendMessage(ACCEPT_STRING);
            mHandler.obtainMessage(BluetoothConnection.CONVERSATION_START).sendToTarget();
        } else if (action.equals(ACTION_GET_DEVICE_INFO)) {
            if(mBluetoothConnect.getState()==BluetoothConnection.STATE_CONNECTED) {
                Intent intent1 = new Intent(BROADCAST_GET_DEVICE_INFO);
                intent1.putExtra(EXTRA_DEVICE_NAME, mBluetoothConnect.getBluetoothDevice().getName());
                intent1.putExtra(EXTRA_ADDRESS, mBluetoothConnect.getBluetoothDevice().getAddress());
                sendBroadcast(intent1);
            }
        }
        else if(action.equals(ACTION_DISCONNECT)){
            mBluetoothConnect.listen();
        }
        else if(action.equals(ACTION_OPENED_CHAT)){
            mBuider.setContentText(getResources().getString(R.string.open_to_chat));
            mManager.notify(notifyID, mBuider.build());
        }

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    private class MainTainConnection extends Thread{
        @Override
        public void run() {
            Log.i("Maintain", "Start");
            lastMaintain = System.currentTimeMillis();
            while (maintain){
                try {
                    Thread.sleep(3000);
                    mBluetoothConnect.sendMessage(MAINTAIN_STRING);
                    if(System.currentTimeMillis() - lastMaintain > 7000){
                        Log.i("Maintain", "timeout");
                        break;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            mHandler.obtainMessage(BluetoothConnection.DISCONNECTED);
            Log.i("Maintain", "End");
        }
    }
}