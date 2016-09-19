package linh.chatbluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by sev_user on 8/5/2016.
 */
public class BlueAdapter extends BaseAdapter {
    private ArrayList<Object> data;
    private Context context;
    private int[] icon = new int[100];

    private static final int TYPE_ITEM_AVALABLE = 0;
    private static final int TYPE_SEPARATOR = 1;
    private static final int TYPE_ITEM_CHATED = 2;


    private final int[] icons = {R.drawable.book, R.drawable.like, R.drawable.pencil, R.drawable.star,
            R.drawable.key, R.drawable.flag, R.drawable.giftbox};

    public BlueAdapter(Context context, ArrayList<Object> data){
        this.data = data;
        this.context = context;
    }

    public int getIcon(int position){
        Random rand = new Random();
        if(position<icon.length && position>=0) {
            if (icon[position] == 0) {
                icon[position] = icons[rand.nextInt(icons.length)];
            }
            return icon[position];
        }
        return icons[rand.nextInt(icons.length)];
    }

    public void addItem(BluetoothDevice device){
        data.add(device);
        notifyDataSetChanged();
    }

    public void addItem(Message pair){
        data.add(pair);
        notifyDataSetChanged();
    }

    public void addItem(String header){
        data.add(header);
        notifyDataSetChanged();
    }

    public void newMessageIncoming(String addr, String name, String content){
        int temp = 0;
        for(int i=0; i<data.size(); i++){
            if(data.get(i) instanceof Message){
                Message msg = (Message) data.get(i);
                if(addr.equals(msg.getAddrDevice())){
                    temp = icon[i];
                    int j;
                    for(j=i; j<data.size(); j++){
                        try {
                            icon[j] = icon[j + 1];
                        }catch (Exception e){
                            break;
                        }
                    }
                    icon[j] = 0;
                    data.remove(i);
                    break;
                }
            }
        }
        if(data.size() == 1){
            data.add(0, context.getResources().getString(R.string.recent));
            data.add(1, new Message(content, addr, name));
        }
        else{
            data.add(1, new Message(content, addr, name));
        }
        for(int i=(data.size()<=100)? data.size():100; i>= 2; i--){
            icon[i] = icon[i-1];
        }
        icon[1] = temp;
        notifyDataSetChanged();
    }

    public void addItems(ArrayList<Message> list){
        data.addAll(list);
    }

    public void clear(){
        int size = data.size();
        for(int i=size-1; i>=0; i--){
            Object obj = data.get(i);
            if(obj instanceof BluetoothDevice) {
                data.remove(i);
                icon[i] = 0;
            }
            else
                break;
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return (data==null)? 0:data.size();
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int rowType = getItemViewType(position);

        switch (rowType) {
            case TYPE_ITEM_CHATED:
                if(convertView==null){
                    convertView = LayoutInflater.from(context).inflate(R.layout.big_text_view, null);
                }
                TextView deviceName = (TextView) convertView.findViewById(R.id.textView);
                TextView content = (TextView)convertView.findViewById(R.id.textView5);
                ImageView img = (ImageView)convertView.findViewById(R.id.order);
                img.setImageResource(getIcon(position));
                Message message = (Message) data.get(position);
                if(message.getNameDevice().isEmpty()){
                    deviceName.setText(message.getAddrDevice());
                }
                else
                    deviceName.setText(message.getNameDevice());
                content.setText(message.getContent());
                break;
            case TYPE_SEPARATOR:
                if(convertView==null){
                    convertView = LayoutInflater.from(context).inflate(R.layout.device_list_header_item, null);
                }
                TextView header = (TextView)convertView.findViewById(R.id.textView4);
                header.setText(data.get(position).toString());
                break;
            case TYPE_ITEM_AVALABLE:
                if(convertView==null){
                    convertView = LayoutInflater.from(context).inflate(R.layout.device_avalable_item, null);
                }
                TextView name = (TextView)convertView.findViewById(R.id.textView6);
                BluetoothDevice device = (BluetoothDevice) data.get(position);
                if(device.getName()!= null && device.getName().isEmpty()){
                    name.setText(device.getAddress());
                }
                else
                    name.setText(device.getName());
                ImageView img1 = (ImageView)convertView.findViewById(R.id.image);
                img1.setImageResource(getIcon(position));
                break;
        }
        return convertView;
    }

    @Override
    public int getViewTypeCount() {
        return 3;
    }

    @Override
    public int getItemViewType(int position) {
        if(data.get(position) instanceof BluetoothDevice)
            return TYPE_ITEM_AVALABLE;
        else if(data.get(position) instanceof String)
            return TYPE_SEPARATOR;
        return TYPE_ITEM_CHATED;
    }

    @Override
    public boolean isEnabled(int position) {
        return getItemViewType(position)!=TYPE_SEPARATOR;
    }
}