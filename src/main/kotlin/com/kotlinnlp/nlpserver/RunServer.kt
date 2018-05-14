/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver

import com.kotlinnlp.languagedetector.LanguageDetector
import com.kotlinnlp.neuralparser.NeuralParser
import com.kotlinnlp.neuraltokenizer.NeuralTokenizer
import com.kotlinnlp.nlpserver.commands.DetectLanguage
import com.kotlinnlp.nlpserver.commands.NLPBuilder
import com.kotlinnlp.nlpserver.commands.Parse
import com.kotlinnlp.nlpserver.commands.Tokenize
import com.xenomachina.argparser.mainBody

/**
 * Run the NLP Server.
 */
fun main(args: Array<String>) = mainBody {

  val parsedArgs = CommandLineArguments(args)

  val languageDetector: LanguageDetector? =
    if (parsedArgs.langDetectorModel != null && parsedArgs.cjkTokenizerModel != null) {
      NLPBuilder.buildLanguageDetector(
        languageDetectorModelFilename = parsedArgs.langDetectorModel!!,
        cjkModelFilename = parsedArgs.cjkTokenizerModel!!,
        frequencyDictionaryFilename = parsedArgs.freqDictionary)
    } else {
      null
    }

  val tokenizers: Map<String, NeuralTokenizer>? = parsedArgs.tokenizerModelsDir?.let { NLPBuilder.buildTokenizers(it) }

  val parser: NeuralParser<*>? = parsedArgs.neuralParserModel?.let { NLPBuilder.buildNeuralParser(it) }

  NLPServer(
    port = parsedArgs.port,
    detectLanguage = languageDetector?.let { DetectLanguage(it) },
    tokenize = tokenizers?.let { Tokenize(tokenizers = it, languageDetector = languageDetector) },
    parse = if (parser != null && tokenizers != null)
      Parse(parser = parser, tokenizers = tokenizers, languageDetector = languageDetector)
    else
      null
  ).start()
}
