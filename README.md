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

## openHAB integration

### Channels & items

### Sitemap

```
Switch item=BenQ_Projector_Power label="Projector"
Switch item=BenQ_Projector_Source label="Source/ input" mappings=[HDMI="HDMI",SVID="SVID",VID="VID",RGB="RGB",RGB2="RGB2"] visibility=[BenQ_Projector_Power==ON]
Default item=BenQ_Projector_Volume label="Volume" visibility=[BenQ_Projector_Power==ON]
Setpoint item=BenQ_Projector_Volume_Target label="Target volume" minValue=0 maxValue=10 step=1 visibility=[BenQ_Projector_Power==ON]
```

### Rules

There is now way to set a target volume (e.g. "3"), only one tick up (`vol=+`) or down (`vol=-`) from the current volume. I therefore created an internal target volume item. The first rule translates the target volume into the respective number of 'volume up' or 'down' ticks and forwards them to the ESP until the target volume is reached.

```
rule "Change_Projector_Volume"
when
    Item BenQ_Projector_Volume_Target changed
then
    while((BenQ_Projector_Volume.state as Number) != (BenQ_Projector_Volume_Target.state as Number)) {
        if((BenQ_Projector_Volume_Target.state as Number) > (BenQ_Projector_Volume.state as Number)) {
            BenQ_Projector_Volume_Change.sendCommand("up")
        } else {
            BenQ_Projector_Volume_Change.sendCommand("down")
        }
        Thread::sleep(5000)
    }
    BenQ_Projector_Volume_Change.postUpdate("")
end
```

The second rule sets the target volume state to the actual volume when the latter is changed with the projector remote (or on-device buttons). This is to avoid a wrong relative volume change based on an incorrect current volume.

```
rule "Sync_Projector_Volume"
when
    Item BenQ_Projector_Volume changed
then
    if (BenQ_Projector_Volume_Change.state == "") {
        BenQ_Projector_Volume_Target.postUpdate(BenQ_Projector_Volume.state)
    }
end
```
