package net.chavezp.indoor_loc_ml;

import android.app.Activity;
import android.graphics.Color;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Created by chavezp on 26/06/2017.
 */

public class Suscription extends Activity implements MqttCallback {

    @Override
    public void connectionLost(Throwable cause) {

    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        String response = new String((message.getPayload()));

    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }
}