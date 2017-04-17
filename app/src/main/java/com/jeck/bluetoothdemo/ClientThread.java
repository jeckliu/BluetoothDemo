
package com.jeck.bluetoothdemo;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

public class ClientThread extends Thread {
    private static String TAG = "ClientThread";
    private BluetoothSocket bluetoothSocket;
    private MainActivity mainActivity;

    public ClientThread(MainActivity activity) {
        BluetoothDevice bluetoothDevice = activity.getBluetoothDevice();
        this.mainActivity = activity;
        try {
            bluetoothSocket = bluetoothDevice
                    .createRfcommSocketToServiceRecord(MainActivity.AGENT_UUID);
        } catch (Exception e) {
        }
    }

    @Override
    public void run() {
        mainActivity.getBluetoothAdapter().cancelDiscovery();
        try {
            bluetoothSocket.connect();
            mainActivity.manageConnectedSocket(bluetoothSocket);
        } catch (Exception e) {
            try {
                bluetoothSocket.close();
            } catch (Exception ignore) {
            }
        }
    }

    public void cancel() {
        try {
            bluetoothSocket.close();
        } catch (Exception ignore) {
        }
    }
}
