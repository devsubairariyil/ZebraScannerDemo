package com.example.zebraconnector;

import android.bluetooth.BluetoothDevice;

import com.zebra.scannercontrol.IDcsScannerEventsOnReLaunch;

public class Application extends android.app.Application implements IDcsScannerEventsOnReLaunch {



    private static Application application;


    public static Application get() {
        return application;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        application = this;
    }

    @Override
    public boolean onLastConnectedScannerDetect(BluetoothDevice bluetoothDevice) {
        return false;
    }

    @Override
    public void onConnectingToLastConnectedScanner(BluetoothDevice bluetoothDevice) {

    }

    @Override
    public void onScannerDisconnect() {

    }


}
