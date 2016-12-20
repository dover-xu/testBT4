package com.mobisport.mobiboat.ui;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.w3c.dom.CDATASection;

import android.R.integer;
import android.R.string;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.StaticLayout;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.mobisport.mobiboat.R;
import com.mobisport.mobiboat.service.*;

/**
 * �ر�˵����HC_BLE�����ǹ��ݻ����Ϣ�Ƽ����޹�˾�����з����ֻ�APP�������û�����08����ģ�顣
 * �����ֻ��֧�ְ�׿�汾4.3����������4.0���ֻ�ʹ�á�
 * ��������Լҵ�05��06ģ�飬Ҫʹ������һ������2.0���ֻ�APP���û������ڻ�йٷ�������������������ء�
 * ������ṩ�����ע�ͣ���Ѹ�����08ģ����û�ѧϰ���о���������������ҵ���������ս���Ȩ�ڹ��ݻ����Ϣ�Ƽ����޹�˾��
 * **/

/** 
 * @Description:  TODO<Ble_Activityʵ������BLE,���ͺͽ���BLE�����> 
 * @author  ���ݻ����Ϣ�Ƽ����޹�˾
 * @data:  2014-10-20 ����12:12:04 
 * @version:  V1.0 
 */ 
public class Ble_Activity extends Activity implements OnClickListener {

	private final static String TAG = Ble_Activity.class.getSimpleName();
	//����4.0��UUID,����0000ffe1-0000-1000-8000-00805f9b34fb�ǹ��ݻ����Ϣ�Ƽ����޹�˾08����ģ���UUID
	public static String HEART_RATE_MEASUREMENT = "0000ffe1-0000-1000-8000-00805f9b34fb";
	public static String EXTRAS_DEVICE_NAME = "DEVICE_NAME";;
	public static String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
	public static String EXTRAS_DEVICE_RSSI = "RSSI";
	//��������״̬
	private boolean mConnected = false;
	private String status = "disconnected";
	//��������
	private String mDeviceName;
	//������ַ
	private String mDeviceAddress;
	//�����ź�ֵ
	private String mRssi;
	private Bundle b;
	private String rev_str = "";
	//����service,�����̨����������
	private static BluetoothLeService mBluetoothLeService;
	//�ı�����ʾ���ܵ�����
	private TextView rev_tv, connect_state;
	//���Ͱ�ť
	private Button send_btn;
	private Button clear_btn;
	private Button data1_btn;
	private Button config_btn;	
	private View viewDia;
	//����ֶ� 
	
