
package com.jeck.bluetoothdemo;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity implements ResultHandler {
    public final static String NAME = "future-agent";
    public final static UUID AGENT_UUID = UUID.nameUUIDFromBytes(NAME.getBytes());

    private TextView tvServer;
    private TextView tvClient;
    private Button btnSend;
    private EditText etSendContent;
    private EditText etReceiveContent;
    private TextView tvBoundDevices;
    private TextView tvFoundDevices;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;
    private ClientThread mClientThread;
    private ServerThread mServerThread;
    private List<BluetoothDevice> foundDevices;

    private final static int BLUETOOTH_INTENT_ENABLE = 1;
    private final static int BLUETOOTH_INTENT_DISCOVERABLE = 3;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                try {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    foundDevices.add(device);
                    StringBuilder builder = new StringBuilder();
                    for(BluetoothDevice device1 : foundDevices){
                        builder.append(device1.getName());
                        builder.append(",");
                    }
                    tvFoundDevices.setText(builder);
                } catch (Exception ignore) {
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initBluetoothAdapter();
    }

    private void initView() {
        tvClient = (TextView) findViewById(R.id.client);
        tvServer = (TextView) findViewById(R.id.server);
        btnSend = (Button) findViewById(R.id.send_action);
        etSendContent = (EditText) findViewById(R.id.send_content);
        etReceiveContent = (EditText) findViewById(R.id.receive_content);
        tvBoundDevices = (TextView) findViewById(R.id.bound_devices);
        tvFoundDevices = (TextView) findViewById(R.id.found_devices);

        btnSend.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mConnectedThread == null) {
                    Toast.makeText(getApplicationContext(), "ConnectedThread is null",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                mConnectedThread.write(etSendContent.getText().toString().getBytes());
                Toast.makeText(getApplicationContext(), "send:" + etSendContent.getText().toString(),
                        Toast.LENGTH_SHORT).show();
            }
        });
        tvServer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE),
                        BLUETOOTH_INTENT_DISCOVERABLE);
            }
        });
        tvClient.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                for(BluetoothDevice device : foundDevices){
                    if(device.getName().equals("阿杰")){
                        bluetoothDevice = device;
                    }
                }
                if (mClientThread != null) {
                    mClientThread.cancel();
                }
                mClientThread = null;
                mClientThread = new ClientThread(MainActivity.this);
                mClientThread.start();
                Toast.makeText(getApplicationContext(), "ClientThread create", Toast.LENGTH_SHORT)
                        .show();
            }
        });
    }

    private void initBluetoothAdapter() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            throw new RuntimeException("bluetoothAdapter is null");
        }
        if (!bluetoothAdapter.isEnabled()) {
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),BLUETOOTH_INTENT_ENABLE);
        }
        foundDevices = new ArrayList<>();
        Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();
        StringBuilder builder = new StringBuilder();
        for(BluetoothDevice device1 : devices){
            builder.append(device1.getName());
            builder.append(",");
        }
        tvBoundDevices.setText(builder);
        startDiscoverDevices();
    }

    private void startDiscoverDevices(){
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        } else {
            bluetoothAdapter.startDiscovery();
            registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        }
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    public void manageConnectedSocket(BluetoothSocket bluetoothSocket) {
        if (bluetoothSocket != null) {
            Globals.bluetoothSocket = bluetoothSocket;
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    startListenThread();
                }
            });
        }
    }

    private ConnectedThread mConnectedThread;

    private void startListenThread() {
        final MainActivity mainsActivity = this;
        mConnectedThread = new ConnectedThread(mainsActivity, mainsActivity);
        mConnectedThread.execute();
        Toast.makeText(getApplicationContext(), "ConnectedThread create", Toast.LENGTH_SHORT)
                .show();
    }

    @Override
    public void handleResult(byte[] parsedData, Activity activity) {
        String val = new String(parsedData);
        etReceiveContent.setText("");
        etReceiveContent.setText(val);
        Toast.makeText(getApplicationContext(), "recv:" + val, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case BLUETOOTH_INTENT_DISCOVERABLE:
                if (mServerThread != null) {
                    mServerThread.cancel();
                }
                mServerThread = null;
                mServerThread = new ServerThread(this);
                mServerThread.start();
                Toast.makeText(getApplicationContext(), "ServerThread create", Toast.LENGTH_SHORT)
                        .show();
                return;
            case BLUETOOTH_INTENT_ENABLE:
//                startDiscoverDevices();
                return;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(receiver);
        } catch (Exception ignore) {
            /* receiver not registered, no big deal, ur server it's normal */ }
        invalidateBluetooth();
    }

    private void invalidateBluetooth() {
        if (mServerThread != null) {
            mServerThread.cancel();
        }
        if (mClientThread != null) {
            mClientThread.cancel();
        }
//        if (bluetoothAdapter != null) {
//            bluetoothAdapter.disable();
//            Toast.makeText(getApplicationContext(), "Bluetooth close", Toast.LENGTH_SHORT).show();
//        }
    }
}
