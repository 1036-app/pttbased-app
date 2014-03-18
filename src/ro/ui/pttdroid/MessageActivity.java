package ro.ui.pttdroid;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import ro.ui.pttdroid.Main.MyHandler;
import ro.ui.pttdroid.ReciveMessage.ReciveBinder;
import ro.ui.pttdroid.settings.CommSettings;
import ro.ui.pttdroid.util.IP;
import ro.ui.pttdroid.util.Log;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MessageActivity extends Activity 
{

	public static Intent reciveIntent;
	public Button sendButton = null;
	public TextView textIP = null;
	public TextView textTotal = null;
	public EditText textMessage = null;
	public String data = null;
	public String dataEnd = null;
	public byte[] messages = null;
	public Runnable updateRecive=null;
	public Runnable updateSend=null;
    public String time=null;
	public TransportData sendData=new TransportData();
	public Intent intent=null;
	private myApplication mAPP = null;   
    private MyHandler mHandler = null;
    public  Socket TCPsocket=null;
    public TCPSendMessage tsm=null;
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		intent=getIntent();  //实现当前Activity的和Main之间的通信
		mAPP = (myApplication) getApplication();  // 获得该共享变量实例                           
        mHandler = mAPP.getHandler();
		super.onCreate(savedInstanceState);
		setContentView(R.layout.send);
		sendButton = (Button) findViewById(R.id.sendButton);
		textIP = (TextView) findViewById(R.id.textIP);
		textTotal = (TextView) findViewById(R.id.textTotal);
		textMessage = (EditText) findViewById(R.id.textMessage);
		sendButton.setOnClickListener(new sendButtonListener());// 为按钮添加监听器
		textTotal.setMovementMethod(ScrollingMovementMethod.getInstance());
		textTotal.setScrollbarFadingEnabled(false);  
		//显示以前的聊天消息读数据库,包括回到Main时保存的信息
		Main.mySqlHelper.queryData(Main.SqlDB, textTotal);
		Main.messageList.clear();
	    tsm=new TCPSendMessage();
		tsm.start();
		updateSend =new Runnable()
		{
			
			public void run()
			{ 
			    textTotal.append("me:     "+time+"\n");  
			    textTotal.append(data +"\n"); 
			    //插入数据库
			    Main.mySqlHelper.insertData(Main.SqlDB,"me",time,data);
			}
			
		};
		//聊天过程中及时收到的消息
		updateRecive =new Runnable()
		{
			public void run()
			{
				for(int i=0;i<Main.messageList.size();i++)
				{
					textTotal.append(Main.messageList.get(i).ipaddress+"     "+Main.messageList.get(i).time+"\n");
					textTotal.append(Main.messageList.get(i).data +"\n");
				}
				Main.messageList.clear();
		
			}
			
		};
		
	}
	@Override
	protected void onResume()
	{
		super.onResume();
	}
	@Override
	protected void onPause()
	{	
		super.onPause();
	}

	@Override
	protected void onDestroy()
	{
		tsm.shutdown();
		mHandler.removeCallbacks(updateRecive);
		mHandler.removeCallbacks(updateSend);
		super.onDestroy();
	}
	/**
	 * 为程序设置菜单，菜单具体内容来自res/menu/menu.xml文件
	 */
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.submenu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		Intent i;                                       // 当用户点击菜单里面的内容时程序的响应
		switch (item.getItemId()) 
		{
		case R.id.Return:
			Destroy();
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	protected void Destroy()
	{
		tsm.shutdown();
		mHandler.removeCallbacks(updateRecive);
		mHandler.removeCallbacks(updateSend);
    }
	 public byte[] handleData()
	 {
		data = textMessage.getText().toString();
		SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd   HH:mm:ss:SSS");     
		Date curDate=new Date(System.currentTimeMillis());
		time=formatter.format(curDate); 
		sendData.data=data;
		sendData.time=time;
	    sendData.ipaddress="me";

	try {  
            ByteArrayOutputStream baos = new ByteArrayOutputStream();  
            ObjectOutputStream oos = new ObjectOutputStream(baos);  
            oos.writeObject(sendData);
            messages = baos.toByteArray();  
            baos.close();  
            oos.close(); 
           
        }  
        catch(Exception e) 
        {   
            e.printStackTrace();  
        }   
	   return messages;
	}
	class sendButtonListener implements OnClickListener 
	{

		public void onClick(View v) 
		{ 
			
			if(CommSettings.getCastType()==CommSettings.BROADCAST&&Main.allIP.size()==0)
			{
			    Toast.makeText(MessageActivity.this, "你的广播列表中没有IP地址", Toast.LENGTH_SHORT).show();
			    byte[] sendData=null;
			    sendData=handleData();
			    mHandler.post(updateSend);
			}
			else
			   tsm.resumeSend();
		}
	}
	public class TCPSendMessage extends Thread
	{
		public volatile boolean running = true;
		public volatile boolean sending = false;
		@Override
		public void run()
		{
          while(running==true)
          {
			if(!Main.messageList.isEmpty())
		     {
				System.out.println("收到信息后更新界面");
			    mHandler.post(updateRecive);   
		     }
          
          if(sending==true)
          {
			if(CommSettings.getCastType()==CommSettings.UNICAST)
			{
			InetAddress dstaddr = CommSettings.getUnicastAddr();
			byte[] sendData=null;
			sendData=handleData();
			mHandler.post(updateSend);
			TCPsendData(dstaddr,sendData);
			
			}	
			else if(CommSettings.getCastType()==CommSettings.BROADCAST)
			{
				byte[] sendData=null;
				sendData=handleData();
				mHandler.post(updateSend);
				System.out.println("广播开始发送");
				System.out.println("总共有IP：    "+Main.allIP.size());
				for(int i=0;i<Main.allIP.size();i++)
				{
				  System.out.println("广播发送第"+(i+1)+"个文件");
				  String ip=null;
				  ip=Main.allIP.get(i);
				  ip=ip.substring(1);
				  InetAddress address=null;
				   try {
					address = InetAddress.getByName(ip);
				   } catch (UnknownHostException e) 
				   {
					e.printStackTrace();
				   }
				   TCPsendData(address,sendData);
				}
				System.out.println("广播发送完毕");
				System.out.println("   ");
			}
			else if(CommSettings.getCastType()==CommSettings.MULTICAST)
			{	
				handleData();
				//组播发送信息
				mHandler.post(updateSend);
			
			}
			sending=false;
          }
         }
		}
		public void resumeSend()
		{
			sending=true;
		}
		public void shutdown()
		{
			running=false;
		}
		public void TCPsendData(InetAddress dstaddr,byte[]messages)
		{
			try {	
				System.out.println("发送数据");
				TCPsocket = new Socket(dstaddr, Main.MessagePort);	
				OutputStream out = TCPsocket.getOutputStream(); // 输出流
				out.write(messages);
				out.flush(); // 关闭输出流
			   TCPsocket.close();	
			} catch (IOException e1) 
			{
				e1.printStackTrace();
			}

		}
	}
}