	private int use;
	//private double dist;
	private int T500;
	private long countMin;
	private int level;
	private int battery;
	//��ʾ�ı���
	private TextView use_tv;
	private TextView t500_tv;
	private TextView countmin_tv;
	private TextView level_tv;
	private TextView battery_tv;
	//�ı��༭��
	private EditText send_et;
	private ScrollView rev_sv;
	private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
	//��������ֵ
	private static BluetoothGattCharacteristic target_chara = null;
	private Handler mhandler = new Handler();
	private Handler myHandler = new Handler()
	{
		// 2.��д��Ϣ���?��
		public void handleMessage(Message msg)
		{
			switch (msg.what)
			{
			// �жϷ��͵���Ϣ
			case 1:
			{
				// ����View
				String state = msg.getData().getString("connect_state");
				connect_state.setText(state);

				break;
			}

			}
			super.handleMessage(msg);
		}

	};

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ble_activity);
		b = getIntent().getExtras();
		//����ͼ��ȡ��ʾ��������Ϣ
		mDeviceName = b.getString(EXTRAS_DEVICE_NAME);
		mDeviceAddress = b.getString(EXTRAS_DEVICE_ADDRESS);
		mRssi = b.getString(EXTRAS_DEVICE_RSSI);

		/* ��������service */
		Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
		bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
		init();
			
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();

        //���㲥������
		unregisterReceiver(mGattUpdateReceiver);
		mBluetoothLeService = null;
	}

	// Activity����ʱ�򣬰󶨹㲥�������������������ӷ��񴫹������¼�
	@Override
	protected void onResume()
	{
		super.onResume();
		//�󶨹㲥������
		registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
		if (mBluetoothLeService != null)
		{    
			//���������ַ����������
			final boolean result = mBluetoothLeService.connect(mDeviceAddress);
			Log.d(TAG, "Connect request result=" + result);
		}
	}

	/** 
	* @Title: init 
	* @Description: TODO(��ʼ��UI�ؼ�) 
	* @param  ��
	* @return void    
	* @throws 
	*/ 
	private void init()
	{
		rev_sv = (ScrollView) this.findViewById(R.id.rev_sv);
		rev_tv = (TextView) this.findViewById(R.id.rev_tv);
		connect_state = (TextView) this.findViewById(R.id.connect_state);
		send_btn = (Button) this.findViewById(R.id.send_btn);
		clear_btn = (Button) this.findViewById(R.id.clear_btn);
		data1_btn = (Button) this.findViewById(R.id.data1_btn);
		config_btn = (Button) this.findViewById(R.id.config_btn);
		send_et = (EditText) this.findViewById(R.id.send_et);		
		
		connect_state.setText(status);
		send_btn.setOnClickListener(this);
		clear_btn.setOnClickListener(this);
		data1_btn.setOnClickListener(this);
		config_btn.setOnClickListener(this);
		
		//�����ʾ�ı��� 
		t500_tv = (TextView)findViewById(R.id.speed_tv);
		countmin_tv = (TextView)findViewById(R.id.countmin_tv);
		level_tv = (TextView)findViewById(R.id.level_tv);
		use_tv = (TextView)findViewById(R.id.use_tv);
		battery_tv = (TextView)findViewById(R.id.battery_tv);
		
		this.registerForContextMenu(rev_tv); 

	}

	/* BluetoothLeService�󶨵Ļص����� */
	private final ServiceConnection mServiceConnection = new ServiceConnection()
	{

		@Override
		public void onServiceConnected(ComponentName componentName,
				IBinder service)
		{
			mBluetoothLeService = ((BluetoothLeService.LocalBinder) service)
					.getService();
			if (!mBluetoothLeService.initialize())
			{
				Log.e(TAG, "Unable to initialize Bluetooth");
				finish();
			}
			// Automatically connects to the device upon successful start-up
			// initialization.
			// ���������ַ�������豸
			mBluetoothLeService.connect(mDeviceAddress);

		}

		@Override
		public void onServiceDisconnected(ComponentName componentName)
		{
			mBluetoothLeService = null;
		}

	};

	/**
	 * �㲥���������������BluetoothLeService�෢�͵����
	 */
	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			final String action = intent.getAction();
			if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action))//Gatt���ӳɹ�
			{
				mConnected = true;
				status = "connected";
				//��������״̬
				updateConnectionState(status);
				System.out.println("BroadcastReceiver :" + "device connected");

			} else if (BluetoothLeService.ACTION_GATT_DISCONNECTED//Gatt����ʧ��
					.equals(action))
			{
				mConnected = false;
				status = "disconnected";
				//��������״̬
				updateConnectionState(status);
				System.out.println("BroadcastReceiver :"
						+ "device disconnected");

			} else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED//����GATT������
					.equals(action))
			{
				// Show all the supported services and characteristics on the
				// user interface.
				//��ȡ�豸��������������
				displayGattServices(mBluetoothLeService
						.getSupportedGattServices());
				System.out.println("BroadcastReceiver :"
						+ "device SERVICES_DISCOVERED");
			} else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action))//��Ч���
			{    
				 //���?�͹��������			
				parseData(intent.getExtras().getByteArray(BluetoothLeService.EXTRA_DATA));				
				
				displayData(intent.getExtras().getString(BluetoothLeService.EXTRA_DATA_STR));				
			}
		}
	};

	/* ��������״̬ */
	private void updateConnectionState(String status)
	{
		Message msg = new Message();
		msg.what = 1;
		Bundle b = new Bundle();
		b.putString("connect_state", status);
		msg.setData(b);
		//������״̬���µ�UI��textview��
		myHandler.sendMessage(msg);
		System.out.println("connect_state:" + status);

	}

	/* ��ͼ������ */
	private static IntentFilter makeGattUpdateIntentFilter()
	{
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
		intentFilter
				.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
		return intentFilter;
	}

	/** 
	* @Title: displayData 
	* @Description: TODO(���յ��������scrollview����ʾ) 
	* @param @param rev_string(���ܵ����)
	* @return void   
	* @throws 
	*/ 
	private void displayData(String rev_string)
	{
		if (rev_string == null) {
			return;
		}
		if (rev_str.length() > 2000) {
			rev_str = rev_str.substring(rev_string.length()+1) + rev_string + "\n";			
		} else {
			rev_str += (rev_string+"\n");
		}
		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				//byte[] buff3 = rev_str.getBytes();
				//String str3 = byte2HexStr(buff3);
				rev_tv.setText(rev_str);
				rev_sv.scrollTo(0, rev_tv.getMeasuredHeight());
				//System.out.println("rev:" + rev_str);				
			}
		});

		if (use==1) {
			use_tv.setText("是");	
		} else {
			use_tv.setText("否");
		}
				
		//speed_tv.setText(String.format("%1$.2f", speed));
