package ro.ui.pttdroid;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;

import org.apache.http.util.EncodingUtils;

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
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class SearchAudioFiles extends ListActivity
{
	public  ArrayList<String> list;
	private AudioTrack 	player=null; //用来播放声音的
	public PlayThread pt=null;
	public String filename ="";
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
		Main.mySqlHelper.selectAudioData(Main.SqlDB, filename);
		pt=new PlayThread (filename);
		pt.start();
		
	}
	public class PlayThread extends Thread
	{
        private String name;
		PlayThread(String fname)
		{
			name=fname;
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
				if(mySQLiteHelper.SelectedPlace.equals("W"))
				{
				String SDPATH=Environment.getExternalStorageDirectory().toString();
				File file = new File(SDPATH,name);
				FileInputStream fin = new FileInputStream(file);
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				int len = -1;
				fin.read(array);
				  while(true)
				    {
					  Speex.decode(array, array.length, pcmFrame);
					  player.write(pcmFrame, 0, Audio.FRAME_SIZE);
				        if((fin.read(array)) == -1) 
				        	break;
				    }		
				 bos.close();				
				 fin.close();	            
				}
				else if(mySQLiteHelper.SelectedPlace.equals("N"))
				{
			    FileInputStream fin = openFileInput(name); 
			    while(true)
			    {
				  Speex.decode(array, array.length, pcmFrame);
				  player.write(pcmFrame, 0, Audio.FRAME_SIZE);
			        if((fin.read(array)) == -1) 
			        	break;
			    }					
			    fin.close();
				}
				//for(int i=0;i<array.length;i++)
				//	System.out.println("array"+array[i]);
				//for(int i=0;i<pcmFrame.length;i++)
			   //  System.out.println("pcmFrame"+pcmFrame[i]);
	 		} catch (IOException e) 
	 		{
	 			
	 			e.printStackTrace();
	 		} 
			//System.out.println("读语音");
	 		player.stop();
	 		player.release();
			}
		
		
		
	}
	
	@Override
	protected void onDestroy() 
	{
		if(pt!=null)
		{
		if(pt.isAlive())
		    pt.stop();
		}
		if(player!=null)
		{
			if(player.getState()==player.PLAYSTATE_PLAYING)
			{
				player.stop();
	 		    player.release();
			}	
		}
		super.onDestroy();
	}

	@Override
	protected void onPause() 
	{
		if(pt!=null)
		{
		if(pt.isAlive())
		   pt.stop();
		}
		if(player!=null)
		{
			if(player.getState()==player.PLAYSTATE_PLAYING)
			{
				player.stop();
	 		    player.release();
			}	
		}
		super.onPause();
	}

}
