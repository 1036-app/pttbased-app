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
	public static String SelectedPlace="";
	
	public mySQLiteHelper(Context context, String name, CursorFactory factory,
			int version) 
	{
		super(context, name, factory, version);
		
	}

	@Override
	public void onCreate(SQLiteDatabase db) //创建数据库后的操作
	{	
	String CREATE_TABLE="create table information( "+"ip,"+"time,"+"content)";
	db.execSQL(CREATE_TABLE);	
	String TABLE="create table AudioData( "+"filename ,"+"filepath,"+"place)";
	db.execSQL(TABLE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)//版本更新的操作
	{
		db.execSQL("ALTER TABLE person ADD salary");
	}
	
	/**
	 * 数据库的插入操作（信息）
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
	 * 删除数据表中的数据
	 */
	public void deleteData(SQLiteDatabase db) 
	{ 
	  String sql = "DELETE FROM information"; 
	  db.execSQL(sql ); 
	  //SqlDB.execSQL("drop table information");
	} 
	/**
	 * 查找数据表中的所有数据，并显示到MessageActivity.
	 * Cursor作为一个指针从数据库查询返回结果集
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
		 *查找数据表中音频数据，并显示到SearchAudioFiles.
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
	 *根据音频文件名查找文件路径.
	 */
  public void selectAudioData(SQLiteDatabase db,String fname) 
  {
    String sql = "SELECT * FROM AudioData where filename='"+fname+"'";
    Cursor cursor = db.rawQuery(sql,null);
	if(cursor.moveToFirst())
	{
		SelectedFilePath=cursor.getString(1);
		SelectedPlace=cursor.getString(2);
    
	}
	else
		System.out.println("找不到指定的文件");
    cursor.close(); 
   
  }
  /**
	 * 数据库的插入操作（音频）
	 */
	public void inserAudiotData(SQLiteDatabase db,String Filename,String Filepath,String Place)
	{
		//String sql = "INSERT INTO AudioData (filename,data) VALUES ('" + Filename + "', " +"'" + Data + "')";
		//db.execSQL(sql ); 
		ContentValues values = new ContentValues(); 
		values.put("Filename", Filename); 
		values.put("Filepath", Filepath); 
		values.put("Place", Place); 
		db.insert("AudioData", null, values); 
	}
	/**
	 * 删除数据表中的数据
	 */
	public void deleteAudioData(SQLiteDatabase db) 
	{ 
	  String sql = "DELETE FROM AudioData"; 
	  db.execSQL(sql ); 
	  //SqlDB.execSQL("drop table information");
	} 
}
