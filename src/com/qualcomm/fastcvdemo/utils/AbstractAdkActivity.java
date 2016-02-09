/*
 * abstract class for Activities have to read ADK
 * for android:minSdkVersion="12"
 * 
 */

package com.qualcomm.fastcvdemo.utils;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;

public abstract class AbstractAdkActivity extends Activity {
	
	private static int RQS_USB_PERMISSION = 0;
	private static final String ACTION_USB_PERMISSION = "arduino-er.usb_permission";
	private PendingIntent PendingIntent_UsbPermission;
	
	private UsbManager myUsbManager;
	private UsbAccessory myUsbAccessory;
	private ParcelFileDescriptor myAdkParcelFileDescriptor;
	private FileInputStream myAdkInputStream;
	private FileOutputStream myAdkOutputStream;
	boolean firstRqsPermission;
	private long lastCommand = System.nanoTime();

	//do something in onCreate()
	protected abstract void doOnCreate(Bundle savedInstanceState);
	//do something after adk read
	protected abstract void doAdkRead(String stringIn);	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.activity_main);
		
		myUsbManager = (UsbManager)getSystemService(Context.USB_SERVICE);
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		registerReceiver(myUsbReceiver, intentFilter);
		
		//Ask USB Permission from user
		Intent intent_UsbPermission = new Intent(ACTION_USB_PERMISSION);
		PendingIntent_UsbPermission = PendingIntent.getBroadcast(
				this, 					//context
				RQS_USB_PERMISSION, 	//request code
				intent_UsbPermission,	//intent 
				0);						//flags
		IntentFilter intentFilter_UsbPermission = new IntentFilter(ACTION_USB_PERMISSION);
		registerReceiver(myUsbPermissionReceiver, intentFilter_UsbPermission);
		
		firstRqsPermission = true;
		doOnCreate(savedInstanceState);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		if(myAdkInputStream == null || myAdkOutputStream == null){
			
			UsbAccessory[] usbAccessoryList = myUsbManager.getAccessoryList();
			UsbAccessory usbAccessory = null;
			if(usbAccessoryList != null){
				usbAccessory = usbAccessoryList[0];
				
				if(usbAccessory != null){
					if(myUsbManager.hasPermission(usbAccessory)){
						//already have permission
						OpenUsbAccessory(usbAccessory);
					}else{
						
						if(firstRqsPermission){
							
							firstRqsPermission = false;
							
							synchronized(myUsbReceiver){
								myUsbManager.requestPermission(usbAccessory, 
										PendingIntent_UsbPermission);
							}
						}
						
					}
				}
			}
		}
	}
	
	//Write String to Adk - Public for testing...
	public void WriteAdk(String text){
		if (System.nanoTime() - lastCommand < 100*1000000)
			return;
		
		lastCommand = System.nanoTime();

		byte[] buffer = text.getBytes();

		if(myAdkOutputStream != null){
			
			try {
				myAdkOutputStream.write(buffer);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		closeUsbAccessory();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(myUsbReceiver);
		unregisterReceiver(myUsbPermissionReceiver);
	}
	
	Runnable runnableReadAdk = new Runnable(){

		@Override
		public void run() {
			int numberOfByteRead = 0;
			byte[] buffer = new byte[255];
			
			while(numberOfByteRead >= 0){
				
				try {
					numberOfByteRead = myAdkInputStream.read(buffer, 0, buffer.length);
					final StringBuilder stringBuilder = new StringBuilder();
					for(int i=0; i<numberOfByteRead; i++){
						stringBuilder.append((char)buffer[i]);
					}
					
					runOnUiThread(new Runnable(){

						@Override
						public void run() {
							doAdkRead(stringBuilder.toString());
						}});
					
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					break;
				}
			}
		}
		
	};
	
	private BroadcastReceiver myUsbReceiver = new BroadcastReceiver(){
		
		@Override
		public void onReceive(Context context, Intent intent) {

			String action = intent.getAction();
			if(action.equals(UsbManager.ACTION_USB_ACCESSORY_DETACHED)){

				UsbAccessory usbAccessory = 
						(UsbAccessory)intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
				
				if(usbAccessory!=null && usbAccessory.equals(myUsbAccessory)){
					closeUsbAccessory();
				}
			}
		}
	};
	
	private BroadcastReceiver myUsbPermissionReceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(action.equals(ACTION_USB_PERMISSION)){

				synchronized(this){
					
					UsbAccessory usbAccessory = 
							(UsbAccessory)intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
					
					if(intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)){
						OpenUsbAccessory(usbAccessory);
					}else{
						finish();
					}
				}
			}
		}
		
	};
	
	private void OpenUsbAccessory(UsbAccessory acc){
		myAdkParcelFileDescriptor = myUsbManager.openAccessory(acc);
		if(myAdkParcelFileDescriptor != null){
			
			myUsbAccessory = acc;
			FileDescriptor fileDescriptor = myAdkParcelFileDescriptor.getFileDescriptor();
			myAdkInputStream = new FileInputStream(fileDescriptor);
			myAdkOutputStream = new FileOutputStream(fileDescriptor);
			
			Thread thread = new Thread(runnableReadAdk);
			thread.start();
		}
	}
	
	private void closeUsbAccessory(){
		
		if(myAdkParcelFileDescriptor != null){
			try {
				myAdkParcelFileDescriptor.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		myAdkParcelFileDescriptor = null;
		myUsbAccessory = null;
	}
}
