package com.sat.mirrorcontorl;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.json.JSONObject;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.DisplayManager;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.InputDevice;
import android.view.MotionEvent;

public class MediaCodeService extends Service implements
		ScreenCaputre.ScreenCaputreListener {

	protected static final String TAG = "MediaCodeService";

	protected static final int CONNECT_SUCCESS = 1;

	protected static final int CONNECT_FAIL = 2;

	protected static final int CLEAR_FAILCOUNT = 3;

	private static final int RESTART_SERVER = 4;

	private static final String JACTION = "action";
	private static final String JX = "x";
	private static final String JY = "y";
	
	private static final int MULTIPORT = 9696;
	private static final int DATAPORT = 8686;
	private static final int TOUCHPORT = 8181;
	private static final int BACKPORT = 9191;

	// private Instrumentation inst;
	private EventInput inst;
	private ScreenCaputre screenCaputre;

	private static MediaCodeService mService;

	public static MediaCodeService getService() {
		return mService;
	}

	private Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case CONNECT_SUCCESS:
				receiver = false;
				startCaputre();
				break;
			case CONNECT_FAIL:
				closeConnect();
				break;
			case CLEAR_FAILCOUNT:
				failCount = 0;
				break;
			case RESTART_SERVER:

//				startDataServer();
//		        startTouchServer();
				receiver = true;
				receiverBroadPackage();

				break;
//			case RECEIVER_BROADPACKAGE:
//				receiverBroadPackage();
//				break;

			default:
				break;
			}
		};
	};


	private InetAddress broadcastAddress;


	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mService = this;

		
	}
	
	private void startDataAndTouchServer(){
		startDataServer();
        startTouchServer();
	}
	
	public static InetAddress getBroadcastAddress(Context context) throws UnknownHostException {
	    WifiManager wifi = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
	    DhcpInfo dhcp = wifi.getDhcpInfo();
	    if(dhcp==null) {
	        return InetAddress.getByName("255.255.255.255");
	    }
	    /*int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
	    byte[] quads = new byte[4];
	    for (int k = 0; k < 4; k++){
	        quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
	    }
	    
	    return InetAddress.getByAddress(quads);*/
	    String ipAddress2 = IpUtils.getIpAddress(context);
	    if (ipAddress2 != null && !ipAddress2.equals("")) {
            String substring = ipAddress2.substring(0, ipAddress2.lastIndexOf(".") + 1);
            Log.i("123", "hdb------substring:"+substring);
            return InetAddress.getByName(substring+"255");
       }
		return null;
	}
	

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// inst = new Instrumentation();
		try {
			inst = new EventInput();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		registNetChangeReceiver();

		return START_STICKY;
	}
	
	private void startReceiverUdpBrodcast(){
		try {
			broadcastAddress = getBroadcastAddress(getApplicationContext());
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}
		
		new Thread() {

			public void run() {
				try {
					if (multicastSocket == null) {
						multicastSocket = new MulticastSocket(MULTIPORT);
					}
					multicastSocket.joinGroup(broadcastAddress);
					
				} catch (Exception e) {
					e.getStackTrace();
				}
			}
		}.start();
		
        ipAddress = IpUtils.getIpAddress(getApplicationContext());
        Log.i(TAG, "hdb----ipAddress:"+ipAddress);
        if (ipAddress != null && broadcastAddress != null) {
        	receiver = true;
			receiverBroadPackage();
		}
	}
	
	
	private NetChangeReceiver netChangeReceiver = null;
	private boolean isNetConnet = false;
	private void registNetChangeReceiver() {
		if (netChangeReceiver == null) {
			netChangeReceiver = new NetChangeReceiver();
			IntentFilter filter = new IntentFilter();
			filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
			registerReceiver(netChangeReceiver, filter );
		}
		
		
	}

	private boolean receiver = false;
	private String ipAddress ;
	private void receiverBroadPackage() {
		IpUtils.openWifiBrocast(getApplicationContext()); //for some phone can not send broadcast
		new Thread(){

			public void run(){
				try {
					while(receiver){
						if (multicastSocket != null) {
							Log.i(TAG, "hdb----ipAddress:"+ipAddress);
							byte[] data = new byte[30];
							DatagramPacket pack = new DatagramPacket(data, data.length);
							multicastSocket.receive(pack);
							String phoneIP = new String(pack.getData(),pack.getOffset(),pack.getLength());
							Log.i(TAG, "hdb-------phoneIP:"+phoneIP+"  ipAddress:"+ipAddress);
							if (phoneIP != null && phoneIP.startsWith("phoneip:")) {
								sendBack(phoneIP);
							}else if ("over".equals(phoneIP)) {
								if (isStart == false) {
									isStart = true;
									startDataAndTouchServer();
								}
								
							}
						}
					}
					
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.start();
		
	}
	
	private boolean isStart = false;
	private DatagramSocket bSocket;
	
	protected void sendBack(String iP) throws SocketException {
		if (bSocket == null) {
			bSocket = new DatagramSocket();
		}
		byte[] data = ("serverip:"+ipAddress+":"+IpUtils.getSerialNumber()).getBytes();
		try {
			String phoneIp = iP.substring(8);
			Log.i(TAG, "hdb-----phoneIp:"+phoneIp+":"+IpUtils.getSerialNumber());
			InetAddress host = InetAddress.getByName(phoneIp);
			DatagramPacket pack = new DatagramPacket(data , data.length, host, BACKPORT);
			bSocket.send(pack);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}
	
	private MulticastSocket multicastSocket;
	


	private DataOutputStream os;
	private DataInputStream disTouch;
	private Socket socketD;
	private ServerSocket dataSockets;

	private ServerSocket touchSockets;
	private Socket socketT;

	private boolean isRun = true;

	private void startDataServer() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					dataSockets = new ServerSocket(8686);
					socketD = dataSockets.accept();
					// os = socketD.getOutputStream();
					os = new DataOutputStream(socketD.getOutputStream());
					if (os != null) {
						mHandler.sendEmptyMessageDelayed(CONNECT_SUCCESS, 500);
					}
				} catch (Exception ex) {
					mHandler.sendEmptyMessage(CONNECT_FAIL);
					ex.toString();
				}
			}
		}).start();
	}

	private void startTouchServer() {
		new Thread(new Runnable() {

			private byte[] buffer;

			@SuppressWarnings("deprecation")
			@Override
			public void run() {
				try {
					touchSockets = new ServerSocket(8181);
					socketT = touchSockets.accept();

					disTouch = new DataInputStream(socketT.getInputStream());
					isRun = true;
					Log.i(TAG, "hdb-------isRun:" + isRun + "  socket:"
							+ socketT.getInetAddress());
					while (isRun) {
						buffer = new byte[1];
						int readLine = disTouch.read(buffer);

						Log.i(TAG, "hdb-------readLine:" + readLine
								+ "   buffer:" + buffer[0]);

						if (readLine > 0) {

							byte[] data = new byte[buffer[0]];
							Log.i(TAG, "hdb--------data:" + new String(data));
							disTouch.readFully(data);
							String point = new String(data, 0, data.length);

							if (point != null) {
								Log.i(TAG, "hdb--------point:" + point);
								JSONObject jObject = new JSONObject(point);
								int action = jObject.getInt(JACTION);
								int x = jObject.getInt(JX);
								int y = jObject.getInt(JY);
								if (action == 0) {
									// String[] split = arseInt(split[2]);
									// up(x, y);point.split(":");
									// int x = Integer.parseInt(split[1]);
									// int y = Integer.p

									inst.injectMotionEvent(
											InputDevice.SOURCE_TOUCHSCREEN, 0,
											SystemClock.uptimeMillis(), x, y,
											1.0f);

								} else if (action == 2) {
									// String[] split = point.split(":");
									// int x = Integer.parseInt(split[1]);
									// int y = Integer.parseInt(split[2]);
									// move(x, y);
									inst.injectMotionEvent(
											InputDevice.SOURCE_TOUCHSCREEN, 2,
											SystemClock.uptimeMillis(), x, y,
											1.0f);
								} else if (action == 1) {
									// String[] split = point.split(":");
									// int x = Integer.parseInt(split[1]);
									// int y = Integer.parseInt(split[2]);
									// up(x, y);
									inst.injectMotionEvent(
											InputDevice.SOURCE_TOUCHSCREEN, 1,
											SystemClock.uptimeMillis(), x, y,
											1.0f);
								}
							}
						} else {
							failCount++;
							if (failCount == 1) {
								mHandler.sendEmptyMessageDelayed(
										CLEAR_FAILCOUNT, 300);
							}
							if (failCount > 5) {
								failCount = 0;
								mHandler.sendEmptyMessage(CONNECT_FAIL);
							}
						}

					}
					mHandler.sendEmptyMessage(CONNECT_FAIL);
				} catch (Exception ex) {
					Log.e(TAG, "hdb---1ex:" + ex.toString());
				}
			}
		}).start();
	}

	private int failCount = 0;

	public void startCaputre() {
		isRun = true;
		if (null == screenCaputre) {
			prepareScreen();
		} else {
			screenCaputre.start();
		}
	}

	public void stopCaputre() {
		screenCaputre.stop();
	}

	public void prepareScreen() {
		/*
		 * mMediaProjectionManager = (MediaProjectionManager)
		 * getSystemService(MEDIA_PROJECTION_SERVICE); Intent captureIntent =
		 * mMediaProjectionManager.createScreenCaptureIntent();
		 * startActivityForResult(captureIntent, REQUEST_CODE);
		 */

		DisplayManager mDisplayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);

		if (mDisplayManager == null) {
			return;
		}
		DisplayMetrics dm = getResources().getDisplayMetrics();
		screenCaputre = new ScreenCaputre(dm.widthPixels, dm.heightPixels,
				mDisplayManager);
		screenCaputre.setScreenCaputreListener(this);
		screenCaputre.start();
	}

	@Override
	public void onImageData(byte[] buf) {
		Log.v(TAG, "onImageData  " + buf.length + "  ------  " + os);
		if (null != os) {
			try {
				byte[] bytes = new byte[buf.length + 3];
				byte[] head = intToBuffer(buf.length);
				System.arraycopy(head, 0, bytes, 0, head.length);
				System.arraycopy(buf, 0, bytes, head.length, buf.length);
				os.write(bytes);
				os.flush();

				bytes = null;
				head = null;
			} catch (IOException e) {
				e.printStackTrace();
				mHandler.sendEmptyMessage(CONNECT_FAIL);
			}
		} else {
			mHandler.sendEmptyMessage(CONNECT_FAIL);
		}
	}

	public static byte[] intToBuffer(int value) {
		byte[] src = new byte[3];
		src[2] = (byte) ((value >> 16) & 0xFF);
		src[1] = (byte) ((value >> 8) & 0xFF);
		src[0] = (byte) (value & 0xFF);
		return src;
	}

	public void down(final float x, final float y) {
		new Thread() {
			public void run() {
				MotionEvent obtain = MotionEvent.obtain(
						SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
						MotionEvent.ACTION_DOWN, x, y, 0);
				obtain.setSource(InputDevice.SOURCE_TOUCHSCREEN);
				// inst.sendPointerSync(obtain);
			}
		}.start();

	}

	public void up(final float x, final float y) {
		new Thread() {
			public void run() {

				MotionEvent obtain = MotionEvent.obtain(
						SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
						MotionEvent.ACTION_UP, x, y, 0);
				obtain.setSource(InputDevice.SOURCE_TOUCHSCREEN);
				// inst.sendPointerSync(obtain);

			}
		}.start();
	}

	public void move(final float x, final float y) {
		new Thread() {
			public void run() {

				MotionEvent obtain = MotionEvent.obtain(
						SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
						MotionEvent.ACTION_MOVE, x, y, 0);
				obtain.setSource(InputDevice.SOURCE_TOUCHSCREEN);
				// inst.sendPointerSync(obtain);
			}
		}.start();
	}

	private void closeConnect() {
		Log.i(TAG, "hdb----closeConnect");
		isRun = false;

		isStart = false;
    	stopCaputre();

		try {
			if (disTouch != null) {
				disTouch.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			if (os != null) {
				os.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			if (socketD != null) {
				socketD.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			if (socketT != null) {
				socketT.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			if (dataSockets != null) {
				dataSockets.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			if (touchSockets != null) {
				touchSockets.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		disTouch = null;
		os = null;
		socketD = null;
		socketT = null;
		dataSockets = null;
		touchSockets = null;

		mHandler.removeMessages(RESTART_SERVER);
		mHandler.sendEmptyMessageDelayed(RESTART_SERVER, 2000);

	}
	
	private class NetChangeReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			ConnectivityManager mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo wifiInfo = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI); 
			if (wifiInfo.isConnected()) {
	        	Log.i(TAG, "hdb-----isConnected");
	        	isNetConnet = true;
	        	startReceiverUdpBrodcast();
	    //    	MainActivity instance = MainActivity.getInstance();
	        	
			}else {
				Log.i(TAG, "hdb---not--Connect");
				isNetConnet = false;
				stopReceiverUdpBrodcast();
			}
		}
		
	}

	public void stopReceiverUdpBrodcast() {
		receiver = false;
		if (multicastSocket != null) {
			try {
				multicastSocket.leaveGroup(broadcastAddress);
				multicastSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		
		
	}

}
