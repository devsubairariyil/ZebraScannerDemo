package com.example.zebraconnector.helpers;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.example.zebraconnector.Application;
import com.zebra.scannercontrol.DCSSDKDefs;
import com.zebra.scannercontrol.DCSScannerInfo;
import com.zebra.scannercontrol.SDKHandler;

import java.util.ArrayList;
import java.util.List;

import static android.os.Looper.getMainLooper;
import static com.example.zebraconnector.helpers.Constants.HW_SERIAL_NUMBER;
import static com.example.zebraconnector.helpers.Constants.LOG_TAG;
import static com.example.zebraconnector.helpers.Constants.logAsMessage;


import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class ScannerEngine {
    private SDKHandler sdkHandler;
    private ForegroundWatcher foregroundWatcher;
    private boolean intentionallyDisconnected = false;

    private DCSSDKDefs.DCSSDK_BT_PROTOCOL selectedProtocol = DCSSDKDefs.DCSSDK_BT_PROTOCOL.SSI_BT_LE;
    private DCSSDKDefs.DCSSDK_BT_SCANNER_CONFIG selectedConfig = DCSSDKDefs.DCSSDK_BT_SCANNER_CONFIG.SET_FACTORY_DEFAULTS;
    private AvailableScanner currentlyAvailableScanner;

    private List<DCSScannerInfo> mScannerInfoList = new ArrayList<>();


    private static ScannerEngine scannerEngine = new ScannerEngine();

    private ScannerEngine(){
        ForegroundWatcher.init(Application.get());
        foregroundWatcher = ForegroundWatcher.get();
    }
    public static ScannerEngine getInstance(){
        return scannerEngine;
    }

    public void initializeDcsSdk() {
        if(isConnected()){
            return;
        }
        sdkHandler = new SDKHandler(Application.get(), false);
        sdkHandler.dcssdkEnableAvailableScannersDetection(false);

        setSelectedProtocol(DCSSDKDefs.DCSSDK_BT_PROTOCOL.SSI_BT_LE);


        sdkHandler.dcssdkSetDelegate(new ScannerApiDelegate());

        subscribeToScannerEvents();

        sdkHandler.dcssdkSetOperationalMode(DCSSDKDefs.DCSSDK_MODE.DCSSDK_OPMODE_BT_LE);
        logAsMessage(Constants.DEBUG_TYPE.TYPE_DEBUG, LOG_TAG, "SDK Initialized.");


    }
    public SDKHandler getSdkHandler(){
        return sdkHandler;
    }
    public void pullTrigger() {
        AvailableScanner scanner = getCurrentScanner();
        if(scanner != null) {
            String in_xml = "<inArgs><scannerID>" + scanner.getScannerId() + "</scannerID></inArgs>";
            executeCommandInHandler(scanner.getScannerId(), DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_DEVICE_PULL_TRIGGER, in_xml, null);
        }else{
            Log.e(LOG_TAG, "Scanner not paired.");

        }
    }

    public void releaseTrigger() {
        AvailableScanner scanner = getCurrentScanner();
        if(scanner != null) {
            String in_xml = "<inArgs><scannerID>" + scanner.getScannerId() + "</scannerID></inArgs>";
            executeCommandInHandler(scanner.getScannerId(), DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_DEVICE_RELEASE_TRIGGER, in_xml, null);
        }else{
            Log.e(LOG_TAG, "Scanner not paired.");

        }
    }
    public void aimOn() {
        AvailableScanner scanner = getCurrentScanner();
        if(scanner != null) {
            String in_xml = "<inArgs><scannerID>" + scanner.getScannerId() + "</scannerID></inArgs>";
            executeCommandInHandler(scanner.getScannerId(), DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_DEVICE_AIM_ON, in_xml, null);
        }else{
            Log.e(LOG_TAG, "Scanner not paired.");

        }
    }

    public void aimOff() {
        AvailableScanner scanner = getCurrentScanner();
        if(scanner != null) {

            String in_xml = "<inArgs><scannerID>" + scanner.getScannerId() + "</scannerID></inArgs>";
            executeCommandInHandler(scanner.getScannerId(), DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_DEVICE_AIM_OFF, in_xml, null);
        }else{
            Log.e(LOG_TAG, "Scanner not paired.");

        }
    }
    public void enableBarcodeMode() {
        AvailableScanner scanner = getCurrentScanner();
        if(scanner != null) {
            String in_xml = "<inArgs><scannerID>" + scanner.getScannerId() + "</scannerID></inArgs>";
            executeCommandInHandler(scanner.getScannerId(), DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_DEVICE_BARCODE_MODE, in_xml, null);
        }else{
            Log.e(LOG_TAG, "Scanner not paired.");

        }
    }
    public void enableScanning() {
        AvailableScanner scanner = getCurrentScanner();
        if (scanner != null) {
            String in_xml = "<inArgs><scannerID>" + scanner.getScannerId() + "</scannerID></inArgs>";
            executeCommandInHandler(scanner.getScannerId(), DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_DEVICE_SCAN_ENABLE, in_xml, null);
        }else{
            Log.e(LOG_TAG, "Scanner not paired.");

        }
    }

    public void enableBarcodeScanning() {
        AvailableScanner scanner = getCurrentScanner();
        if (scanner != null) {
            String in_xml = "<inArgs><scannerID>" + scanner.getScannerId() + "</scannerID></inArgs>";
            executeCommandInHandler(scanner.getScannerId(), DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_DEVICE_SCAN_ENABLE, in_xml, null);
        }else{
            Log.e(LOG_TAG, "Scanner not paired.");

        }
    }

    public void disableScanning() {
        AvailableScanner scanner = getCurrentScanner();
        if (scanner != null) {
            String in_xml = "<inArgs><scannerID>" + scanner.getScannerId() + "</scannerID></inArgs>";
            executeCommandInHandler(scanner.getScannerId(), DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_DEVICE_SCAN_DISABLE, in_xml, null);

        }else{
            Log.e(LOG_TAG, "Scanner not paired.");

        }
    }
    private void executeCommandInHandler(int scannerId, DCSSDKDefs.DCSSDK_COMMAND_OPCODE opcode, String inXML, String outXML){
        new Handler(getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                boolean status = executeCommand(opcode, inXML, outXML, scannerId);
                logAsMessage(Constants.DEBUG_TYPE.TYPE_DEBUG, LOG_TAG, "opcode - "+ status);
            }
        });
    }

    private boolean executeCommand(DCSSDKDefs.DCSSDK_COMMAND_OPCODE opCode, String inXML, String outXML, int scannerID) {
        SDKHandler sdkHandler = getSdkHandler();
        StringBuilder out = new StringBuilder();
        if(outXML != null && !outXML.isEmpty()){
            out.append(outXML);
        }
        if (sdkHandler != null) {
            DCSSDKDefs.DCSSDK_RESULT result = sdkHandler.dcssdkExecuteCommandOpCodeInXMLForScanner(opCode, inXML, out, scannerID);
            if (result == DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_SUCCESS)
                return true;
            else if (result == DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_FAILURE)
                return false;
        }
        return false;
    }

    public void disconnect() {
        SDKHandler sdkHandler = getSdkHandler();
        if (sdkHandler != null && isConnected()) {
            setScannerDisconnectedIntention(true);
            DCSSDKDefs.DCSSDK_RESULT ret = sdkHandler.dcssdkTerminateCommunicationSession(currentlyAvailableScanner.getScannerId());
            setCurrentScanner(null);
            ArrayList<DCSScannerInfo> scannerTreeList = new ArrayList<DCSScannerInfo>();
            sdkHandler.dcssdkGetAvailableScannersList(scannerTreeList);
            sdkHandler.dcssdkGetActiveScannersList(scannerTreeList);
            //sdkHandler.dcssdkClose();
        }
        saveLastScannerAddress("");
    }

    public void resetConnection(){
        if(isConnected()){
            disconnect();
        }
        if(sdkHandler != null) {
            sdkHandler.dcssdkClose();
        }
        currentlyAvailableScanner = null;
    }



    public void saveLastScannerAddress(String scannerAddress){
        SharedPreferences sharedPreferences = Application.get().getSharedPreferences(Constants.PREFS_NAME, 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(HW_SERIAL_NUMBER, scannerAddress);
        editor.apply();
        Log.i(LOG_TAG, "Scanner Address Saved::"+ scannerAddress);

    }
    public String getLastScannerAddress(){
        SharedPreferences settings = Application.get().getSharedPreferences(Constants.PREFS_NAME, 0);
        String address =  settings.getString(HW_SERIAL_NUMBER,"");
        Log.i(LOG_TAG, "Scanner Address Read:"+ address);
        return address;

    }
    public DCSSDKDefs.DCSSDK_BT_PROTOCOL getSelectedProtocol() {
        return selectedProtocol;
    }

    public void setSelectedProtocol(DCSSDKDefs.DCSSDK_BT_PROTOCOL selectedProtocol) {
        this.selectedProtocol = selectedProtocol;
        sdkHandler.dcssdkSetCommunicationProtocol(selectedProtocol);
    }

    public boolean isConnected(){
        return currentlyAvailableScanner != null && currentlyAvailableScanner.isConnected();
    }
    public DCSSDKDefs.DCSSDK_BT_SCANNER_CONFIG getSelectedConfig() {
        String address = ScannerEngine.getInstance().getLastScannerAddress();
        if(address == null || address.isEmpty()){
            selectedConfig = DCSSDKDefs.DCSSDK_BT_SCANNER_CONFIG.RESTORE_FACTORY_DEFAULTS;
        }else{
            selectedConfig = DCSSDKDefs.DCSSDK_BT_SCANNER_CONFIG.KEEP_CURRENT;
        }
        return selectedConfig;
    }

    public void setSelectedConfig(DCSSDKDefs.DCSSDK_BT_SCANNER_CONFIG selectedConfig) {
        this.selectedConfig = selectedConfig;
    }

    public void setScannerDisconnectedIntention(boolean connected) {
        intentionallyDisconnected = connected;
    }

    public void setCurrentScanner(AvailableScanner curAvailableScanner) {
        this.currentlyAvailableScanner = curAvailableScanner;
    }

    private void subscribeToScannerEvents(){
        int notifications_mask = 0;

        notifications_mask |= (DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_APPEARANCE.value | DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_DISAPPEARANCE.value);

        notifications_mask |= (DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SESSION_ESTABLISHMENT.value | DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SESSION_TERMINATION.value);

        notifications_mask |= (DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_BARCODE.value);

        notifications_mask |= (DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_IMAGE.value);

        sdkHandler.dcssdkSubsribeForEvents(notifications_mask);
    }
    public AvailableScanner getCurrentScanner() {
        return currentlyAvailableScanner;
    }

    public void connect(DCSScannerInfo scanner) {
        new ConnectScanner(scanner).execute();
    }
    private void sendLocalBroadcast(String event){
        Intent intent = new Intent("ScannerEvents");
        intent.putExtra("event", event);
        Log.i(LOG_TAG, "Broadcast Sent - "+ event);
        LocalBroadcastManager.getInstance(Application.get()).sendBroadcast(intent);
    }

    public List<DCSScannerInfo> getAvailableScannerInfoList() {
        mScannerInfoList.clear();
        sdkHandler.dcssdkGetAvailableScannersList(mScannerInfoList);
        return mScannerInfoList;
    }
    public List<DCSScannerInfo> getActiveScannerInfoList() {
        mScannerInfoList.clear();
        sdkHandler.dcssdkGetActiveScannersList(mScannerInfoList);
        return mScannerInfoList;
    }

    public void sessionTerminated() {
        currentlyAvailableScanner = null;
    }

    private class ConnectScanner extends AsyncTask<Void, Void, Void> {
        DCSScannerInfo scanner;

        public ConnectScanner(DCSScannerInfo scanner){
            this.scanner=scanner;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            int scannerId = scanner.getScannerID();
            DCSSDKDefs.DCSSDK_RESULT resultStop = sdkHandler.dcssdkTerminateCommunicationSession(scannerId);
            Log.e(LOG_TAG, "Scanner session terminated:"+ resultStop);


            DCSSDKDefs.DCSSDK_RESULT result = sdkHandler.dcssdkEstablishCommunicationSession(scanner.getScannerID());
            if(result == DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_SUCCESS){
                Log.e(LOG_TAG, "Scanner connected:"+ scanner.getScannerID());
                AvailableScanner curAvailableScanner = new AvailableScanner(scanner);
                curAvailableScanner.setConnected(true);
                ScannerEngine.getInstance().setCurrentScanner(curAvailableScanner);
                saveLastScannerAddress(scanner.getScannerHWSerialNumber());
                Log.e(LOG_TAG, "Scanner forcefully connected:"+ scanner.getScannerID());

                sendLocalBroadcast("session_established");

            }else{
                Log.e(LOG_TAG, "Scanner could not be connected:"+ scanner.getScannerID());
                sendLocalBroadcast("disconnected");
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }
    }

    public void logConnectedDevices(){
        List<DCSScannerInfo> pairedList = new ArrayList<>();
        sdkHandler.dcssdkGetActiveScannersList(pairedList);
        for(DCSScannerInfo scanner : pairedList){
            Log.i(LOG_TAG, "Paired Scanner - " + scanner.getScannerName());

        }
        List<DCSScannerInfo> availableList = new ArrayList<>();
        sdkHandler.dcssdkGetAvailableScannersList(availableList);
        for(DCSScannerInfo scanner : availableList){
            Log.i(LOG_TAG, "Available Scanner - " + scanner.getScannerName());
        }
        if(pairedList.isEmpty()){
            Log.e(LOG_TAG, "Paired Scanner List is Empty- ");
        }
        if(availableList.isEmpty()){
            Log.e(LOG_TAG, "Available Scanner List is Empty- ");
        }
    }
}
