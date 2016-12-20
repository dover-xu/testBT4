package com.mobisport.mobiboat.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

/**
 * �ر�˵����HC_BLE�����ǹ��ݻ����Ϣ�Ƽ����޹�˾�����з����ֻ�APP�������û�����08����ģ�顣
 * �����ֻ��֧�ְ�׿�汾4.3����������4.0���ֻ�ʹ�á�
 * ��������Լҵ�05��06ģ�飬Ҫʹ������һ������2.0���ֻ�APP���û������ڻ�йٷ�������������������ء�
 * ������ṩ�����ע�ͣ���Ѹ�����08ģ����û�ѧϰ���о���������������ҵ���������ս���Ȩ�ڹ��ݻ����Ϣ�Ƽ����޹�˾��
 * **/

/** 
 * @Description:  TODO<�������񣬸����ں�̨ʵ�����������ӣ���ݵķ��ͽ���> 
 * @author  ���ݻ����Ϣ�Ƽ����޹�˾
 * @data:  2014-10-22 ����2:30:38 
 * @version:  V1.0 
 */ 
public class BluetoothLeService extends Service {
	private final static String TAG = "BluetoothLeService";// luetoothLeService.class.getSimpleName();
	private List<Sensor> mEnabledSensors = new ArrayList<Sensor>();
	//���������
	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBluetoothAdapter;
	private String mBluetoothDeviceAddress;
	private BluetoothGatt mBluetoothGatt;
	private int mConnectionState = STATE_DISCONNECTED;

	private static final int STATE_DISCONNECTED = 0;
	private static final int STATE_CONNECTING = 1;
	private static final int STATE_CONNECTED = 2;

	public final static String ACTION_GATT_CONNECTED = "com.hc_ble.bluetooth.le.ACTION_GATT_CONNECTED";
	public final static String ACTION_GATT_DISCONNECTED = "com.hc_ble.bluetooth.le.ACTION_GATT_DISCONNECTED";
	public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.hc_ble.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
	public final static String ACTION_DATA_AVAILABLE = "com.hc_ble.bluetooth.le.ACTION_DATA_AVAILABLE";
	public final static String EXTRA_DATA_STR = "com.hc_ble.bluetooth.le.EXTRA_DATA_STR";
	public final static String EXTRA_DATA = "com.hc_ble.bluetooth.le.EXTRA_DATA";

	// public final static UUID UUID_HEART_RATE_MEASUREMENT =zzzzzzzzzzzzz
	// UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT);
	private OnDataAvailableListener mOnDataAvailableListener;

	// Implements callback methods for GATT events that the app cares about. For
	// example,
	// connection change and services discovered.

	public interface OnDataAvailableListener {
		public void onCharacteristicRead(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status);

		public void onCharacteristicWrite(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic);

		public void onCharacteristicChanged(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic);
	}

	public void setOnDataAvailableListener(OnDataAvailableListener l)
	{
		mOnDataAvailableListener = l;
	}

