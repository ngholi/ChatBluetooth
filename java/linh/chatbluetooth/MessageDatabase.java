package linh.chatbluetooth;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by sev_user on 8/8/2016.
 */
public class MessageDatabase {
    final messageDbHelper _messageDbHelper;
    Context context;

    String name;
    ArrayList<Message> readList;



    public MessageDatabase(Context context){
        this.readList = new ArrayList<Message>();
        this.context = context;
        this._messageDbHelper = new messageDbHelper(context);
    }

    public static abstract class messageEntry implements BaseColumns {
        public static final String TABLE_NAME = "message";
        public static final String _ID = "id";
        public static final String CONTENT_MESSAGE = "content";
        public static final String ADDR_DEVICE = "addrDevice";
        public static final String NAME_DEVICE = "nameDevice";
        public static final String TIME_MESSAGE = "timeMessage";
        public static final String IS_SENDER ="isSender";
    }

    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE "+messageEntry.TABLE_NAME+" ("+
                    messageEntry._ID+" INTEGER PRIMARY KEY AUTOINCREMENT,"+
                    messageEntry.CONTENT_MESSAGE+TEXT_TYPE+COMMA_SEP+
                    messageEntry.ADDR_DEVICE+TEXT_TYPE+COMMA_SEP+
                    messageEntry.NAME_DEVICE+TEXT_TYPE+COMMA_SEP+
                    messageEntry.TIME_MESSAGE+TEXT_TYPE+COMMA_SEP+
                    messageEntry.IS_SENDER+" INT"+")";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS "+messageEntry.TABLE_NAME;

    public class messageDbHelper extends SQLiteOpenHelper {

        public static final int DATABASE_VERSION =1;
        public static final String DATABASE_NAME = "message.db";


        public messageDbHelper(Context context) {
            super(context,DATABASE_NAME,null,DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            sqLiteDatabase.execSQL(SQL_CREATE_ENTRIES);
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
            sqLiteDatabase.execSQL(SQL_DELETE_ENTRIES);
            onCreate(sqLiteDatabase);
        }
    }


    public void insert(Message newMessage){
        SQLiteDatabase db = _messageDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(messageEntry.CONTENT_MESSAGE,newMessage.getContent());
        values.put(messageEntry.ADDR_DEVICE, newMessage.getAddrDevice());
        values.put(messageEntry.NAME_DEVICE, newMessage.getNameDevice());
        values.put(messageEntry.TIME_MESSAGE,newMessage.getTime());
        values.put(messageEntry.IS_SENDER,newMessage.isSender());
        db.insert(messageEntry.TABLE_NAME,null,values);
        db.close();
    }

    public ArrayList<Message> readMessageFromAddress(String addr){
        ArrayList<Message> listMessage = new ArrayList<>();
        SQLiteDatabase db= _messageDbHelper.getReadableDatabase();
        String[] projection=new String[]{messageEntry.CONTENT_MESSAGE,
                messageEntry.ADDR_DEVICE,
                messageEntry.NAME_DEVICE,
                messageEntry.TIME_MESSAGE,
                messageEntry.IS_SENDER,
                messageEntry._ID};
        String orderBy=messageEntry._ID+" ASC";
        Cursor c = db.query(messageEntry.TABLE_NAME,
                projection,
                messageEntry.ADDR_DEVICE+"=?",
                new String[]{addr},
                null,
                null,
                orderBy);
        c.moveToFirst();
        String result="";
        if(c.getCount() > 0){
            while(!c.isAfterLast()){
                Log.e("@#$%^","OK");
                listMessage.add(new Message(c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getInt(4)==1));
                c.moveToNext();
            }
        }
        this.name = result;
        c.close();
        db.close();
        return listMessage;
    }

    public ArrayList<Message> getLastMessageOfAll(){
        SQLiteDatabase db = _messageDbHelper.getReadableDatabase();
        ArrayList<Message> result = new ArrayList<>();
        ArrayList<String> addr = new ArrayList<>();
        Cursor cursor = db.query(true, messageEntry.TABLE_NAME, new String[] {messageEntry.ADDR_DEVICE},
                null, null, null, null, null, null);
        if(cursor.moveToFirst()){
            while (!cursor.isAfterLast()){
                addr.add(cursor.getString(0));
                cursor.moveToNext();
            }
        }
        cursor.close();
        for(String address : addr){
            Cursor c = db.query(messageEntry.TABLE_NAME, null, messageEntry.ADDR_DEVICE + "=?",
                    new String[] {address}, null, null, messageEntry._ID + " DESC", "1");
            c.moveToFirst();
            int i;
            Message msg = new Message(c);
            c.close();
            for(i=0; i<result.size(); i++){
                if(msg.getID() > result.get(i).getID())
                    break;
            }
            result.add(i, msg);
        }
        db.close();
        return  result;
    }


}