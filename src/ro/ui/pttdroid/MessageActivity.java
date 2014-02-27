package ro.ui.pttdroid;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
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
	public static SendMessage sendMessage;
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
		sendMessage=new SendMessage();
		sendMessage.start();
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
		sendMessage.shutdown();
		mHandler.removeCallbacks(updateRecive);
		mHandler.removeCallbacks(updateSend);
		super.onPause();
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
		sendMessage.shutdown();
		mHandler.removeCallbacks(updateRecive);
		mHandler.removeCallbacks(updateSend);
    }

	class sendButtonListener implements OnClickListener 
	{

		public void onClick(View v) 
		{
			sendMessage.resumeMessage();
		}
	}
	
	
	public class SendMessage extends Thread
	{
		private DatagramPacket packet = null;
		private DatagramSocket socket = null;
		public volatile boolean sending = false;
		public volatile boolean running = true;

		@Override
		public void run() 
		{
			android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

			while (isRunning())
			{   
				if(!Main.messageList.isEmpty())
			     {
				    mHandler.post(updateRecive);
				    
			     }
				if (isSending())
				{
					try {
						init();
						//System.out.println("发出的数据:" + packet.getData().toString());
						socket.send(packet);
						mHandler.post(updateSend);
					} catch (IOException e) 
					{
						Log.error(getClass(), e);
					}
					sending=false;
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) 
				{
					e.printStackTrace();
				}
			}
			if (socket != null)
				socket.close();

		}

		private void init() 
		{
			try {
				IP.load();
				socket = new DatagramSocket();
				InetAddress addr = null;
				switch (CommSettings.getCastType()) 
				{
				case CommSettings.BROADCAST:
					 socket.setBroadcast(true);
					 addr = CommSettings.getBroadcastAddr();
					 break;
				case CommSettings.MULTICAST:
					 addr = CommSettings.getMulticastAddr();
					 break;
				case CommSettings.UNICAST:
					 addr = CommSettings.getUnicastAddr();
					 break;
				}
				data = textMessage.getText().toString();
				SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd   HH:mm:ss");     
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
		     // packet = new DatagramPacket(messages, messages.length, addr, CommSettings.getPort());
				packet = new DatagramPacket(messages, messages.length, addr,40000);
			
			} catch (SocketException e) {
				Log.error(getClass(), e);
			}
		}

		@Override
		public void destroy()
		{
			super.destroy();
		}

		private synchronized boolean isRunning() 
		{
			return running;
		}

		private synchronized boolean isSending()
		{
			return sending;
		}

		public synchronized void pauseMessage() 
		{
			sending = false;
		}

		public synchronized void resumeMessage() 
		{
			sending = true;
		}

		public synchronized void shutdown() 
		{
			pauseMessage();
			running = false;
			if(socket!=null)
			  socket.close();
		}
	}
}
