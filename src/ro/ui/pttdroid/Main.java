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
			startActivity(i);
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
        
}