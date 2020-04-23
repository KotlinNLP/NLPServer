/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver.routes

import com.beust.klaxon.JsonArray
import com.beust.klaxon.json
import com.kotlinnlp.languagedetector.LanguageDetector
import com.kotlinnlp.linguisticdescription.language.Language
import com.kotlinnlp.linguisticdescription.language.getLanguageByIso
import com.kotlinnlp.neuraltokenizer.NeuralTokenizer
import com.kotlinnlp.neuraltokenizer.Sentence
import com.kotlinnlp.neuraltokenizer.Token
import com.kotlinnlp.nlpserver.LanguageNotSupported
import com.kotlinnlp.nlpserver.routes.utils.TokenizingCommand
import com.kotlinnlp.nlpserver.setAppender
import org.apache.log4j.Logger
import spark.Spark

/**
 * The command executed on the route '/tokenize'.
 *
 * @property tokenizers a map of languages iso-a2 codes to the related [NeuralTokenizer]s
 * @property languageDetector a [LanguageDetector] (can be null)
 */
class Tokenize(
  override val tokenizers: Map<String, NeuralTokenizer>,
  override val languageDetector: LanguageDetector?
) : Route, TokenizingCommand {

  /**
   * The name of the command.
   */
  override val name: String = "tokenize"

  /**
   * The logger of the command.
   */
  override val logger = Logger.getLogger(this::class.simpleName).setAppender()

  /**
   * Initialize the route.
   * Define the paths handled.
   */
  override fun initialize() {

    Spark.get("") { request, _ ->
      this.tokenize(text = request.requiredQueryParam("text"), prettyPrint = request.booleanParam("pretty"))
    }

    Spark.get("/:lang") { request, _ ->
      this.tokenize(
        text = request.requiredQueryParam("text"),
        language = request.params("lang")?.let { getLanguageByIso(it) },
        prettyPrint = request.booleanParam("pretty"))
    }

    Spark.post("") { request, _ ->
      this.tokenize(text = request.getJsonObject().string("text")!!, prettyPrint = request.booleanParam("pretty"))
    }

    Spark.post("/:lang") { request, _ ->
      this.tokenize(
        text = request.getJsonObject().string("text")!!,
        language = request.params("lang")?.let { getLanguageByIso(it) },
        prettyPrint = request.booleanParam("pretty"))
    }
  }

  /**
   * Tokenize the given [text].
   * If a [language] is given the related tokenizer is forced to be used, otherwise the [languageDetector] is used to
   * choose the right tokenizer.
   *
   * @param text the text to tokenize
   * @param language the language with which to force the tokenization (default = null)
   * @param prettyPrint pretty print, used for JSON format (default = false)
   *
   * @return the tokenized [text] in JSON format
   */
  private fun tokenize(text: String, language: Language? = null, prettyPrint: Boolean = false): String {

    this.checkText(text)

    val tokenizerLang: Language = this.getTextLanguage(text = text, forcedLang = language)

    if (tokenizerLang.isoCode !in this.tokenizers) {
      throw LanguageNotSupported(tokenizerLang.isoCode)
    }

    this.logger.debug("Tokenizing text '${text.cutText(50)}'...")

    return this.tokenizers.getValue(tokenizerLang.isoCode).tokenize(text)
      .toJsonSentences().toJsonString(prettyPrint) + if (prettyPrint) "\n" else ""
  }

  /**
   * @return this list of sentences converted to a nested JsonArray of token forms
   */
  private fun List<Sentence>.toJsonSentences(): JsonArray<*> = json {
    array(
      this@toJsonSentences.map {
        obj(
          "start" to it.position.start,
          "end" to it.position.end,
          "tokens" to it.tokens.toJsonTokens()
        )
      }
    )
  }

  /**
   * @return this list of sentences converted to a nested JsonArray of token forms
   */
  private fun List<Token>.toJsonTokens(): JsonArray<*> = json {
    array(
      this@toJsonTokens.map {
        obj(
          "form" to it.form,
          "start" to it.position.start,
          "end" to it.position.end
        )
      }
    )
  }
}
