/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver.commands

import com.beust.klaxon.json
import com.kotlinnlp.languagedetector.LanguageDetector
import com.kotlinnlp.languagedetector.LanguageDetectorModel
import com.kotlinnlp.languagedetector.utils.FrequencyDictionary
import com.kotlinnlp.languagedetector.utils.TextTokenizer
import com.kotlinnlp.neuraltokenizer.NeuralTokenizerModel
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
   *
   * @param text the input text
   *
   * @return the isoA2 code of the detected language
   */
  operator fun invoke(text: String): String {
    return this.languageDetector.detectLanguage(text).isoCode
  }

  /**
   * Classify the language for each token of the given [text].
   * The return value is A JSON string containing a list of token_classifications.
   * Each token token_classification is a list containing the token itself as first element and its classification as
   * second element.
   * The classification is an object containing the probability of each language mapped to its iso-code.
   *
   * @param text the input text
   *
   * @return a JSON list containing the language classification of each token
   */
  fun perToken(text: String): String {

    val tokensClassifications: List<Pair<String, LanguageDetector.TokenClassification>>
      = this.languageDetector.classifyTokens(text)

    val languages = this.languageDetector.model.supportedLanguages

    return json {
      array(tokensClassifications.map {
        obj(
          "word" to it.first,
          "classification" to obj(
            "languages" to obj(*it.second.languages.toDoubleArray().mapIndexed { i, score ->
              Pair(languages[i].isoCode, score)
            }.toTypedArray()),
            "charsImportance" to array(it.second.charsImportance.toDoubleArray().toList())
          )
        )
      })
    }.toJsonString()
  }
}
