package ro.ui.pttdroid;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
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
	private volatile boolean running = true;	
	private volatile boolean playing = true;
	public ShowMessage showMessage=null;
	byte[]data=null;
	public String messages="";
	public InetAddress senderAddress = null;
	public IBinder reciveBinder = new ReciveBinder();
	@Override
	public IBinder onBind(Intent intent) 
	{
		return reciveBinder;
	}
	
	@Override
	public boolean onUnbind(Intent intent)
	{
		showMessage.shutDown();
		reciveBinder=null;
		return super.onUnbind(intent);
	}

	public class ReciveBinder extends Binder 
	{
       public String getMessages()
        {
	     return messages;
        }
       public String getIP()
       {
			return senderAddress.toString();
       }
		ReciveMessage getService() 
		{        
            return ReciveMessage.this;
        }
    }
	@Override
	public void onCreate() 
	{
	
	    showMessage=new ShowMessage();
	    showMessage.start();
		                                          //TelephonyManager类主要提供了一系列用于访问与手机通讯相关的状态和信息的get方法
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
		showMessage.shutDown();
		telephonyManager.listen(phoneCallListener, PhoneStateListener.LISTEN_NONE);
	}
	
	public class ShowMessage extends Thread
	{
		public DatagramSocket 	socket=null;		
		public DatagramPacket 	packet=null;	
		public MessageActivity messActivity;
		@Override
		public void run()
		{
		 android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);				 	
		  while(isRunning())
		  { 
			  init();
		    while(isPlaying())
		   {
		     try 
		     {	
		 	  socket.receive(packet); //接收信息数据包
		 	  senderAddress=packet.getAddress();
		 	  //System.out.println("接收到的数据"+packet.getData());
		     }
		   catch(SocketException e) 
		    {
			   break;
		    }
		   catch(IOException e) 
		   {
			Log.error(getClass(), e);
		   }
		   
		   try 
		   {
			messages=new String(data,"UTF-8");        //byte[]转换为string类型
			int index=0;
			while((index = messages.indexOf("/END")) != -1)
			   {
				   messages = messages.substring(0,index);            
			   }
	        } 
		   catch (IOException e) 
		   {
				e.printStackTrace();
		   }
		   
		 }
		 synchronized(this)
			{
				try 
				{	
					if(isRunning())
						wait();
				}
				catch(InterruptedException e) 
				{
					Log.error(getClass(), e);
				}
			}
		}
	 }

	public void init()
	{
		
		try
		{
		socket=new DatagramSocket(40000);
		data=new byte[256];
		packet=new DatagramPacket(data, data.length);
		}
		catch(SocketException e)
		{
			Log.error(getClass(), e);
		}
		catch(IOException e)
		{
			Log.error(getClass(), e);
		}
		
	 }
	
	  private synchronized boolean isRunning()
	  {
		return running;
	  }
			
	 private synchronized boolean isPlaying()
	  {
		return playing;
	   }
				
		public synchronized void pauseMessage() 
		{				
			playing = false;
			
			try
			{
				if(socket instanceof MulticastSocket)
					((MulticastSocket) socket).leaveGroup(CommSettings.getMulticastAddr());
				 socket.close();
			}
			catch (IOException e) 
			{
				Log.error(getClass(), e);
			}					
		}
		
		public synchronized void resumeMessage() 
		{
			playing = true;
			notify();
		}
									
		public synchronized void shutDown() 
		{			
			pauseMessage();
			running = false;						
			notify();
			if(socket!=null)
			 socket.close();
		}
		
	
	}
	private class PhoneCallListener extends PhoneStateListener
	{
		
		@Override
		public void onCallStateChanged (int state, String incomingNumber)//监听电话的状态，一旦状态改变，则调用该函数
		{
			if(state==TelephonyManager.CALL_STATE_OFFHOOK)        //电话挂机状态
				showMessage.pauseMessage();
			else if(state==TelephonyManager.CALL_STATE_IDLE)      //电话空闲状态
				showMessage.resumeMessage();
		}
		
	}	
}
