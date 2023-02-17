package com.example.zebraconnector.helpers;



import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;

import com.example.zebraconnector.Application;
import com.zebra.barcode.sdk.sms.ConfigurationUpdateEvent;
import com.zebra.scannercontrol.DCSSDKDefs;
import com.zebra.scannercontrol.DCSScannerInfo;
import com.zebra.scannercontrol.FirmwareUpdateEvent;
import com.zebra.scannercontrol.IDcsSdkApiDelegate;

import java.nio.charset.StandardCharsets;

import static com.example.zebraconnector.helpers.Constants.DEBUG_TYPE.TYPE_DEBUG;
import static com.example.zebraconnector.helpers.Constants.logAsMessage;


public class ScannerApiDelegate implements IDcsSdkApiDelegate {

    private static final String LOG_TAG = "ScannerApiDelegate";

    @Override
    public void dcssdkEventScannerAppeared(DCSScannerInfo availableScanner) {
        logAsMessage(TYPE_DEBUG, LOG_TAG, "dcssdkEventScannerAppeared Scanner Appeared");
        ScannerEngine.getInstance().setScannerDisconnectedIntention(true);
        Intent intent = new Intent("ScannerEvents");
        intent.putExtra("event", "appeared");
        intent.putExtra("scannerId", availableScanner.getScannerID() + "");
        intent.putExtra("scannerName", availableScanner.getScannerName());
        intent.putExtra("scannerHW", availableScanner.getScannerHWSerialNumber());
        Log.i(LOG_TAG, "Broadcast Sent - "+ "appeared");
        LocalBroadcastManager.getInstance(Application.get()).sendBroadcast(intent);
    }

    @Override
    public void dcssdkEventScannerDisappeared(int scannerID) {
        logAsMessage(TYPE_DEBUG, LOG_TAG, "Scanner Disappeared");
        sendLocalBroadcast("disappeared");
        ScannerEngine.getInstance().sessionTerminated();

    }

    @Override
    public void dcssdkEventCommunicationSessionEstablished(DCSScannerInfo activeScanner) {
        logAsMessage(TYPE_DEBUG, LOG_TAG, "Communication Session Established");
        onSessionEstablished(activeScanner);
        sendLocalBroadcast("session_established");
    }

    private void onSessionEstablished(DCSScannerInfo activeScanner) {
        AvailableScanner curAvailableScanner = new AvailableScanner(activeScanner);
        curAvailableScanner.setConnected(true);
        ScannerEngine.getInstance().setCurrentScanner(curAvailableScanner);
        setAutoReconnectOption(activeScanner.getScannerID(), true);
        Log.i(LOG_TAG, "Scanner Address :"+ activeScanner.getScannerHWSerialNumber());
        ScannerEngine.getInstance().saveLastScannerAddress(activeScanner.getScannerHWSerialNumber());
    }

    @Override
    public void dcssdkEventCommunicationSessionTerminated(int scannerID) {
        logAsMessage(TYPE_DEBUG, LOG_TAG, "Communication Session Terminated");
        sendLocalBroadcast("disconnected");
        ScannerEngine.getInstance().sessionTerminated();
    }

    @Override
    public void dcssdkEventBarcode(byte[] barcodeData, int barcodeType, int fromScannerID) {
        logAsMessage(TYPE_DEBUG, LOG_TAG, "Event Barcode - " + new String(barcodeData, StandardCharsets.UTF_8));
        sendBarcodeBroadcast(new String(barcodeData, StandardCharsets.UTF_8));
    }

    @Override
    public void dcssdkEventFirmwareUpdate(FirmwareUpdateEvent firmwareUpdateEvent) {
        logAsMessage(TYPE_DEBUG, LOG_TAG, "Scanner FirmwareUpdate");
    }

    @Override
    public void dcssdkEventAuxScannerAppeared(DCSScannerInfo newTopology, DCSScannerInfo auxScanner) {
        logAsMessage(TYPE_DEBUG, LOG_TAG, "Aux Scanner Appeared" );
        Intent intent = new Intent("ScannerEvents");
        intent.putExtra("event", "appeared");
        intent.putExtra("scannerId", auxScanner.getScannerID() + "");
        intent.putExtra("scannerName", auxScanner.getScannerName());
        intent.putExtra("scannerHW", auxScanner.getScannerHWSerialNumber());
        Log.i(LOG_TAG, "Broadcast Sent - "+ "appeared");
        LocalBroadcastManager.getInstance(Application.get()).sendBroadcast(intent);


    }

    @Override
    public void dcssdkEventConfigurationUpdate(ConfigurationUpdateEvent configurationUpdateEvent) {
        logAsMessage(TYPE_DEBUG, LOG_TAG, "Configuration Update");

    }


    @Override
    public void dcssdkEventImage(byte[] imageData, int fromScannerID) {
        logAsMessage(TYPE_DEBUG, LOG_TAG, "Event Image");

    }

    @Override
    public void dcssdkEventVideo(byte[] videoFrame, int fromScannerID) {
        logAsMessage(TYPE_DEBUG, LOG_TAG, "Event Video");
    }

    @Override
    public void dcssdkEventBinaryData(byte[] binaryData, int fromScannerID) {
        // todo: implement this
        logAsMessage(TYPE_DEBUG, LOG_TAG, "BinaryData Event received no.of bytes : " + binaryData.length + " for Scanner ID : " + fromScannerID);
    }
    private void sendLocalBroadcast(String event){
        Intent intent = new Intent("ScannerEvents");
        intent.putExtra("event", event);
        Log.i(LOG_TAG, "Broadcast Sent - "+ event);
        LocalBroadcastManager.getInstance(Application.get()).sendBroadcast(intent);
    }
    private void sendBarcodeBroadcast(String barcode) {
        Intent barcodeBroadcast = new Intent("ScannerEvents");
        barcodeBroadcast.putExtra("event", "barcode_scan");
        barcodeBroadcast.putExtra("barcode", barcode);

//        Intent intent = new Intent("ScannerEvents");
//        intent.putExtra("event", "barcode");
//        intent.putExtra("data", barcode);
        Log.i(LOG_TAG, "Barcode Broadcast Sent - "+ barcode);
        LocalBroadcastManager.getInstance(Application.get()).sendBroadcast(barcodeBroadcast);
    }
    private DCSSDKDefs.DCSSDK_RESULT setAutoReconnectOption(int scannerId, boolean enable) {


        DCSSDKDefs.DCSSDK_RESULT ret;
        if (ScannerEngine.getInstance().getSdkHandler() != null) {
            ret = ScannerEngine.getInstance().getSdkHandler().dcssdkEnableAutomaticSessionReestablishment(enable, scannerId);
            logAsMessage(TYPE_DEBUG, LOG_TAG, "AutoReconnectOption - "+ ret.value);
            return ret;
        }
        logAsMessage(TYPE_DEBUG, LOG_TAG, "AutoReconnectOption - DCSSDK_RESULT.DCSSDK_RESULT_FAILURE");

        return DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_FAILURE;
    }
}
