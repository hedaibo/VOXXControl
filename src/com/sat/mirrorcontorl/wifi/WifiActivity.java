package com.sat.mirrorcontorl.wifi;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.sat.mirrorcontorl.IpUtils;
import com.sat.mirrorcontorl.MediaCodeService;
import com.sat.mirrorcontorl.R;
import com.sat.mirrorcontorl.wifi.VerticalSeekBar.OnSeekBarChangeListener;

import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class WifiActivity extends Activity {

	protected static final int GET_SCAN_RESULT = 1;
	protected static final int SHOE_HAS_CONNECT_DIALOG = 2;
	private ListView lvWifi;
	private MyListAdapter mListAdapter;
	private MyReceiver mBroadcastReceiver;
	private WifiManager wifiManager;
	private List<ScanResult> scanResults;
	private List<ScanResult> availableResults;
	private String pwdtype;
	private AlertDialog hasLinkShow;
	private Handler wifiHandler = new Handler() {
		
		private String positionText = "";

		public void handleMessage(Message msg) {
			switch (msg.what) {
			case GET_SCAN_RESULT:
				scanResults = wifiManager.getScanResults();
				if (scanResults != null) {
					// lvWifi.setAdapter(mListAdapter);
					availableResults.clear();
					for (ScanResult scanResult : scanResults) {
						if (!TextUtils.isEmpty(scanResult.SSID) && scanResult.SSID.length() > 0) {
							availableResults.add(scanResult);
						}
					}
					if (mListAdapter != null) {
						sortByLevel(availableResults);
						int max = availableResults.size() > 6 ? availableResults.size() - 6 : availableResults.size();
						sbVertical.setMax(max);
						mListAdapter.notifyDataSetChanged();
					}
					
				}
				wifiManager.startScan();
				wifiHandler.sendEmptyMessageDelayed(GET_SCAN_RESULT, 5000);
				break;
			case SHOE_HAS_CONNECT_DIALOG:
				String ssid = (String) msg.obj;
				int isConnect = msg.arg1;
				final int position = msg.arg2;
				if (isConnect == 2) {
					positionText  = "Connect";
				}else {
					positionText  = "Disconnect";
				}
				AlertDialog.Builder buider = new AlertDialog.Builder(WifiActivity.this);
				buider.setTitle(ssid);
				buider.setMessage("Do you want to "+positionText+" "+ssid+" ?");
				buider.setPositiveButton("Cancel", null);
				if (isConnect == 2) {
					buider.setNeutralButton("Forget", new AlertDialog.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							wifiManager.removeNetwork(wifiManager.getConfiguredNetworks().get(position).networkId);
						}
					});
				}
				buider.setNegativeButton(positionText , new AlertDialog.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (positionText.equals("Connect")) {
							wifiAdmin.addNetwork(wifiManager.getConfiguredNetworks().get(position));
							tvWifiState.setText(R.string.wifi_state_connect);
						}else {
							wifiAdmin.disconnectWifi(wifiManager.getConfiguredNetworks().get(position).networkId);
						}
						hasLinkShow.dismiss();
					}
				});
				
				
				hasLinkShow = buider.show();
				
				break;

			default:
				break;
			}
		};
	};
	private WifiAdmin wifiAdmin;
	private TextView tvWifiScan;
	private TextView tvWifiState;
	private TextView tvWifiStart;
	private MyWifiChangeReceiver wifiChangeReceiver;
	private ConnectivityManager connectivityManager;
	private ImageButton ibUp;
	private ImageButton ibDown;
	private VerticalSeekBar sbVertical;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_wifi);
		wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
		connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		mListAdapter = new MyListAdapter();
		availableResults = new ArrayList<ScanResult>();
		wifiAdmin = new WifiAdmin(this);
		initView();
		initLisener();
		initDate();
		
		MediaCodeService service = MediaCodeService.getService();
        if (service == null) {
        	startService(new Intent(this, MediaCodeService.class));
		}
	}

	private void initDate() {

	}

	@Override
	public void onStart() {
		
		openWifi();
		super.onStart();
	}

	@Override
	protected void onResume() {
		NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (networkInfo != null && networkInfo.isConnected()) {
			tvWifiStart.setSelected(true);
			tvWifiStart.setClickable(true);
			tvDeviceinfo.setText("1.Use phone connect Wifi:"+networkInfo.getExtraInfo()+" \n2.Use phone connect wireless device: "+IpUtils.getSerialNumber());
		}else {
			tvWifiStart.setSelected(false);
			tvWifiStart.setClickable(false);
			tvDeviceinfo.setText("You must connect to WiFi first");
		}
		super.onResume();
	}
	
	
	@Override
	protected void onStop() {
		wifiHandler.removeMessages(GET_SCAN_RESULT);
		super.onStop();
	}
	
	private void initLisener() {
		// if (swWifi.isChecked()) {
		if (mBroadcastReceiver == null) {
			mBroadcastReceiver = new MyReceiver();
			IntentFilter filter = new IntentFilter();
			filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
			registerReceiver(mBroadcastReceiver, filter);
		}
		wifiManager.startScan();
		
		if (wifiChangeReceiver == null) {
			wifiChangeReceiver = new MyWifiChangeReceiver();
			IntentFilter filter = new IntentFilter();
			filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
			registerReceiver(wifiChangeReceiver, filter );
		}
		
		tvWifiScan.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				if (availableResults != null && availableResults.size() > 0 && mListAdapter != null) {
					availableResults.clear();
					mListAdapter.notifyDataSetChanged();
				}
				openWifi();
				wifiManager.startScan();
				wifiHandler.sendEmptyMessage(GET_SCAN_RESULT);
				tvWifiState.setText(R.string.wifi_state);
			}
		});
		
		tvWifiStart.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				startActivityformComponent("cn.manstep.phonemirror", "cn.manstep.phonemirror.MainActivity");
				finish();
			}
		});

		lvWifi.setOnItemClickListener(new OnItemClickListener() {

			private AlertDialog show;
			private EditText etPwd_dialog;
			private String ssid;
			private String capabilities;

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

				capabilities = availableResults.get(position).capabilities;
				ssid = availableResults.get(position).SSID;
				if (!capabilities.contains("WPA") && !capabilities.contains("WEP") && !capabilities.contains("EAP")) {
					wifiAdmin.addNetwork(wifiAdmin.CreateWifiInfo(ssid, "", 1));
					tvWifiState.setText(R.string.wifi_state_connect);
					return;
				}
				List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();
				String ssid3 = wifiManager.getConnectionInfo().getSSID();
				if (configuredNetworks != null) {
					for(int i=0;i<configuredNetworks.size();i++){
						String ssid2 = configuredNetworks.get(i).SSID;
						System.out.println("ssid:"+ssid+"  ssid2:"+ssid2);
						String newSsid = "\""+ssid+"\"";
						System.out.println("ssid:"+newSsid+"  ssid2:"+ssid2);
						System.out.println("ssid:"+newSsid+"  ssid3:"+ssid3);
						if(newSsid.equals(ssid2)){
							Message msg = new Message();
							msg.what = SHOE_HAS_CONNECT_DIALOG;
							msg.obj = ssid;
							msg.arg2 = i;
							if (newSsid.equals(ssid3)) {
								NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
								if (networkInfo != null && networkInfo.isConnected()) {
									msg.arg1 = 1;
								}else {
									msg.arg1 = 2;
								}
							}else {
								msg.arg1 = 2;
							}
							wifiHandler.sendMessage(msg);
							return;
						}
					}
				}
				
				
				
				AlertDialog.Builder builder = new AlertDialog.Builder(WifiActivity.this);
				builder.setTitle(ssid);
				View infoView = View.inflate(getApplicationContext(), R.layout.dialog_wifi_pwd, null);
				CheckBox cbShowPwd = (CheckBox) infoView.findViewById(R.id.cb_show_pwd);
				etPwd_dialog = (EditText) infoView.findViewById(R.id.et_pwd);
				TextView tvPwdType = (TextView) infoView.findViewById(R.id.tv_pwd_type);
				tvPwdType.setText("Secured with "+getPwdType(position));
				cbShowPwd.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						if (isChecked) {
							etPwd_dialog.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
						}else {
							etPwd_dialog.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
						}
						
					}
				});
				
				builder.setView(infoView);
				
				builder.setPositiveButton(R.string.wifi_dialog_cancel, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						show.dismiss();
						
					}
				});
				
				builder.setNegativeButton(R.string.wifi_dialog_connect, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String etPwd = etPwd_dialog.getText().toString().trim();
					//	if (capabilities.equals("")) {
					//		wifiAdmin.addNetwork(wifiAdmin.CreateWifiInfo(ssid, etPwd, 1));
					//		tvWifiState.setText(R.string.wifi_state_connect);
					//	}else {
							if (!TextUtils.isEmpty(etPwd)) {
								if (capabilities.contains("WEP")) {
									wifiAdmin.addNetwork(wifiAdmin.CreateWifiInfo(ssid, etPwd, 2));
									tvWifiState.setText(R.string.wifi_state_connect);
								}else if (capabilities.contains("WPA")) {
									wifiAdmin.addNetwork(wifiAdmin.CreateWifiInfo(ssid, etPwd, 3));
									tvWifiState.setText(R.string.wifi_state_connect);
								}else {
									Toast.makeText(getApplicationContext(), R.string.wifi_dialog_toast_cant, Toast.LENGTH_SHORT).show();
								}
							}else {
								Toast.makeText(getApplicationContext(), R.string.wifi_dialog_toast, Toast.LENGTH_SHORT).show();
							}
					//	}
						
					}
				});
				
				show = builder.show();
			}

		});
		sbVertical.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
		lvWifi.setOnScrollListener(onSeekBarScrollListener);
		ibDown.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				int positionDown = lvWifi.getLastVisiblePosition();
				if (positionDown < lvWifi.getCount()) {
					int position1 = lvWifi.getFirstVisiblePosition();
					position1 = position1 + 1;
					lvWifi.setSelection(position1);
				}
				
			}
		});
		ibUp.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				int position = lvWifi.getFirstVisiblePosition();
				if (position > 0) {
					position = position - 1;
					lvWifi.setSelection(position);
				}
				
			}
		});
		//lvWifi.seton

	}
	

	private void initView() {
		// swWifi = (Switch) findViewById(R.id.sw_wifi);
		lvWifi = (ListView) findViewById(R.id.lv_wifi);
		tvWifiScan = (TextView) findViewById(R.id.tv_wifi_scan);
		tvWifiState = (TextView) findViewById(R.id.tv_wifi_state);
		tvWifiStart = (TextView) findViewById(R.id.tv_wifi_start);
		ibUp = (ImageButton) findViewById(R.id.ib_up);
		ibDown = (ImageButton) findViewById(R.id.ib_down);
		sbVertical = (VerticalSeekBar) findViewById(R.id.sbv_progress);
		tvDeviceinfo = (TextView) findViewById(R.id.tv_device_info);
		
	}

	@Override
	protected void onDestroy() {
		if (mBroadcastReceiver != null) {
			unregisterReceiver(mBroadcastReceiver);
			mBroadcastReceiver = null;
		}
		if (wifiChangeReceiver != null) {
			unregisterReceiver(wifiChangeReceiver);
			wifiChangeReceiver = null;
		}
		super.onDestroy();
	}

	/**sort by level,if level equality by frequency, ssid*/
	private void sortByLevel(List<ScanResult> list){
		Collections.sort(list, new Comparator<ScanResult>() {

			@Override
			public int compare(ScanResult lhs, ScanResult rhs) {
				int num = rhs.level - lhs.level;
				if (num == 0) {
					num = rhs.frequency - lhs.frequency;
					if (num == 0) {
						num = rhs.SSID.compareTo(lhs.SSID);
					}
				}
				return num;
			}
		});
	}
	
	private void openWifi() {
		if (!wifiManager.isWifiEnabled()) {
			wifiManager.setWifiEnabled(true);
			wifiManager.startScan();
			wifiHandler.sendEmptyMessageDelayed(GET_SCAN_RESULT, 500);
		}
		if (mBroadcastReceiver == null) {
			wifiManager.startScan();
			mBroadcastReceiver = new MyReceiver();
			IntentFilter filter = new IntentFilter();
			filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
			registerReceiver(mBroadcastReceiver, filter);

		}
	}

	private void closeWifi() {
		if (wifiManager.isWifiEnabled()) {
			wifiManager.setWifiEnabled(false);
			wifiHandler.sendEmptyMessageDelayed(GET_SCAN_RESULT, 500);
		}
	}
	
	private void startActivityformComponent(String pkg, String cls ){
		Intent intent = new Intent();
		ComponentName component = new ComponentName(pkg, cls);
		intent.setComponent(component);
		PackageManager pManager = getPackageManager();
		List<ResolveInfo> queryIntentActivities = pManager.queryIntentActivities(intent, 0);
		if (queryIntentActivities.size()>0) {
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
		//	overridePendingTransition(R.anim.default_in_animation, 0);
		}else {
			Toast.makeText(this, "this app no install", 0).show();
		}
	
	}
	
	private String getPwdType(int position){
		String capabilities = availableResults.get(position).capabilities;
		String cureentPwdType = "";
		if (!TextUtils.isEmpty(capabilities)) {
			if (capabilities.contains("WPA-PSK") && capabilities.contains("WPA2-PSK")) {
				cureentPwdType = "WAP-PSK/WAP2-PSK";
			} else if (capabilities.contains("WPA-") && capabilities.contains("WPA2-")) { // 较不安全
				cureentPwdType = "WAP/WAP2";
			} else if (capabilities.contains("WEP")) { // 较不安全
				cureentPwdType = "WEP";
			} else if (capabilities.contains("EAP")) {
				cureentPwdType = "EAP";
			} else if (capabilities.contains("WPA-PSK")) {
				cureentPwdType = "WAP-PSK";
			} else if (capabilities.contains("WPA2-PSK")) {
				cureentPwdType = "WAP2-PSK";
			} else if (capabilities.contains("WPA2")) {
				cureentPwdType = "WAP2";
			} else if (capabilities.contains("WPA")) {
				cureentPwdType = "WAP";
			} else {//no password 
				cureentPwdType = "";
			}
		}
		return cureentPwdType;
	}

	private class MyListAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return availableResults.size();
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder viewHolder;
			if (convertView == null) {
				viewHolder = new ViewHolder();
				convertView = View.inflate(getApplicationContext(), R.layout.item_wifi_list, null);
				viewHolder.tvWifiName = (TextView) convertView.findViewById(R.id.tv_wifi_name);
				viewHolder.tvWifiLevel = (TextView) convertView.findViewById(R.id.tv_wifi_level);
				viewHolder.ivWifiLevel = (ImageView) convertView.findViewById(R.id.iv_wifi_level);
				viewHolder.tvWifiPwd = (TextView) convertView.findViewById(R.id.tv_wifi_pwd);
				convertView.setTag(viewHolder);
			}
			viewHolder = (ViewHolder) convertView.getTag();
			String ssid = availableResults.get(position).SSID;
			String capabilities = availableResults.get(position).capabilities;

			boolean hasPwd = true;

			viewHolder.tvWifiName.setText(ssid);
			// viewHolder.tvWifiLevel.setText(availableResults.get(position).level+"");
			//Log.i("hdb", "capabilities: " + capabilities);
			if (!TextUtils.isEmpty(capabilities)) {
				if (capabilities.contains("WPA-PSK") && capabilities.contains("WPA2-PSK")) {
				//	viewHolder.tvWifiPwd.setText("WAP-PSK/WAP2-PSK");
					viewHolder.tvWifiPwd.setText(R.string.wifi_secured_wpa2andwpa1);
					pwdtype = "WAP-PSK/WAP2-PSK";
				} else if (capabilities.contains("WPA-") && capabilities.contains("WPA2-")) { // 较不安全
				//	viewHolder.tvWifiPwd.setText("WPA/WPA2");
					viewHolder.tvWifiPwd.setText(R.string.wifi_secured_wpa2andwpa1);
					pwdtype = "WAP/WAP2";
				} else if (capabilities.contains("WEP")) { // 较不安全
				//	viewHolder.tvWifiPwd.setText("WEP");
					viewHolder.tvWifiPwd.setText(R.string.wifi_secured_wep);
					pwdtype = "WEP";
				} else if (capabilities.contains("EAP")) {
				//	viewHolder.tvWifiPwd.setText("EAP");
					viewHolder.tvWifiPwd.setText(R.string.wifi_secured_eap);
					pwdtype = "EAP";
				} else if (capabilities.contains("WPA-PSK")) {
				//	viewHolder.tvWifiPwd.setText("WAP-PSK");
					viewHolder.tvWifiPwd.setText(R.string.wifi_secured_wpa1);
					pwdtype = "WAP-PSK";
				} else if (capabilities.contains("WPA2-PSK")) {
				//	viewHolder.tvWifiPwd.setText("WAP2-PSK");
					viewHolder.tvWifiPwd.setText(R.string.wifi_secured_wpa2);
					pwdtype = "WAP2-PSK";
				} else if (capabilities.contains("WPA2")) {
				//	viewHolder.tvWifiPwd.setText("WAP2");
					viewHolder.tvWifiPwd.setText(R.string.wifi_secured_wpa2);
					pwdtype = "WAP2";
				} else if (capabilities.contains("WPA")) {
				//	viewHolder.tvWifiPwd.setText("WAP");
					viewHolder.tvWifiPwd.setText(R.string.wifi_secured_wpa1);
					pwdtype = "WAP";
				} else {//no password 
					hasPwd = false;
					viewHolder.tvWifiPwd.setText("");
					pwdtype = "";
				}
			}

			if (hasPwd) {
				if (-availableResults.get(position).level >= 80) {
					viewHolder.ivWifiLevel.setImageResource(R.drawable.ic_wifi_lock_signal_1_dark);
				} else if (-availableResults.get(position).level >= 70) {
					viewHolder.ivWifiLevel.setImageResource(R.drawable.ic_wifi_lock_signal_2_dark);
				} else if (-availableResults.get(position).level >= 55) {
					viewHolder.ivWifiLevel.setImageResource(R.drawable.ic_wifi_lock_signal_3_dark);
				} else {
					viewHolder.ivWifiLevel.setImageResource(R.drawable.ic_wifi_lock_signal_4_dark);
				}
			} else {
				if (-availableResults.get(position).level >= 80) {
					viewHolder.ivWifiLevel.setImageResource(R.drawable.ic_wifi_signal_1_dark);
				} else if (-availableResults.get(position).level >= 70) {
					viewHolder.ivWifiLevel.setImageResource(R.drawable.ic_wifi_signal_2_dark);
				} else if (-availableResults.get(position).level >= 55) {
					viewHolder.ivWifiLevel.setImageResource(R.drawable.ic_wifi_signal_3_dark);
				} else {
					viewHolder.ivWifiLevel.setImageResource(R.drawable.ic_wifi_signal_4_dark);
				}
			}

			return convertView;
		}

	}

	private class ViewHolder {
		TextView tvWifiName;
		TextView tvWifiLevel;
		ImageView ivWifiLevel;
		TextView tvWifiPwd;
	}

	private class MyReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			scanResults = wifiManager.getScanResults();
			if (scanResults != null) {
				availableResults.clear();
				for (ScanResult scanResult : scanResults) {
					if (!TextUtils.isEmpty(scanResult.SSID) && scanResult.SSID.length() > 0) {
						availableResults.add(scanResult);
					}
				}
				sortByLevel(availableResults);
				lvWifi.setAdapter(mListAdapter);
				int max = availableResults.size() > 6 ? availableResults.size() - 6 : availableResults.size();
				sbVertical.setMax(max);
				mListAdapter.notifyDataSetChanged();
			}

		}

	}
	
	private class MyWifiChangeReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context arg0, Intent intent) {
			NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
			if (networkInfo != null && networkInfo.isConnected()) {
				String extraInfo = networkInfo.getExtraInfo();
				System.out.println("extraInfo:"+extraInfo);
				if (extraInfo != null && extraInfo.length() >0) {		
					extraInfo = (String) extraInfo.subSequence(1, extraInfo.length()-1);
					System.out.println("extraInfo:"+extraInfo);
				}
				String stateConnected = getString(R.string.wifi_state_connected);
				tvWifiState.setText(extraInfo+"  "+stateConnected);
				tvWifiStart.setSelected(true);
				tvWifiStart.setClickable(true);
				tvDeviceinfo.setText("1.Use phone connect Wifi:"+networkInfo.getExtraInfo()+" \n2.Use phone connect wireless device: "+IpUtils.getSerialNumber());
			}else {
				tvWifiState.setText(R.string.wifi_state);
				tvWifiStart.setSelected(false);
				tvWifiStart.setClickable(false);
				tvDeviceinfo.setText("You must connect to WiFi first");
			}
			
		}
		
	}
	
	OnScrollListener onSeekBarScrollListener = new OnScrollListener() {

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {

		}

		@Override
		public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
			// System.out.println("firstVisibleItem: " + firstVisibleItem);
			sbVertical.setProgress(firstVisibleItem);

		}
	};

	OnSeekBarChangeListener mOnSeekBarChangeListener = new OnSeekBarChangeListener() {

		@Override
		public void onProgressChanged(VerticalSeekBar VerticalSeekBar, int progress, boolean fromUser) {
			if (fromUser) {
				// sb_progress.setProgress(arg1);
				if (lvWifi != null) {
					lvWifi.setSelection(progress);
				}
				// ll_video_list.scrollTo(0, progress * lvItemHeight);
			}

		}

		@Override
		public void onStartTrackingTouch(VerticalSeekBar VerticalSeekBar) {

		}

		@Override
		public void onStopTrackingTouch(VerticalSeekBar VerticalSeekBar) {

		}
	};
	private TextView tvDeviceinfo;

}
