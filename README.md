# Sensors2MQTT

Android app for sending sensor data, multitouch and NFC tags via MQTT. A very hacky fork of https://github.com/SensorApps/Sensors2OSC (sending sensor things to OSC) who did all the real work.

Extremely experimental at the moment.

- no error handling
- only first item of float arrays are sent (think of it as "just X axis")
- no extensive testing
- no work whatsoever on disconnection or settings changes (kill the app to do that)
