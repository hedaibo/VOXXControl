package com.sat.mirrorcontorl;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

public class NetBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		boolean isNetConnected = false;
		ConnectivityManager mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	//	NetworkInfo mobileInfo = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);  
        NetworkInfo wifiInfo = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);  
   //     NetworkInfo activeInfo = mConnectivityManager.getActiveNetworkInfo();
        
//        if (mobileInfo.isConnected()) {
//        	isNetConnected = true;
//		}
        
        if (wifiInfo.isConnected()) {
        	isNetConnected = true;
		}
        
//        if (activeInfo.isConnected()) {
//        	isNetConnected = true;
//		}
      //  Toast.makeText(context, "wifi:"+wifiInfo.isConnected(), 1).show();
        
        /*if (isNetConnected) {
        	MediaCodeService service = MediaCodeService.getService();
            if (service == null) {
            	context.startService(new Intent(context, MediaCodeService.class));
    		}
		}*/

	}

}
