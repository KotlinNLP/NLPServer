# NLPServer [![GitHub version](https://badge.fury.io/gh/KotlinNLP%2FNLPServer.svg)](https://badge.fury.io/gh/KotlinNLP%2FNLPServer) [![Build Status](https://travis-ci.org/KotlinNLP/NLPServer.svg?branch=master)](https://travis-ci.org/KotlinNLP/NLPServer)

NLPServer implements a server listening http requests on different routes, to query different modules of the [KotlinNLP](http://kotlinnlp.com/ "KotlinNLP") library.

NLPServer is part of [KotlinNLP](http://kotlinnlp.com/ "KotlinNLP").


## Getting Started

Run the server simply passing configuration parameters by command line to the
[RunServerKt](https://???/ "RunServerKt") script.

### Command line arguments

This is the help command output:
```
usage: [-h] [-p PORT] [-t TOKENIZER_MODELS_DIRECTORY]
       [-l LANGUAGE_DETECTOR_MODEL] [-c CJK_TOKENIZER_MODEL]
       [-f FREQUENCY_DICTIONARY] [-m MORPHOLOGY_DICTIONARY]
       [-e PRE_TRAINED_WORD_EMB] [-n NEURAL_PARSER] [-x FRAME_EXTRACTOR]
       [-s HAN_CLASSIFIER] [-d LOCATIONS_DICTIONARY]

optional arguments:
  -h, --help                                                show this help message and exit

  -p PORT, --port PORT                                      the port listened from the server

  -t TOKENIZER_MODELS_DIRECTORY,                            the directory containing the
  --tokenizer-models-directory TOKENIZER_MODELS_DIRECTORY   serialized models of the neural
                                                            tokenizers (one per language)

  -l LANGUAGE_DETECTOR_MODEL,                               the filename of the language
  --language-detector-model LANGUAGE_DETECTOR_MODEL         detector serialized model

  -c CJK_TOKENIZER_MODEL,                                   the filename of the CJK neural
  --cjk-tokenizer-model CJK_TOKENIZER_MODEL                 tokenizer model used by the
                                                            language detector

  -f FREQUENCY_DICTIONARY,                                  the filename of the frequency
  --frequency-dictionary FREQUENCY_DICTIONARY               dictionary used by the language
                                                            detector

  -m MORPHOLOGY_DICTIONARY,                                 the directory containing the
  --morphology-dictionary MORPHOLOGY_DICTIONARY             morphology dictionaries used by
                                                            the parser (one per language)
                                                            

  -e PRE_TRAINED_WORD_EMB,                                  the directory containing the
  --pre-trained-word-emb PRE_TRAINED_WORD_EMB               pre-trained word embeddings
                                                            files, one per language (the file
                                                            name must end with the ISO 639-1
                                                            language code)

  -n NEURAL_PARSER, --neural-parser NEURAL_PARSER           the directory containing the
                                                            serialized models of the neural
                                                            parsers (one per language)

  -x FRAME_EXTRACTOR, --frame-extractor FRAME_EXTRACTOR     the directory containing the
                                                            serialized models of the frame
                                                            extractors, one per domain

  -s HAN_CLASSIFIER, --han-classifier HAN_CLASSIFIER        the directory containing the
                                                            serialized models of the HAN
                                                            classifier, one per domain

  -d LOCATIONS_DICTIONARY,                                  the filename of the serialized
  --locations-dictionary LOCATIONS_DICTIONARY               locations dictionary
```

### Import with Maven

```xml
<dependency>
    <groupId>com.kotlinnlp</groupId>
    <artifactId>nlpserver</artifactId>
    <version>0.6.1</version>
</dependency>
```


## License

This software is released under the terms of the 
[Mozilla Public License, v. 2.0](https://mozilla.org/MPL/2.0/ "Mozilla Public License, v. 2.0")


## Contributions

We greatly appreciate any bug reports and contributions, which can be made by filing an issue or making a pull 
request through the [github page](https://github.com/KotlinNLP/NLPServer "NLPServer on GitHub").
