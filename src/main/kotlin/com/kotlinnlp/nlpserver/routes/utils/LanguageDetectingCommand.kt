/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver.routes.utils

import com.kotlinnlp.languagedetector.LanguageDetector
import com.kotlinnlp.linguisticdescription.language.Language
import com.kotlinnlp.nlpserver.LanguageNotSupported
import com.kotlinnlp.simplednn.simplemath.ndarray.dense.DenseNDArray

/**
 * Defines a command that uses a language detector.
 */
interface LanguageDetectingCommand : Command {

  /**
   * A language detector (can be null).
   */
  val languageDetector: LanguageDetector?

  /**
   * @param text the input text (of which to detect the language if [forcedLang] is null)
   * @param forcedLang force this language to be returned (if it is supported)
   *
   * @return the language of the given [text]
   */
  fun getTextLanguage(text: String, forcedLang: Language?): Language {

    val language: Language? = forcedLang ?: this.languageDetector?.detectLanguage(text)

    return checkLanguage(language)
  }

  /**
   * @param text the input text (of which to detect the language if [forcedLang] is null)
   * @param forcedLang force this language to be returned (if it is supported)
   *
   * @return the language of a text with the related scores distribution
   */
  fun getTextLanguageDistribution(text: String, forcedLang: Language?): LanguageDistribution {

    val language: Language?
    val distribution: List<Pair<Language, Double>>?

    if (forcedLang == null && this.languageDetector != null) {

      val result: DenseNDArray = this.languageDetector!!.predict(text)

      language = this.languageDetector!!.getLanguage(result)
      distribution = this.languageDetector!!.getFullDistribution(result)

    } else {
      language = forcedLang
      distribution = null
    }

    return LanguageDistribution(language = checkLanguage(language), distribution = distribution)
  }

  /**
   * Check that the language is valid.
   *
   * @param language a language
   *
   * @throws LanguageNotSupported when the given [language] is not supported
   * @throws RuntimeException when [language] is 'null' and the language detector is missing
   *
   * @return the given [language]
   */
  fun checkLanguage(language: Language?): Language =
    language ?: throw RuntimeException("Cannot determine language automatically (missing language detector)")
}
