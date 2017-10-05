/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver

/**
 * Run the NLP Server.
 */
fun main(args: Array<String>) {

  val parsedArgs = CommandLineArguments(args)

  NLPServer(
    port = parsedArgs.port,
    tokenizerModelFilename = parsedArgs.tokenizerModel,
    languageDetectorModelFilename = parsedArgs.langDetectorModel,
    frequencyDictionaryFilename = parsedArgs.freqDictionary
  ).start()
}
