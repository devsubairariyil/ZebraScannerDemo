package com.example.zebraconnector.helpers;


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

import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.Group;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;


import com.example.zebraconnector.R;
import com.zebra.scannercontrol.BarCodeView;
import com.zebra.scannercontrol.DCSSDKDefs;
import com.zebra.scannercontrol.DCSScannerInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static com.example.zebraconnector.helpers.Constants.LOG_TAG;


public class ZebraConnectionDialog extends DialogFragment {

    private static final String KEY_MESSAGE = "PrePickedNoCollectionDialog.message";
    private static final String KEY_TITLE = "PrePickedNoCollectionDialog.title";

    public static ZebraConnectionDialog getInstance() {
        ZebraConnectionDialog dialog = new ZebraConnectionDialog();
        return dialog;
    }


    private ZebraConnectionListener connectionListener;

    private AlertDialog alertDialog;
    private View statusView;
    private TextView connectionStatusMessage;
    private Group connectionView;
    private FrameLayout llBarcode;
    private LinearLayout zebraStatusView;
    private TextView scannerInfo;

    private void initialize() {
        if (BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            //TODO implement for Android 12 and above
//            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
//                startActivity(enableBtIntent);
//            }
            ScannerEngine.getInstance().initializeDcsSdk();
            generatePairingBarcode();
            broadcastSCAisListening();
        }

        Log.i("ParingDevice", ScannerEngine.getInstance().getSdkHandler().dcssdkGetAvailableScannersList().size() + "");
    }

    private void broadcastSCAisListening() {
        Intent intent = new Intent();
        intent.setAction("com.zebra.scannercontrol.LISTENING_STARTED");
        getContext().sendBroadcast(intent);
    }

    private void generatePairingBarcode() {
        DCSSDKDefs.DCSSDK_BT_PROTOCOL protocol = ScannerEngine.getInstance().getSelectedProtocol();
        DCSSDKDefs.DCSSDK_BT_SCANNER_CONFIG config = ScannerEngine.getInstance().getSelectedConfig();

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -1);
        BarCodeView barCodeView = ScannerEngine.getInstance().getSdkHandler().dcssdkGetPairingBarcode(protocol, config);
        if (barCodeView != null) {
            updateBarcodeView(layoutParams, barCodeView);
        } else {
            Toast.makeText(getContext(), "Barcode View could not be created.", Toast.LENGTH_LONG).show();
        }

    }

    private static final String[] BLE_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
    };

    private BroadcastReceiver scannerReceiever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getStringExtra("event");
            Log.i(LOG_TAG, "Broadcast Received - " + action);
            switch (action.toLowerCase(Locale.ENGLISH)) {
                case "appeared":
                    statusView.setBackgroundColor(Color.BLUE);
                    connectionStatusMessage.setTextColor(Color.BLUE);
                    connectionStatusMessage.setText("Connecting..");
                    break;
                case "session_established":
                    statusView.setBackgroundColor(Color.GREEN);
                    connectionStatusMessage.setTextColor(Color.GREEN);
                    connectionStatusMessage.setText("Connected");
                    //updateStatusView();
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        dismiss();
                        if (connectionListener != null) {
                            connectionListener.onConnected();
                        }
                    }, 500);
//
                    break;
                case "disappeared":
                    connectionStatusMessage.setTextColor(Color.RED);
                    connectionStatusMessage.setText("Searching for devices..");
                    statusView.setBackgroundColor(Color.RED);
                    break;
                case "disconnected":
                    connectionStatusMessage.setTextColor(Color.RED);
                    connectionStatusMessage.setText("Disconnected");
                    statusView.setBackgroundColor(Color.RED);
                    break;

            }
        }
    };

    private void updateStatusView() {
        zebraStatusView.setVisibility(View.VISIBLE);
        connectionView.setVisibility(View.GONE);
        String config = ScannerEngine.getInstance().getSelectedConfig().name();
        String protocol = ScannerEngine.getInstance().getSelectedProtocol().name();
        String scannerName = ScannerEngine.getInstance().getCurrentScanner().getScannerName();
        scannerInfo.setText(String.format("Name : %s\nProtocol : %s\nConfig : %s", scannerName, protocol, config));
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
                                } else if (!hasPermission(getContext(), BLE_PERMISSIONS[i])) {
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
                        } else {
                            initialize();
                        }
                    });

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.ErrorDialogStyle);


    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getDialog().setCanceledOnTouchOutside(false);
        setCancelable(true);
        return inflater.inflate(R.layout.dialog_zebra_pair_device, container, false);
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        llBarcode = (FrameLayout) view.findViewById(R.id.scan_to_connect_barcode);
        zebraStatusView = view.findViewById(R.id.zebraStatusView);
        connectionStatusMessage = view.findViewById(R.id.statusMessage);
        statusView = view.findViewById(R.id.connectionStatus);
        connectionView = view.findViewById(R.id.connectionView);
        scannerInfo = view.findViewById(R.id.scannerInfo);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            askForPermissions(Arrays.asList(BLE_PERMISSIONS));
        }
        view.findViewById(R.id.btnStartCounting).setOnClickListener(view1 -> {
            dismiss();
            if (connectionListener != null) {
                connectionListener.onConnected();
            }

        });

        view.findViewById(R.id.btnPull).setOnClickListener(view1 -> {
            ScannerEngine.getInstance().pullTrigger();

        });
        view.findViewById(R.id.btnRelease).setOnClickListener(view1 -> {
            ScannerEngine.getInstance().releaseTrigger();
        });

        view.findViewById(R.id.btn_close).setOnClickListener(view1 -> {
            dismiss();
        });
        view.findViewById(R.id.btnDisconnect).setOnClickListener(view1 -> {
            ScannerEngine.getInstance().disconnect();
            ScannerEngine.getInstance().initializeDcsSdk();
            //resetConnectionView();
            dismiss();
            if (connectionListener != null) {
                connectionListener.onDisconnected();
            }
        });

        view.findViewById(R.id.btnConnect).setOnClickListener(v -> {
            //Log.i("Scanner", "Scanner List size = "+ ScannerEngine.getInstance().getScannerInfoList().size());
            List<DCSScannerInfo> deviceList = ScannerEngine.getInstance().getAvailableScannerInfoList();
            if(deviceList.size() > 0) {
                ScannerEngine.getInstance().connect(deviceList.get(0));
            }else{
                Toast.makeText(getActivity(), "Please scan the barcode", Toast.LENGTH_SHORT).show();
            }
        });

        if (ScannerEngine.getInstance().isConnected()) {
            updateStatusView();
        }

    }

    private void resetConnectionView() {
        zebraStatusView.setVisibility(View.GONE);
        connectionView.setVisibility(View.VISIBLE);
    }

    private void updateBarcodeView(LinearLayout.LayoutParams layoutParams, BarCodeView barCodeView) {
        Display display = getActivity().getWindowManager().getDefaultDisplay();
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
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(scannerReceiever);

    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(scannerReceiever, new IntentFilter("ScannerEvents"));

    }

    public void setDialogActionListener(ZebraConnectionListener connectionListener) {
        this.connectionListener = connectionListener;
    }



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
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
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
        }
    }

    public void openAppSettings() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", this.getContext().getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    private boolean hasPermission(Context context, String permissionStr) {
        return ContextCompat.checkSelfPermission(context, permissionStr) == PackageManager.PERMISSION_GRANTED;
    }

    public interface ZebraConnectionListener {
        void onConnected();
        void onDisconnected();
    }
}
