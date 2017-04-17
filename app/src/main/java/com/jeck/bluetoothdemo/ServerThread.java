
package com.jeck.bluetoothdemo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;

public class ServerThread extends Thread {
    private BluetoothServerSocket serverSocket;
    private MainActivity mainActivity;

    public ServerThread(MainActivity shipsActivity) {
        this.mainActivity = shipsActivity;
        BluetoothServerSocket bluetoothServerSocket = null;
        BluetoothAdapter bluetoothAdapter = shipsActivity.getBluetoothAdapter();
        try {
            bluetoothServerSocket = bluetoothAdapter
                    .listenUsingRfcommWithServiceRecord(MainActivity.NAME, MainActivity.AGENT_UUID);
        } catch (IOException ignore) {
        }
        serverSocket = bluetoothServerSocket;
    }

    @Override
    public void run() {
        while (true) {
            try {
                BluetoothSocket bluetoothSocket = serverSocket.accept();
                mainActivity.manageConnectedSocket(bluetoothSocket);
            } catch (Exception e) {
                Log.e(ServerThread.class.getSimpleName(),
                        "Exception while obtaining Bluetooth socket.", e);
                break;
            }
        }
    }

    public void cancel() {
        try {
            serverSocket.close();
        } catch (IOException ignore) {
        }
    }
}
