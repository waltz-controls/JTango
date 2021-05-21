# ezTangoAPI - Simple (ez = easy) Tango Java Client API

[![codebeat badge](https://codebeat.co/badges/15e5c44d-a40c-4d3c-a0d6-3f667c21f63a)](https://codebeat.co/projects/github-com-hzg-wpi-ez-tango-api-master)

[ ![Download](https://api.bintray.com/packages/hzgde/hzg-wpn-projects/ezTangoAPI/images/download.svg) ](https://bintray.com/hzgde/hzg-wpn-projects/ezTangoAPI/_latestVersion)

This library provides simplified client API for [JTango](https://github.com/tango-controls/JTango)

Why simplified? Compare these two code snippets:

```java
//Pure TangORB
    DeviceProxy proxy = new DeviceProxy("tango://whatever:10000/sys/tg_test/1");
    DeviceAttribute attribute = proxy.read_attribute("double_scalar");
    if(result.hasFailed()){
        throw new Exception("Can not read attribute.");
    }
    int dataFormat = result.getDataFormat()
    int dataType = result.getType()
    double result;
    switch(dataType){
        case Tango_DEV_Double:
            switch(dataFormat){
                case _SCALAR:
                    result = attribute.extractDouble()
                ...
            }
        ...
    }
    ...

//VS

//ezTangORB
    TangoProxy proxy = TangoProxies.newDeviceWrapper("tango://whatever:10000/sys/tg_test/1");
    double result = proxy.<Double>readAttribute("double_scalar");
    ...

```

## Getting started

1. Add the following repository to your settings.xml or pom.xml:

```xml
<repositories>
  <repository>
    <id>hzg-bintray</id>
    <url>http://dl.bintray.com/hzgde/hzg-wpn-projects</url>
    <releases>
      <enabled>true</enabled>
    </releases>
    <snapshots>
      <enabled>false</enabled>
    </snapshots>
  </repository>
</repositories>
```


2. Add the following dependency to your pom.xml:
```xml
    <dependency>
        <groupId>org.tango</groupId>
        <artifactId>ezTangORB</artifactId>
        <version>1.1.7</version>
    </dependency>
```



# How to use

In the code create a TangoProxy instance using one of the factory methods of the TangoProxies class:

```java
TangoProxy proxy = TangoProxies.newDeviceProxyWrapper("tango://whatever:10000/sys/tg_test/1");

//OR

//This creates interface specific Tango proxe. See below for more details
SomeTangoServer proxy = TangoProxies.newTangoProxy("tango://whatever:10000/domain/some/0",SomeTangoServer.class);
```

## Read/Write attributes

```java
//simply read attribute value
T data = proxy.readAttribute("some_attr");//may throw ClassCastException if return value does not match T

//read attribute value and time
Map.Entry<T,long> data_time = proxy.readAttributeValueTime("some_attr");

//read attribute value,time and quality
Triplet<T,long,Quality> data_time_quality = proxy.readAttributeValueTimeQuality("some_attr");

//write attribute
T data =  ...;
proxy.writeAttribute("some_attr_w",data);//may throw ClassCastException if return value does not match T
```

## Execute commands

```java
T input = ...;
V output = proxy.executeCommand("some_cmd",input);//may throw ClassCastException
```

## Handle events

Currently AttrConfig and DataReady events are not supported. Use standard TangORB API if you need them.

```java
//1st you need to subscribe to an event
TangoEvent event = TangoEvent.CHANGE;//.USER,.ARCHIVE,.PERIODIC
proxy.subscribeToEvent("some_attr",event);

//then you can add a number of listeners to the subscription
TangoEventListener listener = new TangoEventListener(){
    public void onEvent(data){...}
    public void onError(error){...}
}
//user must keep reference to this listener. See explanation below
proxy.addListener("some_attr",event,listener);

//...

//finally unsubscribe
proxy.unsubscribeFromEvent("some_attr",event);
```

Here is a small explanation: 

Implementation of the TangoProxy guarantees the following - a subscription to an event will be performed only once during the first call of the TangoProxy#subscribeToEvent method. This creates a single instance of ITangoWhateverListner, for instance, [ITangoChangeListener](https://javadoc.io/doc/org.tango-controls/JTangoCommons/9.5.17/fr/esrf/TangoApi/events/ITangoChangeListener.html) with an empty list of user defined listeners. 

User adds then listeners to this single tango event listener (TangoProxy.addListener). Effectively a response from Tango event system is forwarded to these listeners. So adding and removing listeners does not invoke network call to the remote Tango. These listeners are stored in a weakly tight list to prevent memory leaks. Therefore user must keep reference to the listeners all the time they are needed.

All this is done to prevent extensive communication when a large number of threads try to subscribe to an event.

Summarizing - subscribe to the event in intialization phase. You may add any number of temporary listener objects during the execution phase. All unused isteners will be automatically removed.

## Standard TangORB API

Since ezTangORB is just a Façade on top of TangORB you may use the standard API as well:

```java
fr.esrf.tango.DeviceProxy deviceProxy = proxy.toDeviceproxy();

fr.esrf.tango.TangoEventsAdapter eventsAdapter = proxy.toTangoEventsAdapter();
```

## Interface Specific proxy

Users may also use interface specific proxy. For instance, lets say we have some device described by this interface:

```java
//vendor provides this interface in a dedicated jar file
public interface SomeTangoDevice extends TangoProxy {

     int getIntAttr();

     void someCommand(int[] args);

}
```

Now we can create its proxy:

```java
import org.vendor.SomeTangoDevice;
//...

SomeTangoDevice device = TangoProxies.newTangoProxy("tango://whatever:10000/sys/some/0",SomeTangoDevice.class);
```

and execute commands or read attributes like this:

```java
int attrValue = device.getIntAttr();

device.someCommand(new int[]{1,2,3});
```


## Thread safety

TangoProxy implementation guarantees thread-safety for its methods.
