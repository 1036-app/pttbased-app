package ro.ui.pttdroid;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.util.EncodingUtils;

import ro.ui.pttdroid.Main.MyHandler;
import ro.ui.pttdroid.codecs.Speex;
import ro.ui.pttdroid.settings.AudioSettings;
import ro.ui.pttdroid.util.Audio;

import android.app.Activity;
import android.app.ListActivity;
import android.content.res.ColorStateList;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Environment;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class SearchAudioFiles extends ListActivity {
	 public LinearLayout myListViewlayout;
     public ListView mListView=null;
     public SimpleAdapter adapter=null;
     public  int MID;
     // ����һ��List������������б����ÿһ��map��Ϣ
    ArrayList<Map<String, String>> list = new ArrayList<Map<String, String>>();
	private AudioTrack player = null; // ��������������
	public PlayThread pt = null;
	public String filename = "";
	public Runnable updateWarning = null;
	public boolean isRun = true;
	public boolean is = true;
	public TextView sendName=null;
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
          super.onCreate(savedInstanceState);
          setContentView(R.layout.search);
          mListView = (ListView) findViewById(R.id.mylist);
          //���������ò�ͬ������ɫ????
         // sendName= (TextView)findViewById(R.id.send_name); 
         // sendName.setTextColor(Color.BLUE);  
    		list=Main.mySqlHelper.queryAudioData(Main.SqlDB,list); 
    		loaddata();
            mListView.setOnItemClickListener(new OnItemClickListener() {  
                public void onItemClick(AdapterView<?> parent, View view,  
                        int position, long id)
                {   
                	Map<String, String> map= new HashMap<String, String>();
                	map= list.get(position);
                	filename=map.get("filename");
                	playAudioData(filename);  
                }  
            });  
             
            ItemOnLongClick();
            
           
    }
    public void loaddata()
    {
        adapter = new SimpleAdapter(SearchAudioFiles.this,
                   list, R.layout.audiolist, 
                   new String[] { "name", "filename"},
                   new int[] { R.id.send_name, R.id.file });
        mListView.setAdapter(adapter);
    }
  public void ItemOnLongClick() 
  {  
	  
       //setOnCreateContextMenuListener��������onContextItemSelected����ʹ�õ�
       mListView .setOnCreateContextMenuListener(new OnCreateContextMenuListener()
       {
    	   
         public void onCreateContextMenu(ContextMenu menu, View v,
                                        ContextMenuInfo menuInfo) 
         {
        	 menu.setHeaderTitle("Files Operation");  
               menu.add(0,0,0,"Play");
               menu.add(0,1,0,"Delete");
               menu.add(0,2,0,"DeleteALL");

        }
     });
     
    }
  
  /*
   * �����˵�����Ӧ����
   * 
   * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
   */
  @Override  
  public boolean onContextItemSelected(MenuItem item) 
  {
      AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
                          .getMenuInfo();
      MID = (int) info.id;          // �����info.id��Ӧ�ľ������ݿ���_id��ֵ
      Map<String, String> map= new HashMap<String, String>();
      map= list.get(MID);
      filename=map.get("filename");
      switch(item.getItemId()) 
      {
          case 0:   //������Ƶ����
                  playAudioData(filename);
                  break;

          case 1:   //ɾ��ָ������Ƶ����
        	      File f = new File(Main.SDPATH, filename); 
        	      if (f.exists())
        	    	{
        	    	  f.delete();
        	    	  Main.mySqlHelper.deleteSelectedData(Main.SqlDB,filename);
        	    	  Toast.makeText(this, "The file has deleted", Toast.LENGTH_LONG).show();
        	    	  list.remove(MID);
        	    	  loaddata();
        	    	}
                  break;

          case 2:   //ɾ��ȫ������Ƶ����
        	      Main.mySqlHelper.deleteAudioData(Main.SqlDB);
        	      File fi=null;
        	      String status = Environment.getExternalStorageState();
        	      if (status.equals(Environment.MEDIA_MOUNTED))  
        	        {
        	         fi=Environment.getExternalStorageDirectory();
        	        }
        	        else                              
        	        {
        	        fi=getFilesDir(); 
        	        } 
        	      if (fi.isDirectory())
              	   {  
                      File[] childFile = fi.listFiles();  
                      if (childFile == null || childFile.length == 0)
                      {  
                      } 
                      else
                      {
                      	 for (File fg : childFile)
                           {  
                             fg.delete();  
                           }  
                      }
                  } 
        	      Toast.makeText(this, "ALL files have deleted", Toast.LENGTH_LONG).show();  
        	      list.removeAll(list);
    	    	  loaddata();
        	      break;

          default:
                  break;
          }

          return super.onContextItemSelected(item);

  }
  /**
   * ������Ƶ�ļ��ĺ���
   * @param filename
   */
    public void playAudioData(String filename)
    {
    	
    	File f = new File(Main.SDPATH, filename);   // �൱��ֻ�ǽ����� ���ã�Ҫ�����������õ���
                                               // file.createNewFile()������
    	if (f.exists())
    	{
    	   if (player != null&& player.getPlayState() == player.PLAYSTATE_PLAYING)
    	  {
    	   pt.stopThread();
    	   }
    	   while (true) // �������ڲ��ŵ�����ʲôʱ�����
    	   {
    	     if (is == true) // ����һ���������Ž���ʱ���ſ��Բ�����һ��
    	     {
    	        pt = new PlayThread(f);
    	        pt.start();
    	        is = false;
    	        return;
    	      }
    	  }
    	} 
    	else 
    	{
    	   Toast toast = Toast.makeText(SearchAudioFiles.this,
    	             "The file has been deleted", Toast.LENGTH_SHORT);
    	   toast.setGravity(Gravity.CENTER, 0, 0);
    	   toast.show();
    	}
    }

	public class PlayThread extends Thread 
	{
		private File file;

		PlayThread(File f) {
			file = f;
		}

		@Override
		public void run() {
			super.run();

			player = new AudioTrack(AudioManager.STREAM_MUSIC,
					Audio.SAMPLE_RATE, AudioFormat.CHANNEL_CONFIGURATION_MONO,
					Audio.ENCODING_PCM_NUM_BITS, Audio.TRACK_BUFFER_SIZE,
					AudioTrack.MODE_STREAM);
			player.play();
			try {
				byte[] array = null;
				short[] pcmFrame = new short[Audio.FRAME_SIZE];
				array = new byte[Speex.getEncodedSize(AudioSettings
						.getSpeexQuality())];
				FileInputStream fin = new FileInputStream(file);
				fin.read(array);
				isRun = true;
				while (isRun) 
				{
					Speex.decode(array, array.length, pcmFrame);
					player.write(pcmFrame, 0, Audio.FRAME_SIZE);

					if ((fin.read(array)) == -1)
						break;
				}
				fin.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			player.stop();
			player.release();
			is = true;
		}

		public void stopThread() {
			isRun = false;
		}

		@Override
		public void destroy() {
			super.destroy();
		}
	}

	@Override
	protected void onDestroy() {

		isRun = false;
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		isRun = false;
		super.onPause();
	}

}
