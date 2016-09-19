package linh.chatbluetooth;

import android.database.Cursor;

/**
 * Created by sev_user on 8/8/2016.
 */
public class Message {
    private String content;
    private String addrDevice;
    private String nameDevice;
    private boolean isSender;
    private String time;
    private int ID;

    public Message(String content, boolean isSender){
        this.content = content;
        this.isSender = isSender;
    }

    public Message(Message newMessage) {
        this.content = newMessage.getContent();
        this.addrDevice=newMessage.getAddrDevice();
        this.nameDevice  = newMessage.getNameDevice();
        this.time = newMessage.getTime();
        this.isSender = newMessage.isSender();
    }
    public Message(String content,String addrDevice,String nameDevice,String time,boolean isSender){
        this.content=content;
        this.nameDevice=nameDevice;
        this.addrDevice=addrDevice;
        this.isSender = isSender;
        this.time =time;
    }

    public Message(String content, String addr, String name){
        this.content = content;
        this.addrDevice = addr;
        this.nameDevice = name;
    }

    public Message (Cursor cursor){
        this.content = cursor.getString(cursor.getColumnIndex(MessageDatabase.messageEntry.CONTENT_MESSAGE));
        this.nameDevice = cursor.getString(cursor.getColumnIndex(MessageDatabase.messageEntry.NAME_DEVICE));
        this.addrDevice = cursor.getString(cursor.getColumnIndex(MessageDatabase.messageEntry.ADDR_DEVICE));
        this.isSender = cursor.getInt(cursor.getColumnIndex(MessageDatabase.messageEntry.IS_SENDER))==1;
        this.ID = cursor.getInt(cursor.getColumnIndex(MessageDatabase.messageEntry._ID));
    }

    public String getContent(){
        return this.content;
    }
    public String getNameDevice(){
        return this.nameDevice;
    }
    public String getAddrDevice(){
        return this.addrDevice;
    }
    public String getTime(){return this.time;}
    public boolean isSender(){return  this.isSender;}
    public int getID(){
        return ID;
    }
}