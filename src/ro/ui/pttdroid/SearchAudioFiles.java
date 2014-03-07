package ro.ui.pttdroid;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;

import org.apache.http.util.EncodingUtils;

import ro.ui.pttdroid.Main.MyHandler;
import ro.ui.pttdroid.codecs.Speex;
import ro.ui.pttdroid.settings.AudioSettings;
import ro.ui.pttdroid.util.Audio;

import android.app.Activity;
import android.app.ListActivity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Environment;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class SearchAudioFiles extends ListActivity
{
	public  ArrayList<String> list;
	private AudioTrack 	player=null; //用来播放声音的
	public PlayThread pt=null;
	public String filename ="";
	public Runnable updateWarning=null;
	public boolean isRun=true;
	public boolean is=true;
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search);
		list = new ArrayList<String>();
		//读出数据库中的所有音频文件
		list=Main.mySqlHelper.queryAudioData(Main.SqlDB, list);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.audiolist,
                R.id.file, list);
		setListAdapter(adapter);
	
		
	}


	@Override
	protected void onListItemClick(ListView l, View v, int position, long id)
	{
		
		super.onListItemClick(l, v, position, id);
	    filename=list.get(position);
		//Main.mySqlHelper.selectAudioData(Main.SqlDB, filename);
		File f=new File(Main.SDPATH,filename);//相当于只是见了引用，要真正创建，得调用 file.createNewFile()方法。
		if(f.exists())
		  { 
		     if(player!=null&&player.getPlayState()==player.PLAYSTATE_PLAYING)
		     {
		      pt.stopThread();	
		     }
		     while(true)     //监听正在播放的语言什么时候结束
		     {
			  if(is==true)   //当上一段语音播放结束时，才可以播放下一段
			  {
		       pt=new PlayThread (f);
		       pt.start();
		       is=false;
		       return;
		      }
		    }
		  }
		else
		{
			Toast toast=Toast.makeText(SearchAudioFiles.this,"The file has been deleted",Toast.LENGTH_SHORT );
		    toast.setGravity(Gravity.CENTER, 0, 0);
		    toast.show();
		}
	}
	public class PlayThread extends Thread
	{
        private File file;
		PlayThread(File f)
		{
			file=f;
		}
		@Override
		public void run() 
		{
			super.run();
			
			  player = new AudioTrack(
					AudioManager.STREAM_MUSIC, 
					Audio.SAMPLE_RATE, 
					AudioFormat.CHANNEL_CONFIGURATION_MONO, 
					Audio.ENCODING_PCM_NUM_BITS, 
					Audio.TRACK_BUFFER_SIZE, 
					AudioTrack.MODE_STREAM);
			player.play();
			try {
				short[] pcmFrame =new short[Audio.FRAME_SIZE];
				byte[] array = new byte[Speex.getEncodedSize(AudioSettings.getSpeexQuality())];	
				FileInputStream fin = new FileInputStream(file);
				fin.read(array);
				isRun=true;
				  while(isRun)
				    {
					  Speex.decode(array, array.length, pcmFrame);
					  player.write(pcmFrame, 0, Audio.FRAME_SIZE);
				      if((fin.read(array)) == -1) 
				        	break;
				    }					
				 fin.close();	            
			    
				/*for(int i=0;i<array.length;i++)
					System.out.println("array"+array[i]);
				  for(int i=0;i<pcmFrame.length;i++)
			        System.out.println("pcmFrame"+pcmFrame[i]);
			    */
			    
	 		} catch (IOException e) 
	 		{
	 			e.printStackTrace();
	 		} 
	 		player.stop();
	 		player.release();
	 		is=true;
	 		//System.out.println("当前线程结束"+pt.getId()+player.getPlayState());
		}
		public void stopThread() 
		{	
			isRun=false;
		}
		@Override
		public void destroy()
		{
			super.destroy();
		}
	}
	
	@Override
	protected void onDestroy() 
	{
		
		isRun=false;
		super.onDestroy();
	}

	@Override
	protected void onPause() 
	{
		isRun=false;
		super.onPause();
	}

}
