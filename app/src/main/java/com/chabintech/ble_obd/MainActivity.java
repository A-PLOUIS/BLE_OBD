package com.chabintech.ble_obd;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.UiThread;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.chabintech.ble_obd.dialog.BLEDevicesDialog;

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
    private static final UUID mServiceUUID = UUID.fromString("FFF0");
    private static final UUID mWriteUUID = UUID.fromString("FFF1");
    private static final UUID mReadUUID = UUID.fromString("FFF2");

    private String mCurrentCommand;


    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning, mIsConnected, mWaitingForResult;
    private Handler mHandler;
    private BluetoothGatt mBluetoothGatt;

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            Log.d(LOG_GATT, "Characteristic " + characteristic.getUuid().toString() + " value : " + Arrays.toString(characteristic.getValue()));
            mWaitingForResult = false;
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(LOG_GATT, "Characteristic " + characteristic.getUuid().toString() + " write : " + Arrays.toString(characteristic.getValue()));
                readOBDResult();
            }
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mIsConnected = true;
                Log.i(LOG_GATT, "******Connected to GATT server*****");
                Log.i(LOG_GATT, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
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
                }

                initialiseOBDInterface();
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
        setContentView(R.layout.activity_main);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
        devices = new HashSet<>();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        scanLeDevice(true);
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

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    Log.d(LOG_SCAN, "******Stopping BLEScan*****");
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    showDevicesDialog();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            Log.d(LOG_SCAN, "*****Starting BLEScan*****");
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            Log.d(LOG_SCAN, "******Stopping BLEScan*****");
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    @UiThread
    private void showDevicesDialog() {
        Log.d(LOG_SCAN, "Showing devices dialog");
        BLEDevicesDialog.newInstance(devices).show(getSupportFragmentManager(), "dialogdevices");
    }

    @Override
    public void connectToBLEDevice(BluetoothDevice p_device) {
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
        writeOBDCommand("AT E0");
        writeOBDCommand("AT ST " + Integer.toHexString(0xFF & 62));
        writeOBDCommand("AT SP 0");
    }

    private void writeOBDCommand(String p_command) {
        if (mIsConnected) {
            while (mWaitingForResult) {
                try {
                    this.wait(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            BluetoothGattCharacteristic writeChar = mBluetoothGatt.getService(mServiceUUID)
                    .getCharacteristic(mWriteUUID);
            byte[] data = (p_command + '\r').getBytes();

            writeChar.setValue(data);
            mBluetoothGatt.writeCharacteristic(writeChar);
            mWaitingForResult = true;
            mCurrentCommand = p_command;
        } else {
            Toast.makeText(getBaseContext(), "Can't write OBD Command\nNot Connected", Toast.LENGTH_SHORT).show();
        }
    }

    private void readOBDResult() {
        if (mIsConnected) {
            BluetoothGattCharacteristic readChar = mBluetoothGatt.getService(mServiceUUID)
                    .getCharacteristic(mReadUUID);
            mBluetoothGatt.readCharacteristic(readChar);
        } else {
            Toast.makeText(getBaseContext(), "Can't read OBD Result\nNot Connected", Toast.LENGTH_SHORT).show();
        }
    }
}
