Disko comes with several predefined text analysis data flow networks.

The table below contains all implementations of the `disko.flow.Processor` interface bundled with the Disko distribution. All classes listed below are to be found under the `disko.flow.processor` package.

| **Class Name** | **Description** | **Input Channels** | **Output Channels** |
|:---------------|:----------------|:-------------------|:--------------------|
|ScopeCleaner    |Remove a scope tree from the HyperGraphDB instance of the current context.|none                |none                 |
|ParagraphAnalyzer|Splits the input text into paragraphs.|SIGNAL\_DB\_CLEAN   |PARAGRAPH\_CHANNEL   |