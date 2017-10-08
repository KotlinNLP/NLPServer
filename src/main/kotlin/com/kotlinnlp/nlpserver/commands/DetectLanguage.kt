/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver.commands

import com.beust.klaxon.JsonArray
import com.beust.klaxon.json
import com.kotlinnlp.languagedetector.LanguageDetector
import com.kotlinnlp.languagedetector.LanguageDetectorModel
import com.kotlinnlp.languagedetector.utils.FrequencyDictionary
import com.kotlinnlp.languagedetector.utils.Language
import com.kotlinnlp.languagedetector.utils.TextTokenizer
import com.kotlinnlp.neuraltokenizer.NeuralTokenizerModel
import com.kotlinnlp.simplednn.simplemath.ndarray.dense.DenseNDArray
import java.io.File
import java.io.FileInputStream
import java.util.logging.Logger

/**
 * The command executed on the route '/detect-language'.
 *
 * @param modelFilename the filename of the serialized [LanguageDetectorModel]
 * @param cjkModelFilename the filename of the serialized [NeuralTokenizerModel] used to tokenize Chinese, Japanese and
 *                         Korean texts
 * @param frequencyDictionaryFilename the filename of the [FrequencyDictionary] (can be null)
 */
class DetectLanguage(modelFilename: String, cjkModelFilename: String, frequencyDictionaryFilename: String?) {

  /**
   * The logger of this command.
   */
  private val logger = Logger.getLogger("NLP Server - Tokenize")

  /**
   * A [LanguageDetector].
   */
  private val languageDetector: LanguageDetector

  /**
   * Load the models and initialize the language detector.
   */
  init {

    this.logger.info("Loading language detector model from '$modelFilename'\n")
    val model = LanguageDetectorModel.load(FileInputStream(File(modelFilename)))

    this.logger.info("Loading CJK tokenizer model from '$cjkModelFilename'\n")
    val tokenizer = TextTokenizer(cjkModel = NeuralTokenizerModel.load(FileInputStream(File(cjkModelFilename))))

    val freqDictionary = if (frequencyDictionaryFilename != null) {
      this.logger.info("Loading frequency dictionary from '$frequencyDictionaryFilename'\n")
      FrequencyDictionary.load(FileInputStream(File(frequencyDictionaryFilename)))
    } else {
      this.logger.info("No frequency dictionary used to detect the language\n")
      null
    }

    this.languageDetector = LanguageDetector(model = model, tokenizer = tokenizer, frequencyDictionary = freqDictionary)
  }

  /**
   * Detect the language of the given [text].
   * The return value is A JSON string containing the detected language iso-a2 code and the complete classification.
   *
   * The template of the JSON object:
   *  {
   *    "language": <STRING>, // iso-a2 code
   *    "classification": {
   *      "en": <DOUBLE>,
   *      "ar": <DOUBLE>,
   *      ...
   *    }
   *  }
   *
   * @param text the input text
   *
   * @return a [String] with a JSON object containing the detected language iso-a2 code and the complete classification
   */
  operator fun invoke(text: String): String {

    val prediction: DenseNDArray = this.languageDetector.predict(text)
    val language: Language = this.languageDetector.extractLanguage(prediction)

    return json {
      obj(
        "language" to language.isoCode,
        "classification" to prediction.toLanguageScorePairs()
      )
    }.toJsonString()
  }

  /**
   * Classify the language for each token of the given [text].
   *
   * The return value is A JSON string containing a list of token_classifications.
   * Each token token_classification is a list containing the token itself as first element and its classification as
   * second element.
   * The classification is an object containing the probability of each language mapped to its iso-code.
   *
   * The template of the JSON object:
   *  {
   *    "word": <STRING>,
   *    "classification": {
   *      "languages": {
   *        "en": <DOUBLE>,
   *        "ar": <DOUBLE>,
   *        ...
   *      },
   *      "charsImportance": [<DOUBLE>, <DOUBLE>, ...] // same length of the token
   *    }
   *  }
   *
   * @param text the input text
   *
   * @return a [String] with a JSON list containing the language classification of each token
   */
  fun perToken(text: String): String {

    val tokensClassifications: List<Pair<String, LanguageDetector.TokenClassification>>
      = this.languageDetector.classifyTokens(text)

    return json {
      array(tokensClassifications.map {
        obj(
          "word" to it.first,
          "classification" to obj(
            "languages" to obj(*it.second.languages.toLanguageScorePairs()),
            "charsImportance" to it.second.charsImportance.toJSONArray()
          )
        )
      })
    }.toJsonString()
  }

  /**
   *
   */
  private fun DenseNDArray.toJSONArray(): JsonArray<Any?> = json { array(this@toJSONArray.toDoubleArray().toList()) }

  /**
   * Convert a [DenseNDArray] representing a languages classification to an [Array] of [Pair]s <isoA2-code, score>.
   *
   * @return an [Array] of [Pair]s <isoA2-code, score>
   */
  private fun DenseNDArray.toLanguageScorePairs(): Array<Pair<String, Double>> {

    val languages = this@DetectLanguage.languageDetector.model.supportedLanguages

    require(this.length == languages.size) {
      "Invalid this (length %d, supported languages %d)".format(this.length, languages.size)
    }

    return this.toDoubleArray().mapIndexed { i, score ->
      Pair(languages[i].isoCode, score)
    }.toTypedArray()
  }
}
