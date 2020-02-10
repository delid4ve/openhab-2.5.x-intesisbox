# IntesisBox Binding

This binding connects to WiFi [IntesisBox](http://www.intesisbox.com/) devices, speaking an ASCII protocol.
It does _not_ support [IntesisHome](http://www.intesishome.com/) devices. This binding has only been tested against a FJ-RC-WMP-1 IntesisBox.

## Supported Things

This binding only supports one thing type:

| Thing      | Thing Type | Description                                                            |
|------------|------------|------------------------------------------------------------------------|
| intesisbox | Thing      | Represents a single thermostat                                         |

## Discovery

Discovery is done via a proprietary UDP broadcast. Discovered things will be added to the inbox and configured by MAC address.
You can also configure a static IP on the individual thing, or manually add a thing via IP address.

## Thing Configuration

You can manually configure an intesisbox thing. If you provide only a MAC Address, it will go through the discovery
process to determine its current IP before connecting. Alternately, you can provide an IP Address for direct connection
without discovery.

## Channels

| Channel ID | Item Type          | Description                                                           | Possible Values |
|------------|--------------------|-----------------------------------------------------------------------|-|
| onoff      | Switch             | Turns power on/off for your climate system.                           | ON, OFF |
| mode       | String             | The heating/cooling mode.                                             | AUTO, HEAT, COOL, DRY, FAN |
| setptemp   | Number:Temperature | The currently set target temperature.                                 | |
| ambtemp    | Number:Temperature | (Readonly) The ambient air temperature.                               | |
| fansp      | String             | Fan speed (if applicable)                                             | AUTO, 1-9 |
| vaneud     | String             | Control of up/down vanes (if applicable)                              | AUTO, 1-9, SWING, PULSE |
| vanelr     | String             | Control of left/right vanes (if applicable)                           | AUTO, 1-9, SWING, PULSE |
| errstatus  | String             | (Readonly) A description of an error coming from your climate system. | |
| errcode    | Number             | (Readonly) An error code coming from your climate system.             | |

Note that individual A/C units may not support all channels, or all possible values for those channels.
For example, not all A/C units have controllable vanes. Or fan speed may be limited to 1-4, instead of all of 1-9.
The set point temperature is also limited to a device specific range. For set point temperature, sending an invalid value
will cause it to choose the minimum/maximum allowable value as appropriate. The device will also round it to
whatever step size it supports. For all other channels, invalid values
are ignored.

## Full Example

```intesis.items
Group gLoftThermostat "Loft Minisplit" [ "Thermostat" ]

Switch LoftMinisplit_Switch "Power" { channel="intesisbox:intesisbox:001dc9835e29:onoff" }
String LoftMinisplit_Mode "Mode" { channel="intesisbox:intesisbox:001dc9835e29:mode" }
Number:Temperature LoftMinisplit_SetPoint "Set Point" (gLoftThermostat) [ "TargetTemperature" ] { channel="intesisbox:intesisbox:001dc9835e29:setptemp" }
Number:Temperature LoftMinisplit_AmbTemp "Ambient Temperature" (gLoftThermostat) [ "CurrentTemperature" ] { channel="intesisbox:intesisbox:001dc9835e29:ambtemp" }
```
