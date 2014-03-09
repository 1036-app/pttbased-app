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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;

import ro.ui.pttdroid.codecs.Speex;
import ro.ui.pttdroid.settings.AudioSettings;
import ro.ui.pttdroid.settings.CommSettings;
import ro.ui.pttdroid.util.Audio;
import ro.ui.pttdroid.util.IP;
import ro.ui.pttdroid.util.Log;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.os.Environment;

public class Recorder extends Thread
{	
	private AudioRecord recorder;
	private volatile boolean recording = false;
	private volatile boolean running = true;
	private DatagramSocket socket;
	private DatagramPacket packet;
	private short[] pcmFrame = new short[Audio.FRAME_SIZE];
	private byte[] encodedFrame;
    public  FileOutputStream outStream=null;
    public  File myfile=null;
    public  String fname=null;
    public boolean ending=false;
    public String isCoded="YES";
    public boolean start=false;
			
	public void run() 
	{
		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
		
		while(isRunning()) 
		{	init();	
			recorder.startRecording();
			while(isRecording()) 
			{
				if(start==true)
				{
				   if(AudioSettings.useSpeex()==AudioSettings.USE_SPEEX)      
					   isCoded="YES";
				   else 
				       isCoded="NO";	
				   byte[] Frame=null;
				   try {
				    	Frame = new byte[isCoded.getBytes("UTF8").length];
					    Frame=isCoded.getBytes("UTF8");
				     } catch (UnsupportedEncodingException e1)
				     {
					 e1.printStackTrace();
				     } 
				
			       packet = new DatagramPacket(
					   Frame, 
					   Frame.length, 
					   CommSettings.getBroadcastAddr(), 
					   CommSettings.getPort());  
				}
				else 
				 if(ending==false)
				  {
					 if(AudioSettings.useSpeex()==AudioSettings.USE_SPEEX)      //用户选择了默认值USE_SPEEX，即自己选择码率编码
				    	{   
						    isCoded="YES";
				    		encodedFrame = new byte[Speex.getEncodedSize(AudioSettings.getSpeexQuality())];//获得音频数据编码以后的大小
				    		recorder.read(pcmFrame, 0, Audio.FRAME_SIZE);    //把音频数据记录到缓存pcmFrame中
							Speex.encode(pcmFrame, encodedFrame);	//数据进行编码，结果放到enc
				    	}
				    	else 
				    	{  
				    		isCoded="NO";
							encodedFrame = new byte[Audio.FRAME_SIZE_IN_BYTES]; 
							recorder.read(encodedFrame, 0, Audio.FRAME_SIZE_IN_BYTES);
				    	}
						packet = new DatagramPacket(
								encodedFrame, 
								encodedFrame.length, 
								CommSettings.getBroadcastAddr(), 
								CommSettings.getPort());  
				   try {
					   if(outStream!=null)
					     outStream.write(encodedFrame);
				    } catch (IOException e1) 
				    {
					e1.printStackTrace();
				    }   //将编码后的语音信息存入文件中
				 
				 }
				else
				{
					String jieshu="END";
					try {
						int a= jieshu.getBytes("UTF8").length;
						encodedFrame=new byte[a];
						encodedFrame=jieshu.getBytes("UTF8");
					} catch (UnsupportedEncodingException e) 
					{
						e.printStackTrace();
					}
					packet = new DatagramPacket(
							encodedFrame, 
							encodedFrame.length, 
							CommSettings.getBroadcastAddr(), 
							CommSettings.getPort());
				}
				try 
				{	
					socket.send(packet);
					if(start==true)
					    start=false;
				    if(ending==true)
				    {
				    	//System.out.println("发送了标志着结束的语音包");
				    	recording = false;		
				    }
				}
				catch(Exception e) 
				{
					Log.error(getClass(), e);
				}	
				
			}		
			
			recorder.stop();
				
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
		
		socket.close();				
		recorder.release();
	}
	
	private void init() 
	{				
		try 
		{	    	
			IP.load();
			socket = new DatagramSocket();			
			InetAddress addr = null;	
			switch(CommSettings.getCastType()) 
			{
				case CommSettings.BROADCAST:
					 socket.setBroadcast(true);		
					 addr = CommSettings.getBroadcastAddr();
				     break;
				case CommSettings.MULTICAST:
					 addr = CommSettings.getMulticastAddr();					
				     break;
				case CommSettings.UNICAST:
					 addr = CommSettings.getUnicastAddr();					
				     break;
			}							
			
			if(AudioSettings.useSpeex()==AudioSettings.USE_SPEEX)      //用户选择了默认值USE_SPEEX，即自己选择码率编码
			{
				isCoded="YES";
				encodedFrame = new byte[Speex.getEncodedSize(AudioSettings.getSpeexQuality())];//获得音频数据编码以后的大小
			}
			else 
			{   isCoded="NO";
				encodedFrame = new byte[Audio.FRAME_SIZE_IN_BYTES]; 
			}
			packet = new DatagramPacket(
					encodedFrame, 
					encodedFrame.length, 
					addr, 
					CommSettings.getPort());                   //音频数据包，包括音频数据，数据长度值，目的地址，目的端口
	    	recorder = new AudioRecord(
	    			AudioSource.MIC, 
	    			Audio.SAMPLE_RATE, 
	    			AudioFormat.CHANNEL_CONFIGURATION_MONO, 
	    			Audio.ENCODING_PCM_NUM_BITS, 
	    			Audio.RECORD_BUFFER_SIZE);							
		}
		catch(SocketException e) 
		{
			Log.error(getClass(), e);
		}	
	}
	
	private synchronized boolean isRunning()
	{
		return running;
	}
	
	private synchronized boolean isRecording()
	{
		return recording;
	}
	
	public synchronized void pauseAudio() 
	{				
		if(outStream!=null)
		{
		 try {
				outStream.flush();
				outStream.close();
				outStream=null;
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		
		  if(fname!=myfile.getName())
		   {
			 fname=myfile.getName();
		     Main.mySqlHelper.inserAudiotData(Main.SqlDB,myfile.getName(),myfile.getAbsolutePath(),isCoded);
		   }
		ending=true;
		}	
	}	 
	public synchronized void setRecordFalse() 
	{	
		recording = false;
	}
	
	public synchronized void resumeAudio() 
	{	
		
		SimpleDateFormat formatter= new SimpleDateFormat("yyyyMMdd-HH-mm-ss-SSS");     
		Date curDate=new Date(System.currentTimeMillis());
		String time=formatter.format(curDate);
		String filepath="me--"+time;
		myfile=new File(Main.SDPATH,filepath);
		try {
			outStream = new FileOutputStream(myfile);
			
		} catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		}
		start=true;
		recording = true;
		ending=false;
		notify();
	}
				
	public synchronized void shutdown() 
	{
		recording = false;	
		running = false;				
		notify();
	}
	
	
}
