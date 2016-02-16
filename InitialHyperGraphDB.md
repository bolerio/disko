To create a brand new HyperGraphDB instance for text processing in Disko, do the following steps:

  1. Create a HyperGrapDB instance (a call to HGEnvironment(location) would do it).
  1. Load WordNet into the graph. See http://code.google.com/p/hypergraphdb/wiki/WordNet on how to do this.
  1. Install the DISCOApplication on the HyperGraphDB instance with the following code:
```
disko.DISCOApplication discoApp = new disko.DISCOApplication();
discoApp.setName("discoApp");
org.hypergraphdb.app.management.HGManagement.ensureInstalled(graph, discoApp);
```
where `graph` is your HGDB instance.

Also, install the Prolog HGDB integration if Prolog will be used for semantic analysis. This is done with the following code:
```
alice.tuprolog.hgdb.PrologHGDBApp prologApp = new alice.tuprolog.hgdb.PrologHGDBApp();
org.hypergraphdb.app.management.HGManagement.ensureInstalled(graph, prologApp);
```