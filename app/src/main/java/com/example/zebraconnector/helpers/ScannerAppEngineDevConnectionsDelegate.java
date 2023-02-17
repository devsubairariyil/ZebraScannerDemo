package com.example.zebraconnector.helpers;

public class ScannerAppEngineDevConnectionsDelegate implements ScannerAppEngine.IScannerAppEngineDevConnectionsDelegate {
    @Override
    public boolean scannerHasAppeared(int scannerID) {
        return false;
    }

    @Override
    public boolean scannerHasDisappeared(int scannerID) {
        return false;
    }

    @Override
    public boolean scannerHasConnected(int scannerID) {
        return false;
    }

    @Override
    public boolean scannerHasDisconnected(int scannerID) {
        return false;
    }
}
