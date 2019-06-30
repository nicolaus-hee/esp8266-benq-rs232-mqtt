# esp8266-benq-rs232-mqtt
Objective: Control this [BenQ TH530 projector](https://www.benq.eu/de-de/projector/home-entertainment/th530.html) with an ESP8266 and MQTT via its RS232 interface and [openHAB](http://openhab.org). I follow [BenQ's RS232 documentation](https://benqimage.blob.core.windows.net/driver-us-file/RS232-commands_all%20Product%20Lines.pdf).

## What you need

* ESP8266 (I use model E12 on the Wemos D1 Mini breakout board)
* RS232 to TTL converter with female DB9 connector
* Basic wires

Connect pins like this:

ESP8266 | RS232-TTL
------- | ---------
G | GND
3V | VCC
D4 | TXD
RX | RXD

<img src="https://github.com/nicolaus-hee/esp8266-benq-rs232-mqtt/blob/master/images/esp8266_rs232-ttl.jpg" width="600" />

## What the code does

* Read power / source / volume status from projector
* Send power (on/off), source change, volume change commands to projector
* Publish status updates to MQTT server
* Listen for commands from MQTT server, then execute them
* Send custom commands via MQTT message
* Respond to custom commands via MQTT message

## How to use: MQTT

`stat` topics are published by the module, `cmnd` topics are listened to by the module and acted upon.

Topic | Payload | Comment
----- | ------- | --------
stat/projector/STATUS | {"POWER":"ON","SOURCE":"HDMI","VOLUME":"4"} | Published every 5 seconds
cmnd/projector/POWER | ON, OFF | Power on or off
cmnd/projector/SOURCE | HDMI, SVID, VID, RGB, RGB2 | Set source / input
cmnd/projector/VOLUME | 0...10 | Set volume
cmnd/projector/COMMAND | --> | [Any command, e.g. vol=+](https://benqimage.blob.core.windows.net/driver-us-file/RS232-commands_all%20Product%20Lines.pdf)
stat/projector/COMMAND | {"COMMAND":"...","RESPONSE":"..."} | Returns result of above

## How to use: openHAB

### Channels & items

```
Channel label: Power
MQTT state topic: stat/projector/STATUS
MQTT command topic: cmnd/projector/POWER
Incoming value transformation: JSONPATH:$.POWER
Item id: BenQ_Projector_Power
Item type: Switch

Label: Source
MQTT state topic: stat/projector/STATUS
MQTT command topic: cmnd/projector/SOURCE
Incoming value transformation: JSONPATH:$.SOURCE
Item id: BenQ_Projector_Source
Item type: String

Label: Volume
MQTT state topic: stat/projector/STATUS
MQTT command topic: cmnd/projector/VOLUME
Incoming value transformation: JSONPATH:$.VOLUME
Item id: BenQ_Projector_Volume
Item type: Number

```

When all channels are set up, this is what you should see:

![OpenHAB projector channels](https://github.com/nicolaus-hee/esp8266-benq-rs232-mqtt/blob/master/images/openhab_projector_channels.JPG)

### Sitemap

```
Switch item=BenQ_Projector_Power label="Projector"
Switch item=BenQ_Projector_Source label="Source/ input" mappings=[HDMI="HDMI",SVID="SVID",VID="VID",RGB="RGB",RGB2="RGB2"] visibility=[BenQ_Projector_Power==ON]
Setpoint item=BenQ_Projector_Volume label="Volume" minValue=0 maxValue=10 step=1 visibility=[BenQ_Projector_Power==ON]
```

This is what that looks like in Basic UI:

![OpenHAB sitemap](https://github.com/nicolaus-hee/esp8266-benq-rs232-mqtt/blob/master/images/openhab_sitemap_projector_on.JPG)
