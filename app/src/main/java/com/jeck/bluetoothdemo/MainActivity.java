
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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
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
    private Button btnSend;
    private EditText etSendContent;
    private EditText etReceiveContent;
    private RecyclerView recyclerViewBound;
    private RecyclerView recyclerViewFound;
    private MyAdapter adapterBound;
    private MyAdapter adapterFound;
    private BluetoothAdapter bluetoothAdapter;
    private ClientThread mClientThread;
    private ServerThread mServerThread;
    private List<BluetoothDevice> foundDevices = new ArrayList<>();

    private final static int BLUETOOTH_INTENT_ENABLE = 1;
    private final static int BLUETOOTH_INTENT_DISCOVERABLE = 3;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                try {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    foundDevices.add(device);
                    adapterFound.setDevices(foundDevices);
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
        tvServer = (TextView) findViewById(R.id.server);
        btnSend = (Button) findViewById(R.id.send_action);
        etSendContent = (EditText) findViewById(R.id.send_content);
        etReceiveContent = (EditText) findViewById(R.id.receive_content);

        recyclerViewBound = (RecyclerView) findViewById(R.id.recycler_view_bound);
        recyclerViewBound.setLayoutManager(new LinearLayoutManager(this));
        adapterBound = new MyAdapter(this);
        recyclerViewBound.setAdapter(adapterBound);

        recyclerViewFound = (RecyclerView) findViewById(R.id.recycler_view_found);
        recyclerViewFound.setLayoutManager(new LinearLayoutManager(this));
        adapterFound = new MyAdapter(this);
        recyclerViewFound.setAdapter(adapterFound);

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
    }

    private void initBluetoothAdapter() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            throw new RuntimeException("bluetoothAdapter is null");
        }
        if (!bluetoothAdapter.isEnabled()) {
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), BLUETOOTH_INTENT_ENABLE);
        }

        startDiscoverDevices();
    }

    private void startDiscoverDevices() {
        Set<BluetoothDevice> boundDevices = bluetoothAdapter.getBondedDevices();
        List<BluetoothDevice> boundDeviceList = new ArrayList<>();
        for (BluetoothDevice device : boundDevices) {
            boundDeviceList.add(device);
        }
        adapterBound.setDevices(boundDeviceList);

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
                startDiscoverDevices();
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
            /* receiver not registered, no big deal, ur server it's normal */
        }
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

    private class MyAdapter extends RecyclerView.Adapter<MyHolder> {
        private Context context;
        private List<BluetoothDevice> devices = new ArrayList<>();

        public MyAdapter(Context context) {
            this.context = context;
        }

        public void setDevices(List<BluetoothDevice> devices) {
            if (devices != null && devices.size() > 0) {
                this.devices = devices;
                notifyDataSetChanged();
            }
        }

        @Override
        public MyHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TextView tv = new TextView(context);
            return new MyHolder(tv, devices);
        }

        @Override
        public void onBindViewHolder(MyHolder holder, int position) {
            BluetoothDevice device = devices.get(position);
            holder.tv.setText(device.getName());
        }

        @Override
        public int getItemCount() {
            return devices.size();
        }
    }

    private class MyHolder extends RecyclerView.ViewHolder {
        private TextView tv;
        private List<BluetoothDevice> devices;

        public MyHolder(View itemView, List<BluetoothDevice> devices) {
            super(itemView);
            this.devices = devices;
            tv = (TextView) itemView;
            action();
        }

        private void action() {
            tv.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mClientThread != null) {
                        mClientThread.cancel();
                    }
                    BluetoothDevice bluetoothDevice = devices.get(getAdapterPosition());
                    mClientThread = null;
                    mClientThread = new ClientThread(MainActivity.this, bluetoothDevice);
                    mClientThread.start();
                    Toast.makeText(getApplicationContext(), "ClientThread create", Toast.LENGTH_SHORT)
                            .show();
                }
            });
        }
    }
}
