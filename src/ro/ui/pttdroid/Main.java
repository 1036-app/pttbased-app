/* Copyright 2011 Ionut Ursuleanu
 
This file is part of pttdroid.
 
pttdroid is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
 
pttdroid is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
 
You should have received a copy of the GNU General Public License
along with pttdroid.  If not, see <http://www.gnu.org/licenses/>. */

package ro.ui.pttdroid;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import ro.ui.pttdroid.Player.PlayerBinder;
import ro.ui.pttdroid.ReciveMessage.ReciveBinder;
import ro.ui.pttdroid.codecs.Speex;
import ro.ui.pttdroid.settings.AudioSettings;
import ro.ui.pttdroid.settings.CommSettings;
import ro.ui.pttdroid.util.Audio;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.Toast;

public class Main extends Activity
{
	private static boolean firstLaunch = true;			
	private static volatile	Player	player;
	private static Recorder 		recorder;	
	private MicrophoneSwitcher 	microphoneSwitcher;
    private static Intent 		playerIntent;
    private MyHandler handler=null;
    private myApplication mAPP=null;
    public TransportData receivedData =null;
    public TransportData received=null;
    public ReciveBinder recivebinder = null;
    public Recieve recieve = null;
    public static SQLiteDatabase SqlDB;
    public static mySQLiteHelper mySqlHelper;
    public static Intent reciveIntent;
    public ServiceConnection conn = null;
    public String receivedIP = "";
    public static ArrayList<TransportData>messageList=new ArrayList<TransportData>();
    public Runnable updateWarning=null;
    public static String SDPATH=null;
    public static String myIPAddres=null;
    public static Socket TCPsocket=null;
    public static int MessagePort=40000;
    public static int AudioPort=49999;
    public static List<String> allIP=null;
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        mAPP = (myApplication) getApplication();   
        handler = new MyHandler(); 
        mAPP.setHandler(handler);
        setContentView(R.layout.main);           
        init();  
        conn();
        allIP=new ArrayList<String>();
        myIPAddres="/"+getIp(); 
        String status = Environment.getExternalStorageState();
        if (status.equals(Environment.MEDIA_MOUNTED)) //�ж�sdcard�Ƿ����
        {
         SDPATH=Environment.getExternalStorageDirectory().toString();
        }
        else                              
        {
         SDPATH=getFilesDir().toString(); // �浽�ֻ��ڲ��洢�� 
        } 
        sendIP senIP=new sendIP();
        senIP.start();
       
        //����Ĵ��������ɾ�����ݿ� �ļ�  
        File fi=getFilesDir();
        String parentPath=null;
        parentPath= fi.getParent(); 
        fi=new File(parentPath,"databases");
         
       /* 
         if (fi.isDirectory())
        	{  
                File[] childFile = fi.listFiles();  
                if (childFile == null || childFile.length == 0)
                {  
                } 
                else
                {
                	 for (File f : childFile)
                     {  
                       f.delete();  
                     }  
                }
            } 
       */ 
        	
       
       
