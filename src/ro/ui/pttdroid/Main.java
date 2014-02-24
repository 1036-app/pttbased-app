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

import java.io.Serializable;

import ro.ui.pttdroid.Player.PlayerBinder;
import ro.ui.pttdroid.codecs.Speex;
import ro.ui.pttdroid.settings.AudioSettings;
import ro.ui.pttdroid.settings.CommSettings;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
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
    public String Wmessage=null; 
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);           
        init();          
    }
    
    @Override
    public void onPause()
    {
    	super.onPause();
    	recorder.pauseAudio();
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
			Wmessage=i.getStringExtra("WARNING");
			System.out.println("sdddddds"+Wmessage);
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
    
    private void shutdown() 
    {    	  
    	
    	firstLaunch = true;    	
    	stopService(playerIntent);
    	recorder.shutdown();    		
        Speex.close();                   
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
        
}