	/* ����Զ���豸�Ļص����� */
	private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback()
	{
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status,
				int newState)
		{
			String intentAction;
			if (newState == BluetoothProfile.STATE_CONNECTED)//���ӳɹ�
			{
				intentAction = ACTION_GATT_CONNECTED;
				mConnectionState = STATE_CONNECTED;
				/* ͨ��㲥��������״̬ */
				broadcastUpdate(intentAction);
				Log.i(TAG, "Connected to GATT server.");
				// Attempts to discover services after successful connection.
				Log.i(TAG, "Attempting to start service discovery:"
						+ mBluetoothGatt.discoverServices());

			} else if (newState == BluetoothProfile.STATE_DISCONNECTED)//����ʧ��
			{
				intentAction = ACTION_GATT_DISCONNECTED;
				mConnectionState = STATE_DISCONNECTED;
				Log.i(TAG, "Disconnected from GATT server.");
				broadcastUpdate(intentAction);
			}
		}
        /*
         * ��дonServicesDiscovered��������������
         * 
         * */
		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status)
		{
			if (status == BluetoothGatt.GATT_SUCCESS)//���ֵ�����
			{
				broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
				Log.i(TAG, "--onServicesDiscovered called--");
			} else
			{
				Log.w(TAG, "onServicesDiscovered received: " + status);
				System.out.println("onServicesDiscovered received: " + status);
			}
		}
         /*
          * ����ֵ�Ķ�д
          * */
		@Override
		public void onCharacteristicRead(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status)
		{
			if (status == BluetoothGatt.GATT_SUCCESS)
			{
				Log.i(TAG, "--onCharacteristicRead called--");
				//������ֵ��ȡ���
				byte[] sucString = characteristic.getValue();
				String string = new String(sucString);
				//�����ͨ��㲥��Ble_Activity
				broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
			}
			
		}
         /*
          * ����ֵ�ĸı�
          * */
		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic)
		{
			//System.out.println("++++++++++++++++");
			broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
			//Log.d("xudong_tag", "onCharacteristicChanged");
		}
        /*
         * ����ֵ��д
         * */
		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status)
		{
			
			Log.w(TAG, "--onCharacteristicWrite--: " + status);
			//Log.d("xudong_tag", "onCharacteristicRead");
			// �������ʵ�� ��������ݻ�Ҳ��ʾ��������
			//broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
		}
        /*
         * ������ֵ
         * */
		@Override
		public void onDescriptorRead(BluetoothGatt gatt,
				BluetoothGattDescriptor descriptor, int status)
		{
			// TODO Auto-generated method stub
			// super.onDescriptorRead(gatt, descriptor, status);
			Log.w(TAG, "----onDescriptorRead status: " + status);
			byte[] desc = descriptor.getValue();
			if (desc != null)
			{
				Log.w(TAG, "----onDescriptorRead value: " + new String(desc));
			}
			//Log.d("xudong_tag", "onDescriptorRead");

		}
         /*
          * д����ֵ
          * */
		@Override
		public void onDescriptorWrite(BluetoothGatt gatt,
				BluetoothGattDescriptor descriptor, int status)
		{
			// TODO Auto-generated method stub
			// super.onDescriptorWrite(gatt, descriptor, status);
			Log.w(TAG, "--onDescriptorWrite--: " + status);
			//Log.d("xudong_tag", "onDescriptorWrite");
		}
         /*
          * ��д�����ź�ֵ
          * */
		@Override
		public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status)
		{
			// TODO Auto-generated method stub
			// super.onReadRemoteRssi(gatt, rssi, status);
			Log.w(TAG, "--onReadRemoteRssi--: " + status);
			broadcastUpdate(ACTION_DATA_AVAILABLE, rssi);
			//Log.d("xudong_tag", "onReadRemoteRssi");
		}

		@Override
		public void onReliableWriteCompleted(BluetoothGatt gatt, int status)
		{
			// TODO Auto-generated method stub
			// super.onReliableWriteCompleted(gatt, status);
			Log.w(TAG, "--onReliableWriteCompleted--: " + status);
			//Log.d("xudong_tag", "onReliableWriteCompleted");
		}

	};
    //�㲥��ͼ
	private void broadcastUpdate(final String action, int rssi)
	{
		final Intent intent = new Intent(action);
		intent.putExtra(EXTRA_DATA, String.valueOf(rssi));
		sendBroadcast(intent);
	}
     //�㲥��ͼ
	private void broadcastUpdate(final String action)
	{
		final Intent intent = new Intent(action);
		sendBroadcast(intent);
	}

	/* �㲥Զ�̷��͹�������� */
	public void broadcastUpdate(final String action,
			final BluetoothGattCharacteristic characteristic)
	{
		final Intent intent = new Intent(action);
		//������ֵ��ȡ���
		final byte[] data = characteristic.getValue();
		if (data != null && data.length > 0)
		{
			final StringBuilder stringBuilder = new StringBuilder(data.length);
			for (byte byteChar : data)
			{
				stringBuilder.append(String.format("%02X ", byteChar));
				//Log.i(TAG, "***broadcastUpdate: byteChar = " + byteChar);				
			}
			intent.putExtra(EXTRA_DATA, data);
			intent.putExtra(EXTRA_DATA_STR, stringBuilder.toString());
			//Log.d("xudong_tag", "stringbuider:"+stringBuilder.toString());
			//System.out.println("broadcastUpdate for  read data:" + new String(data));
		}
		sendBroadcast(intent);
	}

	public class LocalBinder extends Binder {
		public BluetoothLeService getService()
		{
			return BluetoothLeService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent)
	{
		close();
		return super.onUnbind(intent);
	}

	private final IBinder mBinder = new LocalBinder();
	
	

	/**
	 * Initializes a reference to the local Bluetooth adapter.
	 * 
	 * @return Return true if the initialization is successful.
	 */
	/* service ��������ʼ�� */
	public boolean initialize()
	{
		// For API level 18 and above, get a reference to BluetoothAdapter
		// through
		// BluetoothManager.
		if (mBluetoothManager == null)
		{   //��ȡϵͳ������������
			mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
			if (mBluetoothManager == null)
			{
				Log.e(TAG, "Unable to initialize BluetoothManager.");
				return false;
			}
		}
          
		mBluetoothAdapter = mBluetoothManager.getAdapter();
		if (mBluetoothAdapter == null)
		{
			Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
			return false;
		}

		return true;
	}

	/**
	 * Connects to the GATT server hosted on the Bluetooth LE device.
	 * 
	 * @param address
	 *            The device address of the destination device.
	 * 
	 * @return Return true if the connection is initiated successfully. The
	 *         connection result is reported asynchronously through the
	 *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
	 *         callback.
	 */
	// ����Զ������
	public boolean connect(final String address)
	{
		if (mBluetoothAdapter == null || address == null)
		{
			Log.w(TAG,
					"BluetoothAdapter not initialized or unspecified address.");
			return false;
		}

		// Previously connected device. Try to reconnect.
		if (mBluetoothDeviceAddress != null
				&& address.equals(mBluetoothDeviceAddress)
				&& mBluetoothGatt != null)
		{
			Log.d(TAG,
					"Trying to use an existing mBluetoothGatt for connection.");
			if (mBluetoothGatt.connect())//������������ʵ���ǵ���BluetoothGatt�����ӷ���
			{
				mConnectionState = STATE_CONNECTING;
				return true;
			} else
			{
				return false;
			}
		}
		/* ��ȡԶ�˵������豸 */
		final BluetoothDevice device = mBluetoothAdapter
				.getRemoteDevice(address);
		if (device == null)
		{
			Log.w(TAG, "Device not found.  Unable to connect.");
			return false;
		}
		// We want to directly connect to the device, so we are setting the
		// autoConnect
		// parameter to false.
		/* ����device�е�connectGatt���ӵ�Զ���豸 */
		mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
		Log.d(TAG, "Trying to create a new connection.");
		mBluetoothDeviceAddress = address;
		mConnectionState = STATE_CONNECTING;
		System.out.println("device.getBondState==" + device.getBondState());
		return true;
	}

	/**
	 * Disconnects an existing connection or cancel a pending connection. The
	 * disconnection result is reported asynchronously through the
	 * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
	 * callback.
	 */
	/*
	 * ȡ������
	 * 
	 * */
	/** 
	* @Title: disconnect 
	* @Description: TODO(ȡ����������) 
	* @param   ��
	* @return void    
	* @throws 
	*/ 
	public void disconnect()
	{
		if (mBluetoothAdapter == null || mBluetoothGatt == null)
		{
			Log.w(TAG, "BluetoothAdapter not initialized");
			return;
		}
		mBluetoothGatt.disconnect();

	}

	/**
	 * After using a given BLE device, the app must call this method to ensure
	 * resources are released properly.
	 */
	/** 
	* @Title: close 
	* @Description: TODO(�ر�������������) 
	* @param  ��
	* @return void   
	* @throws 
	*/ 
	public void close()
	{
		if (mBluetoothGatt == null)
		{
			return;
		}
		mBluetoothGatt.close();
		mBluetoothGatt = null;
	}

	/**
	 * Request a read on a given {@code BluetoothGattCharacteristic}. The read
	 * result is reported asynchronously through the
	 * {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
	 * callback.
	 * 
	 * @param characteristic
	 *            The characteristic to read from.
	 */
	/** 
	* @Title: readCharacteristic 
	* @Description: TODO(��ȡ����ֵ) 
	* @param @param characteristic��Ҫ��������ֵ��
	* @return void    �������� 
	* @throws 
	*/ 
	public void readCharacteristic(BluetoothGattCharacteristic characteristic)
	{
		if (mBluetoothAdapter == null || mBluetoothGatt == null)
		{
			Log.w(TAG, "BluetoothAdapter not initialized");
			return;
		}
		mBluetoothGatt.readCharacteristic(characteristic);

	}

	// д������ֵ
	public void writeCharacteristic(BluetoothGattCharacteristic characteristic)
	{
		if (mBluetoothAdapter == null || mBluetoothGatt == null)
		{
			Log.w(TAG, "BluetoothAdapter not initialized");
			return;
		}
		mBluetoothGatt.writeCharacteristic(characteristic);

	}

	// ��ȡRSSi
	public void readRssi()
	{
		if (mBluetoothAdapter == null || mBluetoothGatt == null)
		{
			Log.w(TAG, "BluetoothAdapter not initialized");
			return;
		}
		mBluetoothGatt.readRemoteRssi();
	}

	/**
	 * Enables or disables notification on a give characteristic.
	 * 
	 * @param characteristic
	 *            Characteristic to act on.
	 * @param enabled
	 *            If true, enable notification. False otherwise.
	 */
	/** 
	* @Title: setCharacteristicNotification 
	* @Description: TODO(��������ֵͨ�仯֪ͨ) 
	* @param @param characteristic������ֵ��
	* @param @param enabled ��ʹ�ܣ�   
	* @return void    
	* @throws 
	*/ 
	public void setCharacteristicNotification(
			BluetoothGattCharacteristic characteristic, boolean enabled)
	{
		if (mBluetoothAdapter == null || mBluetoothGatt == null)
		{
			Log.w(TAG, "BluetoothAdapter not initialized");
			return;
		}
		mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

		BluetoothGattDescriptor clientConfig = characteristic
				.getDescriptor(UUID
						.fromString("00002902-0000-1000-8000-00805f9b34fb"));

		if (enabled)
		{
			clientConfig
					.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
		} else
		{
			clientConfig
					.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
		}
		mBluetoothGatt.writeDescriptor(clientConfig);
		//Log.d("xudong_tag", "setNotify");
	}

	/** 
	* @Title: getCharacteristicDescriptor 
	* @Description: TODO(�õ�����ֵ�µ�����ֵ) 
	* @param @param ��
	* @return void   
	* @throws 
	*/ 
	public void getCharacteristicDescriptor(BluetoothGattDescriptor descriptor)
	{
		if (mBluetoothAdapter == null || mBluetoothGatt == null)
		{
			Log.w(TAG, "BluetoothAdapter not initialized");
			return;
		}

		mBluetoothGatt.readDescriptor(descriptor);
	}

	/**
	 * Retrieves a list of supported GATT services on the connected device. This
	 * should be invoked only after {@code BluetoothGatt#discoverServices()}
	 * completes successfully.
	 * 
	 * @return A {@code List} of supported services.
	 */
	/** 
	* @Title: getSupportedGattServices 
	* @Description: TODO(�õ����������з���) 
	* @param @return    ��
	* @return List<BluetoothGattService>    
	* @throws 
	*/ 
	public List<BluetoothGattService> getSupportedGattServices()
	{
		if (mBluetoothGatt == null)
			return null;
		return mBluetoothGatt.getServices();

	}

}