          updateWarning =new Runnable() 
		{
			
			public void run()
			{ 
			 Toast.makeText(Main.this,"you have a new message",Toast.LENGTH_SHORT ).show();
			}
			
		}; 
    }
    
    @Override
    public void onPause()
    {
    	super.onPause();
    	recorder.setRecordFalse();
    }
                   
    @Override
    public void onDestroy() 
    {
    	super.onDestroy();
    	microphoneSwitcher.shutdown();    	    
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
    	getMenuInflater().inflate(R.menu.menu, menu); //Ϊ�������ò˵����˵�������������res/menu/menu.xml�ļ�
    	return true;
    }
 
    @Override
    //
    public boolean onOptionsItemSelected(MenuItem item) 
    {
    	Intent i;                                     //���û�����˵����������ʱ�������Ӧ
    	switch(item.getItemId()) {
    	case R.id.quit:
    		shutdown();
    		return true;
    	case R.id.send:
    		i = new Intent(this, MessageActivity.class); 
			startActivityForResult(i, 0); 
            return true;
     	case R.id.OSMmap:
    		i = new Intent(this, OSMActivity.class); 
			startActivityForResult(i, 0); 
            return true;
     	case R.id.baidumap:
    		i = new Intent(this, baiduMap.class); 
			startActivityForResult(i, 0); 
            return true;
    	case R.id.searchAudio:
    		i = new Intent(this, SearchAudioFiles.class); 
			startActivityForResult(i, 0);
			return true;
    	case R.id.settings_comm:
    		
    		i = new Intent(this, CommSettings.class); //���û�ѡ����communication ��һ�������CommSettingsActivity
    		startActivityForResult(i, 0);    	      //�ܷ��ص�֮ǰ��Activity	
    		return true;
    	case R.id.settings_audio:
    		
    		i = new Intent(this, AudioSettings.class);
    		startActivityForResult(i, 0);             //����onActivityResult		
    		return true;    
    	case R.id.settings_reset_all:
    		return resetAllSettings();                //ȫ���ָ�����ʼֵ		
    	default:
    		return super.onOptionsItemSelected(item);
    	}
    	
    }
    
    /**
     * Reset all settings to their default value
     * @return
     */
    private boolean resetAllSettings() 
    {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);	
    	Editor editor = prefs.edit();
    	editor.clear();
    	editor.commit();   
        Toast toast = Toast.makeText(this, getString(R.string.setting_reset_all_confirm), Toast.LENGTH_SHORT);    	
    	toast.setGravity(Gravity.CENTER, 0, 0);
    	toast.show();                                 //����������Ϣ
    	return true;
    }
    
    @Override                                        //�����ص���ǰActivityʱ�����øûص�����
    public void onActivityResult(int requestCode, int resultCode, Intent data) 
    {
    	CommSettings.getSettings(this);              //�����µ�������Ϣ��ǰ�����Ա�����
    	AudioSettings.getSettings(this);    
    }
    public class MyHandler extends Handler 
    {  
        @Override  
        public void handleMessage(Message msg) 
        {  
            super.handleMessage(msg);  
 
        }  
    }  
    private void init() 
    {    	    	    	
    	if(firstLaunch) 
    	{    		
    		CommSettings.getSettings(this);          //��ø����Ե�ֵ
    		AudioSettings.getSettings(this);
    		setVolumeControlStream(AudioManager.STREAM_MUSIC);//�������������Ƶ���Ƶ��
    	    Speex.open(AudioSettings.getSpeexQuality());      //���û�ѡ�������ʽ�����Ƶѹ��    	    	 
    		playerIntent = new Intent(this, Player.class);            
            startService(playerIntent);             //����player����         
    		recorder = new Recorder();
    		recorder.start();     		    		   		
    		firstLaunch = false;    
    			
    	}
    	
		microphoneSwitcher = new MicrophoneSwitcher();
		microphoneSwitcher.init();
    }
    private void conn()
    {
    	
		recieve = new Recieve();
		receivedData=new TransportData();
	    recieve.start();
		reciveIntent = new Intent(Main.this, ReciveMessage.class);
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
        mySqlHelper=new mySQLiteHelper(this,"mydata.db",null,1);
		SqlDB=mySqlHelper.getWritableDatabase();	
		
    }
    /**
     * �ر�Acitivity���õĺ���
     */
    private void shutdown() 
    {    	  
    	
    	firstLaunch = true;    	
    	stopService(playerIntent);
    	getApplicationContext().unbindService(conn);
		stopService(reciveIntent);
		recieve.destroy();
    	recorder.shutdown();    		
        Speex.close();
        mySqlHelper.deleteData(SqlDB) ;   //ɾ�����ݱ��е�������Ϣ����
		mySqlHelper.close();    
        finish();
       
    }     
    
    private class PlayerServiceConnection implements ServiceConnection
	{					
		public void onServiceConnected(ComponentName arg0, IBinder arg1) 
		{
			player = ((PlayerBinder) arg1).getService();			
		}
		
		public void onServiceDisconnected(ComponentName arg0) 
		{					
			player = null;		
		}
	};
	/**
	 * MicrophoneSwitcher��һ���̣߳����û������׼����������ͨ�������ͷ���Ļ��ͨ��������ʱ��ִ�еĸ��ֲ���
	 */
	private class MicrophoneSwitcher implements Runnable, OnTouchListener 
	{	
		private Handler 	handler = new Handler();
		private ImageView 	microphoneImage;	
		public static final int MIC_STATE_NORMAL = 0;
		public static final int MIC_STATE_PRESSED = 1;
		public static final int MIC_STATE_DISABLED = 2;
		private int microphoneState = MIC_STATE_NORMAL;
		private int previousProgress = 0;
		private static final int	PROGRESS_CHECK_PERIOD = 100;
		private volatile boolean	running = false;
		private ServiceConnection	playerServiceConnection;
		
		
		public void init()
		{
	    	microphoneImage = (ImageView) findViewById(R.id.microphone_image);
	    	microphoneImage.setOnTouchListener(this);               //��ͼƬ�����õ��������
	    	Intent intent = new Intent(Main.this, Player.class); 
    		playerServiceConnection = new PlayerServiceConnection();
    		bindService(intent, playerServiceConnection, Context.BIND_AUTO_CREATE);
                                                                    // �󶨷��񣬴���һ�����ڴ��ڵ�����
	    	handler.postDelayed(this, PROGRESS_CHECK_PERIOD);       //100ms֮��ִ�иã�MicrophoneSwitcher���߳�
		}
	    
		public synchronized void run() 
		{					
			if(running) 
			{
				int currentProgress = player.getProgress(); 	
				if(currentProgress > previousProgress)              //����Ƶ�����ڲ���ʱ
				{
					if(microphoneState!=MIC_STATE_DISABLED)         //����ǰ�����Ļ����ͨ��
					{
						recorder.pauseAudio();  //��ͣ��¼
						setMicrophoneState(MIC_STATE_DISABLED); 	//���õ�ǰ�����Ļ������						
					}						 							
				}
				else 
				{
					if(microphoneState==MIC_STATE_DISABLED)         //����ǰ�����Ļ������
						setMicrophoneState(MIC_STATE_NORMAL);       //�ظ������Ļ���ã��ɽ�������ͨ����
				}
				previousProgress = currentProgress;
				handler.postDelayed(this, PROGRESS_CHECK_PERIOD);
			}
				
		}
		
	/**
	 * ���û������Ļ����Ӧ
	 */
		public boolean onTouch(View v, MotionEvent e) 
	    {
	    	if(microphoneState!=MicrophoneSwitcher.MIC_STATE_DISABLED) 
	    	{    		
	    		switch(e.getAction()) {
	    		case MotionEvent.ACTION_DOWN:                       //���û�������Ļ			
	    			recorder.resumeAudio();                         //֪ͨrecorder��ʼ��¼
	    			setMicrophoneState(MicrophoneSwitcher.MIC_STATE_PRESSED); //������Ļ���������
	    			break;
	    		case MotionEvent.ACTION_UP:                         //�û��ſ�����Ļ
	    			setMicrophoneState(MicrophoneSwitcher.MIC_STATE_NORMAL); //������Ļ�������
	    			recorder.pauseAudio();                          //֪ͨrecorderֹͣ��¼ 			
	    			break;
	    		}
	    	}
	    	return true;
	    }
		/**
		 * ����Ļ���ڲ�ͬ״̬�����£��ɵ�������ɵ����ʱ��Ϊ�����ò�ͬ��ͼƬ
		 * @param state
		 */
		public void setMicrophoneState(int state) 
	    {
	    	switch(state) {
	    	case MIC_STATE_NORMAL:
	    		microphoneState = MIC_STATE_NORMAL;
	    		microphoneImage.setImageResource(R.drawable.microphone_normal_image);
	    		break;
	    	case MIC_STATE_PRESSED:
	    		microphoneState = MIC_STATE_PRESSED;
	    		microphoneImage.setImageResource(R.drawable.microphone_pressed_image);
	    		break;
	    	case MIC_STATE_DISABLED:
	    		microphoneState = MIC_STATE_DISABLED;
	    		microphoneImage.setImageResource(R.drawable.microphone_disabled_image);
	    		break;    		
	    	}
	    }
	    /**
	     * �߳̽���ʱҪ����󶨵ķ��񣬲����̶߳������Ƴ����߳�
	     */
		public synchronized void shutdown()
		{
			unbindService(playerServiceConnection);
			handler.removeCallbacks(microphoneSwitcher);			
			running = false;
		}
		
	};
	/**
	 * ������Ϣ���߳�
	 * @author PC
	 *
	 */
	
	public class Recieve extends Thread 
	{
		public boolean rec=false;
		@Override
		public void run() 
		{
			initRecive();                    // ��rec=false�������߳�
			while (rec == true)
			{
				if (recivebinder.getMessages() !=null)
				{
					received=recivebinder.getMessages();
					
				if (!receivedData.time.equals(received.time)) 
				   {
					System.out.println("��һ��ʱ��:"+receivedData.time );
					System.out.println("����ʱ��:"+received.time);
					System.out.println("  ");
					receivedData = received;
					receivedIP = received.ipaddress;
					myIPAddres="/"+getIp(); 
				    if(!receivedIP.equals(myIPAddres))  //�������Լ���������Ϣ
					  {		
				    	messageList.add(receivedData);
				    	mySqlHelper.insertData(SqlDB,receivedData.ipaddress,receivedData.time,receivedData.data);
				    	handler.post(updateWarning);  //��ʾ�յ��µ���Ϣ
					  }
				
				   }
				 }
				try {
					Thread.sleep(50);
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
	    String Ip=intToIp(IpAdd);                 //�����͵�ַת���ɡ�*.*.*.*����ַ  
	    return Ip;    
	}  
	public String intToIp(int IpAdd) 
	{  
	    return (IpAdd & 0xFF ) + "." +  
	    ((IpAdd >> 8 ) & 0xFF) + "." +  
	    ((IpAdd >> 16 ) & 0xFF) + "." +  
	    ( IpAdd >> 24 & 0xFF) ;  
	} 
	
	public class sendIP extends Thread
	{	
		public  DatagramSocket socket;
		public  DatagramPacket packet;
	    public  InetAddress Addr=null;
	    public  byte [] IPcontext=null;
	    public  boolean sending=true;
	   
		@Override
		public void run()
		{
			 try {
				String context="Hello";
			    IPcontext=new byte[context.getBytes("UTF8").length];
				IPcontext=context.getBytes("UTF8");
				Addr=InetAddress.getByName("255.255.255.255");
				socket = new DatagramSocket();
			} catch (Exception e)
			{
				e.printStackTrace();
			}
			
			 packet = new DatagramPacket(IPcontext,IPcontext.length,Addr,CommSettings.getPort());
			while(sending)
			{
			   try {
			    	socket.send(packet);
			    	Thread.sleep(60000);
			  } catch (Exception e) 
			  {
				e.printStackTrace();
			  }
			
			}
			 super.run();
		}
		public void shutdown()
		{
			sending=false;
		}
	}
}