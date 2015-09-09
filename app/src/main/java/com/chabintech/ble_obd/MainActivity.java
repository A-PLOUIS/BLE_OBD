package com.chabintech.ble_obd;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.UiThread;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.chabintech.ble_obd.dialog.BLEDevicesDialog;
import com.chabintech.ble_obd.utils.LogUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements BLEDevicesDialog.OnFragmentInteractionListener {
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    private static final String LOG_SCAN = "BLEScan";
    private static final String LOG_GATT = "BLE_Gatt";

    //Bluetooth UUID
    private static final UUID mServiceUUID = UUID.fromString("0000FFF0-0000-1000-8000-00805F9B34FB");
    private static final UUID mWriteUUID = UUID.fromString("0000FFF2-0000-1000-8000-00805F9B34FB");
    private static final UUID mNotifyUUID = UUID.fromString("0000FFF1-0000-1000-8000-00805F9B34FB");
    private static final UUID CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private DialogFragment mDevicesDialog;
    private ProgressDialog mWaitingDialog;
    private String mCurrentCommand;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning, mIsConnected, mWaitingForResult;
    private Handler mHandler;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(LOG_GATT, "Characteristic " + characteristic.getUuid().toString() + " value : " + Arrays.toString(characteristic.getValue()));
                LogUtils.logInfo("Read characteristic value " + Arrays.toString(characteristic.getValue()) + " in FFF1");
            } else {
                LogUtils.logInfo("Couldnt read characteristic " + mNotifyUUID.toString());
            }

            mWaitingForResult = false;
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                LogUtils.logInfo("Command wroten");
                Log.d(LOG_GATT, "Characteristic " + characteristic.getUuid().toString() + " write : " + Arrays.toString(characteristic.getValue()));
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            LogUtils.logInfo(characteristic.getUuid().toString() + " value have been updated :\nValue = " + Arrays.toString(characteristic.getValue()));
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mIsConnected = true;
                Log.i(LOG_GATT, "******Connected to GATT server*****");
                mWaitingDialog.dismiss();
                Log.i(LOG_GATT, "Attempting to start service discovery");
                mBluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                disconnectFromBLEDevice();
                mIsConnected = false;
                Log.i(LOG_GATT, "******Disconnected from GATT server******");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(LOG_GATT, "******Services discovered*****");
                for (BluetoothGattService service : gatt.getServices()) {
                    Log.i(LOG_GATT, "UUID : " + service.getUuid().toString());
                    LogUtils.logInfo(service.getUuid().toString());
                }
                suscribeToNotification();
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (descriptor.getValue() == BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) {
                    LogUtils.logInfo("Subscribed");
                    initialiseOBDInterface();
                }
            } else {
                LogUtils.logInfo("Failed Subscribed");
            }
        }
    };
    private Set<BluetoothDevice> devices;
    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            Log.d(LOG_SCAN, "Found a device");
            devices.add(device);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }


        setContentView(R.layout.activity_main);
        findViewById(R.id.btn_find_devices).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanLeDevice(true);
            }
        });

        devices = new HashSet<>();
        mHandler = new Handler();
        mWaitingDialog = new ProgressDialog(this);
        mWaitingDialog.setMessage("Scanning");
        mWaitingDialog.setIndeterminate(true);
        mWaitingDialog.setCancelable(false);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    Log.d(LOG_SCAN, "******Stopping BLEScan*****");
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    mWaitingDialog.dismiss();
                    showDevicesDialog();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            Log.d(LOG_SCAN, "*****Starting BLEScan*****");
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            mWaitingDialog.show();
        } else {
            mScanning = false;
            Log.d(LOG_SCAN, "******Stopping BLEScan*****");
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mWaitingDialog.dismiss();
        }
    }

    @UiThread
    private void showDevicesDialog() {
        Log.d(LOG_SCAN, "Showing devices dialog");
        mDevicesDialog = BLEDevicesDialog.newInstance(devices);
        mDevicesDialog.show(getSupportFragmentManager(), "dialogdevices");
    }

    @Override
    public void connectToBLEDevice(BluetoothDevice p_device) {
        mDevicesDialog.dismiss();
        mWaitingDialog.setMessage("Connecting to Device");
        mWaitingDialog.show();
        mBluetoothGatt = p_device.connectGatt(this, false, mGattCallback);
    }

    public void disconnectFromBLEDevice() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    private void initialiseOBDInterface() {
        writeOBDCommand("AT Z");
        /*
        writeOBDCommand("AT E0");
        writeOBDCommand("AT ST " + Integer.toHexString(0xFF & 62));
        writeOBDCommand("AT SP 0");*/
    }

    private void suscribeToNotification() {
        //subscribe to notification
        BluetoothGattCharacteristic characteristic = mBluetoothGatt.getService(mServiceUUID).getCharacteristic(mNotifyUUID);
        mBluetoothGatt.setCharacteristicNotification(characteristic, true);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);
    }

    private void writeOBDCommand(String p_command) {
        if (mIsConnected) {
//            while (mWaitingForResult) {
//                try {
//                    this.wait(50);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
            BluetoothGattCharacteristic writeChar = mBluetoothGatt.getService(mServiceUUID)
                    .getCharacteristic(mWriteUUID);

            LogUtils.logInfo("About to write " + p_command + " in FFF1");

            writeChar.setValue(p_command + '\r');
            mBluetoothGatt.writeCharacteristic(writeChar);
            mWaitingForResult = true;
            mCurrentCommand = p_command;
        } else {
            Toast.makeText(this, "Can't write OBD Command\nNot Connected", Toast.LENGTH_SHORT).show();
        }
    }

}
