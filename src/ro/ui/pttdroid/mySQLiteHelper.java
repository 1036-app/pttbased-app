package ro.ui.pttdroid;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.TextView;

public  class mySQLiteHelper extends SQLiteOpenHelper  
{

	public mySQLiteHelper(Context context, String name, CursorFactory factory,
			int version) 
	{
		super(context, name, factory, version);
		
	}

	@Override
	public void onCreate(SQLiteDatabase db) //�������ݿ��Ĳ���
	{	
	String CREATE_TABLE="create table information( "+"ip,"+"time,"+"content)";
	db.execSQL(CREATE_TABLE);	
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)//�汾���µĲ���
	{
		db.execSQL("ALTER TABLE person ADD salary");
	}
	
	/**
	 * ���ݿ�Ĳ������
	 */
	public void insertData(SQLiteDatabase db,String ip,String time,String content)
	{
		ContentValues values = new ContentValues(); 
		values.put("ip", ip); 
		values.put("time", time); 
		values.put("content", content); 
		db.insert("information", null, values); 
	}
	/**
	 * ɾ�����ݱ��е�����
	 */
	public void deleteData(SQLiteDatabase db) 
	{ 
	  String sql = "DELETE FROM information"; 
	  db.execSQL(sql ); 
	  //SqlDB.execSQL("drop table information");
	} 
	/**
	 * �������ݱ��е��������ݣ�����ʾ��MessageActivity.
	 * Cursor��Ϊһ��ָ������ݿ��ѯ���ؽ����
	 */
	public void queryData(SQLiteDatabase db,TextView textTotal) 
	{
	  String ipAddress="";
	  String oldTime="";
	  String oldContent="";
	  String sql = "SELECT * FROM information";
	  Cursor cursor = db.rawQuery(sql,null); 
	  cursor.moveToFirst(); 
	  while (!cursor.isAfterLast()) 
	  { 
		ipAddress=cursor.getString(0); 
	    oldTime=cursor.getString(1); 
        oldContent=cursor.getString(2);
        textTotal.append(ipAddress+"     "+oldTime+"\n");
        textTotal.append(oldContent +"\n"); 
        cursor.moveToNext(); 
      } 
	  cursor.close();
	  
	}
}
