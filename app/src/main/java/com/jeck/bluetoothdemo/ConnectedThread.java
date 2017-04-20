
package com.jeck.bluetoothdemo;

import android.app.Activity;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ConnectedThread extends Thread {

    private static final int BUFFER_SIZE = 64;

    private ResultHandler resultHandler;
    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private Activity activity;
    private byte[] buffer;
    private int size;

    public ConnectedThread(ResultHandler resultHandler, Activity activity, BluetoothSocket bluetoothSocket) {
        this.resultHandler = resultHandler;
        this.activity = activity;
        this.bluetoothSocket = bluetoothSocket;
        buffer = new byte[BUFFER_SIZE];
        try {
            inputStream = bluetoothSocket.getInputStream();
            outputStream = bluetoothSocket.getOutputStream();
        } catch (Exception e) {
            Log.e(ConnectedThread.class.getSimpleName(), e.toString(), e);
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                size = inputStream.read(buffer);
                if (size > 0) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            resultHandler.handleResult(buffer,size);
                        }
                    });

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void write(byte[] bytes) {
        try {
            outputStream.write(bytes);
            outputStream.flush();
        } catch (Exception e) {
            Log.e(ConnectedThread.class.getSimpleName(), e.toString(), e);
        }
    }

    public void cancel() {
        try {
            bluetoothSocket.close();
        } catch (Exception e) {
            Log.e(ConnectedThread.class.getSimpleName(), e.toString(), e);
        }
    }
}
