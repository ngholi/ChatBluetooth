package linh.chatbluetooth;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.RelativeLayout;

import java.util.ArrayList;

/**
 * Created by Linh on 10/08/2016.
 */
public class HistoryFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        getActivity().setTitle(bundle.getString("name"));
        View view = inflater.inflate(R.layout.chat_fragment, container, false);
        MessageDatabase db = new MessageDatabase(getContext());
        ListView listView = (ListView) view.findViewById(R.id.chat);
        RelativeLayout layout = (RelativeLayout) view.findViewById(R.id.panel);
        layout.setVisibility(View.GONE);
        String addr = bundle.getString("address");
        ArrayList<Message> messages = db.readMessageFromAddress(addr);
        ChatAdapter adapter = new ChatAdapter(getContext(), messages);
        listView.setAdapter(adapter);
        if(adapter.getCount()>0){
            listView.setSelection(adapter.getCount()-1);
        }

        return view;
    }
}