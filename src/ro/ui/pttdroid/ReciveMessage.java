package ro.ui.pttdroid;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

import ro.ui.pttdroid.settings.CommSettings;
import ro.ui.pttdroid.util.Log;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

public class ReciveMessage extends Service
{
	
	private TelephonyManager	telephonyManager;
	private PhoneCallListener	phoneCallListener;
	public TcpReceieveMessage TreceiveMessage=null;
	public InetAddress senderAddress = null;
	public IBinder reciveBinder = new ReciveBinder();
	public TransportData recieveData=null;
	public byte[]data=null;
	public String messages="";
	public String addr="";
	@Override
	public IBinder onBind(Intent intent) 
	{
		return reciveBinder;
	}
	@Override
	public boolean onUnbind(Intent intent)
	{
		TreceiveMessage.shutDown();
		reciveBinder=null;
		return super.onUnbind(intent);
	}

	public class ReciveBinder extends Binder 
	{
       public TransportData getMessages()
        {
	     return recieveData; 
        }
       public String getIP()
       {
			return recieveData.ipaddress;
       }
		ReciveMessage getService() 
		{        
            return ReciveMessage.this;
        }
    }
	@Override
	public void onCreate() 
	{ 
		TreceiveMessage=new TcpReceieveMessage();  //TCP接受信息的線程
		TreceiveMessage.start();               //TelephonyManager类主要提供了一系列用于访问与手机通讯相关的状态和信息的get方法
		telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		phoneCallListener = new PhoneCallListener();
		telephonyManager.listen(phoneCallListener, PhoneStateListener.LISTEN_CALL_STATE);//监听手机的call状态
		                                         //创建一个通知，制定其图标，标题及通知时间
		Notification notification = new Notification(R.drawable.send, getText(R.string.app_name),System.currentTimeMillis());
		Intent notificationIntent = new Intent(this, Main.class);
		                                         //当点击消息时就会向系统发送notificationIntent
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		notification.setLatestEventInfo(this, getText(R.string.app_name),getText(R.string.app_running), pendingIntent);
		startForeground(1, notification);  
		
    }
	
	@Override
	public void onDestroy()
	{
		TreceiveMessage.shutDown();
		telephonyManager.listen(phoneCallListener, PhoneStateListener.LISTEN_NONE);
	}
	/**
	 * 處理接收到的数据
	 * @param data
	 * @param addr
	 */
	public void GetData(byte[] data,String addr)
	{
	   try {  
		    ByteArrayInputStream bais = new ByteArrayInputStream(data);  
		    ObjectInputStream ois = new ObjectInputStream(bais);  
		    recieveData = (TransportData)ois.readObject(); 
		    bais.close();  
		    ois.close();  
		    recieveData.ipaddress=addr;
		  }  
		  catch(Exception e)
		  {    
		    System.out.println(e.toString());
		    e.printStackTrace();  
		  }  
	}
	public class TcpReceieveMessage extends Thread
	{
		public ServerSocket ReceieveSocket=null;
		public Socket SenderSocket=null;
		private  boolean playing = true;
		@Override
		public void run()
		{
		 android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);				 	
		    try {
				ReceieveSocket = new ServerSocket(Main.MessagePort);
			} catch (IOException e1) 
			{
				e1.printStackTrace();
			}
		   while(playing)
		   {	SenderSocket=null; 
		      try {
		    	   SenderSocket=ReceieveSocket.accept();
		    	   System.out.println("连接到接收端成功");
			      } catch (IOException e1) 
			      {
				 e1.printStackTrace();
			     }
		      if(SenderSocket!=null)
		        new Thread(new ReceieveThread(SenderSocket)).start(); 
		 }
	 }	
		public synchronized void resumeMessage() 
		{
			playing = true;  
			notify();
		}
		public synchronized void pauseMessage() 
		{				
			playing = false;
		}
		public synchronized void shutDown() 
		{	
		    playing= false;						
			notify();
		   try {
			   if(ReceieveSocket!=null)
				   ReceieveSocket.close();
			   if(SenderSocket!=null)
				   SenderSocket.close();
				} catch (Exception e) 
				{	
					e.printStackTrace();
				}
		}
	}	
	
	 public class ReceieveThread extends Thread
	   {  	  
		 private Socket SenderSocket = null;  
	     public ReceieveThread(Socket SS)
	     {  
			 this.SenderSocket = SS;  
	     }  
		  @Override  
	     public void run()
	     {  
			 byte[] Tcpdata=new byte[256];
			 String address="";
	         InputStream in=null;
			try {
				in = SenderSocket.getInputStream(); 
	            int cr=0;
	            address="/"+SenderSocket.getInetAddress().getHostAddress().toString();
	            while(cr != -1)
	            {
	             cr = in.read(Tcpdata);
	             }
			   } catch (IOException e)
			   {
				e.printStackTrace();
			   } 
			 System.out.println("处理数据");
			 GetData(Tcpdata,address); 
		 }
	   }
	private class PhoneCallListener extends PhoneStateListener
	{
		
		@Override
		public void onCallStateChanged (int state, String incomingNumber)//监听电话的状态，一旦状态改变，则调用该函数
		{
			if(state==TelephonyManager.CALL_STATE_OFFHOOK)        //电话挂机状态
			{	
		    	TreceiveMessage.pauseMessage();
			}
			else if(state==TelephonyManager.CALL_STATE_IDLE)      //电话空闲状态
			{
			    TreceiveMessage.resumeMessage();
			}
		}
		
	}	
}
