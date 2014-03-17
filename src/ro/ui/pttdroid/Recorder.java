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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
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
	public FileOutputStream outStream = null;
	public File myfile = null;
	public String fname = null;
	public boolean ending = false;
    public byte[] fileData=null;
    InetAddress addr = null;
	public void run() 
	{
		android.os.Process
				.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

		while (isRunning()) 
		{
			init();
			recorder.startRecording();
			while (isRecording()) 
			{
				
			      encodedFrame = new byte[Speex
								.getEncodedSize(AudioSettings.getSpeexQuality())];// 获得音频数据编码以后的大小
			      recorder.read(pcmFrame, 0, Audio.FRAME_SIZE); // 把音频数据记录到缓存pcmFrame中
						Speex.encode(pcmFrame, encodedFrame); // 数据进行编码，结果放到encodedFrame
					
					packet = new DatagramPacket(encodedFrame,
							encodedFrame.length,
							CommSettings.getBroadcastAddr(),
							CommSettings.getPort());
					try {
						if (outStream != null)
							outStream.write(encodedFrame);
					} catch (IOException e1) 
					{
						e1.printStackTrace();
					} // 将编码后的语音信息存入文件中
			
				try {
					socket.send(packet);
				} catch (Exception e) 
				{
					Log.error(getClass(), e);
				}

			}
			recorder.stop();

			synchronized (this) 
			{
				try {
					if (isRunning())
						wait();
				} catch (InterruptedException e) 
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
		try {
			IP.load();
			socket = new DatagramSocket();
			
			switch (CommSettings.getCastType()) 
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

		   encodedFrame = new byte[Speex.getEncodedSize(AudioSettings
						.getSpeexQuality())];// 获得音频数据编码以后的大小
			
			packet = new DatagramPacket(encodedFrame, encodedFrame.length,
					addr, CommSettings.getPort()); // 音频数据包，包括音频数据，数据长度值，目的地址，目的端口
			recorder = new AudioRecord(AudioSource.MIC, Audio.SAMPLE_RATE,
					AudioFormat.CHANNEL_CONFIGURATION_MONO,
					Audio.ENCODING_PCM_NUM_BITS, Audio.RECORD_BUFFER_SIZE);
		} catch (SocketException e)
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
		if (outStream != null) {
			try {
				outStream.flush();
				outStream.close();
				outStream = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			if (fname != myfile.getName()) 
			{
				fname = myfile.getName();
				Main.mySqlHelper.inserAudiotData(Main.SqlDB,"me", myfile.getName(),
						myfile.getAbsolutePath());
				if(CommSettings.getCastType()==CommSettings.UNICAST)
				{
				    TCPSendFiles tcs=new TCPSendFiles(myfile,addr);
				    tcs.start();
				    recording = false;
				}
				
				else if(CommSettings.getCastType()==CommSettings.BROADCAST&&Main.allIP.size()!=0)
				{
				
					System.out.println("所在子网有多少个IP在线"+Main.allIP.size());
					for(int i=0;i<Main.allIP.size();i++)
					{
					  String ip=null;
					  ip=Main.allIP.get(i);
					  ip=ip.substring(1);
					   InetAddress address=null;
					   try {
						address = InetAddress.getByName(ip);
					   } catch (UnknownHostException e) 
					   {
						e.printStackTrace();
					   }
					   TCPSendFiles tcs=new TCPSendFiles(myfile,address);
				       tcs.start();
					}
					recording = false;	
				}
				else if(CommSettings.getCastType()==CommSettings.MULTICAST)
				{ 
					//组播语音文件怎么发
					recording = false;
				}
			}
			
		}
	}

	public synchronized void setRecordFalse() {
		recording = false;
	}

	public synchronized void resumeAudio() 
	{

		SimpleDateFormat formatter = new SimpleDateFormat(
				"yyyyMMdd-HH-mm-ss-SSS");
		Date curDate = new Date(System.currentTimeMillis());
		String time = formatter.format(curDate);
		String filepath = time;
		myfile = new File(Main.SDPATH, filepath);
		try {
			outStream = new FileOutputStream(myfile);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		recording = true;
		notify();
	}

	public synchronized void shutdown()
	{
		recording = false;
		running = false;
		notify();
	}
	public class TCPSendFiles extends Thread
	{
       public Socket TCPsocket =null;
       public File file=null;
       public InetAddress address=null;
       TCPSendFiles (File f,InetAddress address)
       {
    	   this.file=f;
    	   this.address=address;
       }
		@Override
		public void run()
		{
			try {
				byte[] filedata=new byte[256];
				TCPsocket = new Socket(address, CommSettings.getPort());
				OutputStream out = TCPsocket.getOutputStream(); // 输出流
				FileInputStream fin=new FileInputStream(file);
				int aa=0;
				while(aa!=-1)
				{
				aa=fin.read(filedata);
				out.write(filedata);
				}
				out.flush(); // 关闭输出流
				TCPsocket.close();
			} catch (IOException e1) 
			{
				e1.printStackTrace();
			}
		}
	}
}
