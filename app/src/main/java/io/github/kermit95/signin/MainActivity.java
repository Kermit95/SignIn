package io.github.kermit95.signin;

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.sensoro.beacon.kit.Beacon;
import com.sensoro.beacon.kit.BeaconManagerListener;
import com.sensoro.cloud.SensoroManager;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    public static final int PERMISSION_REQUEST_COARSE_LOCATION = 0x01;

    private static final String TAG = "MainActivity";

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;

    /*
     * Sensoro Manager
     */
    private SensoroManager sensoroManager;
    private BeaconManagerListener beaconManagerListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initBeaconListener();
        initBroadcast();

        sensoroManager = SensoroManager.getInstance(this);
        sensoroManager.setCloudServiceEnable(true);

        activeBluetooth();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            // Android M Permission check 
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("需要蓝牙权限");
                builder.setMessage("请打开蓝牙以接受蓝牙信号");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                        }
                    }
                });
                builder.show();
            }
        }
        startSensoro();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, " 被授予COARSE_LOCATION permission");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("功能受限");
                    builder.setMessage("由于未能得到权限, 将无法接收蓝牙信号");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }
                    });
                    builder.show();
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void initBroadcast() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

        registerReceiver(new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
                    if (state == BluetoothAdapter.STATE_ON) {
                        startSensoro();
                    }
                }
            }
        }, filter);
    }

    private void initBeaconListener(){
        beaconManagerListener = new BeaconManagerListener() {
            @Override
            public void onNewBeacon(final Beacon beacon) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, beacon.getMajor() + " " + beacon.getMinor() + " detected", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onGoneBeacon(final Beacon beacon) {
                final Beacon finalBeacon = beacon;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, beacon.getMajor() + " " + beacon.getMinor() + " Gone", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onUpdateBeacon(ArrayList<Beacon> arrayList) {
            }
        };
    }

    private void startSensoro(){
        sensoroManager.setBeaconManagerListener(beaconManagerListener);
        try {
            sensoroManager.startService();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void activeBluetooth(){
        bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setNegativeButton(R.string.blue_tooth_dialog_no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            }).setPositiveButton(R.string.blue_tooth_dialog_yes, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivity(intent);
                }
            }).setTitle(R.string.blue_tooth_dialog_title);
            builder.show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sensoroManager.stopService();
    }
}
