package ro.ui.pttdroid;

import ro.ui.pttdroid.Main.MyHandler;
import android.app.Application;

public class myApplication extends Application
{
	    // �������  
	    private MyHandler handler = null;  
	    public void setHandler(MyHandler handler) 
	    {  
	        this.handler = handler;  
	    }  
	      
	    public MyHandler getHandler() 
	    {  
	        return handler;  
	    }  
}  

