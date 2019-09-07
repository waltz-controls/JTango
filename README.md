# RxJTango
Reactive Streams for Tango Events

The intention of this project is to implement reactive streams for Tango Controls. 

This will be done following [reactive-streams-jvm](https://github.com/reactive-streams/reactive-streams-jvm) specification.

The ultimate goal is to allow clients to executes Tango Controls sequences using builder pattern:

```java
new TangoClient()
    .executeCommand(host,device,command,argin)
    .writeAttribute(host,device,attr,value)
    .executeCommand(host,device,command1,argin)
    .readAttribute(host,device,attr2)
    .subscribe(...);
```

> Note: host, device etc can be of any valid value, i.e. we can execute command Z on host X, device Y then write attribute C on host A, device B etc

The above example builds a sequence of actions terminated by the `subscribe` method. Under the hood it chains a number of Publisher's. Subscriber will get the results of each step in a stream.

*Concerns*: pass the result of the previous step

Another "straightforward" application of the streams is Tango events.

Client should be able to combine multiple event streams, filter them, map etc
