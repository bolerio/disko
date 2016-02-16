First, create the network and makes sure that it runs correctly locally. We'll call this the logical network because - it is the blueprint (the program) of the distributed process that will be running based on it.

```
  myNetwork = new MyNetwork();
  myNetwork.start().get();
```

Then, save a copy of that network in a HGDB instance. This can be done with the help of the DistUtils class:

```
HGHandle networkHandle = DistUtils.saveDataFlowNetwork(graph, myNetwork, null);
```