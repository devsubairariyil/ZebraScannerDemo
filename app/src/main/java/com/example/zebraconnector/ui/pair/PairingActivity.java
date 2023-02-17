package com.example.zebraconnector.ui.pair;


import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;



import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;


import com.example.zebraconnector.R;
import com.example.zebraconnector.helpers.ScannerEngine;
import com.example.zebraconnector.helpers.ZebraConnectionDialog;
import com.example.zebraconnector.ui.scan.ScanActivity;
import com.zebra.scannercontrol.BarCodeView;
import com.zebra.scannercontrol.DCSSDKDefs;
import com.zebra.scannercontrol.DCSScannerInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static com.example.zebraconnector.helpers.Constants.LOG_TAG;


public class PairingActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 101;
    AlertDialog alertDialog;
    private static final String[] BLE_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
//            Manifest.permission.BLUETOOTH_SCAN,
//            Manifest.permission.BLUETOOTH_CONNECT,
//            Manifest.permission.BLUETOOTH_ADVERTISE
    };

    private BroadcastReceiver scannerReceiever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            View statusView = findViewById(R.id.connectionStatus);
            TextView statusMessageView = findViewById(R.id.statusMessage);
            String action = intent.getStringExtra("event");
            Log.i(LOG_TAG, "Broadcast Received - " + action);
            switch (action.toLowerCase(Locale.ENGLISH)) {
                case "appeared":
                    statusView.setBackgroundColor(Color.BLUE);
                    statusMessageView.setTextColor(Color.BLUE);
                    statusMessageView.setText(String.format("Device found \n %s", intent.getStringExtra("scannerName")));
                    if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                        Toast.makeText(context, "Bluetooth not accessible.", Toast.LENGTH_SHORT).show();

                    }
                    break;
                case "session_established":
                    statusMessageView.setTextColor(Color.GREEN);
                    statusView.setBackgroundColor(Color.GREEN);
                    statusMessageView.setText("Connected");
                   // showZebraConnectionDialog();
                    //finish();
                    startActivity(new Intent(context, ScanActivity.class));
                    break;
                case "disappeared":
                case "disconnected":
                    statusMessageView.setTextColor(Color.RED);
                    statusView.setBackgroundColor(Color.RED);
                    statusMessageView.setText("Searching for devices..");
                    ScannerEngine.getInstance().disconnect();
                    break;

            }
        }
    };

    private void showZebraConnectionDialog() {
        ZebraConnectionDialog zebraConnectionDialog = ZebraConnectionDialog.getInstance();
        zebraConnectionDialog.setDialogActionListener(new ZebraConnectionDialog.ZebraConnectionListener() {
            @Override
            public void onConnected() {
                finish();
            }

            @Override
            public void onDisconnected() {
                initialize();
            }
        });
        zebraConnectionDialog.setCancelable(false);
        zebraConnectionDialog.show(getSupportFragmentManager(), "zebra_connection");
    }

    ActivityResultLauncher<String[]> permissionsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    result -> {
                        ArrayList<Boolean> list = new ArrayList<>(result.values());
                        List<String> permissionsList = new ArrayList<>();
                        int permissionsCount = 0;
                        for (int i = 0; i < list.size(); i++) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                if (shouldShowRequestPermissionRationale(BLE_PERMISSIONS[i])) {
                                    permissionsList.add(BLE_PERMISSIONS[i]);
                                } else if (!hasPermission(PairingActivity.this, BLE_PERMISSIONS[i])) {
                                    Toast.makeText(this, "Permission not granted for " + BLE_PERMISSIONS[i], Toast.LENGTH_SHORT).show();
                                    permissionsCount++;
                                }
                            }
                        }
                        if (permissionsList.size() > 0) {
                            //Some permissions are denied and can be asked again.
                            askForPermissions(permissionsList);
                        } else if (permissionsCount > 0) {
                            //Show alert dialog
                            showPermissionDialog();
                        }else{
                            initialize();
                        }
                    });
    private FrameLayout llBarcode;

    private void initialize() {
        if (BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            //TODO implement for Android 12 and above
//            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
//                startActivity(enableBtIntent);
//            }
            ScannerEngine.getInstance().initializeDcsSdk();
            ScannerEngine.getInstance().logConnectedDevices();
            llBarcode = (FrameLayout) findViewById(R.id.scan_to_connect_barcode);
            generatePairingBarcode();
            broadcastSCAisListening();
        }else{
            Toast.makeText(this, "Bluetooth not accessible.", Toast.LENGTH_SHORT).show();
        }

        Log.i("ParingDevice", ScannerEngine.getInstance().getSdkHandler().dcssdkGetAvailableScannersList().size() + "");
    }

    private void broadcastSCAisListening() {
        Intent intent = new Intent();
        intent.setAction("com.zebra.scannercontrol.LISTENING_STARTED");
        sendBroadcast(intent);
    }

    private void generatePairingBarcode() {
        DCSSDKDefs.DCSSDK_BT_PROTOCOL protocol = ScannerEngine.getInstance().getSelectedProtocol();
        DCSSDKDefs.DCSSDK_BT_SCANNER_CONFIG config = ScannerEngine.getInstance().getSelectedConfig();

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -1);
        BarCodeView barCodeView = ScannerEngine.getInstance().getSdkHandler().dcssdkGetPairingBarcode(protocol, config);
        if (barCodeView != null) {
            updateBarcodeView(layoutParams, barCodeView);
        } else {
            Toast.makeText(this, "Barcode View could not be created.", Toast.LENGTH_LONG).show();
        }

    }



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zebra_pair_device);

        if (ScannerEngine.getInstance().isConnected()) {
           showZebraConnectionDialog();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            askForPermissions(Arrays.asList(BLE_PERMISSIONS));
        }

        //Toolbar toolbar = findViewById(R.id.tool_bar);
      //  ((TextView) toolbar.findViewById(R.id.txtTitleToolbar)).setText("Pair Device");

        findViewById(R.id.btnConnect).setOnClickListener(view -> {
            //Log.i("Scanner", "Scanner List size = "+ ScannerEngine.getInstance().getScannerInfoList().size());
            List<DCSScannerInfo> deviceList = ScannerEngine.getInstance().getAvailableScannerInfoList();
            if(deviceList.size() > 0) {
                ScannerEngine.getInstance().connect(deviceList.get(0));
            }else{
                Toast.makeText(this, "Please scan the barcode", Toast.LENGTH_LONG).show();
            }
        });
    }



    private void updateBarcodeView(LinearLayout.LayoutParams layoutParams, BarCodeView barCodeView) {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

        int orientation = this.getResources().getConfiguration().orientation;
        int x = width * 9 / 10;
        int y = x / 3;

        barCodeView.setSize(x, y);
        llBarcode.addView(barCodeView, layoutParams);
    }


    @Override
    protected void onPause() {
        super.onPause();
       // LocalBroadcastManager.getInstance(this).unregisterReceiver(scannerReceiever);

    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(scannerReceiever, new IntentFilter("ScannerEvents"));

    }


    /**
     * Method to request required permissions with the permission launcher
     *
     * @param permissionsList list of permissions
     */
    private void askForPermissions(List<String> permissionsList) {
        String[] newPermissionStr = new String[permissionsList.size()];
        for (int i = 0; i < newPermissionStr.length; i++) {
            newPermissionStr[i] = permissionsList.get(i);
        }
        if (newPermissionStr.length > 0) {
            permissionsLauncher.launch(newPermissionStr);
        } else {
            showPermissionDialog();
        }


    }

    private void showPermissionDialog() {
        String permissionText = "Some permissions are need to be allowed to use this app without any problems.";
       Toast.makeText(this, permissionText, Toast.LENGTH_SHORT).show();

        /*AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permission required")
                .setCancelable(false)
                .setMessage("Some permissions are need to be allowed to use this app without any problems.")
                .setPositiveButton("Settings", (dialog, which) -> {
                    //askForPermissions(permissionsList);
                    openAppSettings();
                    dialog.dismiss();
                });
        if (alertDialog == null) {
            alertDialog = builder.create();
            if (!alertDialog.isShowing()) {
                alertDialog.show();
            }
        }*/
    }

    public void openAppSettings() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", this.getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    private boolean hasPermission(Context context, String permissionStr) {
        return ContextCompat.checkSelfPermission(context, permissionStr) == PackageManager.PERMISSION_GRANTED;
    }
}
