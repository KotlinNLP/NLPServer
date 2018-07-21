/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver

import com.kotlinnlp.geolocation.dictionary.LocationsDictionary
import com.kotlinnlp.languagedetector.LanguageDetector
import com.kotlinnlp.languagedetector.LanguageDetectorModel
import com.kotlinnlp.languagedetector.utils.FrequencyDictionary
import com.kotlinnlp.languagedetector.utils.TextTokenizer
import com.kotlinnlp.neuralparser.NeuralParser
import com.kotlinnlp.neuralparser.NeuralParserFactory
import com.kotlinnlp.neuralparser.NeuralParserModel
import com.kotlinnlp.neuraltokenizer.NeuralTokenizer
import com.kotlinnlp.neuraltokenizer.NeuralTokenizerModel
import java.io.File
import java.io.FileInputStream
import java.util.logging.Logger

/**
 * Helper that builds various NLP components logging the loading steps.
 */
object NLPBuilder {

  /**
   * The logger of the [NLPBuilder].
   */
  private val logger = Logger.getLogger("NLP Builder")

  /**
   * Build a [LanguageDetector].
   *
   * @param languageDetectorModelFilename the filename of the language detector model
   * @param cjkModelFilename the filename of the CJK tokenizer used by the language detector
   * @param frequencyDictionaryFilename the filename of the frequency dictionary (can be null)
   *
   * @return a language detector
   */
  fun buildLanguageDetector(languageDetectorModelFilename: String,
                            cjkModelFilename: String,
                            frequencyDictionaryFilename: String?): LanguageDetector {

    logger.info("Loading language detector model from '$languageDetectorModelFilename'\n")
    val model = LanguageDetectorModel.load(FileInputStream(File(languageDetectorModelFilename)))

    logger.info("Loading CJK tokenizer model from '$cjkModelFilename'\n")
    val tokenizer = TextTokenizer(cjkModel = NeuralTokenizerModel.load(FileInputStream(File(cjkModelFilename))))

    val freqDictionary = if (frequencyDictionaryFilename != null) {
      logger.info("Loading frequency dictionary from '$frequencyDictionaryFilename'\n")
      FrequencyDictionary.load(FileInputStream(File(frequencyDictionaryFilename)))
    } else {
      logger.info("No frequency dictionary used to detect the language\n")
      null
    }

    return LanguageDetector(model = model, tokenizer = tokenizer, frequencyDictionary = freqDictionary)
  }

  /**
   * Build the [Map] of languages iso-a2 codes to the related [NeuralTokenizer]s.
   *
   * @param tokenizerModelsDir the directory containing the tokenizers models
   *
   * @return a [Map] of languages iso-a2 codes to the related [NeuralTokenizer]s
   */
  fun buildTokenizers(tokenizerModelsDir: String): Map<String, NeuralTokenizer> {

    logger.info("Loading tokenizer models from '$tokenizerModelsDir'\n")
    val modelsDirectory = File(tokenizerModelsDir)

    require(modelsDirectory.isDirectory) { "$tokenizerModelsDir is not a directory" }

    val tokenizersMap = mutableMapOf<String, NeuralTokenizer>()
    val modelsFiles: Array<File> = modelsDirectory.listFiles()

    modelsFiles.forEachIndexed { i, modelFile ->

      logger.info("Loading '${modelFile.name}'..." + if (i == modelsFiles.lastIndex) "\n" else "")
      val model = NeuralTokenizerModel.load(FileInputStream(modelFile))

      tokenizersMap[model.language] = NeuralTokenizer(model = model, useDropout = false)
    }

    return tokenizersMap.toMap()
  }


  /**
   * Build a [NeuralParser].
   *
   * @param neuralParserModelFilename the filename of the neural parser
   *
   * @return a neural parser
   */
  fun buildNeuralParser(neuralParserModelFilename: String): NeuralParser<*> {

    logger.info("Loading neural parser model from '$neuralParserModelFilename'\n")

    return NeuralParserFactory(model = NeuralParserModel.load(FileInputStream(File(neuralParserModelFilename))))
  }

  /**
   * Load a serialized [LocationsDictionary] from file.
   *
   * @param locationsDictionaryFilename the filename of the serialized locations dictionary
   *
   * @return a locations dictionary
   */
  fun buildLocationsDictionary(locationsDictionaryFilename: String): LocationsDictionary {

    logger.info("Loading locations dictionary from '$locationsDictionaryFilename'\n")

    return LocationsDictionary.load(FileInputStream(File(locationsDictionaryFilename)))
  }
}
