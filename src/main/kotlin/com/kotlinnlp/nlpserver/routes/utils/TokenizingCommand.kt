/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver.routes.utils

import com.kotlinnlp.linguisticdescription.language.Language
import com.kotlinnlp.neuraltokenizer.NeuralTokenizer
import com.kotlinnlp.neuraltokenizer.Sentence
import com.kotlinnlp.nlpserver.LanguageNotSupported

/**
 * Defines a command that uses a language detector and tokenizers.
 */
internal interface TokenizingCommand : LanguageDetectingCommand {

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

  /**
   * Tokenize a given text.
   * If a [language] is given the related tokenizer is forced to be used, otherwise the [languageDetector] is used to
   * choose the right tokenizer.
   *
   * @param text the input text
   * @param language the language with which to force the tokenization or null to detect it automatically
   *
   * @return the text split in sentences and tokens
   */
  fun tokenize(text: String, language: Language? = null): List<Sentence> {

    val tokenizerLang: Language = this.getTextLanguage(text = text, forcedLang = language)

    if (tokenizerLang.isoCode !in this.tokenizers)
      throw LanguageNotSupported(tokenizerLang.isoCode)

    this.logger.debug("Tokenizing text '${text.cutText(50)}'...")

    return this.tokenizers.getValue(tokenizerLang.isoCode).tokenize(text)
  }
}
