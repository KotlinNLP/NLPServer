/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver.commands

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
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
   *    "classification": [
   *      {
   *        "language": <STRING>, // iso-a2 code,
   *        "score": <DOUBLE>
   *      }
   *      ...
   *    ]
   *  }
   *
   * @param text the input text
   * @param distribution whether to include the distribution in the response (default = true)
   * @param prettyPrint pretty print, used for JSON format (default = false)
   *
   * @return a JSON string containing the ISO 639-1 code of the detected language (and the probability distribution)
   */
  operator fun invoke(text: String, distribution: Boolean = true, prettyPrint: Boolean = false): String {

    val prediction: DenseNDArray = this.languageDetector.predict(text)
    val language: Language = this.languageDetector.getLanguage(prediction)

    return json {

      val jsonObj: JsonObject = obj("language" to language.isoCode)

      if (distribution) jsonObj["distribution"] = prediction.toJSONScoredLanguages()

      jsonObj

    }.toJsonString(prettyPrint) + "\n"
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
   *    "language": <STRING>, // iso-a2 code
   *    "distribution": {
   *      "languages": [
   *        {
   *          "language": <STRING>, // iso-a2 code,
   *          "score": <DOUBLE>
   *        }
   *        ...
   *      ],
   *      "charsImportance": [<DOUBLE>, <DOUBLE>, ...] // same length of the token
   *    }
   *  }
   *
   * @param text the input text
   * @param distribution whether to include the distribution in the response (default = true)
   * @param prettyPrint pretty print, used for JSON format (default = false)
   *
   * @return a [String] with a JSON list containing the language classification of each token
   */
  fun perToken(text: String, distribution: Boolean = true, prettyPrint: Boolean = false): String {

    val languages: List<Language> = this@DetectLanguage.languageDetector.model.supportedLanguages
    val tokensClassifications: List<Pair<String, LanguageDetector.TokenClassification>> =
      this.languageDetector.classifyTokens(text)

    return json {

      array(tokensClassifications.map {

        val jsonObj: JsonObject = obj(
          "word" to it.first,
          "language" to languages[it.second.languages.argMaxIndex()].isoCode
        )

        if (distribution) jsonObj["distribution"] = obj(
          "languages" to it.second.languages.toJSONScoredLanguages(),
          "charsImportance" to it.second.charsImportance.toJSONArray()
        )

        jsonObj
      })
    }.toJsonString(prettyPrint) + "\n"
  }

  /**
   *
   */
  private fun DenseNDArray.toJSONArray(): JsonArray<Any?> = json { array(this@toJSONArray.toDoubleArray().toList()) }

  /**
   * Convert a [DenseNDArray] representing a languages classification to a JSON array of 'lang' + 'score' objects.
   *
   * @return a JSON array of objects with 'lang' and 'score' keys
   */
  private fun DenseNDArray.toJSONScoredLanguages(): JsonArray<*> = json {

    val self: DenseNDArray = this@toJSONScoredLanguages
    val languages = this@DetectLanguage.languageDetector.model.supportedLanguages

    require(self.length == languages.size) {
      "Invalid this (length %d, supported languages %d)".format(self.length, languages.size)
    }

    array(self.toDoubleArray().withIndex().sortedByDescending { it.value }.map {
      obj(
        "language" to languages[it.index].isoCode,
        "score" to it.value
      )
    })
  }
}
