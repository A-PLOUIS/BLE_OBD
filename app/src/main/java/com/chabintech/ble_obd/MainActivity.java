package com.chabintech.ble_obd;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
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

import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements BLEDevicesDialog.OnFragmentInteractionListener {
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    private static final String LOG_SCAN = "BLEScan";


    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private BluetoothGatt mBluetoothGatt;

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

    }
}
