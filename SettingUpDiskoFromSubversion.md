# Obtain from SVN #

First, get the project latest version from Subversion:

svn checkout http://disko.googlecode.com/svn/trunk/ disko

# Directory Structure #

To make it easy to setup, all compile and runtime dependencies are part of the project directory structure. Some big data files needed by various NLP tools are also checked in SVN. Build scripts and config file are at the top-level. The directories are as follows:

| **Directory Name** | **Description** |
|:-------------------|:----------------|
| data               | Contains the various data files for all NLP processing tools that Disko uses: LinkGrammar, OpenNLP, GATE, Relex etc. |
| src                | Disko's own source code. |
| lib                | All Java dependencies. |
| native             | All native dependencies, further sub-divided by platform. |
| test               | Some test data and configuration files. |
| xquery-lib         | Some XQuery scripts for HTML information extraction. |

# Building #

The code is build with the ANT build system. Use 'ant -p' to see the list of tasks. Type 'ant jar' to build the `disko.jar` file.

# Running #

Disko uses [HyperGraphDB](http://www.hypergraphdb.org) to store processing results as well as for distributed processing. It also relies on the WordNet lexical database. Before you can run it, you will need to create a HyperGraphDB instance populated with WordNet data. Then you can parse a document or a single sentence on the command line and see what gets stored:

1. Download and install WordNet 2.1 from http://wordnet.princeton.edu/wordnet/download/old-versions/. Note that the more recent versions don't work because the Java parsing library JWNL hasn't kept up.

2. Create a WordNet HGDB instance by typing something like
```
ant create-wordnet-db -Dplatform=linux/x86_64 -Ddictionary=$WORDNET_HOME/dict -Ddblocation=/tmp/wordnetdb
```
Here, replace the platform parameter with the correct `native` sub-folder containing the native libraries for your platform (e.g. `windows` for Win32bit or `windows/amd64` for 64bit etc.). Replace `$WORDNET_HOME` with the installation directory of WordNet from the previous step. And put whatever location you want for your brand new HGDB instance instead of '/tmp/wordnetdb'

3. Compile disko with `ant compile`.

4. Start the link grammar server. The link grammar parser (see http://www.abisource.com/projects/link-grammar) is run as a sockets based server rather inside the Java process, due to occasional crashes that bring down the JVM. You must start it by running link-grammar-server.sh on Linux/Cygwin or link-grammar-server.bat on Windows. Both scripts point to 32bit DLLs by default, so you must edit them to change the java.library.path JVM parameter if you're running on 64bits.

5. Make a test run by typing
```
ant run -Dplatform=linux/x86_64 -Dtext=test/trivial-corpus.txt -Ddblocation=/tmp/wordnetdb
```
Again, replace the platform parameter to point to appropriate native libs for your platform, and the location of the HyperGraphDB instance if you put a different one above. You can also pass in some other text file to analyze or a single sentence.

You should see a print out of the analyzed document, sentence be sentence with all [Relex](http://wiki.opencog.org/w/RelEx) relations extracted from it.