//		if (speed >= 0.1) {
//			speed_tv.setText(String.format("%1$02d:%2$02d", (int)(500/speed)/60%99, (int)(500/speed)%60));
//		} else {
//			speed_tv.setText(String.format("00:00(%1$.2f)", speed));
//		}
		t500_tv.setText(String.format("%1$02d:%2$02d", T500/60, T500%60));
		level_tv.setText(String.valueOf(level));
		countmin_tv.setText(String.valueOf(countMin));
		battery_tv.setText(String.valueOf(battery));
	}

	private long unsignedByte(byte signedByte)
	{
		long unByte = signedByte >=0 ? signedByte : signedByte + 256;
		return unByte;
	}
	private void parseData(byte[] pack) 
	{
		int len = pack.length;
		long data_len;
		byte checkEven = 0;
		int _t500 = 0;
		long _countMin = 0;
		
		// 校验侦头和长度
		if (len > 5) {			
			data_len = (unsignedByte(pack[2])<<8) + unsignedByte(pack[3]);
			if (((byte)0xAB != pack[0]) || (len != data_len+5)){
				return;
			}					
		} else {			
			return ;
		}
		
		// 校验码
		for (int i = 0; i < len-1; i++) {								
			checkEven ^= pack[i];
		}				
		if (checkEven != pack[len-1])
			return;

		if ((0x03 == pack[1]) || (0x01 == pack[1])) {			
			use = pack[4];
			
			battery = pack[5];
			
			level = pack[6];
			
			_t500 |= unsignedByte(pack[7])<<24;
			_t500 |= unsignedByte(pack[8])<<16;
			_t500 |= unsignedByte(pack[9])<<8;
			_t500 |= unsignedByte(pack[10]);
			
			_countMin |= unsignedByte(pack[11])<<8;
			_countMin |= unsignedByte(pack[12]);
						
			//dist = dist / 100.0;
			T500 = _t500;
			countMin = _countMin;				
		} else if (0x05 == pack[1]) {
			long kt500 = 0;
			long level = 0;
			
			if (viewDia==null)
				return;
			EditText kt500_et = (EditText)viewDia.findViewById(R.id.kt500_et);
			EditText level_et = (EditText)viewDia.findViewById(R.id.level_et);
			
			kt500 |= unsignedByte(pack[4])<<8;
			kt500 |= unsignedByte(pack[5]);
			kt500_et.setText(""+kt500);
			
			level |= unsignedByte(pack[6])<<8;
			level |= unsignedByte(pack[7]);
			level_et.setText(""+level);
	
		}
	}

	/** 
	* @Title: displayGattServices 
	* @Description: TODO(������������) 
	* @param ��  
	* @return void  
	* @throws 
	*/ 
	private void displayGattServices(List<BluetoothGattService> gattServices)
	{

		if (gattServices == null)
			return;
		String uuid = null;
		String unknownServiceString = "unknown_service";
		String unknownCharaString = "unknown_characteristic";

		// �������,����չ�����б�ĵ�һ�����
		ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();

		// ������ݣ�������ĳһ���������������ֵ���ϣ�
		ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData = new ArrayList<ArrayList<HashMap<String, String>>>();

		// ���ֲ�Σ���������ֵ����
		mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

		// Loops through available GATT Services.
		for (BluetoothGattService gattService : gattServices)
		{

			// ��ȡ�����б�
			HashMap<String, String> currentServiceData = new HashMap<String, String>();
			uuid = gattService.getUuid().toString();

			// ��?��ݸ�uuid��ȡ��Ӧ�ķ�����ơ�SampleGattAttributes�������Ҫ�Զ��塣

			gattServiceData.add(currentServiceData);

			System.out.println("Service uuid:" + uuid);

			ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<HashMap<String, String>>();

			// �ӵ�ǰѭ����ָ��ķ����ж�ȡ����ֵ�б�
			List<BluetoothGattCharacteristic> gattCharacteristics = gattService
					.getCharacteristics();

			ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<BluetoothGattCharacteristic>();

			// Loops through available Characteristics.
			// ���ڵ�ǰѭ����ָ��ķ����е�ÿһ������ֵ
			for (final BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics)
			{
				charas.add(gattCharacteristic);
				HashMap<String, String> currentCharaData = new HashMap<String, String>();
				uuid = gattCharacteristic.getUuid().toString();

				if (gattCharacteristic.getUuid().toString()
						.equals(HEART_RATE_MEASUREMENT))
				{
					// ���Զ�ȡ��ǰCharacteristic��ݣ��ᴥ��mOnDataAvailable.onCharacteristicRead()
					mhandler.postDelayed(new Runnable()
					{

						@Override
						public void run()
						{
							// TODO Auto-generated method stub
							mBluetoothLeService
									.readCharacteristic(gattCharacteristic);
						}
					}, 200);

					// ����Characteristic��д��֪ͨ,�յ�����ģ�����ݺ�ᴥ��mOnDataAvailable.onCharacteristicWrite()
					mBluetoothLeService.setCharacteristicNotification(
							gattCharacteristic, true);
					
					target_chara = gattCharacteristic;
					// �����������
					// ������ģ��д�����
					// mBluetoothLeService.writeCharacteristic(gattCharacteristic);
				}
				List<BluetoothGattDescriptor> descriptors = gattCharacteristic
						.getDescriptors();
				for (BluetoothGattDescriptor descriptor : descriptors)
				{
					System.out.println("---descriptor UUID:"
							+ descriptor.getUuid());
					// ��ȡ����ֵ������
					mBluetoothLeService.getCharacteristicDescriptor(descriptor);
					// mBluetoothLeService.setCharacteristicNotification(gattCharacteristic,
					// true);
				}

				gattCharacteristicGroupData.add(currentCharaData);
			}
			// ���Ⱥ�˳�򣬷ֲ�η�������ֵ�����У�ֻ������ֵ
			mGattCharacteristics.add(charas);
			// �����ڶ�����չ�б?�������������ֵ��
			gattCharacteristicData.add(gattCharacteristicGroupData);

		}

	}
	/**
	 * ����ݷְ�
	 * 
	 * **/
	public int[] dataSeparate(int len)
	{   
		int[] lens = new int[2];
		lens[0]=len/20;
		lens[1]=len-20*lens[0];
		return lens;
	}
	
	private byte charToByte(char c) {  
	    return (byte) "0123456789ABCDEF".indexOf(c);  
	}  
	
	/** 
	 * Convert hex string to byte[] 
	 * @param hexString the hex string 
	 * @return byte[] 
	 */  
	public byte[] hexStringToBytes(String hexString) {  
	    if (hexString == null || hexString.equals("")) {  
	        return null;  
	    }  
	    hexString = hexString.toUpperCase();  
	    int length = hexString.length() / 2;  
	    char[] hexChars = hexString.toCharArray();  
	    byte[] d = new byte[length];  
	    for (int i = 0; i < length; i++) {  
	        int pos = i * 2;  
	        d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));  
	    }  
	    return d;  
	}  

