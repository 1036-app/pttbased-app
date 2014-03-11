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
    private MapView mMapView = null;               //MapView �ǵ�ͼ���ؼ� 
   
    private MapController mMapController = null;   // ��MapController��ɵ�ͼ���� 
    MKMapViewListener mMapListener = null;         //MKMapViewListener ���ڴ����ͼ�¼��ص�  
  
    @Override  
    protected void onCreate(Bundle savedInstanceState)
    {  
        super.onCreate(savedInstanceState);  
        // ʹ�õ�ͼsdkǰ���ȳ�ʼ��BMapManager�����������setContentView()�ȳ�ʼ��    
        mBMapManager = new BMapManager(getApplication());                          
        //��һ��������API key,   
        //�ڶ��������ǳ����¼���������������ͨ�������������Ȩ��֤����ȣ����Բ��������ص��ӿ�   
        mBMapManager.init("Pudm6jQ0AsGE3qgXhHmCqei4", new MKGeneralListener() 
        {       
            //��Ȩ�����ʱ����õĻص�����   
            public void onGetPermissionState(int iError)
            {  
                if (iError ==  MKEvent.ERROR_PERMISSION_DENIED) 
                {  
                    showToast("API KEY����, ���飡");  
                }  
            }  
              
            //һЩ����״̬�Ĵ�����ص�����   
            public void onGetNetworkState(int iError)
            {  
                if (iError == MKEvent.ERROR_NETWORK_CONNECT)
                {  
                    Toast.makeText(getApplication(), "���������������", Toast.LENGTH_LONG).show();  
                }  
            }  
        });  
          
        setContentView(R.layout.baidumap);      
        mMapView = (MapView) findViewById(R.id.bmap);  
        mMapController = mMapView.getController();   
        mMapController.enableClick(true); // ���õ�ͼ�Ƿ���Ӧ����¼�  . 
        mMapController.setZoom(12);  //���õ�ͼ���ż��� 
        mMapView.setBuiltInZoomControls(true);  //��ʾ�������ſؼ� 
        //���澫�Ⱥ�γ�ȵ���, 
        GeoPoint p =  new GeoPoint((int) (39.915 * 1E6),(int) (116.404 * 1E6));
        mMapController.setCenter(p);  
        mMapView.regMapViewListener(mBMapManager, new MKMapViewListener() {  
              
            /** 
             * ��ͼ�ƶ����ʱ��ص��˽ӿ� ���� 
             */   
            public void onMapMoveFinish()
            {  
              
            }  
              
            /** 
             * ��ͼ������ϻص��˽ӿڷ��� 
             */  
            public void onMapLoadFinish() 
            {  
                showToast("��ͼ������ϣ�");  
            }  
              
            /** 
             *  ��ͼ��ɴ������Ĳ�������: animationTo()���󣬴˻ص������� 
             */  
            public void onMapAnimationFinish() 
            {  
                  
            }  
              
            /** 
             *  �����ù� mMapView.getCurrentMap()�󣬴˻ص��ᱻ���� 
             *  ���ڴ˱����ͼ���洢�豸 
             */  
            public void onGetCurrentMap(Bitmap arg0) 
            {  
                  
            }  
              
            /** 
             * �����ͼ�ϱ���ǵĵ�ص��˷��� 
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
     * ��ʾToast��Ϣ  
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
