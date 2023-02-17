package com.example.zebraconnector.helpers;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;

import com.example.zebraconnector.Application;
import com.zebra.scannercontrol.DCSSDKDefs;
import com.zebra.scannercontrol.DCSScannerInfo;

import static com.example.zebraconnector.helpers.Constants.DEBUG_TYPE.TYPE_DEBUG;
import static com.example.zebraconnector.helpers.Constants.logAsMessage;


public class ScannerMessageHandler extends Handler {
    private static final String TAG = "ScannerMessageHandler";

    public ScannerMessageHandler(Looper looper){
        super(looper);
    }
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case Constants.IMAGE_RECEIVED:
                logAsMessage(TYPE_DEBUG, TAG, "Image Received");
                break;
            case Constants.VIDEO_RECEIVED:
                logAsMessage(TYPE_DEBUG, TAG, "Video Received");
                //Toast.makeText(getApplicationContext(),"Image event received 000000000",Toast.LENGTH_SHORT).show();
                break;
            case Constants.FW_UPDATE_EVENT:
                logAsMessage(TYPE_DEBUG, TAG, "FW_UPDATE_EVENT Received.");

                break;
            case Constants.BARCODE_RECEIVED:
                logAsMessage(TYPE_DEBUG, TAG, "Barcode Received");
                Barcode barcode = (Barcode) msg.obj;
                logAsMessage(TYPE_DEBUG, TAG, "Barcode Received " + barcode.toString());

                break;
            case Constants.SESSION_ESTABLISHED:
                // delegate event - scannerHasConnectionEstablished();
                DCSScannerInfo activeScanner = (DCSScannerInfo) msg.obj;
                boolean notificaton_processed = false;
                AvailableScanner curAvailableScanner = new AvailableScanner(activeScanner);
                curAvailableScanner.setConnected(true);
                ScannerEngine.getInstance().setCurrentScanner(curAvailableScanner);
                setAutoReconnectOption(activeScanner.getScannerID(), true);

                sendLocalBroadcast("session_established");
                break;
            case Constants.SESSION_TERMINATED:
                int scannerID = (Integer) msg.obj;
                String scannerName = "";
                notificaton_processed = false;
                sendLocalBroadcast("disconnected");
                break;

            case Constants.SCANNER_APPEARED:

            case Constants.AUX_SCANNER_CONNECTED:
                notificaton_processed = false;

                DCSScannerInfo availableScanner = (DCSScannerInfo) msg.obj;

                //only on appeared
                if(msg.what==Constants.SCANNER_APPEARED){
                    //scannerHasAppearedOnStart(availableScanner);
                }
                sendLocalBroadcast("appeared");
                break;
            case Constants.SCANNER_DISAPPEARED:
                logAsMessage(TYPE_DEBUG, TAG, "ScannerAppEngine:dcssdkEventScannerDisappeared: SCANNER_DISAPPEARED");
                sendLocalBroadcast("disappeared");

                break;
            case Constants.CONFIG_UPDATE_EVENT:
                logAsMessage(TYPE_DEBUG, TAG, "CONFIGURATION_UPDATE_EVENT Received. Client count");

                break;
        }
    }

    public DCSSDKDefs.DCSSDK_RESULT setAutoReconnectOption(int scannerId, boolean enable) {
        DCSSDKDefs.DCSSDK_RESULT ret;
        if (ScannerEngine.getInstance().getSdkHandler() != null) {
            ret = ScannerEngine.getInstance().getSdkHandler().dcssdkEnableAutomaticSessionReestablishment(enable, scannerId);
            return ret;
        }
        return DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_FAILURE;
    }

    private void sendLocalBroadcast(String event){
        Intent intent = new Intent("ScannerEvents");
        intent.putExtra("event", event);
        Log.i(TAG, "Broadcast Sent - "+ event);
        LocalBroadcastManager.getInstance(Application.get()).sendBroadcast(intent);
    }
}
