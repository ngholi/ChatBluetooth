package linh.chatbluetooth;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

public class HomeActivity extends AppCompatActivity implements DeviceListFragment.iDeviceListFragment,
        ChatFragment.IChatFragment, AcceptConnectDialogFragment.iDialog, CloseConnectFragment.ICloseFragment{
    public static final String ACTION_OPEN_CHAT = "action.i.define.open.chat";

    private DeviceListFragment mDeviceListFragment;
    private ChatFragment mChatFragment;
    private BluetoothAdapter mBluetoothAdapter;
    private BroadcastReceiver receiver;
    private ProgressDialog progressDialog;
    private AcceptConnectDialogFragment mAcceptDialog;
    private HistoryFragment mHistoryFragment;
    private boolean inConversation;
    private CloseConnectFragment mCloseConnectFragment;

    private final int REQUEST_ENABLE_BT = 1;


    private final int REQUEST_COARSE_LOCATION = 1;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                switch (action) {
                    case BluetoothDevice.ACTION_FOUND:
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        Log.i("FoundDevice", "Device name: " + device.getName());
                        if(mDeviceListFragment!=null)
                            mDeviceListFragment.add(device);
                        break;
                    case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                        if(mDeviceListFragment!=null)
                            mDeviceListFragment.setButtonAction(DeviceListFragment.ACTION_STOP_DISCOVERY);
                        break;
                    case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                        if(mDeviceListFragment!=null)
                            mDeviceListFragment.setButtonAction(DeviceListFragment.ACTION_DISCOVERY);
                        break;
                    case BluetoothAdapter.ACTION_STATE_CHANGED:
                        int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                        if (state == BluetoothAdapter.STATE_OFF) {
                            if(mDeviceListFragment!=null)
                                mDeviceListFragment.setButtonAction(DeviceListFragment.ACTION_TURN_ON_BT);
                        }
                        else if(state == BluetoothAdapter.STATE_ON){
                            Intent intent1 = new Intent(HomeActivity.this, ChatService.class);
                            intent.setAction(ChatService.ACTION_START);
                            startService(intent1);
                        }
                        break;
                    case ChatService.BROADCAST_RECEIVE_MESSAGE:
                        String message = intent.getStringExtra(ChatService.EXTRA_MESSAGE);
                        if(mChatFragment!=null)
                            mChatFragment.onReceiveMessage(message);
                        if(mDeviceListFragment!=null)
                            mDeviceListFragment.newMessageIncoming(intent.getStringExtra(ChatService.EXTRA_DEVICE_NAME), intent.getStringExtra(ChatService.EXTRA_ADDRESS), message);
                        break;
                    case ChatService.BROADCAST_DISCONNECTED:
                        inConversation = false;
                        Log.i("HomeActivity", "Disconnected");
                        if (mChatFragment != null && mChatFragment.isVisible()) {
                            HomeActivity.super.onBackPressed();
                        } else {
                            if (progressDialog != null)
                                progressDialog.dismiss();
                        }
                        break;
                    case ChatService.BROADCAST_UNABLE_TO_CONNECT:
                        Toast.makeText(HomeActivity.this, getString(R.string.unable_to_connect), Toast.LENGTH_LONG).show();
                        if(progressDialog!=null)
                            progressDialog.dismiss();
                        break;
                    case ChatService.BROADCAST_WAITING_FOR_ACCEPT:
                        if(progressDialog!=null)
                            progressDialog.setMessage(getString(R.string.waiting_for_accept));
                        break;
                    case ChatService.BROADCAST_OTHER_DEVICE_CONNECT:
                        openAcceptFragment(intent.getStringExtra(ChatService.EXTRA_DEVICE_NAME));
                        break;
                    case ChatService.BROADCAST_CONNECT_REQUEST_ACCEPT:
                        if(progressDialog!=null)
                            progressDialog.dismiss();
                        break;
                    case ChatService.BROADCAST_CONVERSATION_START:
                        if(progressDialog!=null)
                            progressDialog.dismiss();
                        inConversation = true;
                        openChatFragment();
                        break;
                    case ChatService.BROADCAST_STATE:
                        int serviceState = intent.getIntExtra(ChatService.EXTRA_STATE, 0);
                        if(serviceState==ChatService.STATE_CONNECTING){
                            if((progressDialog==null || !progressDialog.isShowing()) && (mChatFragment==null || !mChatFragment.isVisible()))
                                progressDialog = ProgressDialog.show(HomeActivity.this, "", getString(R.string.connecting));
                        }
                        else if(serviceState==ChatService.STATE_IN_CONVERSATION){
                            inConversation = true;
                            if(savedInstanceState==null)
                                openChatFragment();
                        }
                        else if(serviceState==ChatService.STATE_REQUEST_CONNECT){
                            String name = intent.getStringExtra(ChatService.EXTRA_DEVICE_NAME);
                            openAcceptFragment(name);
                        }
                        else if(serviceState==ChatService.STATE_WAITING_ACCEPT){
                            if((progressDialog==null || !progressDialog.isShowing()) && (mChatFragment==null || !mChatFragment.isVisible()))
                                progressDialog = ProgressDialog.show(HomeActivity.this, "", getString(R.string.waiting_for_accept));
                        }
                        break;
                }
            }
        };

        mBluetoothAdapter = ((BluetoothManager)getSystemService(BLUETOOTH_SERVICE)).getAdapter();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        String action = getIntent().getAction();
        if(action!=null && action.equals(ACTION_OPEN_CHAT)){
            mChatFragment = (ChatFragment) getSupportFragmentManager().findFragmentByTag("chat");
            if(mChatFragment == null) {
                mChatFragment = new ChatFragment();
                transaction.add(R.id.container, mChatFragment, "chat").commit();
            }
            Intent intent = new Intent(this, ChatService.class);
            intent.setAction(ChatService.ACTION_OPENED_CHAT);
            startService(intent);
        }
        else {
            mDeviceListFragment = (DeviceListFragment)getSupportFragmentManager().findFragmentByTag("deviceList");
            if(mDeviceListFragment==null) {
                mDeviceListFragment = new DeviceListFragment();
                transaction.add(R.id.container, mDeviceListFragment, "deviceList").commit();
                Intent intent = new Intent(this, ChatService.class);
                intent.setAction(ChatService.ACTION_START);
                startService(intent);
            }
        }
    }

    private void openAcceptFragment(String deviceName){
        mAcceptDialog = (AcceptConnectDialogFragment) getFragmentManager().findFragmentByTag("dialog");
        if(mAcceptDialog==null) {
            mAcceptDialog = new AcceptConnectDialogFragment();
            Bundle nameDevice = new Bundle();
            nameDevice.putString("device", deviceName);
            mAcceptDialog.setArguments(nameDevice);
            mAcceptDialog.show(getFragmentManager(), "dialog");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id){
            case android.R.id.home:
                Log.i("HomeAsBack", "Pressed");
                onBackPressed();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean BTisOn() {
        return mBluetoothAdapter.enable();
    }

    @Override
    public boolean isDiscovering() {
        return mBluetoothAdapter.isDiscovering();
    }

    @Override
    public void discovery() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_COARSE_LOCATION);
        }
        else {
            if(mBluetoothAdapter.isDiscovering()) return;
            mDeviceListFragment.clearData();
            mBluetoothAdapter.startDiscovery();
        }
    }

    @Override
    public void turnOnBT() {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(intent, REQUEST_ENABLE_BT);
    }

    @Override
    public void stopDiscovery() {
        mBluetoothAdapter.cancelDiscovery();
    }

    @Override
    public void connect(BluetoothDevice device) {
        if(!BTisOn()) return;
        Intent intent = new Intent(this, ChatService.class);
        intent.setAction(ChatService.ACTION_CONNECT);
        intent.putExtra(ChatService.EXTRA_ADDRESS, device.getAddress());
        startService(intent);
        progressDialog = ProgressDialog.show(this, "", getString(R.string.connecting));
    }

    @Override
    public void openHistory(Message message) {
        if(mHistoryFragment==null || !mHistoryFragment.isVisible()) {
            mHistoryFragment = new HistoryFragment();
            Bundle bundle = new Bundle();
            bundle.putString("address", message.getAddrDevice());
            bundle.putString("name", message.getNameDevice());
            mHistoryFragment.setArguments(bundle);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.setCustomAnimations(R.anim.slide_in, R.anim.slide_out, R.anim.pop_slide_in, R.anim.pop_slide_out);
            transaction.replace(R.id.container, mHistoryFragment);
            transaction.addToBackStack(null);
            transaction.commit();
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean inConversation() {
        return inConversation;
    }

    @Override
    public void showCloseConnectFragment() {
        if(mCloseConnectFragment==null) {
            mCloseConnectFragment = new CloseConnectFragment();
            mCloseConnectFragment.show(getFragmentManager(), "dialogDisconnect");
        }
        else if(!mCloseConnectFragment.isVisible()){
            mCloseConnectFragment.show(getFragmentManager(), "dialogDisconnect");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==REQUEST_COARSE_LOCATION && grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
            discovery();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(ChatService.BROADCAST_CONNECT_REQUEST_ACCEPT);
        filter.addAction(ChatService.BROADCAST_CONNECTED);
        filter.addAction(ChatService.BROADCAST_DISCONNECTED);
        filter.addAction(ChatService.BROADCAST_OTHER_DEVICE_CONNECT);
        filter.addAction(ChatService.BROADCAST_RECEIVE_MESSAGE);
        filter.addAction(ChatService.BROADCAST_UNABLE_TO_CONNECT);
        filter.addAction(ChatService.BROADCAST_WAITING_FOR_ACCEPT);
        filter.addAction(ChatService.BROADCAST_CONVERSATION_START);
        filter.addAction(ChatService.BROADCAST_STATE);
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(receiver);
    }

    @Override
    public void sendMessage(String msg) {
        Intent intent = new Intent(this, ChatService.class);
        intent.setAction(ChatService.ACTION_SEND_MESSAGE);
        intent.putExtra(ChatService.EXTRA_MESSAGE, msg);
        startService(intent);
    }

    @Override
    public void sendAcceptMessage() {
        Intent intent = new Intent(this, ChatService.class);
        intent.setAction(ChatService.ACTION_SEND_ACCEPT_STRING);
        startService(intent);
    }

    public void openChatFragment(){
        if(mChatFragment==null || !mChatFragment.isVisible()) {
            mChatFragment = new ChatFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.setCustomAnimations(R.anim.slide_in, R.anim.slide_out, R.anim.pop_slide_in, R.anim.pop_slide_out);
            transaction.replace(R.id.container, mChatFragment);
            transaction.addToBackStack(null);
            transaction.commit();
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void refuseConnect() {
        Intent intent = new Intent(this, ChatService.class);
        intent.setAction(ChatService.ACTION_REFUSE_CONNECT);
        startService(intent);
    }

    @Override
    public void disconnect() {
        Intent intent = new Intent(this, ChatService.class);
        intent.setAction(ChatService.ACTION_DISCONNECT);
        startService(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}