/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver

import com.xenomachina.argparser.mainBody

/**
 * Run the NLP Server.
 */
fun main(args: Array<String>) = mainBody {

  val parsedArgs = CommandLineArguments(args)

  NLPServer(
    port = parsedArgs.port,
    tokenizerModelsDir = parsedArgs.tokenizerModelsDir,
    languageDetectorModelFilename = parsedArgs.langDetectorModel,
    cjkModelFilename = parsedArgs.cjkTokenizerModel,
    frequencyDictionaryFilename = parsedArgs.freqDictionary,
    morphologyDictionaryFilename = parsedArgs.morphoDictionary,
    neuralParserModelFilename = parsedArgs.neuralParserModel
  ).start()
}
