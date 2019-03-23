# esp8266-benq-rs232-mqtt
Control this [BenQ TH530 projector](https://www.benq.eu/de-de/projector/home-entertainment/th530.html) with an ESP8266 and MQTT via its RS232 interface.

## Objective

Integrate my BenQ projector into [openHAB](https://openhab.org) to control it with MQTT using its [officially documented](https://benqimage.blob.core.windows.net/driver-us-file/RS232-commands_all%20Product%20Lines.pdf) RS232 interface.

## What you need

* ESP8266 (I use model E12 on the Wemos D1 Mini Lite breakout board)
* RS232 to TTL converter with female DB9 connector
* Basic wires

## What the code does

* Read power / source / volume status from projector
* Send power (on/off), source change, volume change commands to projector
* Publish status updates to MQTT server
* Listen for commands from MQTT server, then execute them
* Send custom commands via MQTT message
* Respond to custom commands via MQTT message

## openHAB integration

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

This is what that looks like in Paper UI:

![OpenHAB sitemap](https://github.com/nicolaus-hee/esp8266-benq-rs232-mqtt/blob/master/images/openhab_sitemap_projector_on.JPG)
