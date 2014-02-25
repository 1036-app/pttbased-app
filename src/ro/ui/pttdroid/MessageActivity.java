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
	public ReciveBinder recivebinder = null;
	public ServiceConnection conn = null;
	public Runnable updateRecive=null;
	public Runnable updateSend=null;
	
	public Runnable updateWarning=null;
	public Recieve recieve = null;
	public boolean rec = false;
	public String receivedIP = "";
    public Handler updateHandler; 
    public String time=null;
	public TransportData sendData=new TransportData();
	public TransportData receivedData =null;
	public Intent intent=null;
	private myApplication mAPP = null;   
    private MyHandler mHandler = null;
    private SQLiteDatabase SqlDB;
    private mySQLiteHelper mySqlHelper;
	private void init() 
	{
		
		sendMessage = new SendMessage();
		sendMessage.start();
		recieve = new Recieve();
		recieve.start();
		reciveIntent = new Intent(MessageActivity.this, ReciveMessage.class);
		conn = new ServiceConnection() 
		{
			                                                // ��������ʱ���õĺ���
			public void onServiceConnected(ComponentName name,IBinder reciveBinder) 
			{

				recivebinder = (ReciveBinder) reciveBinder; // ��ȡ����˵�reciveBinder���Ա�������ͨ��
				recieve.resumRecive();                      // ���Ѵ�����յ���Ϣ���߳�
			}
                                                           // �Ͽ�����ʱ���õĺ���
			public void onServiceDisconnected(ComponentName arg0) 
			{
				recivebinder = null;
				recieve.pauseRecive();
			}
		};
		getApplicationContext().bindService(reciveIntent, conn,Context.BIND_AUTO_CREATE);
		                                                    // �󶨷��񣬴���һ�����ڴ��ڵ�����
		  //�������ݿ�message.db
        mySqlHelper=new mySQLiteHelper(this,"message.db",null,1);
		SqlDB=mySqlHelper.getWritableDatabase();	
	}

	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		intent=getIntent();  //ʵ�ֵ�ǰActivity�ĺ�Main֮���ͨ��
		updateHandler=new Handler();
		mAPP = (myApplication) getApplication();  // ��øù������ʵ��                           
        mHandler = mAPP.getHandler();
		init();
		super.onCreate(savedInstanceState);
		setContentView(R.layout.send);
		sendButton = (Button) findViewById(R.id.sendButton);
		textIP = (TextView) findViewById(R.id.textIP);
		textTotal = (TextView) findViewById(R.id.textTotal);
		textMessage = (EditText) findViewById(R.id.textMessage);
		sendButton.setOnClickListener(new sendButtonListener());// Ϊ��ť��Ӽ�����
		textTotal.setMovementMethod(ScrollingMovementMethod.getInstance());
		textTotal.setScrollbarFadingEnabled(false);  
		//��ʾ��ǰ��������Ϣ
		queryData();
		updateSend =new Runnable()
		{
			
			public void run()
			{ 
			    textTotal.append("me:     "+time+"\n");  
			    textTotal.append(data +"\n");  
			    insertData("me",time,data);
			}
			
		};
		
		updateRecive =new Runnable()
		{
			public void run()
			{
				
				String IP="/"+getIp();
				if(!receivedIP.equals(IP))  //�������Լ���������Ϣ
				{	
		        textTotal.append(receivedIP+"     "+receivedData.time+"\n");
		        textTotal.append(receivedData.data +"\n");
		        insertData(receivedIP,receivedData.time,receivedData.data);
				}
				
			}
			
		};
		   updateWarning =new Runnable() 
			{
				
				public void run()
				{ 
				 Toast.makeText(MessageActivity.this,"you have a new message",Toast.LENGTH_SHORT ).show();
				}
				
			};
			
	}
	/**
	 * ���ݿ�Ĳ������
	 */
	public void insertData(String ip,String time,String content)
	{
		ContentValues values = new ContentValues(); 
		values.put("ip", ip); 
		values.put("time", time); 
		values.put("content", content); 
		SqlDB.insert("information", null, values); 
	}
	/**
	 * ɾ�����ݱ��е�����
	 */
	public void deleteData() 
	{ 
	  String sql = "DELETE FROM information"; 
	  SqlDB.execSQL(sql ); 
	  //SqlDB.execSQL("drop table information");
	} 
	/**
	 * �������ݱ��е��������ݣ�����ʾ��MessageActivity.
	 * Cursor��Ϊһ��ָ������ݿ��ѯ���ؽ����
	 */
	public void queryData() 
	{
	  String ipAddress="";
	  String oldTime="";
	  String oldContent="";
	  String sql = "SELECT * FROM information";
	  Cursor cursor = SqlDB.rawQuery(sql,null); 
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
	 * ��ȡ�Լ���IP,��ת���ɡ�*.*.*.*����ַ  
	 * @return
	 */
	public String getIp()
	{  
	    WifiManager wm=(WifiManager)getSystemService(Context.WIFI_SERVICE);  
	    if(!wm.isWifiEnabled())                     //���Wifi״̬     
	     wm.setWifiEnabled(true);  
	    WifiInfo wi=wm.getConnectionInfo();        //��ȡ32λ����IP��ַ     
	    int IpAdd=wi.getIpAddress(); 
	   // System.out.println("sssssssssss"+IpAdd);
	    String Ip=intToIp(IpAdd);                 //�����͵�ַת���ɡ�*.*.*.*����ַ  
	   // System.out.println("sssdddddd"+Ip);
	    return Ip; 
	   
	}  
	private String intToIp(int IpAdd) 
	{  
	    return (IpAdd & 0xFF ) + "." +  
	    ((IpAdd >> 8 ) & 0xFF) + "." +  
	    ((IpAdd >> 16 ) & 0xFF) + "." +  
	    ( IpAdd >> 24 & 0xFF) ;  
	}   

	/**
	 * Ϊ�������ò˵����˵�������������res/menu/menu.xml�ļ�
	 */
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.submenu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		Intent i;                                       // ���û�����˵����������ʱ�������Ӧ
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
	   getApplicationContext().unbindService(conn);
		stopService(reciveIntent);
		sendMessage.shutdown();
		//sendMessage.stop();
		recieve.destroy();
		//recieve.stop();
		updateHandler.removeCallbacks(updateRecive);
		updateHandler.removeCallbacks(updateSend);
		updateHandler.removeCallbacks(updateWarning);
		deleteData() ;   //ɾ�����ݱ��е���������
		mySqlHelper.close();   //�ر����ݿ�
    }

	class sendButtonListener implements OnClickListener 
	{

		public void onClick(View v) 
		{
			sendMessage.resumeMessage();
		}
	}
	
	public class Recieve extends Thread 
	{
		@Override
		public void run() 
		{
			initRecive();                    // ��rec=false�������߳�
			while (rec == true)
			{
	
				if (recivebinder.getMessages() !=null&& receivedData != recivebinder.getMessages()) 
				   {
					receivedData = recivebinder.getMessages();
					receivedIP = recivebinder.getIP();
					//System.out.println("�յ��ķ�������Ϣ" + receivedMessages);
					updateHandler.post(updateRecive);
					String IP="/"+getIp();
				    if(!receivedIP.equals(IP))  //�������Լ���������Ϣ
					  {	//****һ���յ���Ϣ��ʾ�ķ���
					    updateHandler.post(updateWarning);
				        //***��һ���յ���Ϣ��ʾ�ķ������õ���application,������Main,����MessagyActivity����ʾ
					   /* Message msg=mHandler.obtainMessage();
					    msg.what=1;
					    mHandler.sendMessage(msg);*/
					
					  }
				
				   }
			
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) 
				{
					e.printStackTrace();
				}
				
			}
		}

		public synchronized void initRecive() 
		{
			try {
				wait();
			} catch (InterruptedException e1) 
			{
				e1.printStackTrace();
			}
		}

		public synchronized void resumRecive()
		{
			rec = true;
			notify();
		}

		public synchronized void pauseRecive() 
		{
			rec = false;

		}

		@Override
		public void destroy()
		{
			pauseRecive();
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
				if (isSending())
				{
					try {
						init();
						//System.out.println("����������:" + packet.getData());
						socket.send(packet);
						updateHandler.post(updateSend);
					} catch (IOException e) 
					{
						Log.error(getClass(), e);
					}
					
				}
				
				synchronized (this) 
				{
					try 
					{
						if (isRunning())
							wait();
					} catch (InterruptedException e)
					{
						Log.error(getClass(), e);
					}
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
				//System.out.println("�����Ķ���" + data);
				SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd   HH:mm:ss");     
				Date curDate=new Date(System.currentTimeMillis());
				time=formatter.format(curDate); 
				sendData.data=data;
				sendData.time=time;
				/*try {
					                                   
					messages = data.getBytes("UTF8");// ��string���͵�����ת��Ϊbyte[]���͵�
					
				} catch (UnsupportedEncodingException e)
				{
					e.printStackTrace();
				}*/
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
			notify();
		}

		public synchronized void shutdown() 
		{
			pauseMessage();
			running = false;
			notify();
			if(socket!=null)
			  socket.close();
		}
	}
}
