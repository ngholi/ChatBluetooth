package linh.chatbluetooth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by sev_user on 8/6/2016.
 */
public class ChatFragment extends Fragment {
    private ListView listView;
    private EditText content;
    private ImageButton send;
    private IChatFragment iChatFragment;
    private ChatAdapter arrayAdapter;
    private ArrayList<Message> messages;
    private MessageDatabase db;
    private BroadcastReceiver receiver;


    private LinearLayout iconList;
    private ImageView iconShow;
    private int stateIconShow=0;

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.chat_fragment, container, false);
        init(view);

        for(final Map.Entry<String,Integer> item : ChatAdapter.icon.entrySet()){
            ImageView temp = new ImageView(getContext());
            temp.setImageResource(item.getValue());
            temp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String t = item.getKey()+" ";
                    content.append(t);
                    iconList.setVisibility(View.GONE);
                    stateIconShow=0;
                }
            });
            temp.setPadding(16,0,16,0);
            iconList.addView(temp);
        }
        iconList.setVisibility(View.GONE);
        iconShow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(stateIconShow==0){
                    iconList.setVisibility(View.VISIBLE);
                    iconList.setAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.bottom_up));
                    stateIconShow=1;
                }else{
                    Animation anim = AnimationUtils.loadAnimation(getContext(), R.anim.top_down);
                    iconList.setAnimation(anim);
                    iconList.setVisibility(View.GONE);
                    stateIconShow=0;
                }
            }
        });





        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = content.getText().toString();
                if(msg.length()>0) {
                    Message message = new Message(msg, true);
                    messages.add(message);
                    arrayAdapter.notifyDataSetChanged();
                    Log.i("ChatFragment", "Out: " + msg);
                    listView.setSelection(arrayAdapter.getCount()-1);
                    content.setText("");
                    iChatFragment.sendMessage(msg);
                }
            }
        });

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals(ChatService.BROADCAST_GET_DEVICE_INFO)){
                    String name = intent.getStringExtra(ChatService.EXTRA_DEVICE_NAME);
                    String addr = intent.getStringExtra(ChatService.EXTRA_ADDRESS);
                    getActivity().setTitle(name);
                    if(messages.size()==0) {
                        messages = db.readMessageFromAddress(addr);
                        arrayAdapter = new ChatAdapter(getContext(), messages);
                        listView.setAdapter(arrayAdapter);
                        if(arrayAdapter.getCount()>0){
                            listView.setSelection(arrayAdapter.getCount()-1);
                        }
                    }
                }
            }
        };

        return view;
    }

    private void init(View root){
        db = new MessageDatabase(getContext());
        listView = (ListView)root.findViewById(R.id.chat);
        content = (EditText)root.findViewById(R.id.chat_content);
        send = (ImageButton) root.findViewById(R.id.send);
        iChatFragment = (IChatFragment)getActivity();
        messages = new ArrayList<>();

        iconList= (LinearLayout)root.findViewById(R.id.listIcon);
        iconShow = (ImageView)root.findViewById(R.id.iconShow);
    }

    public void onReceiveMessage(String msg){
        Message message = new Message(msg, false);
        messages.add(message);
        arrayAdapter.notifyDataSetChanged();
        Log.i("ChatFragment", "In: " + msg);
        listView.setSelection(arrayAdapter.getCount()-1);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(receiver, new IntentFilter(ChatService.BROADCAST_GET_DEVICE_INFO));
        Intent intent = new Intent(getContext(), ChatService.class);
        intent.setAction(ChatService.ACTION_GET_DEVICE_INFO);
        getActivity().startService(intent);
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().unregisterReceiver(receiver);
    }

    public interface IChatFragment{
        void sendMessage(String msg);
    }

}