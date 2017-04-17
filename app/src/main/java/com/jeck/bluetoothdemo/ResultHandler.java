package com.jeck.bluetoothdemo;

import android.app.Activity;

public interface ResultHandler {
    void handleResult(byte[] ints, Activity activity);
}
