package com.nikolay.halftoneplotter.bluetooth;

import android.bluetooth.BluetoothSocket;

public class SocketContainer {
    private static BluetoothSocket bluetoothSocket;

    public static BluetoothSocket getBluetoothSocket() {
        return bluetoothSocket;
    }

    public static void setBluetoothSocket(BluetoothSocket bluetoothSocket) {
        SocketContainer.bluetoothSocket = bluetoothSocket;
    }
}
