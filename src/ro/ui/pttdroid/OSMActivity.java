package ro.ui.pttdroid;

import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import android.app.Activity;
import android.os.Bundle;

public class OSMActivity extends Activity 
{
	public MapView mMapView =null;
	public IMapController mController = null;
	public DefaultResourceProxyImpl mResourceProxy=null;
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{   
		super.onCreate(savedInstanceState);
		setContentView(R.layout.osmmap); 
		mMapView = (MapView) findViewById(R.id.mapview);
	    mResourceProxy = new DefaultResourceProxyImpl(this); 
	    mMapView.setTileSource(TileSourceFactory.MAPNIK);  
	    mMapView.setBuiltInZoomControls(true); //显示内置缩放控件 
	    mMapView.setMultiTouchControls(true); 
	    mController = mMapView.getController();
	    GeoPoint center=  new GeoPoint((int) (39.945 * 1E6),(int) (116.404 * 1E6));
	    //GeoPoint center = new GeoPoint(39.901873, 116.326655);
        mController.setCenter(center);  
        mController.setZoom(12);  //设置地图缩放级别 

	}

}
