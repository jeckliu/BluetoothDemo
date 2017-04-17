
package com.jeck.bluetoothdemo;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;

public class ConnectedThread extends AsyncTask<Void, Void, Void> {

    private static final int BUFFER_SIZE = 1024;

    private ResultHandler resultHandler;
    private InputStream inputStream;
    private OutputStream outputStream;
    private Activity activity;
    private byte[] buffer;
    private boolean repeat;

    public ConnectedThread(ResultHandler resultHandler, Activity activity) {
        this.resultHandler = resultHandler;
        this.activity = activity;
        repeat = true;
        buffer = new byte[BUFFER_SIZE];
        try {
            inputStream = Globals.bluetoothSocket.getInputStream();
            outputStream = Globals.bluetoothSocket.getOutputStream();
        } catch (Exception e) {
            Log.e(ConnectedThread.class.getSimpleName(), e.toString(), e);
        }
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            do {
                if (inputStream.read(buffer) == -1) {
                    throw new Exception("Error reading data.");
                }
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        resultHandler.handleResult(buffer, activity);
                    }
                });
            } while (repeat);
        } catch (Exception e) {
            Log.e(ConnectedThread.class.getSimpleName(), e.toString(), e);
        }
        return null;
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
            Globals.bluetoothSocket.close();
        } catch (Exception e) {
            Log.e(ConnectedThread.class.getSimpleName(), e.toString(), e);
        }
    }
}
