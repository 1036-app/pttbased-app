package ro.ui.pttdroid;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import ro.ui.pttdroid.ReciveMessage.ReciveBinder;
import ro.ui.pttdroid.settings.CommSettings;
import ro.ui.pttdroid.util.IP;
import ro.ui.pttdroid.util.Log;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MessageActivity extends Activity 
{

	public static Intent reciveIntent;
	public static SendMessage sendMessage;
	//public static ReciveMessage reciveMessage;
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
	public Recieve recieve = null;
	public boolean rec = false;
	public String receivedMessages = "";
	public String receivedIP = "";
    public Handler updateHandler;
	private void init() 
	{
		sendMessage = new SendMessage();
		sendMessage.start();
		recieve = new Recieve();
		recieve.start();
		reciveIntent = new Intent(MessageActivity.this, ReciveMessage.class);
		conn = new ServiceConnection() 
		{
			                                                // 建立链接时调用的函数
			public void onServiceConnected(ComponentName name,IBinder reciveBinder) 
			{

				recivebinder = (ReciveBinder) reciveBinder; // 获取服务端的reciveBinder，以便与服务端通信
				recieve.resumRecive();                      // 唤醒处理接收到短息的线程
			}
                                                           // 断开连接时调用的函数
			public void onServiceDisconnected(ComponentName arg0) 
			{
				recivebinder = null;
				recieve.pauseRecive();
			}
		};
		getApplicationContext().bindService(reciveIntent, conn,Context.BIND_AUTO_CREATE);
		                                                    // 绑定服务，创建一个长期存在的连接

	}

	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		updateHandler=new Handler();
		
		init();
		super.onCreate(savedInstanceState);
		setContentView(R.layout.send);
		sendButton = (Button) findViewById(R.id.sendButton);
		textIP = (TextView) findViewById(R.id.textIP);
		textTotal = (TextView) findViewById(R.id.textTotal);
		textMessage = (EditText) findViewById(R.id.textMessage);
		sendButton.setOnClickListener(new sendButtonListener());// 为按钮添加监听器
		textTotal.setMovementMethod(ScrollingMovementMethod.getInstance());
		textTotal.setScrollbarFadingEnabled(false);
		updateSend =new Runnable()
		{
			public void run()
			{
			    textTotal.append("me: "+data + "\n");  
			}
			
		};
		updateRecive =new Runnable()
		{
			public void run()
			{
		     textTotal.append(receivedIP+": "+receivedMessages + "\n");
			}
			
		};
	
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
		case R.id.returnmain:
			Destroy();
			finish();
			i = new Intent(this, Main.class);
			startActivity(i);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void Destroy() 
	{
		getApplicationContext().unbindService(conn);
		stopService(reciveIntent);
		sendMessage.shutdown();
		recieve.destroy();
		updateHandler.removeCallbacks(updateRecive);
		updateHandler.removeCallbacks(updateSend);
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
			initRecive();                                  // 若rec=false则阻塞线程
			while (rec == true)
			{
				                                           // 接收到发来的短信
				if (recivebinder.getMessages() != ""&& receivedMessages != recivebinder.getMessages()) 
				{
					receivedMessages = recivebinder.getMessages();
					receivedIP = recivebinder.getIP();
					//System.out.println("收到的发来的消息" + receivedMessages);
					updateHandler.post(updateRecive);
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
						//System.out.println("发出的数据:" + packet.getData());
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
				dataEnd=data+"/END";
				//System.out.println("发出的短信" + data);
				try {
					                                   
					messages = dataEnd.getBytes("UTF8");// 将string类型的数据转换为byte[]类型的
					
				} catch (UnsupportedEncodingException e)
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
