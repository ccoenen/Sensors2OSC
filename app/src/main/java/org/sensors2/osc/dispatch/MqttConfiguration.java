package org.sensors2.osc.dispatch;

import android.content.SharedPreferences;

public class MqttConfiguration {
	private static MqttConfiguration instance;
	public String host = null;
	public int port = 0;

	public String topicPrefix = "";
	public String username = null;  // TODO this might have to be implemented some day.
	public String password = "";

	private MqttConfiguration() {
	}

	public static MqttConfiguration getInstance() {
		if (instance == null) {
			instance = new MqttConfiguration();
		}
		return instance;
	}

	public void configureFromPreferences(SharedPreferences prefs) {
		this.host = prefs.getString("pref_comm_mqtt_host", "example.com");
		this.port = Integer.valueOf(prefs.getString("pref_comm_mqtt_port", "1883"));

		this.topicPrefix = prefs.getString("pref_comm_mqtt_prefix", "sensor2mqtt/");
	}

	public String getUri() {
		return "tcp://" + this.host + ":" + this.port;
	}
}
