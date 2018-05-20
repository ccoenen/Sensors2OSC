package org.sensors2.osc.dispatch;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.util.Log;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.sensors2.common.dispatch.DataDispatcher;
import org.sensors2.common.dispatch.Measurement;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.sensors2.osc.sensors.Parameters;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by thomas on 07.11.14.
 */
public class MqttDispatcher implements DataDispatcher {
    private List<SensorConfiguration> sensorConfigurations = new ArrayList<SensorConfiguration>();
    private MqttAndroidClient communication;
    private float[] gravity;
    private float[] geomagnetic;
    private SensorManager sensorManager;

    public MqttDispatcher(final Context context) {
        MqttConfiguration config = MqttConfiguration.getInstance();
        communication = new MqttAndroidClient(context, config.getUri(), "abc");

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);
        // mqttConnectOptions.setUserName(config.username);
        // mqttConnectOptions.setPassword(config.password.toCharArray());

        try {
            communication.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Toast.makeText(context, "connected", Toast.LENGTH_SHORT);
                    Log.d("mqtt", "connection success");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Toast.makeText(context, exception.toString(), Toast.LENGTH_LONG);
                    Log.w("mqtt", "connection failed " + exception.toString());
                }
            });
        } catch (MqttException e) {
            Toast.makeText(context, e.toString(), Toast.LENGTH_LONG);
            Log.e("mqtt", "connection failed unexpectedly " + e.toString());
        }
    }

    public void addSensorConfiguration(SensorConfiguration sensorConfiguration) {
        this.sensorConfigurations.add(sensorConfiguration);
    }

    @Override
    public void dispatch(Measurement sensorData) {
        for (SensorConfiguration sensorConfiguration : this.sensorConfigurations) {
            if (sensorConfiguration.getSensorType() == sensorData.getSensorType()) {
                if (sensorData.getValues() != null) {
                    trySend(sensorConfiguration, sensorData.getValues());
                } else {
                    trySend(sensorConfiguration, sensorData.getStringValue());
                }
            }
            if (sensorConfiguration.getSensorType() == Parameters.FAKE_ORIENTATION || sensorConfiguration.getSensorType() == Parameters.INCLINATION) {
                // Fake orientation
                if (sensorData.getSensorType() != Sensor.TYPE_ACCELEROMETER && sensorData.getSensorType() != Sensor.TYPE_MAGNETIC_FIELD) {
                    continue;
                }
                if (sensorData.getSensorType() == Sensor.TYPE_ACCELEROMETER) {
                    this.gravity = sensorData.getValues();
                }

                if (sensorData.getSensorType() == Sensor.TYPE_MAGNETIC_FIELD) {
                    this.geomagnetic = sensorData.getValues();
                }
                if (this.gravity != null && this.geomagnetic != null) {
                    float rotationMatrix[] = new float[9];
                    float inclinationMatrix[] = new float[9];

                    boolean success = this.sensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix, this.gravity, this.geomagnetic);
                    if (success) {
                        if (sensorConfiguration.getSensorType() == Parameters.FAKE_ORIENTATION) {
                            float orientation[] = new float[3];
                            this.sensorManager.getOrientation(rotationMatrix, orientation);
                            this.trySend(sensorConfiguration, orientation);
                        }
                        if (sensorConfiguration.getSensorType() == Parameters.INCLINATION) {
                            float inclination[] = new float[1];
                            inclination[0] = this.sensorManager.getInclination(inclinationMatrix);
                            this.trySend(sensorConfiguration, inclination);
                        }
                    }
                }
            }
        }
    }

    private void trySend(SensorConfiguration sensorConfiguration, float[] values) {
        if (!sensorConfiguration.sendingNeeded(values)) {
            return;
        }
        if (!communication.isConnected()) {
            Log.d("mqtt", "not connected yet");
            return;
        }

        MqttMessage m = new MqttMessage();
        m.setPayload(Float.toString(values[0]).getBytes());
        try {
            communication.publish(MqttConfiguration.getInstance().topicPrefix + sensorConfiguration.getOscParam(), m);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void trySend(SensorConfiguration sensorConfiguration, String value) {
        if (!communication.isConnected()) {
            Log.d("mqtt", "not connected yet");
            return;
        }

        MqttMessage m = new MqttMessage();
        m.setPayload(value.getBytes());
        try {
            communication.publish(MqttConfiguration.getInstance().topicPrefix + sensorConfiguration.getOscParam(), m);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    public void setSensorManager(SensorManager sensorManager) {
        this.sensorManager = sensorManager;
    }
}
