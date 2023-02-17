package com.example.zebraconnector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.Toast;

import com.example.zebraconnector.helpers.Constants;

import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {

    private final BroadcastReceiver onNotification = new BroadcastReceiver() {
        public void onReceive(Context ctxt, Intent i) {

            //Since the application is in foreground, show a dialog.
            Toast.makeText(ctxt, i.getStringExtra(Constants.NOTIFICATIONS_TEXT), Toast.LENGTH_SHORT).show();

            //Abort the broadcast since it has been handled.
            abortBroadcast();
        }
    };
    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(Constants.ACTION_SCANNER_CONNECTED);
        filter.addAction(Constants.ACTION_SCANNER_DISCONNECTED);
        filter.addAction(Constants.ACTION_SCANNER_AVAILABLE);
        filter.addAction(Constants.ACTION_SCANNER_CONN_FAILED);

        //Use a positive priority
        filter.setPriority(2);
        registerReceiver(onNotification, filter);
    }
}
