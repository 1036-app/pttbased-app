package ro.ui.pttdroid;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.TextView;

public  class mySQLiteHelper extends SQLiteOpenHelper  
{
	public static String SelectedFilePath="";
	public static String IsCoded="";
	
	
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
	String TABLE="create table AudioData( "+"filename ,"+"filepath,"+"coded)";
	db.execSQL(TABLE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)//�汾���µĲ���
	{
		db.execSQL("ALTER TABLE person ADD salary");
	}
	
	/**
	 * ���ݿ�Ĳ����������Ϣ��
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
	  /**
		 *�������ݱ�����Ƶ���ݣ�����ʾ��SearchAudioFiles.
		 */
	public ArrayList<String> queryAudioData(SQLiteDatabase db,ArrayList<String> list ) 
	{
	  String filename="";
	  String sql = "SELECT * FROM AudioData";
	  Cursor cursor = db.rawQuery(sql,null); 
	  cursor.moveToFirst(); 
	  while (!cursor.isAfterLast()) 
	  { 
		filename=cursor.getString(0); 
        list.add(filename); 
        cursor.moveToNext(); 
      } 
	  cursor.close();
	  return list;
	}
	/**
	 *������Ƶ�ļ��������ļ�·��.
	 */
  public void selectAudioData(SQLiteDatabase db,String fname) 
  {
    String sql = "SELECT * FROM AudioData where filename='"+fname+"'";
    Cursor cursor = db.rawQuery(sql,null);
	if(cursor.moveToFirst())
	{
		SelectedFilePath=cursor.getString(1);
		IsCoded=cursor.getString(2);
    
	}
	else
		System.out.println("�Ҳ���ָ�����ļ�");
    cursor.close(); 
   
  }
  /**
	 * ���ݿ�Ĳ����������Ƶ��
	 */
	public void inserAudiotData(SQLiteDatabase db,String Filename,String Filepath,String iscoded)
	{
		//String sql = "INSERT INTO AudioData (filename,data) VALUES ('" + Filename + "', " +"'" + Data + "')";
		//db.execSQL(sql ); 
		ContentValues values = new ContentValues(); 
		values.put("filename", Filename); 
		values.put("filepath", Filepath); 
		values.put("coded", iscoded); 
		db.insert("AudioData", null, values); 
	}
	/**
	 * ɾ�����ݱ��е�����
	 */
	public void deleteAudioData(SQLiteDatabase db) 
	{ 
	  String sql = "DELETE FROM AudioData"; 
	  db.execSQL(sql ); 
	  //SqlDB.execSQL("drop table information");
	} 
}
