package net.chavezp.indoor_loc_ml;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private WifiManager mWifiManager;

    final String STRING_CONN = "tcp://200.5.235.52:1883";
    final String TAG = "ml";

    private String clientId;
    private Boolean iamconnected = false;
    private Boolean scanning = false;

    private Button buttonConnect;
    private EditText editTextLugar;

    public static Button buttonRegistrar;
    public MqttAndroidClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        registerReceiver(mWifiScanReceiver,
                new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        buttonConnect = (Button) findViewById(R.id.button_connect);
        buttonConnect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                disconnect();
            }
        });

        buttonRegistrar = (Button) findViewById(R.id.button_registrar);
        buttonRegistrar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                scan();
            }
        });

        editTextLugar = (EditText) findViewById(R.id.editText);

        connect();
    }

    private final BroadcastReceiver mWifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                List<ScanResult> mScanResults = mWifiManager.getScanResults();                // add your logic here

                for (ScanResult result : mScanResults) {
                    publish("machinelearning/wifi", result.BSSID + "," + result.level + "," + editTextLugar.getText().toString());
                }
                scanning = false;
            }
        }
    };

    @Override
    protected void onStop() {
        // call the superclass method first
        super.onStop();
        //Disconnect
        disconnect();
    }

    public void connect() {
        if (iamconnected) return;

        clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(this.getApplicationContext(), STRING_CONN, clientId);
        MqttConnectOptions options = new MqttConnectOptions();

        options.setUserName("mi_usuario");
        options.setPassword("mi_clave".toCharArray());

        {
            try {
                IMqttToken token = client.connect(options);
                token.setActionCallback(new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Log.d(TAG, "onSuccess");
                        iamconnected = true;
                        refreshUI();
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        Log.d(TAG, "onFailure");
                        iamconnected = false;
                        refreshUI();
                    }
                });
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    public void refreshUI() {
        if (iamconnected) {
            //Connect button
            buttonConnect.setText("Desconectar");
            buttonConnect.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (iamconnected) disconnect();
                }
            });

            //Persistence button
            buttonRegistrar.setEnabled(true);

            //Location editText
            editTextLugar.setEnabled(true);

        }
        //Not connected
        else {
            //Connect Button
            buttonConnect.setText("Conectar");
            buttonConnect.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (!iamconnected) connect();
                }
            });

            //Persistence button
            buttonRegistrar.setEnabled(false);

            //Location editText
            editTextLugar.setEnabled(false);
        }
    }

    public void disconnect() {
        if (!iamconnected) return;
        try {
            IMqttToken disconToken = client.disconnect();
            disconToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    iamconnected = false;
                    refreshUI();

                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    //refreshUI();
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }

    }

    public void publish(String topic, String payload) {
        byte[] encodedPayload = new byte[0];
        {
            try {
                encodedPayload = payload.getBytes("UTF-8");
                MqttMessage message = new MqttMessage(encodedPayload);
                client.publish(topic, message);
            } catch (UnsupportedEncodingException | MqttException e) {
                e.printStackTrace();
            }
        }
    }

    public void suscribe(String topic, int qos){
        try {
            final IMqttToken subToken = client.subscribe(topic, qos);
            subToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    client.setCallback(new Suscription());
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    // The subscription could not be performed, maybe the user was not
                    // authorized to subscribe on the specified topic e.g. using wildcards

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void scan() {
        if (scanning) return;
        mWifiManager.startScan();
        scanning = false;
        /*    Runnable myRunnable = new Runnable() {
                @Override
                public void run() {
                    while (registering) {
                    try {

                        // Level of a Scan Result
                        List<ScanResult> wifiList = wifiManager.getScanResults();
                        for (ScanResult scanResult : wifiList) {
                            int level = WifiManager.calculateSignalLevel(scanResult.level, 5);
                            publish("machinelearning/wifi", level + "");
                        }

                        // Level of current connection
                        int rssi = wifiManager.getConnectionInfo().getRssi();
                        int level = WifiManager.calculateSignalLevel(rssi, 5);

                        publish("machinelearning/wifi", level + "");
                        Thread.sleep(1000); // Waits for 1 second (1000 milliseconds)
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                        //publish("machinelearning/wifi", editTextLugar.getText().toString());
                    }
                }
            };
        Thread myThread = new Thread(myRunnable);
        myThread.start();
        */
    }
}