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
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

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
    public static byte[] Frame=null;
    public static String myIPAddres=null;
  
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
        myIPAddres="/"+getIp();
        if(AudioSettings.useSpeex()==AudioSettings.USE_SPEEX) 
        	Frame = new byte[Speex.getEncodedSize(AudioSettings.getSpeexQuality())];
		else 
			Frame = new byte[Audio.FRAME_SIZE_IN_BYTES];
        String s="hello";
        Frame=s.getBytes();
        //for(int i=0;i<Frame.length;i++)
		//	  Frame[i]=1;
        String status = Environment.getExternalStorageState();
        if (status.equals(Environment.MEDIA_MOUNTED)) //�ж�sdcard�Ƿ����
        {
         SDPATH=Environment.getExternalStorageDirectory().toString();
        }
        else                              
        {
         SDPATH=getFilesDir().toString(); // �浽�ֻ��ڲ��洢�� 
        } 
        	//����Ĵ�������ɾ���ڲ������ļ�
         //File fi=getFilesDir();
       /*
       // File fi=Environment.getExternalStorageDirectory();
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
     	case R.id.googlemap:
    		i = new Intent(this, mapActivity.class); 
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
        mySqlHelper=new mySQLiteHelper(this,"mes.db",null,1);
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
        mySqlHelper.deleteData(SqlDB) ;   //ɾ�����ݱ��е���������
        //mySqlHelper.deleteAudioData(SqlDB); //ɾ����Ƶ����
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
				
				if (recivebinder.getMessages() !=null&& receivedData != recivebinder.getMessages()) 
				   {
					receivedData = recivebinder.getMessages();
					receivedIP = recivebinder.getIP();
				
					//String IP="/"+getIp();
				    if(!receivedIP.equals(myIPAddres))  //�������Լ���������Ϣ
					  {		
				    	messageList.add(receivedData);
				    	mySqlHelper.insertData(SqlDB,receivedData.ipaddress,receivedData.time,receivedData.data);
				    	handler.post(updateWarning);  //��ʾ�յ��µ���Ϣ
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
	private String intToIp(int IpAdd) 
	{  
	    return (IpAdd & 0xFF ) + "." +  
	    ((IpAdd >> 8 ) & 0xFF) + "." +  
	    ((IpAdd >> 16 ) & 0xFF) + "." +  
	    ( IpAdd >> 24 & 0xFF) ;  
	} 
     
}