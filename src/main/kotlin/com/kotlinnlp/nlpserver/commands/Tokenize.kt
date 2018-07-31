/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver.commands

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.kotlinnlp.languagedetector.LanguageDetector
import com.kotlinnlp.linguisticdescription.language.Language
import com.kotlinnlp.neuraltokenizer.NeuralTokenizer
import com.kotlinnlp.neuraltokenizer.Sentence
import com.kotlinnlp.neuraltokenizer.Token
import com.kotlinnlp.nlpserver.LanguageNotSupported

/**
 * The command executed on the route '/tokenize'.
 *
 * @property tokenizers a map of languages iso-a2 codes to the related [NeuralTokenizer]s
 * @property languageDetector a [LanguageDetector] (can be null)
 */
class Tokenize(
  private val tokenizers: Map<String, NeuralTokenizer>,
  private val languageDetector: LanguageDetector?
) {

  /**
   * Tokenize the given [text].
   * If a [language] is given the related tokenizer is forced to be used, otherwise the [languageDetector] is used to
   * choose the right tokenizer.
   *
   * @param text the text to tokenize
   * @param language the language with which to force the tokenization (default = null)
   *
   * @return the tokenized [text] in JSON format
   */
  operator fun invoke(text: String, language: Language? = null): String {

    val tokenizerLang: Language = this.getTokenizerLanguage(text = text, forcedLang = language)

    if (tokenizerLang.isoCode !in this.tokenizers) {
      throw LanguageNotSupported(tokenizerLang.isoCode)
    }

    return this.tokenizers.getValue(tokenizerLang.isoCode).tokenize(text).toJsonSentences().toJsonString() + "\n"
  }

  /**
   *
   */
  private fun getTokenizerLanguage(text: String, forcedLang: Language?): Language {

    return if (this.languageDetector == null) {

      if (forcedLang == null)
        throw RuntimeException("Cannot determine language automatically (missing language detector)")

      if (forcedLang.isoCode !in this.tokenizers) throw LanguageNotSupported(forcedLang.isoCode)

      forcedLang

    } else {

      val tokenizerLang: Language = forcedLang ?: this.languageDetector.detectLanguage(text)

      if (tokenizerLang.isoCode !in this.tokenizers) throw LanguageNotSupported(tokenizerLang.isoCode)

      tokenizerLang
    }
  }

  /**
   * @return this list of sentences converted to a nested JsonArray of token forms
   */
  private fun List<Sentence>.toJsonSentences(): JsonArray<JsonObject> {

    return JsonArray(*Array(
      size = this.size,
      init = { i ->
        JsonObject(mapOf(
          Pair("startAt", this[i].position.start),
          Pair("endAt", this[i].position.end),
          Pair("tokens", this[i].tokens.toJsonTokens())
        ))
      }
    ))
  }

  /**
   * @return this list of sentences converted to a nested JsonArray of token forms
   */
  private fun List<Token>.toJsonTokens(): JsonArray<JsonObject> {

    return JsonArray(*Array(
      size = this.size,
      init = { i ->
        JsonObject(mapOf(
          Pair("form", this[i].form),
          Pair("startAt", this[i].position.start),
          Pair("endAt", this[i].position.end)
        ))
      }
    ))
  }
}
