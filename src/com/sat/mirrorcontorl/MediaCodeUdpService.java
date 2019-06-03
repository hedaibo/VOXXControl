package com.sat.mirrorcontorl;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.json.JSONObject;

import android.app.Instrumentation;
import android.app.Service;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.text.AndroidCharacter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

public class MediaCodeUdpService extends Service implements ScreenCaputre.ScreenCaputreListener {

	protected static final String TAG = "MediaCodeService";

	protected static final int CONNECT_SUCCESS = 1;

	protected static final int CONNECT_FAIL = 2;

	protected static final int CLEAR_FAILCOUNT = 3;

	private static final int RESTART_SERVER = 4;

	private static final String JACTION = "action";
	private static final String JX = "x";
	private static final String JY = "y";

	// private Instrumentation inst;
	private EventInput inst;
	private ScreenCaputre screenCaputre;

	private Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case CONNECT_SUCCESS:
				startCaputre();
				break;
			case CONNECT_FAIL:
			//	closeConnect();
				break;
			case CLEAR_FAILCOUNT:
				failCount = 0;
				break;
			case RESTART_SERVER:
				startDataServer();
				startTouchServer();
				break;

			default:
				break;
			}
		};
	};

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// inst = new Instrumentation();
		try {
			inst = new EventInput();
		} catch (Exception e) {
			e.printStackTrace();
		}
		startDataServer();
		startTouchServer();
		return START_STICKY;
	}

	// private OutputStream os;
	// private DataInputStream disTouch;
	// private Socket socketD;
	// private ServerSocket dataSockets;

	// private ServerSocket touchSockets;
	// private Socket socketT;

	private DatagramSocket dSocketData;
	private DatagramSocket dSocketTouch;

	private boolean isRun = true;

	private void startDataServer() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					dSocketData = new DatagramSocket(8686);

					mHandler.sendEmptyMessageDelayed(CONNECT_SUCCESS, 500);
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

					dSocketTouch = new DatagramSocket(8181);
					isRun = true;
					while (isRun) {
						byte[] data = new byte[50];
						DatagramPacket pack = new DatagramPacket(data, data.length);
						dSocketTouch.receive(pack);
						Log.i(TAG, "hdb-----lenght:" + pack.getLength() + "   offset:" + pack.getOffset());
						String point = new String(pack.getData(), pack.getOffset(), pack.getLength());
						data = null;

						if (point != null) {
							Log.i(TAG, "hdb--------point:" + point);
							JSONObject jObject = new JSONObject(point);
							int action = jObject.getInt(JACTION);
							int x = jObject.getInt(JX);
							int y = jObject.getInt(JY);

							inst.injectMotionEvent(InputDevice.SOURCE_TOUCHSCREEN, 0, SystemClock.uptimeMillis(), x, y,
									1.0f);

						}
					}
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
		screenCaputre = new ScreenCaputre(dm.widthPixels, dm.heightPixels, mDisplayManager);
		screenCaputre.setScreenCaputreListener(this);
		screenCaputre.start();
	}

	@Override
	public void onImageData(byte[] buf) {
		Log.v(TAG, "onImageData  " + buf.length );
		if (null != dSocketData) {
			try {
				/*byte[] bytes = new byte[buf.length + 4];
				byte[] head = intToBuffer(buf.length);
				System.arraycopy(head, 0, bytes, 0, head.length);
				System.arraycopy(buf, 0, bytes, head.length, buf.length);
				os.write(bytes);*/
				InetAddress host = InetAddress.getByName("192.168.0.163");
				DatagramPacket dPacket = new DatagramPacket(buf, buf.length, host, 8686);
				dSocketData.send(dPacket);
				dPacket = null;
				
				
			} catch (IOException e) {
				e.printStackTrace();
				mHandler.sendEmptyMessage(CONNECT_FAIL);
			}
		} else {
			mHandler.sendEmptyMessage(CONNECT_FAIL);
		}
	}

	public static byte[] intToBuffer(int value) {
		byte[] src = new byte[4];
		src[3] = (byte) ((value >> 24) & 0xFF);
		src[2] = (byte) ((value >> 16) & 0xFF);
		src[1] = (byte) ((value >> 8) & 0xFF);
		src[0] = (byte) (value & 0xFF);
		return src;
	}

	public void down(final float x, final float y) {
		new Thread() {
			public void run() {
				MotionEvent obtain = MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
						MotionEvent.ACTION_DOWN, x, y, 0);
				obtain.setSource(InputDevice.SOURCE_TOUCHSCREEN);
				// inst.sendPointerSync(obtain);
			}
		}.start();

	}

	public void up(final float x, final float y) {
		new Thread() {
			public void run() {

				MotionEvent obtain = MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
						MotionEvent.ACTION_UP, x, y, 0);
				obtain.setSource(InputDevice.SOURCE_TOUCHSCREEN);
				// inst.sendPointerSync(obtain);

			}
		}.start();
	}

	public void move(final float x, final float y) {
		new Thread() {
			public void run() {

				MotionEvent obtain = MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
						MotionEvent.ACTION_MOVE, x, y, 0);
				obtain.setSource(InputDevice.SOURCE_TOUCHSCREEN);
				// inst.sendPointerSync(obtain);
			}
		}.start();
	}

	private void closeConnect() {
		Log.i(TAG, "hdb----closeConnect");
		isRun = false;
		stopCaputre();
		
		mHandler.removeMessages(RESTART_SERVER);
		mHandler.sendEmptyMessageDelayed(RESTART_SERVER, 3000);
	}

}
