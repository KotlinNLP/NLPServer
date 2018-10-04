/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver.commands.utils

import com.kotlinnlp.languagedetector.LanguageDetector
import com.kotlinnlp.linguisticdescription.language.Language
import com.kotlinnlp.neuraltokenizer.NeuralTokenizer
import com.kotlinnlp.nlpserver.LanguageNotSupported

/**
 * Defines a command that uses a language detector and tokenizers.
 */
interface TokenizingCommand {

  /**
   * A map of languages ISO 639-1 codes to the related [NeuralTokenizer]s.
   */
  val tokenizers: Map<String, NeuralTokenizer>

  /**
   * A language detector (can be null).
   */
  val languageDetector: LanguageDetector?

  /**
   * @param text the input text (of which to detect the language if [forcedLang] is null)
   * @param forcedLang force this language to be returned (if it is supported)
   *
   * @throws LanguageNotSupported when the returning language is not supported
   * @throws RuntimeException when [forcedLang] is 'null' but the language detector is missing
   *
   * @return the language of the given [text]
   */
  fun getTextLanguage(text: String, forcedLang: Language?): Language {

    return if (this.languageDetector == null) {

      if (forcedLang == null)
        throw RuntimeException("Cannot determine language automatically (missing language detector)")

      if (forcedLang.isoCode !in this.tokenizers) throw LanguageNotSupported(forcedLang.isoCode)

      forcedLang

    } else {

      val tokenizerLang: Language = forcedLang ?: this.languageDetector!!.detectLanguage(text)

      if (tokenizerLang.isoCode !in this.tokenizers) throw LanguageNotSupported(tokenizerLang.isoCode)

      tokenizerLang
    }
  }
}