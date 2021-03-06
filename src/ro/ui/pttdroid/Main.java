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
        if (status.equals(Environment.MEDIA_MOUNTED)) //判断sdcard是否插入
        {
         SDPATH=Environment.getExternalStorageDirectory().toString();
        }
        else                              
        {
         SDPATH=getFilesDir().toString(); // 存到手机内部存储里 
        } 
        sendIP senIP=new sendIP();
        senIP.start();
       
        //下面的代码可用来删除数据库 文件  
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
    	getMenuInflater().inflate(R.menu.menu, menu); //为程序设置菜单，菜单具体内容来自res/menu/menu.xml文件
    	return true;
    }
 
    @Override
    //
    public boolean onOptionsItemSelected(MenuItem item) 
    {
    	Intent i;                                     //当用户点击菜单里面的内容时程序的响应
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
    		
    		i = new Intent(this, CommSettings.class); //若用户选择了communication 这一项，则跳到CommSettingsActivity
    		startActivityForResult(i, 0);    	      //能返回到之前的Activity	
    		return true;
    	case R.id.settings_audio:
    		
    		i = new Intent(this, AudioSettings.class);
    		startActivityForResult(i, 0);             //调用onActivityResult		
    		return true;    
    	case R.id.settings_reset_all:
    		return resetAllSettings();                //全部恢复到初始值		
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
    	toast.show();                                 //设置提醒信息
    	return true;
    }
    
    @Override                                        //当返回到当前Activity时，调用该回调函数
    public void onActivityResult(int requestCode, int resultCode, Intent data) 
    {
    	CommSettings.getSettings(this);              //将最新的设置信息当前的属性变量中
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
    		CommSettings.getSettings(this);          //获得各属性的值
    		AudioSettings.getSettings(this);
    		setVolumeControlStream(AudioManager.STREAM_MUSIC);//设置音量键控制的音频流
    	    Speex.open(AudioSettings.getSpeexQuality());      //按用户选定的码率进行音频压缩    	    	 
    		playerIntent = new Intent(this, Player.class);            
            startService(playerIntent);             //启动player服务         
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
		//创建数据库message.db	
        mySqlHelper=new mySQLiteHelper(this,"mydata.db",null,1);
		SqlDB=mySqlHelper.getWritableDatabase();	
		
    }
    /**
     * 关闭Acitivity调用的函数
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
        mySqlHelper.deleteData(SqlDB) ;   //删除数据表中的所有消息数据
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
	 * MicrophoneSwitcher是一个线程，当用户点击（准备进行语音通话），释放屏幕（通话结束）时，执行的各种操作
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
	    	microphoneImage.setOnTouchListener(this);               //在图片上设置点击监听器
	    	Intent intent = new Intent(Main.this, Player.class); 
    		playerServiceConnection = new PlayerServiceConnection();
    		bindService(intent, playerServiceConnection, Context.BIND_AUTO_CREATE);
                                                                    // 绑定服务，创建一个长期存在的连接
	    	handler.postDelayed(this, PROGRESS_CHECK_PERIOD);       //100ms之后执行该（MicrophoneSwitcher）线程
		}
	    
		public synchronized void run() 
		{					
			if(running) 
			{
				int currentProgress = player.getProgress(); 	
				if(currentProgress > previousProgress)              //当音频数据在播放时
				{
					if(microphoneState!=MIC_STATE_DISABLED)         //若当前点击屏幕可以通话
					{
						recorder.pauseAudio();  //暂停记录
						setMicrophoneState(MIC_STATE_DISABLED); 	//设置当前点击屏幕不可用						
					}						 							
				}
				else 
				{
					if(microphoneState==MIC_STATE_DISABLED)         //若当前点击屏幕不可用
						setMicrophoneState(MIC_STATE_NORMAL);       //回复点击屏幕可用（可进行语言通话）
				}
				previousProgress = currentProgress;
				handler.postDelayed(this, PROGRESS_CHECK_PERIOD);
			}
				
		}
		
	/**
	 * 对用户点击屏幕的响应
	 */
		public boolean onTouch(View v, MotionEvent e) 
	    {
	    	if(microphoneState!=MicrophoneSwitcher.MIC_STATE_DISABLED) 
	    	{    		
	    		switch(e.getAction()) {
	    		case MotionEvent.ACTION_DOWN:                       //若用户按了屏幕			
	    			recorder.resumeAudio();                         //通知recorder开始记录
	    			setMicrophoneState(MicrophoneSwitcher.MIC_STATE_PRESSED); //设置屏幕点击不可用
	    			break;
	    		case MotionEvent.ACTION_UP:                         //用户放开了屏幕
	    			setMicrophoneState(MicrophoneSwitcher.MIC_STATE_NORMAL); //设置屏幕点击可用
	    			recorder.pauseAudio();                          //通知recorder停止记录 			
	    			break;
	    		}
	    	}
	    	return true;
	    }
		/**
		 * 当屏幕属于不同状态（按下，可点击，不可点击）时，为其设置不同的图片
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
	     * 线程结束时要解除绑定的服务，并从线程队列中移除该线程
	     */
		public synchronized void shutdown()
		{
			unbindService(playerServiceConnection);
			handler.removeCallbacks(microphoneSwitcher);			
			running = false;
		}
		
	};
	/**
	 * 接收信息的线程
	 * @author PC
	 *
	 */
	
	public class Recieve extends Thread 
	{
		public boolean rec=false;
		@Override
		public void run() 
		{
			initRecive();                    // 若rec=false则阻塞线程
			while (rec == true)
			{
				if (recivebinder.getMessages() !=null)
				{
					received=recivebinder.getMessages();
					
				if (!receivedData.time.equals(received.time)) 
				   {
					System.out.println("上一次时间:"+receivedData.time );
					System.out.println("本次时间:"+received.time);
					System.out.println("  ");
					receivedData = received;
					receivedIP = received.ipaddress;
					myIPAddres="/"+getIp(); 
				    if(!receivedIP.equals(myIPAddres))  //不接收自己发出的信息
					  {		
				    	messageList.add(receivedData);
				    	mySqlHelper.insertData(SqlDB,receivedData.ipaddress,receivedData.time,receivedData.data);
				    	handler.post(updateWarning);  //提示收到新的消息
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
	 * 获取自己的IP,并转换成“*.*.*.*”地址  
	 * @return
	 */
	public String getIp()
	{  
	    WifiManager wm=(WifiManager)getSystemService(Context.WIFI_SERVICE);  
	    if(!wm.isWifiEnabled())                     //检查Wifi状态     
	     wm.setWifiEnabled(true);  
	    WifiInfo wi=wm.getConnectionInfo();        //获取32位整型IP地址     
	    int IpAdd=wi.getIpAddress(); 
	    String Ip=intToIp(IpAdd);                 //把整型地址转换成“*.*.*.*”地址  
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