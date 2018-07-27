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
import com.kotlinnlp.morphologicalanalyzer.dictionary.MorphologyDictionary
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

    this.logger.info("Loading language detector model from '$languageDetectorModelFilename'")
    val model = LanguageDetectorModel.load(FileInputStream(File(languageDetectorModelFilename)))

    this.logger.info("Loading CJK tokenizer model from '$cjkModelFilename'")
    val tokenizer = TextTokenizer(cjkModel = NeuralTokenizerModel.load(FileInputStream(File(cjkModelFilename))))

    val freqDictionary = if (frequencyDictionaryFilename != null) {
      this.logger.info("Loading frequency dictionary from '$frequencyDictionaryFilename'")
      FrequencyDictionary.load(FileInputStream(File(frequencyDictionaryFilename)))
    } else {
      this.logger.info("No frequency dictionary used to detect the language")
      null
    }

    return LanguageDetector(model = model, tokenizer = tokenizer, frequencyDictionary = freqDictionary)
  }

  /**
   * Build the [Map] of languages ISO 639-1 codes to the related [NeuralTokenizer]s.
   *
   * @param tokenizerModelsDir the directory containing the tokenizers models
   *
   * @return a [Map] of languages ISO 639-1 codes to the related [NeuralTokenizer]s
   */
  fun buildTokenizers(tokenizerModelsDir: String): Map<String, NeuralTokenizer> {

    this.logger.info("Loading tokenizer models from '$tokenizerModelsDir'")
    val modelsDirectory = File(tokenizerModelsDir)

    require(modelsDirectory.isDirectory) { "$tokenizerModelsDir is not a directory" }

    return modelsDirectory.listFiles().associate { modelFile ->

      this.logger.info("Loading '${modelFile.name}'...")
      val model = NeuralTokenizerModel.load(FileInputStream(modelFile))

      model.language.isoCode to NeuralTokenizer(model = model, useDropout = false)
    }
  }

  /**
   * Build the [Map] of languages ISO 639-1 codes to the related [NeuralParser]s.
   *
   * @param neuralParserModelsDir the directory containing the neural parser models
   *
   * @return a [Map] of languages ISO 639-1 codes to the related [NeuralParser]s
   */
  fun buildNeuralParsers(neuralParserModelsDir: String): Map<String, NeuralParser<*>> {

    this.logger.info("Loading neural parser models from '$neuralParserModelsDir'")
    val modelsDirectory = File(neuralParserModelsDir)

    require(modelsDirectory.isDirectory) { "$neuralParserModelsDir is not a directory" }

    return modelsDirectory.listFiles().associate { modelFile ->

      this.logger.info("Loading '${modelFile.name}'...")
      val model: NeuralParserModel = NeuralParserModel.load(FileInputStream(modelFile))

      model.language.isoCode to NeuralParserFactory(model)
    }
  }

  /**
   * Build the [Map] of languages ISO 639-1 codes to the related [MorphologyDictionary]s.
   *
   * @param morphoDictionariesDir the directory containing the morphology dictionaries
   *
   * @return a [Map] of languages ISO 639-1 codes to the related [MorphologyDictionary]
   */
  fun buildMorphoDictionaries(morphoDictionariesDir: String): Map<String, MorphologyDictionary> {

    this.logger.info("Loading morphology dictionaries from '$morphoDictionariesDir'")
    val morphoDictDir = File(morphoDictionariesDir)

    require(morphoDictDir.isDirectory) { "$morphoDictionariesDir is not a directory" }

    return morphoDictDir.listFiles().associate { dictionaryFile ->

      this.logger.info("Loading '${dictionaryFile.name}'...")
      val dictionary: MorphologyDictionary = MorphologyDictionary.load(FileInputStream(dictionaryFile))

      dictionary.language.isoCode to dictionary
    }
  }

  /**
   * Load a serialized [LocationsDictionary] from file.
   *
   * @param locationsDictionaryFilename the filename of the serialized locations dictionary
   *
   * @return a locations dictionary
   */
  fun buildLocationsDictionary(locationsDictionaryFilename: String): LocationsDictionary {

    this.logger.info("Loading locations dictionary from '$locationsDictionaryFilename'")

    return LocationsDictionary.load(FileInputStream(File(locationsDictionaryFilename)))
  }
}
