package com.sat.mirrorcontorl;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity  {

	private static final String TAG = "MainActivity";
	private static MainActivity mActivity;
	private Button btGo;
	private LinearLayout lLWifi;
	private LinearLayout lLInfo;
	private TextView tvDevice;
	private TextView tvWifi;
	
	public static MainActivity getInstance(){
		return mActivity;
	} 
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mActivity = this;
        MediaCodeService service = MediaCodeService.getService();
        if (service == null) {
        	startService(new Intent(this, MediaCodeService.class));
		}
        
        initView();
    }
    private void initView() {
		btGo = (Button) findViewById(R.id.bt_connet_wifi);
		lLWifi = (LinearLayout) findViewById(R.id.ll_connet_wifi);
		lLInfo = (LinearLayout) findViewById(R.id.ll_wifi_info);
		tvDevice = (TextView) findViewById(R.id.tv_device_info);
		tvWifi = (TextView) findViewById(R.id.tv_wifi_info);
		
		btGo.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				startActivityformComponent("com.android.settings","com.android.settings.Settings");
			}
		});
	}

	@Override
    protected void onStart() {
    	super.onStart();
    	ConnectivityManager mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo wifiInfo = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (wifiInfo.isConnected()) {
        	String typeName = wifiInfo.getTypeName();
        	String extraInfo = wifiInfo.getExtraInfo();
        	String reason = wifiInfo.getReason();
        	Log.i(TAG, "hdb---typeName:"+typeName+"  extraInfo:"+extraInfo+"  reason:"+reason);
        	
        	isConnet(extraInfo);
		}else {
			Log.i(TAG, "hdb---wifiInfo:"+wifiInfo);
			isNotConnet();
		}
    }
    
    public void wifiHasConnet(String wifiName){
    	isConnet(wifiName);
    }
    
    public void wifiNotConnet(){
    	isNotConnet();
    }
    
    private void isNotConnet(){
    	if (lLInfo != null && lLWifi != null) {
    		lLInfo.setVisibility(View.GONE);
        	lLWifi.setVisibility(View.VISIBLE);
		}
    }
    
    private void isConnet(String wifiName){
    	if (lLInfo != null && lLWifi != null) {
    		lLInfo.setVisibility(View.VISIBLE);
        	lLWifi.setVisibility(View.GONE);
        	String wifi = getString(R.string.tv_cureent_wifi);
        	String device = getString(R.string.tv_info);
        	tvWifi.setText(wifi + "  "+wifiName);
        	//tvDevice.setText(device + )
        	tvDevice.setText(device + "  RSE_"+getSerialNumber());
		}
    }
    
    
    
    /*@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK || requestCode != REQUEST_CODE) return;
        mediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);
        if (mediaProjection == null) {
            return;
        }
        DisplayMetrics dm = getResources().getDisplayMetrics();
        screenCaputre = new ScreenCaputre(dm.widthPixels, dm.heightPixels, mediaProjection);
        screenCaputre.setScreenCaputreListener(this);
        screenCaputre.start();
    }*/
    
    
    private void startActivityformComponent(String pkg, String cls) {
		Intent intent = new Intent();
		ComponentName component = new ComponentName(pkg, cls);
		intent.setComponent(component);
		PackageManager pManager = mActivity.getPackageManager();
		List<ResolveInfo> queryIntentActivities = pManager
				.queryIntentActivities(intent, 0);
		if (queryIntentActivities.size() > 0) {
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
		} else {
			Toast.makeText(mActivity, R.string.toast_appuninstall,
					Toast.LENGTH_SHORT).show();
		}

	}
    
    //startActivityformComponent("com.android.settings","com.android.settings.Settings");

    
    private static String getSerialNumber() { 
    	FileInputStream is; 
    	String serial = ""; 
    	byte[] buffer = new byte[16]; 
    	try { 
    	is = new FileInputStream(new File("/sys/devices/platform/cpu-id/chip_id")); 
    	is.read(buffer); 
    	is.close(); 
    	serial = new String(buffer); 
    	} catch (Exception e) { 
    	e.printStackTrace(); 
    	} 

    	if (serial.length() > 11) {
    		String substring = serial.substring(4, 11);
    		return substring;
		}
    	
    	return serial; 
    	}
}
