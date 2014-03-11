package ro.ui.pttdroid;
import android.app.Activity;  
import android.graphics.Bitmap;  
import android.os.Bundle;  
import android.widget.Toast;  
  
import com.baidu.mapapi.BMapManager;  
import com.baidu.mapapi.MKGeneralListener;  
import com.baidu.mapapi.map.MKEvent;  
import com.baidu.mapapi.map.MKMapViewListener;  
import com.baidu.mapapi.map.MapController;  
import com.baidu.mapapi.map.MapPoi;  
import com.baidu.mapapi.map.MapView;  
import com.baidu.platform.comapi.basestruct.GeoPoint;  
  
public class baiduMap extends Activity {  
    private Toast mToast;  
    BMapManager mBMapManager=null;   
    private MapView mMapView = null;               //MapView 是地图主控件 
   
    private MapController mMapController = null;   // 用MapController完成地图控制 
    MKMapViewListener mMapListener = null;         //MKMapViewListener 用于处理地图事件回调  
  
    @Override  
    protected void onCreate(Bundle savedInstanceState)
    {  
        super.onCreate(savedInstanceState);  
        // 使用地图sdk前需先初始化BMapManager，这个必须在setContentView()先初始化    
        mBMapManager = new BMapManager(getApplication());                          
        //第一个参数是API key,   
        //第二个参数是常用事件监听，用来处理通常的网络错误，授权验证错误等，可以不添加这个回调接口   
        mBMapManager.init("Pudm6jQ0AsGE3qgXhHmCqei4", new MKGeneralListener() 
        {       
            //授权错误的时候调用的回调函数   
            public void onGetPermissionState(int iError)
            {  
                if (iError ==  MKEvent.ERROR_PERMISSION_DENIED) 
                {  
                    showToast("API KEY错误, 请检查！");  
                }  
            }  
              
            //一些网络状态的错误处理回调函数   
            public void onGetNetworkState(int iError)
            {  
                if (iError == MKEvent.ERROR_NETWORK_CONNECT)
                {  
                    Toast.makeText(getApplication(), "您的网络出错啦！", Toast.LENGTH_LONG).show();  
                }  
            }  
        });  
          
        setContentView(R.layout.baidumap);      
        mMapView = (MapView) findViewById(R.id.bmap);  
        mMapController = mMapView.getController();   
        mMapController.enableClick(true); // 设置地图是否响应点击事件  . 
        mMapController.setZoom(12);  //设置地图缩放级别 
        mMapView.setBuiltInZoomControls(true);  //显示内置缩放控件 
        //保存精度和纬度的类, 
        GeoPoint p =  new GeoPoint((int) (39.915 * 1E6),(int) (116.404 * 1E6));
        mMapController.setCenter(p);  
        mMapView.regMapViewListener(mBMapManager, new MKMapViewListener() {  
              
            /** 
             * 地图移动完成时会回调此接口 方法 
             */   
            public void onMapMoveFinish()
            {  
              
            }  
              
            /** 
             * 地图加载完毕回调此接口方法 
             */  
            public void onMapLoadFinish() 
            {  
                showToast("地图载入完毕！");  
            }  
              
            /** 
             *  地图完成带动画的操作（如: animationTo()）后，此回调被触发 
             */  
            public void onMapAnimationFinish() 
            {  
                  
            }  
              
            /** 
             *  当调用过 mMapView.getCurrentMap()后，此回调会被触发 
             *  可在此保存截图至存储设备 
             */  
            public void onGetCurrentMap(Bitmap arg0) 
            {  
                  
            }  
              
            /** 
             * 点击地图上被标记的点回调此方法 
             *  
             */  
            public void onClickMapPoi(MapPoi arg0)
            {  
                if (arg0 != null){  
                    showToast(arg0.strText);  
                }  
            }  
        });  
    }  
    @Override  
    protected void onResume() 
    {  
        mMapView.onResume();  
        super.onResume();  
    }  
    @Override  
    protected void onPause()
    {    
        mMapView.onPause();  
        super.onPause();  
    }  
  
    @Override  
    protected void onDestroy() 
    {     
        mMapView.destroy();          
        if(mBMapManager != null)
        {  
            mBMapManager.destroy();  
            mBMapManager = null;  
        }  
          
        super.onDestroy();  
    }  
     /**  
     * 显示Toast消息  
     * @param msg  
     */    
    private void showToast(String msg)
    {    
        if(mToast == null)
        {    
            mToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);    
        }else{    
            mToast.setText(msg);    
            mToast.setDuration(Toast.LENGTH_SHORT);  
        }    
        mToast.show();    
    }   
      
      
}  
