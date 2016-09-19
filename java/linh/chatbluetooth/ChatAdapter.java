package linh.chatbluetooth;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by sev_user on 8/8/2016.
 */
public class    ChatAdapter extends BaseAdapter {
    private ArrayList<Message> data;
    private Context context;

    public static HashMap<String,Integer> icon = new HashMap<String,Integer>();

    @Override
    public boolean isEnabled(int position) {
        return false;
    }

    static {
        icon.put(":!",R.drawable.emotionredicon);
        icon.put(":@",R.drawable.emotionahicon);
        icon.put(":#",R.drawable.emotionconfuseicon);
        icon.put(":$",R.drawable.emotionevilgrinicon);
        icon.put(":%",R.drawable.emotionexcitingicon);
        icon.put(":^",R.drawable.emotiongrinicon);
        icon.put(":&",R.drawable.emotionhappyicon);
        icon.put(":*",R.drawable.emotionsmileicon);
        icon.put(":(",R.drawable.emotionbadeggicon);
    }

    public ChatAdapter(Context context, ArrayList<Message> messages){
        this.context = context;
        this.data = messages;
    }

    @Override
    public int getCount() {
        return (data!=null)? data.size():0;
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
        if(convertView==null){
            convertView = LayoutInflater.from(context).inflate(R.layout.message_item, null);
        }
        TextView txt = (TextView) convertView.findViewById(R.id.message_content);
        TextView myTxt = (TextView)convertView.findViewById(R.id.message_my_content);
        Message msg = data.get(position);
        float scale = context.getResources().getDisplayMetrics().density;
        int dpAsPixels = (int) (10*scale + 0.5f);
        int dp16AsPixels = (int) (16*scale + 0.5f);
        int dp1AsPixels = (int) (scale + 0.5f);
        convertView.setPadding(0,dp1AsPixels,0,0);
        if(msg.isSender()){
            myTxt.setText(getTextWithIcon(msg.getContent()));
            myTxt.setVisibility(View.VISIBLE);
            txt.setVisibility(View.GONE);
            switch (checkType(position)){
                case TYPE_ALONE:
                    if(position!=0)
                        convertView.setPadding(0, dp16AsPixels, 0, 0);
                    myTxt.setBackgroundResource(R.drawable.my_rounded_corner);
                    break;
                case TYPE_TOP:
                    if(position!=0)
                        convertView.setPadding(0, dp16AsPixels, 0, 0);
                    myTxt.setBackgroundResource(R.drawable.my_rounded_corner_top);
                    break;
                case TYPE_CENTER:
                    myTxt.setBackgroundResource(R.drawable.my_rounded_corner_center);
                    break;
                case TYPE_BOTTOM:
                    myTxt.setBackgroundResource(R.drawable.my_rounded_corner_bottom);
                    break;
            }
            myTxt.setPadding(dpAsPixels, dpAsPixels, dpAsPixels, dpAsPixels);
        }
        else {
            txt.setText(getTextWithIcon(msg.getContent()));
            txt.setVisibility(View.VISIBLE);
            myTxt.setVisibility(View.GONE);
            switch (checkType(position)){
                case TYPE_ALONE:
                    if(position!=0)
                        convertView.setPadding(0, dp16AsPixels, 0, 0);
                    txt.setBackgroundResource(R.drawable.rounded_corner);
                    break;
                case TYPE_TOP:
                    if(position!=0)
                        convertView.setPadding(0, dp16AsPixels, 0, 0);
                    txt.setBackgroundResource(R.drawable.rounded_corner_top);
                    break;
                case TYPE_CENTER:
                    txt.setBackgroundResource(R.drawable.rounded_corner_center);
                    break;
                case TYPE_BOTTOM:
                    txt.setBackgroundResource(R.drawable.rounded_corner_bottom);
                    break;
            }
            txt.setPadding(dpAsPixels, dpAsPixels, dpAsPixels, dpAsPixels);
        }

        return convertView;
    }



    public Spannable getTextWithIcon(String text) {
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        if (icon.size() > 0) {
            int index;
            for (index = 0; index < builder.length(); index++) {
                if (Character.toString(builder.charAt(index)).equals(":")) {
                    for (Map.Entry<String, Integer> entry : icon.entrySet()) {
                        int length = entry.getKey().length();
                        if (index + length > builder.length())
                            continue;
                        if (builder.subSequence(index, index + length).toString().equals(entry.getKey())) {
                            builder.setSpan(new ImageSpan(context, entry.getValue()), index, index + length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            index += length - 1;
                            break;
                        }
                    }
                }
            }
        }
        return builder;
    }

    private final int TYPE_ALONE = 0;
    private final int TYPE_TOP = 1;
    private final int TYPE_CENTER = 2;
    private final int TYPE_BOTTOM = 3;
    private int checkType(int position){
        boolean here = data.get(position).isSender();
        boolean before = (position-1 >= 0)? data.get(position-1).isSender():!here;
        boolean after = (position+1 < data.size())? data.get(position+1).isSender():!here;
        if(here!=before && here!=after)
            return TYPE_ALONE;
        else if(here != before)
            return TYPE_TOP;
        else if(here == after)
            return TYPE_CENTER;
        else
            return TYPE_BOTTOM;
    }
}