/**  
	 * bytesת����ʮ������ַ�  
	 * @param byte[] b byte����  
	 * @return String ÿ��Byteֵ֮��ո�ָ�  
	 */    
	public String byte2HexStr(byte[] b)    
	{    
	    String stmp="";    
	    StringBuilder sb = new StringBuilder("");    
	    for (int n=0;n<b.length;n++)    
	    {    
	        stmp = Integer.toHexString(b[n] & 0xFF);    
	        sb.append((stmp.length()==1)? "0"+stmp : stmp);    
	        sb.append(" ");    
	    }    
	    return sb.toString().toUpperCase().trim();    
	}  

	/* (non-Javadoc)
	 * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		if (item.getItemId() == 0) {
			rev_str = "";
		}
		return super.onContextItemSelected(item);
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu, android.view.View, android.view.ContextMenu.ContextMenuInfo)
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		// TODO Auto-generated method stub
		super.onCreateContextMenu(menu, v, menuInfo);
		 // ���� R.id.txt1 �������Ĳ˵�  
        if (v == (TextView) this.findViewById(R.id.rev_tv)) {             
            menu.add(1, 0, 0, "清空");    
        }   
	}
	/*自定义对话框*/  
    private void showCustomDia()  
    {      	
        AlertDialog.Builder customDia=new AlertDialog.Builder(Ble_Activity.this);  
        viewDia=LayoutInflater.from(Ble_Activity.this).inflate(R.layout.config_dialog, null);    
        customDia.setTitle("自定义对话框");  
        customDia.setView(viewDia);          
        customDia.setNegativeButton("取消", new DialogInterface.OnClickListener() {  
            
          @Override  
          public void onClick(DialogInterface dialog, int which) {  
        	  try  
              {  
                  Field field = dialog.getClass().getSuperclass().getDeclaredField("mShowing");  
                  field.setAccessible(true);  
                   //设置mShowing值，欺骗android系统   
                  field.set(dialog, true);  //需要关闭的时候 将这个参数设置为true 他就会自动关闭了
              }catch(Exception e) {  
                  e.printStackTrace();  
              } 
          }
        });  
        customDia.setNeutralButton("查询", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				byte[] inst = {(byte) 0xAB, 0x05, 0x00, 0x00, (byte) 0xAE};
				target_chara.setValue(inst);
				mBluetoothLeService.writeCharacteristic(target_chara);
				 try  
                 {  
                     Field field = dialog.getClass().getSuperclass().getDeclaredField("mShowing");  
                     field.setAccessible(true);  
                      //设置mShowing值，欺骗android系统   
                     field.set(dialog, false);  //需要关闭的时候 将这个参数设置为true 他就会自动关闭了
                 }catch(Exception e) {  
                     e.printStackTrace();  
                 }  
			}
		});
        customDia.setPositiveButton("确定", new DialogInterface.OnClickListener() {  
              
            @Override  
            public void onClick(DialogInterface dialog, int which) {  
                // TODO Auto-generated method stub  
            	EditText[] diaInput = new EditText[4];
                String str;
                int value = 0;            
                
                diaInput[0] = (EditText) viewDia.findViewById(R.id.kt500_et);                
                diaInput[1] = (EditText) viewDia.findViewById(R.id.level_et);

                byte[] buff = {(byte) 0xAB, 0x06, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00};
                for (int i = 0; i < 2; i++) {               
                	str = diaInput[i].getText().toString().trim();
                	if (str.isEmpty()){                		
                	      try  
                          {  
                              Field field = dialog.getClass().getSuperclass().getDeclaredField("mShowing");  
                              field.setAccessible(true);  
                               //设置mShowing值，欺骗android系统   
                              field.set(dialog, false);  //需要关闭的时候 将这个参数设置为true 他就会自动关闭了
                          }catch(Exception e) {  
                              e.printStackTrace();  
                          }  
                	      Toast.makeText(Ble_Activity.this, "请填写所有参数", Toast.LENGTH_SHORT).show();
                	      return ;
                	}
                	value = Integer.parseInt(diaInput[i].getText().toString().trim());
                	buff[4+i*2] = (byte)(value / 256);
                	buff[5+i*2] = (byte)(value % 256);
                }
                byte checkeven=0;
            	for (int i = 0; i < 8; i++) {
            		checkeven ^= buff[i];
            	}
            	buff[8] = checkeven; 
                     
				target_chara.setValue(buff);		
				mBluetoothLeService.writeCharacteristic(target_chara);
                Toast.makeText(Ble_Activity.this, "ok", Toast.LENGTH_SHORT).show();
            }  
        });  
        customDia.create().show();  
    }  
	/* 
	 * ���Ͱ������Ӧ�¼�����Ҫ�����ı�������
	 */
	@Override
	public void onClick(View v)
	{
		
		switch (v.getId()) {
		case R.id.send_btn:
			byte[] buff = send_et.getText().toString().getBytes();
			int len = buff.length;
			int[] lens = dataSeparate(len);
			
			for(int i =0;i<lens[0];i++)
			{				
				String str = new String(buff, 20*i, 20);
				byte[] buff2 = hexStringToBytes(str);
				target_chara.setValue(buff2);//ֻ��һ�η���20�ֽڣ���������Ҫ�ְ���
				//�������������д����ֵ����ʵ�ַ������			
				mBluetoothLeService.writeCharacteristic(target_chara);			
			}
			if(lens[1]!=0)
			{
				String str = new String(buff, 20*lens[0], lens[1]);
				byte[] buff2 = hexStringToBytes(str);
				target_chara.setValue(buff2);
				//�������������д����ֵ����ʵ�ַ������
				mBluetoothLeService.writeCharacteristic(target_chara);
			}
			break;
		case R.id.clear_btn:
			byte[] inst1 = {(byte) 0xAB, 0x02, 0x00, 0x00, (byte) 0xA9};
			target_chara.setValue(inst1);
			mBluetoothLeService.writeCharacteristic(target_chara);
			break;		
		case R.id.data1_btn:
			if ("Stop".equals(data1_btn.getText().toString())) {
				data1_btn.setText("Start");
				byte[] inst3 = {(byte) 0xAB, 0x04, 0x00, 0x00, (byte) 0xAF};
				target_chara.setValue(inst3);				
			} else if ("Start".equals(data1_btn.getText().toString())) {
				data1_btn.setText("Stop");
				byte[] inst3 = {(byte) 0xAB, 0x03, 0x00, 0x00, (byte) 0xA8};
				target_chara.setValue(inst3);
			}
			mBluetoothLeService.writeCharacteristic(target_chara);
			break;
		case R.id.config_btn:
			showCustomDia();
			break;
		default:
			break;
		}
	}
}
