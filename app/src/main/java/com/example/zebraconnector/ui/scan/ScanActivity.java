package com.example.zebraconnector.ui.scan;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.example.zebraconnector.Application;
import com.example.zebraconnector.R;
import com.example.zebraconnector.helpers.AvailableScanner;
import com.example.zebraconnector.helpers.ScannerEngine;

import java.util.Locale;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import static com.example.zebraconnector.helpers.Constants.LOG_TAG;

public class ScanActivity extends AppCompatActivity {

    private BroadcastReceiver scannerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            TextView barcodeView = findViewById(R.id.barcode);
            String event = intent.getStringExtra("event");
            Log.i(LOG_TAG, "Broadcast Received - " + event);
            if ("barcode_scan".equals(event.toLowerCase(Locale.ENGLISH))) {
                String barcode = intent.getStringExtra("barcode");
                barcodeView.setText(String.format("%s\n%s", barcodeView.getText().toString(), barcode));
            }else if ("disconnected".equals(event.toLowerCase(Locale.ENGLISH))) {
                ScannerEngine.getInstance().disconnect();
               finish();
            }else if ("disappeared".equals(event.toLowerCase(Locale.ENGLISH))) {
                ScannerEngine.getInstance().disconnect();
                finish();
            }
        }
    };
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scsan);

        findViewById(R.id.btnPull).setOnClickListener(v -> {
            ScannerEngine.getInstance().pullTrigger();

        });
        findViewById(R.id.btnRelease).setOnClickListener(v -> {
            ScannerEngine.getInstance().releaseTrigger();
        });
        findViewById(R.id.btnDisconnect).setOnClickListener(v -> {
            AvailableScanner scanner = ScannerEngine.getInstance().getCurrentScanner();
            if (scanner != null && scanner.isConnected()) {
                ScannerEngine.getInstance().disconnect();
                finish();
            }
        });
        TextView scannerInfo = findViewById(R.id.scannerInfo);
        if(ScannerEngine.getInstance().getCurrentScanner() != null){
            String config = ScannerEngine.getInstance().getSelectedConfig().name();
            String protocol = ScannerEngine.getInstance().getSelectedProtocol().name();
            String scannerName = ScannerEngine.getInstance().getCurrentScanner().getScannerName();
            scannerInfo.setText(String.format("Name : %s\nProtocol : %s\nConfig : %s", scannerName, protocol, config));
        }else{
            scannerInfo.setText("No Scanner paired. !!");
        }

        ScannerEngine.getInstance().logConnectedDevices();

    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(scannerReceiver, new IntentFilter("ScannerEvents"));

    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(scannerReceiver);


    }
}
