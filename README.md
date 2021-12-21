# esp8266-benq-rs232-mqtt
Objective: Control this [BenQ TH530 projector](https://www.benq.eu/de-de/projector/home-entertainment/th530.html) via [its RS232 interface](https://benqimage.blob.core.windows.net/driver-us-file/RS232-commands_all%20Product%20Lines.pdf) with an [ESP8266](https://en.wikipedia.org/wiki/ESP8266), [MQTT](https://mqtt.org) and [openHAB](http://openhab.org).

<img src="https://github.com/nicolaus-hee/esp8266-benq-rs232-mqtt/blob/master/images/esp8266_rs232-ttl.jpg" width="600" />

## Features

:information_source: Read power / source / volume status  
:bulb: Read lamp mode & hours  
:zap: Trigger power (on/off) commands  
:tv: Trigger source changes (HDMI etc.)  
:mute: Change volume or mute  
:leaves: Change lamp mode  
:mega: Publish status updates via MQTT  
:ear: Listen for MQTT commands  
:point_right: Send custom commands via MQTT  
:speech_balloon: Respond to custom commands via MQTT  

## Installation

### :one: Required hardware

* ESP8266 (I used a Wemos D1 Mini)
* RS232 to TTL converter with female DB9

### :two: ESP8266 and RS232 to TTL converter

1. Wire ESP8266 and RS232 to TTL converter.  
(ESP → TTL)  
G → GND  
5V → VCC  
D4 → TXD  
RX → RXD
2. Add your MQTT broker & WiFI credentials to the `esp8266-benq-rs232-mqtt.ino` sketch, then flash it to your board.
3. Plug the DB9 connector to the `RS232` port of the projector.

:white_check_mark: *Your ESP will now publish projector status MQTT messages and listen for commands.*

### :three: openHAB (optional)

1. Make sure you have the [JsonPath](https://www.openhab.org/addons/transformations/jsonpath/) transformation service, [MQTT binding](https://www.openhab.org/addons/bindings/mqtt/) and a MQTT broker installed.
2. Create a new Generic MQTT thing, choose `34c510f090:20807f1aae` as the identifier.
3. Edit the new thing, paste the contents of `benq_thing.yaml` in the 'Code' tab and save.
4. Place `benq.items` in your `openhab-conf/items` folder (e.g. `/etc/openhab/items`)
5. Place `benq.sitemap` in your `openhab-conf/sitemaps` folder or paste the contents to your existing sitemap.

:white_check_mark: *You can now control the projector using the openHAB GUI.*

![OpenHAB projector channels](https://github.com/nicolaus-hee/esp8266-benq-rs232-mqtt/blob/master/images/openhab_channels.png)

![OpenHAB sitemap](https://github.com/nicolaus-hee/esp8266-benq-rs232-mqtt/blob/master/images/openhab_sitemap.png)

### :four: Google Assistant via openHAB (optional)

1. Complete the openHAB steps above.
2. Connect your openHAB instance to the [openHAB Cloud connector](https://www.openhab.org/addons/integrations/openhabcloud/).
3. Expose the newly created projector items to the openHAB Cloud  (Settings → openHAB Cloud → Items to Expose).
4. Ask Google Assistant to "Talk to openHAB" to link your openHAB Cloud account to your Assistant.

:white_check_mark: *A `TV` device will appear in your Google Home app and you can now control the projector via the app or with voice commands such as "mute my TV".*

![Google Home app, room view](https://github.com/nicolaus-hee/esp8266-benq-rs232-mqtt/blob/master/images/ga_room.png)

![Google Home app, device view](https://github.com/nicolaus-hee/esp8266-benq-rs232-mqtt/blob/master/images/ga_device.png)

![Google Home app, assistant dialogue](https://github.com/nicolaus-hee/esp8266-benq-rs232-mqtt/blob/master/images/ga_dialogue.png)

## MQTT implementation

`stat` topics are published by the module and contain status messages. `cmnd` topics are used to execute commands on the projector.

Topic | Payload | Comment
----- | ------- | --------
`stat/projector/STATUS` | `{"POWER":"ON","SOURCE":"HDMI","VOLUME":4, "LAMP_MODE":"ECO","LAMP_HOURS":105,"MUTE":"OFF"}` | Every 5 seconds
`cmnd/projector/POWER` | `ON`, `OFF` | Power on / off
`cmnd/projector/SOURCE` | `HDMI`, `SVID`, `VID`, `RGB`, `RGB2` | Set source
`cmnd/projector/VOLUME` | `0`...`10` | Set volume
`cmnd/projector/MUTE` | `ON`, `OFF` | (Un)mute
`cmnd/projector/LAMP_MODE` | `LNOR`, `ECO`, `SECO`, `SECO2` | Set lamp mode
`cmnd/projector/COMMAND` | See [BenQ docu](https://benqimage.blob.core.windows.net/driver-us-file/RS232-commands_all%20Product%20Lines.pdf), e.g. `VOL=?` | 
`stat/projector/COMMAND` | `{"COMMAND":"...","RESPONSE":"..."}` | Returns result of above
