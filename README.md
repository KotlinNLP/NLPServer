# NLPServer [![GitHub version](https://badge.fury.io/gh/KotlinNLP%2FNLPServer.svg)](https://badge.fury.io/gh/KotlinNLP%2FNLPServer) [![Build Status](https://travis-ci.org/KotlinNLP/NLPServer.svg?branch=master)](https://travis-ci.org/KotlinNLP/NLPServer)

NLPServer implements a server listening http requests on different routes, to query different modules of the [KotlinNLP](http://kotlinnlp.com/ "KotlinNLP") library.

NLPServer is part of [KotlinNLP](http://kotlinnlp.com/ "KotlinNLP").


## Getting Started

Run the server simply passing configuration parameters by command line to the
[RunServerKt](https://???/ "RunServerKt") script.

### Command line arguments

This is the help command output:
```
required arguments:
  -p PORT,                     the port listened from the server
  -t TOKENIZER_MODEL,          the filename of the model of the neural tokenizer
  -l LANGUAGE_DETECTOR_MODEL,  the filename of the model of the language detector
  -f FREQUENCY_DICTIONARY      the filename of the frequency dictionary used by the 
                               language detector
```

### Import with Maven

```xml
<dependency>
    <groupId>com.kotlinnlp</groupId>
    <artifactId>nlpserver</artifactId>
    <version>0.4.1</version>
</dependency>
```


## License

This software is released under the terms of the 
[Mozilla Public License, v. 2.0](https://mozilla.org/MPL/2.0/ "Mozilla Public License, v. 2.0")


## Contributions

We greatly appreciate any bug reports and contributions, which can be made by filing an issue or making a pull 
request through the [github page](https://github.com/KotlinNLP/NLPServer "NLPServer on GitHub").
