package linh.chatbluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;

/**
 * Created by sev_user on 8/6/2016.
 */
public class DeviceListFragment extends Fragment {
    private ListView listView;
    private BlueAdapter arrayAdapter;
    private ArrayList<Object> data;
    private iDeviceListFragment iFragment;
    private Button btnDiscovery;
    private Button btnDiscoverable;
    private MessageDatabase db;

    public static final int ACTION_TURN_ON_BT = 1;
    public static final int ACTION_DISCOVERY = 2;
    public static final int ACTION_STOP_DISCOVERY = 3;

    private final String STRING_TURN_ON_BT = "Mở Bluetooth";
    private final String STRING_DISCOVERY = "Tìm thiết bị";
    private final String STRING_STOP_DISCOVERY = "Dừng tìm";


    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.device_list_fragment, container, false);
        init(view);

        getActivity().setTitle("Device List");
        ((AppCompatActivity)getActivity()).getSupportActionBar().setHomeButtonEnabled(false);
        ((AppCompatActivity)getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object obj = data.get(position);
                if(obj instanceof BluetoothDevice) {
                    if(iFragment.inConversation()){
                        iFragment.showCloseConnectFragment();
                    }
                    else {
                        iFragment.connect((BluetoothDevice) data.get(position));
                    }
                }
                else if(obj instanceof Message){
                    iFragment.openHistory((Message)obj);
                }
            }
        });

        btnDiscoverable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                startActivity(intent);
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        Log.i("DeviceListFragment", "onResume: called");
        super.onResume();
        if(!iFragment.BTisOn()){
            setButtonAction(ACTION_TURN_ON_BT);
        }
        else{
            if(iFragment.isDiscovering())
                setButtonAction(ACTION_STOP_DISCOVERY);
            else
                setButtonAction(ACTION_DISCOVERY);
        }
    }

    private void init(View root){
        db = new MessageDatabase(getContext());
        listView = (ListView)root.findViewById(R.id.list_device);
        data = new ArrayList<>();
        arrayAdapter = new BlueAdapter(getContext(), data);
        listView.setAdapter(arrayAdapter);
        ArrayList<Message> list = db.getLastMessageOfAll();
        if(list.size() > 0) {
            arrayAdapter.addItem(getActivity().getString(R.string.recent));
            arrayAdapter.addItems(list);
        }
        arrayAdapter.addItem(getActivity().getString(R.string.available));

        iFragment = (iDeviceListFragment)getActivity();
        btnDiscovery = (Button)root.findViewById(R.id.btnDiscovery);
        btnDiscoverable = (Button) root.findViewById(R.id.btnDiscoverable);
    }

    public void setButtonAction(int action){
        switch (action){
            case ACTION_TURN_ON_BT:
                btnDiscovery.setText(STRING_TURN_ON_BT);
                btnDiscovery.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        iFragment.turnOnBT();
                    }
                });
                break;
            case ACTION_DISCOVERY:
                btnDiscovery.setText(STRING_DISCOVERY);
                btnDiscovery.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        iFragment.discovery();
                    }
                });
                break;
            case ACTION_STOP_DISCOVERY:
                btnDiscovery.setText(STRING_STOP_DISCOVERY);
                btnDiscovery.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        iFragment.stopDiscovery();
                    }
                });
                break;
        }
    }

    public void clearData(){
        arrayAdapter.clear();
    }

    public void add(BluetoothDevice device){
        int size = (data==null)? 0:data.size();
        for(int i=size-1; i>=0; i--){
            if(data.get(i) instanceof BluetoothDevice){
                if(device.getAddress().equals(((BluetoothDevice)data.get(i)).getAddress()))
                    return;
            }else
                break;
        }
        arrayAdapter.addItem(device);
    }

    public void newMessageIncoming(String name, String addr, String content){
        if(arrayAdapter!=null)
            arrayAdapter.newMessageIncoming(addr, name, content);
    }

    public interface iDeviceListFragment{
        boolean BTisOn();
        boolean isDiscovering();
        void discovery();
        void turnOnBT();
        void stopDiscovery();
        void connect(BluetoothDevice device);
        void openHistory(Message message);
        boolean inConversation();
        void showCloseConnectFragment();
    }
}