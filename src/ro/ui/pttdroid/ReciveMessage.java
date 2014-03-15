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
	public UdpReceieveMessage UreceiveMessage=null;
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
		UreceiveMessage.shutDown();
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
	
		UreceiveMessage=new UdpReceieveMessage();  //UDP������Ϣ�ľ���
		UreceiveMessage.start();    
		TreceiveMessage=new TcpReceieveMessage();  //TCP������Ϣ�ľ���
		TreceiveMessage.start();               //TelephonyManager����Ҫ�ṩ��һϵ�����ڷ������ֻ�ͨѶ��ص�״̬����Ϣ��get����
		telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		phoneCallListener = new PhoneCallListener();
		telephonyManager.listen(phoneCallListener, PhoneStateListener.LISTEN_CALL_STATE);//�����ֻ���call״̬
		                                         //����һ��֪ͨ���ƶ���ͼ�꣬���⼰֪ͨʱ��
		Notification notification = new Notification(R.drawable.send, getText(R.string.app_name),System.currentTimeMillis());
		Intent notificationIntent = new Intent(this, Main.class);
		                                         //�������Ϣʱ�ͻ���ϵͳ����notificationIntent
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		notification.setLatestEventInfo(this, getText(R.string.app_name),getText(R.string.app_running), pendingIntent);
		startForeground(1, notification);  
		
    }
	
	@Override
	public void onDestroy()
	{
		UreceiveMessage.shutDown();
		TreceiveMessage.shutDown();
		telephonyManager.listen(phoneCallListener, PhoneStateListener.LISTEN_NONE);
	}
	/**
	 * ̎����յ�������
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
	public class UdpReceieveMessage extends Thread
	{
		private DatagramSocket 	UDPsocket=null;
		private DatagramPacket 	packet=null;
		private volatile boolean running = true;	
		private volatile boolean playing = true;
		@Override
		public void run()
		{
		 android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);				 	
		  while(running)
		  { 
			  init();
		   while(playing)
		   {	 
			 try
		      {	  
			  data=new byte[256]; 
			  packet=new DatagramPacket(data, data.length);
		      UDPsocket.receive(packet); //������Ϣ���ݰ�
		      addr=packet.getAddress().toString();
		 	   // ����Ƿ������ݰ����������ݰ�Ҫ���͵�Ŀ���ַ������ǽ������ݰ��򷵻ط��ʹ����ݰ���Դ��ַ��       
		     }
		    catch(Exception e) 
			 {
				break;
			 }
			 GetData(packet.getData(),addr);    
		 }
		 synchronized(this)
			{
				try 
				{	
					if(running)
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
		UDPsocket=new DatagramSocket(Main.MessagePort);
		}
		catch(Exception e)
		{
			Log.error(getClass(), e);
		}
		
	 }
				
	public synchronized void pauseMessage() 
		{				
			playing = false;
			
			try
			{
				if(UDPsocket instanceof MulticastSocket)
					((MulticastSocket) UDPsocket).leaveGroup(CommSettings.getMulticastAddr());
				 UDPsocket.close();
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
		   try {
			   if(UDPsocket!=null)  
				   UDPsocket.close();
				} catch (Exception e) 
				{	
					e.printStackTrace();
				}
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
		   {	 
		      try {
		    	   SenderSocket=ReceieveSocket.accept();
		    	  // System.out.println("�ͻ������ӳɹ�");
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
	            address=SenderSocket.getInetAddress().getHostAddress().toString();
	            while(cr != -1)
	            {
	             cr = in.read(Tcpdata);
	             }
			   } catch (IOException e)
			   {
				e.printStackTrace();
			   } 
			 GetData(Tcpdata,address); 
		 }
	   }
	private class PhoneCallListener extends PhoneStateListener
	{
		
		@Override
		public void onCallStateChanged (int state, String incomingNumber)//�����绰��״̬��һ��״̬�ı䣬����øú���
		{
			if(state==TelephonyManager.CALL_STATE_OFFHOOK)        //�绰�һ�״̬
			{	
				UreceiveMessage.pauseMessage();
		    	TreceiveMessage.pauseMessage();
			}
			else if(state==TelephonyManager.CALL_STATE_IDLE)      //�绰����״̬
			{
				UreceiveMessage.resumeMessage();
			    TreceiveMessage.resumeMessage();
			}
		}
		
	}	
}
