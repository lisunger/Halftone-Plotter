package com.nikolay.halftoneplotter.bluetooth;

public interface BluetoothResponseListener {
    public void onInstructionExecuted();
    public void onReceiveCoordinates(int[] coordinates);
}
