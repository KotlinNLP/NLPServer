/* Copyright 2020-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver.routes

import com.beust.klaxon.JsonObject
import com.beust.klaxon.json
import com.kotlinnlp.languagedetector.LanguageDetector
import com.kotlinnlp.linguisticdescription.InvalidLanguageCode
import com.kotlinnlp.linguisticdescription.language.Language
import com.kotlinnlp.linguisticdescription.language.getLanguageByIso
import com.kotlinnlp.linguisticdescription.sentence.RealSentence
import com.kotlinnlp.linguisticdescription.sentence.properties.TokensRange
import com.kotlinnlp.linguisticdescription.sentence.token.RealToken
import com.kotlinnlp.morphologicalanalyzer.MorphologicalAnalyzer
import com.kotlinnlp.neuraltokenizer.NeuralTokenizer
import com.kotlinnlp.nlpserver.routes.utils.TokenizingCommand
import com.kotlinnlp.nlpserver.setAppender
import com.kotlinnlp.utils.JSONSerializable
import org.apache.log4j.Logger
import spark.Spark

/**
 * The command executed on the route '/morpho'.
 *
 * @param languageDetector a language detector (can be null)
 * @param tokenizers a map of neural tokenizers associated by language ISO 639-1 code
 * @param analyzers a map of morphological analyzers associated by language ISO 639-1 code
 */
class Morpho(
  override val languageDetector: LanguageDetector?,
  override val tokenizers: Map<String, NeuralTokenizer>,
  private val analyzers: Map<String, MorphologicalAnalyzer>
) : Route, TokenizingCommand {

  /**
   * The name of the command.
   */
  override val name: String = "morpho"

  /**
   * The logger of the command.
   */
  override val logger = Logger.getLogger(this::class.simpleName).setAppender()

  /**
   * Initialize the route.
   * Define the paths handled.
   */
  override fun initialize() {

    Spark.post("/numbers") { request, _ ->
      this.findNumbers(
        text = request.getJsonObject().string("text")!!,
        lang = request.queryParams("lang")?.let { getLanguageByIso(it) },
        prettyPrint = request.booleanParam("pretty"))
    }

    Spark.post("/numbers/:lang") { request, _ ->
      this.findNumbers(
        text = request.getJsonObject().string("text")!!,
        lang = getLanguageByIso(request.params("lang")),
        prettyPrint = request.booleanParam("pretty"))
    }

    Spark.post("/datetimes") { request, _ ->
      this.findDateTimes(
        text = request.getJsonObject().string("text")!!,
        lang = request.queryParams("lang")?.let { getLanguageByIso(it) },
        prettyPrint = request.booleanParam("pretty"))
    }

    Spark.post("/datetimes/:lang") { request, _ ->
      this.findDateTimes(
        text = request.getJsonObject().string("text")!!,
        lang = getLanguageByIso(request.params("lang")),
        prettyPrint = request.booleanParam("pretty"))
    }
  }

  /**
   * Find numerical expressions in a text.
   *
   * @param text the input text
   * @param lang the language of the text or null if it is unknown
   * @param prettyPrint pretty print, used for JSON format (default = false)
   *
   * @return the numerical expressions found in the given text
   */
  private fun findNumbers(text: String, lang: Language?, prettyPrint: Boolean): String {

    this.logger.debug("Searching for numerical expressions in the text `${text.cutText(200)}`")

    val langCode: String = this.getTextLanguage(text = text, forcedLang = lang).isoCode
    val analyzer: MorphologicalAnalyzer = this.analyzers[langCode] ?: throw InvalidLanguageCode(langCode)
    val tokenizer: NeuralTokenizer = this.tokenizers.getValue(langCode)

    @Suppress("UNCHECKED_CAST")
    val sentences: List<RealSentence<RealToken>> = tokenizer.tokenize(text).map { it as RealSentence<RealToken> }

    return json {
      array(
        sentences.asSequence()
          .flatMap { s -> analyzer.findNumbers(s).asSequence().map { it.toJsonWithChars(s.tokens) } }
          .toList()
      )
    }.toJsonString(prettyPrint) + if (prettyPrint) "\n" else ""
  }

  /**
   * Find date-time expressions in a text.
   *
   * @param text the input text
   * @param lang the language of the text or null if it is unknown
   * @param prettyPrint pretty print, used for JSON format (default = false)
   *
   * @return the date-time expressions found in the given text
   */
  private fun findDateTimes(text: String, lang: Language?, prettyPrint: Boolean): String {

    this.logger.debug("Searching for numerical expressions in the text `${text.cutText(200)}`")

    val langCode: String = this.getTextLanguage(text = text, forcedLang = lang).isoCode
    val analyzer: MorphologicalAnalyzer = this.analyzers[langCode] ?: throw InvalidLanguageCode(langCode)
    val tokenizer: NeuralTokenizer = this.tokenizers.getValue(langCode)

    @Suppress("UNCHECKED_CAST")
    val sentences: List<RealSentence<RealToken>> = tokenizer.tokenize(text).map { it as RealSentence<RealToken> }

    return json {
      array(
        sentences.asSequence()
          .flatMap { s -> analyzer.findDateTimes(s).asSequence().map { it.toJsonWithChars(s.tokens) } }
          .toList()
      )}.toJsonString(prettyPrint) + if (prettyPrint) "\n" else ""
  }

  /**
   * Note: this tokens range is required to be [JSONSerializable].
   *
   * @param tokens the sentence tokens from which this range has been extracted
   *
   * @return the JSON representation of this range, including the start-end chars indices
   */
  private fun TokensRange.toJsonWithChars(tokens: List<RealToken>): JsonObject {

    val (start, end) = this.getRefTokens(tokens).let { it.first().position.start to it.last().position.end }

    return (this as JSONSerializable).toJSON().apply {
      set("startChar", start)
      set("endChar", end)
    }
  }
}
