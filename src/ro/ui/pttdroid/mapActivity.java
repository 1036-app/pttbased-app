package ro.ui.pttdroid;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;


import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;

public class mapActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.googlemap);
    }
}

