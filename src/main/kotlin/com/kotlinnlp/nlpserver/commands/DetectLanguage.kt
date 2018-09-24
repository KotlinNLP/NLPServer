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
import com.kotlinnlp.linguisticdescription.language.Language
import com.kotlinnlp.simplednn.simplemath.ndarray.dense.DenseNDArray

/**
 * The command executed on the route '/detect-language'.
 *
 * @property languageDetector a [LanguageDetector]
 */
class DetectLanguage(private val languageDetector: LanguageDetector) {

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
   * @param prettyPrint pretty print, used for JSON format (default = false)
   *
   * @return a [String] with a JSON object containing the detected language iso-a2 code and the complete classification
   */
  operator fun invoke(text: String, prettyPrint: Boolean = false): String {

    val prediction: DenseNDArray = this.languageDetector.predict(text)
    val language: Language = this.languageDetector.getLanguage(prediction)

    return json {
      obj(
        "language" to language.isoCode,
        "classification" to obj(*prediction.toLanguageScorePairs())
      )
    }.toJsonString(prettyPrint)
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
   * @param prettyPrint pretty print, used for JSON format (default = false)
   *
   * @return a [String] with a JSON list containing the language classification of each token
   */
  fun perToken(text: String, prettyPrint: Boolean = false): String {

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
    }.toJsonString(prettyPrint)
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
