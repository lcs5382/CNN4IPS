package yg.devp.cnn4ips;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import yg.devp.util.SignalDTO;
import yg.devp.util.Useful;

public class BLEActivity extends AppCompatActivity {

    private static String TAG = "SCAN";

    // BLE
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private BluetoothAdapter mBluetoothAdapter;
    private List<ScanFilter> filters;

    // Etc
    private int REQUEST_ENABLE_BT = 1;
    private String beacon1MacAddress = "F7:16:EF:05:86:47"; // beacon1 ID(lux beacon)
    private String beacon2MacAddress = "C2:01:19:00:03:AF"; // beacon2 ID
    private String beacon3MacAddress = "C2:01:19:00:03:B0"; // beacon3 ID
    private String beacon4MacAddress = "C2:01:19:00:03:B1"; // beacon4 ID
    private String cellNumber = "0"; // cellNumber
    private static int buttonType = 0; // 1: reset, 2: query, 3: save, 4: learn
    private static int modelType = 0;  // 5: modelA, 6: modelB

    private static int setNumber = 0; // beacon set's number
    private static int setNumberForLearning = 0; // beacon set's number

    //버튼을 상수화
    private final static int BTN_RESET = 1;
    private final static int BTN_QUERY = 2;
    private final static int BTN_SAVE  = 3;
    private final static int BTN_LEARN = 4;
    private final static int BTN_MODEL_A = 5;
    private final static int BTN_MODEL_B = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Need Location Permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, Useful.PERMISSION_REQUEST_COARSE_LOCATION);
        }

        setupBLE();
        cellNumber = "0";
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
            settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
            filters = new ArrayList<ScanFilter>();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void setupBLE() {
        // Use this check to determine whether BLE is supported on the device.
        // Then you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "이 디바이스는 BLE를 지원하지 않습니다.", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter. For API level 18 and above,
        // get a reference to BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "이 디바이스는 BLE를 지원하지 않습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }

    // 블루투스 신호 스캔
    protected boolean scanLeDevice(final boolean enable) {
        if (enable) {
            if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
//                Toast.makeText(this, "Run", Toast.LENGTH_SHORT).show();
                mLEScanner.startScan(filters, settings, mScanCallback);
            }
        } else {
            if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
//                Toast.makeText(this, "Stop", Toast.LENGTH_SHORT).show();
                mLEScanner.stopScan(mScanCallback);
            }
        }
        return enable;
    }

    // BLE signal 수신 시 호출되는 함수
    private ScanCallback mScanCallback = new ScanCallback() {
        // Save signals
        SignalDTO signalDTO = new SignalDTO();

        @Override
        public void onScanResult(int callbackType, ScanResult result) {

            final BluetoothDevice device = result.getDevice();
            Log.i("SCAN1", "[" + device.getName() + "], ByteArray:" + Useful.printByteArray(result.getScanRecord().getBytes()));

            if (getModelType() == BTN_MODEL_A){
               signalDTO.setModelName("model_a");
            }
            else if (getModelType() == BTN_MODEL_B){
                signalDTO.setModelName("model_b");
            }

            //비콘신호값들 저장
            if (device.getAddress().equals(beacon1MacAddress)) {
                signalDTO.setSignal1(result.getRssi());
            }
            else if (device.getAddress().equals(beacon2MacAddress)) {
                signalDTO.setSignal2(result.getRssi());
            }
            else if (device.getAddress().equals(beacon3MacAddress)) {
                signalDTO.setSignal3(result.getRssi());
            }
            else if (device.getAddress().equals(beacon4MacAddress)) {
                signalDTO.setSignal4(result.getRssi());
            }

            // if you received all signals, then you access cnn4ips server
            if (signalDTO.isFull()) {
                Log.i("beacon1", "[b1]:" + signalDTO.getSignal1());
                Log.i("beacon2", "[b2]:" + signalDTO.getSignal2());
                Log.i("beacon3", "[b3]:" + signalDTO.getSignal3());
                Log.i("beacon4", "[b4]:" + signalDTO.getSignal4());

                String modelName = signalDTO.getModelName();
                int b1Rssi = signalDTO.getSignal1();
                int b2Rssi = signalDTO.getSignal2();
                int b3Rssi = signalDTO.getSignal3();
                int b4Rssi = signalDTO.getSignal4();


                String queryUrl = Useful.URL_QUERY + modelName + "/" + b1Rssi + "/" + b2Rssi + "/" + b3Rssi + "/" + b4Rssi + "/";
                String saveUrl = Useful.URL_SAVE + modelName + "/" + b1Rssi + "/" + b2Rssi + "/" + b3Rssi + "/" + b4Rssi +"/" + cellNumber + "/" + setNumberForLearning + "/";
                String learnUrl = Useful.URL_LEARN + setNumberForLearning + "/";

                signalDTO.empty();

                if (buttonType == BTN_QUERY) {
                    new MainActivity.CNN4IPSNetworkTask(queryUrl, null).execute();
                } else if (buttonType == BTN_SAVE) {
                    if (setNumber > 0) {
                        setNumber--;
                        Log.i("url", "url:" + saveUrl);
                        new MainActivity.CNN4IPSNetworkTask(saveUrl, null).execute();
                    } else {
                        scanLeDevice(false);
                        setNumberForLearning = 0;
                        new MainActivity.CNN4IPSNetworkTask(learnUrl, null).execute();
                    }
                }

            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                Log.i("SCAN2", "ScanResult - Results:" + sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "Scan Failed:Error Code: " + errorCode);
        }
    };

    // setter : Cell number & Set number
    protected void setCellandSetNumber(String cellNumber, int setNumber) {
        this.cellNumber = cellNumber;
        this.setNumber = setNumber;
        this.setNumberForLearning = setNumber;
    }

    protected void setModelType(int modelType){
        this.modelType = modelType;
    }

    public static int getModelType() {
        return modelType;
    }

    protected void setButtonType(int buttonType) {
        this.buttonType = buttonType;
    }

    public static int getButtonType() {
        return buttonType;
    }

    public static int getSetNumber() {
        return setNumber;
    }

    public static int getSetNumberForLearning() {
        return setNumberForLearning;
    }

    // Need Location Permission
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case Useful.PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, yay! Start the Bluetooth device scan.
                } else {
                    // Alert the user that this application requires the location permission to perform the scan.
                }
            }
        }
    }
}
