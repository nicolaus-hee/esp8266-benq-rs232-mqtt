#include <ESP8266WiFi.h>
#include "Adafruit_MQTT.h"
#include "Adafruit_MQTT_Client.h"
#include <ArduinoOTA.h>
#include <Regexp.h>

// BEN Q SPECIFICS
#define serial_command_prepend "\r*"
#define serial_command_append "#\r"
#define serial_baud_rate 115200

// WIFI
#define WLAN_SSID       "ssid"
#define WLAN_PASS       "pass"
WiFiClient client;

//MQTT SERVER
#define AIO_SERVER      "mqtt-server"
#define AIO_SERVERPORT  1883
#define AIO_USERNAME    "mqtt-user"
#define AIO_KEY         "mqtt-pass"
Adafruit_MQTT_Client mqtt(&client, AIO_SERVER, AIO_SERVERPORT, AIO_USERNAME, AIO_KEY);
Adafruit_MQTT_Publish benq_status_pub = Adafruit_MQTT_Publish(&mqtt, "stat/projector/STATUS");
Adafruit_MQTT_Publish benq_any_command_pub = Adafruit_MQTT_Publish(&mqtt, "stat/projector/COMMAND");
Adafruit_MQTT_Subscribe benq_power_sub = Adafruit_MQTT_Subscribe(&mqtt, "cmnd/projector/POWER");
Adafruit_MQTT_Subscribe benq_volume_sub = Adafruit_MQTT_Subscribe(&mqtt, "cmnd/projector/VOLUME");
Adafruit_MQTT_Subscribe benq_source_sub = Adafruit_MQTT_Subscribe(&mqtt, "cmnd/projector/SOURCE");
Adafruit_MQTT_Subscribe benq_any_command_sub = Adafruit_MQTT_Subscribe(&mqtt, "cmnd/projector/COMMAND");

void MQTT_connect() {
  int8_t ret;

  // Stop if already connected.
  if (mqtt.connected()) {
    return;
  }

  Serial.print("Connecting to MQTT... ");

  uint8_t retries = 3;
  while ((ret = mqtt.connect()) != 0) { // connect will return 0 for connected
       Serial.println(mqtt.connectErrorString(ret));
       Serial.println("Retrying MQTT connection in 5 seconds...");
       mqtt.disconnect();
       delay(5000);  // wait 5 seconds
       retries--;
       if (retries == 0) {
         // basically die and wait for WDT to reset me
         while (1);
       }
  }
  Serial.println("MQTT connected!");
}

void setup() {
  Serial.begin(115200);
  Serial1.begin(serial_baud_rate);

  // Connect to WiFi access point.
  Serial.print("Connecting to ");
  Serial.print(WLAN_SSID);
  WiFi.hostname("benq-controller");
  wifi_station_set_hostname("benq-controller");
  WiFi.begin(WLAN_SSID, WLAN_PASS);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println();
  Serial.println("WiFi connected");
  Serial.println("IP address: ");
  Serial.println(WiFi.localIP());

  ArduinoOTA.onStart([]() {
    String type;
    if (ArduinoOTA.getCommand() == U_FLASH) {
      type = "sketch";
    } else { // U_SPIFFS
      type = "filesystem";
    }
    Serial.println("Start updating " + type);
  });
  ArduinoOTA.onEnd([]() {
    Serial.println("\nEnd");
  });
  ArduinoOTA.onProgress([](unsigned int progress, unsigned int total) {
    Serial.printf("Progress: %u%%\r", (progress / (total / 100)));
  });
  ArduinoOTA.onError([](ota_error_t error) {
    Serial.printf("Error[%u]: ", error);
    if (error == OTA_AUTH_ERROR) {
      Serial.println("Auth Failed");
    } else if (error == OTA_BEGIN_ERROR) {
      Serial.println("Begin Failed");
    } else if (error == OTA_CONNECT_ERROR) {
      Serial.println("Connect Failed");
    } else if (error == OTA_RECEIVE_ERROR) {
      Serial.println("Receive Failed");
    } else if (error == OTA_END_ERROR) {
      Serial.println("End Failed");
    }
  });
  ArduinoOTA.begin();  

  // Setup MQTT subscriptions
  mqtt.subscribe(&benq_power_sub);
  mqtt.subscribe(&benq_volume_sub);
  mqtt.subscribe(&benq_source_sub);
  mqtt.subscribe(&benq_any_command_sub);
}

void loop() {
  ArduinoOTA.handle();
  MQTT_connect();
  Adafruit_MQTT_Subscribe *subscription;

  while ((subscription = mqtt.readSubscription(5000))) {
    if (subscription == &benq_power_sub) {
      benq_send_any_command("pow="+String((char *)benq_power_sub.lastread));
      benq_publish_status();
    }
    if (subscription == &benq_volume_sub) {
      benq_set_volume(atoi((char *)benq_volume_sub.lastread));
      benq_publish_status();
    }
    if (subscription == &benq_source_sub) {
      benq_send_any_command("sour="+String((char *)benq_source_sub.lastread));
      benq_publish_status();
    }    
    if (subscription == &benq_any_command_sub) {
      benq_send_any_command((char *)benq_any_command_sub.lastread);
      benq_publish_status();
    }
  }
  benq_publish_status();
}

String serial_send_command(String serial_command) {
  Serial1.print(serial_command_prepend);
  Serial1.print(serial_command);
  Serial1.print(serial_command_append);
  
  String serial_response = Serial.readString();
  return serial_response;
}

String benq_send_any_command(String command) {
  String response = serial_send_command(command);
  String status_response = "{\"COMMAND\":\"" + command + "\",\"RESPONSE\":\"" + response + "\"}";
  benq_any_command_pub.publish(status_response.c_str());
  return response;
}

String regex(char match_array[], char match_string[]) {
  MatchState parse_result;
  char match_result[100];
  
  parse_result.Target(match_array);
  if(parse_result.Match(match_string) == REGEXP_MATCHED) {
    parse_result.GetCapture(match_result, 0);
    return String(match_result); 
  } else {
    return "UNKNOWN";
  }
}

String benq_get_power_status() {
  char current_power_status[50];
  serial_send_command("pow=?").toCharArray(current_power_status,50);
  String result = regex(current_power_status, "POW=([^#]*)");
  if(result == "UNKNOWN") {
    return "OFF";
  } else {
    return result;
  }
}

String benq_get_source_status() {
  char current_source_status[50];
  serial_send_command("sour=?").toCharArray(current_source_status,50);
  return regex(current_source_status, "SOUR=([^#]*)");
}

int benq_get_volume_status() {
  char current_volume_status[50];
  serial_send_command("vol=?").toCharArray(current_volume_status,50);
  return (regex(current_volume_status, "VOL=([^#]*)")).toInt();
}

String benq_collect_status() {
  String current_status;
  current_status += "{";
  current_status += "\"POWER\":\"" + benq_get_power_status() + "\"";
  current_status += ",";
  current_status += "\"SOURCE\":\"" + benq_get_source_status() + "\"";
  current_status += ",";
  current_status += "\"VOLUME\":\"" + String(benq_get_volume_status()) + "\"";  
  current_status += "}";
  return current_status;
}

void benq_publish_status() {
  benq_status_pub.publish(benq_collect_status().c_str());
}

void benq_set_volume(int target_volume) {
  int delta = target_volume - benq_get_volume_status();
  for(int i = 0; i <= abs(delta); i++) {
    if(delta > 0) {
      benq_send_any_command("vol=+");
    } else {
      benq_send_any_command("vol=-");
    }
  }
}
