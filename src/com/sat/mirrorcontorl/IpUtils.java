package com.sat.mirrorcontorl;


import java.io.File;
import java.io.FileInputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.util.Log;

public class IpUtils {



	public static String getIpAddress(Context context) {
		ConnectivityManager conMann = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		String ip = null;

		NetworkInfo mobileNetworkInfo = conMann
				.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
		NetworkInfo wifiNetworkInfo = conMann
				.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

		if (mobileNetworkInfo != null && mobileNetworkInfo.isConnected()) {
			ip = getLocalIpAddress();
		} else if (wifiNetworkInfo != null && wifiNetworkInfo.isConnected()) {

			WifiInfo wifiInfo = wifiManager.getConnectionInfo();
			int ipAddress = wifiInfo.getIpAddress();
			ip = intToIp(ipAddress);
		}

		return ip;
	}


	public static String getLocalIpAddress() {    
        try {    
            String ipv4;    
            ArrayList<NetworkInterface>  nilist = Collections.list(NetworkInterface.getNetworkInterfaces());    
            for (NetworkInterface ni: nilist)     
            {    
                ArrayList<InetAddress>  ialist = Collections.list(ni.getInetAddresses());    
                for (InetAddress address: ialist){    
                    if ((!address.isLoopbackAddress()) && (address instanceof Inet4Address))     
                    {     
                        return address.getHostAddress();    
                    }    
                }    
     
            }    
     
        } catch (SocketException ex) {    
            Log.e("localip", ex.toString());    
        }    
        return null;    
    }
	
	public static String intToIp(int ipInt) {    
        StringBuilder sb = new StringBuilder();    
        sb.append(ipInt & 0xFF).append(".");    
        sb.append((ipInt >> 8) & 0xFF).append(".");    
        sb.append((ipInt >> 16) & 0xFF).append(".");    
        sb.append((ipInt >> 24) & 0xFF);    
        return sb.toString();    
    }
	
	public static InetAddress getBroadcastAddress(Context context) throws UnknownHostException {
	    WifiManager wifi = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
	    DhcpInfo dhcp = wifi.getDhcpInfo();
	    if(dhcp==null) {
	        return InetAddress.getByName("255.255.255.255");
	    }
	    int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
	    byte[] quads = new byte[4];
	    for (int k = 0; k < 4; k++){
	        quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
	    }
	    
	    return InetAddress.getByAddress(quads);//
	   //  return InetAddress.getByName("192.168.43.255");

	}

	public static MulticastLock openWifiBrocast(Context context){  
  		WifiManager wifiManager=(WifiManager)context.getSystemService(Context.WIFI_SERVICE);  
  		MulticastLock  multicastLock=wifiManager.createMulticastLock("MediaRender");  
  		if (multicastLock != null){  
  			multicastLock.acquire();  
  		}  
  		return multicastLock;  
  	}
	
	
	public static String getSerialNumber() { 
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
    		return "RSE_"+substring;
		}
    	
    	return "RSE"+serial; 
    	}
}
