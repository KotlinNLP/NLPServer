/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver.routes.utils

import com.kotlinnlp.linguisticdescription.language.Language
import com.kotlinnlp.neuraltokenizer.NeuralTokenizer
import com.kotlinnlp.nlpserver.LanguageNotSupported

/**
 * Defines a command that uses a language detector and tokenizers.
 */
interface TokenizingCommand : LanguageDetectingCommand {

  /**
   * Tokenizers associated by language ISO 639-1 code.
   */
  val tokenizers: Map<String, NeuralTokenizer>

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
  override fun checkLanguage(language: Language?): Language = super.checkLanguage(language).also {
    if (it.isoCode !in this.tokenizers) throw LanguageNotSupported(it.isoCode)
  }
}
