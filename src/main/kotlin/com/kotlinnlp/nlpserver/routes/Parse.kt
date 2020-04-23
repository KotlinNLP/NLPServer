/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver.routes

import com.beust.klaxon.JsonObject
import com.beust.klaxon.json
import com.kotlinnlp.conllio.Sentence as CoNLLSentence
import com.kotlinnlp.conllio.Token as CoNLLToken
import com.kotlinnlp.languagedetector.LanguageDetector
import com.kotlinnlp.linguisticdescription.POSTag
import com.kotlinnlp.linguisticdescription.language.Language
import com.kotlinnlp.linguisticdescription.language.getLanguageByIso
import com.kotlinnlp.linguisticdescription.sentence.MorphoSynSentence
import com.kotlinnlp.linguisticdescription.sentence.token.MorphoSynToken
import com.kotlinnlp.linguisticdescription.sentence.token.RealToken
import com.kotlinnlp.neuralparser.NeuralParser
import com.kotlinnlp.neuralparser.helpers.preprocessors.MorphoPreprocessor
import com.kotlinnlp.neuraltokenizer.NeuralTokenizer
import com.kotlinnlp.nlpserver.routes.utils.LanguageDistribution
import com.kotlinnlp.nlpserver.routes.utils.ParsingCommand
import spark.Response
import spark.Spark

/**
 * The command executed on the route '/parse'.
 *
 * @param languageDetector a language detector (can be null)
 * @param tokenizers a map of tokenizers associated by language ISO 639-1 code
 * @param parsers a map of morpho-syntactic parsers associated by language ISO 639-1 code
 * @param morphoPreprocessors a map of morpho-preprocessors associated by language ISO 639-1 code
 */
class Parse(
  override val languageDetector: LanguageDetector?,
  override val tokenizers: Map<String, NeuralTokenizer>,
  override val parsers: Map<String, NeuralParser<*>>,
  override val morphoPreprocessors: Map<String, MorphoPreprocessor>
) : Route, ParsingCommand {

  /**
   * The format of the parsing response.
   *
   * @property CoNLL the response will be written in CoNLL format
   * @property JSON the response will be written in JSON format
   */
  private enum class ResponseFormat {
    CoNLL,
    JSON;

    companion object {

      /**
       * @param formatString a string representing a parsing response format ('JSON', 'CoNLL')
       *
       * @return the parse response format related to the given [formatString]
       */
      fun fromString(formatString: String): ResponseFormat = when (formatString.toLowerCase()) {
        "json" -> JSON
        "conll" -> CoNLL
        else -> throw RuntimeException("Invalid parsing response format: $formatString.")
      }
    }
  }

  /**
   * The name of the command.
   */
  override val name: String = "parse"

  /**
   * Initialize the route.
   * Define the paths handled.
   */
  override fun initialize() {

    Spark.get("") { request, response ->
      this.parse(
        text = request.requiredQueryParam("text"),
        lang = request.queryParams("lang")?.let { getLanguageByIso(it) },
        format = ResponseFormat.fromString(request.queryParams("format") ?: "JSON"),
        response = response,
        prettyPrint = request.booleanParam("pretty"))
    }

    Spark.get("/:lang") { request, response ->
      this.parse(
        text = request.requiredQueryParam("text"),
        lang = request.params("lang")?.let { getLanguageByIso(it) },
        format = ResponseFormat.fromString(request.queryParams("format") ?: "JSON"),
        response = response,
        prettyPrint = request.booleanParam("pretty"))
    }

    Spark.post("") { request, response ->

      val jsonBody: JsonObject = request.getJsonObject()

      this.parse(
        text = jsonBody.string("text")!!,
        lang = null,
        format = ResponseFormat.fromString(request.queryParams("format") ?: "JSON"),
        response = response,
        prettyPrint = request.booleanParam("pretty"))
    }

    Spark.post("/:lang") { request, response ->

      val jsonBody: JsonObject = request.getJsonObject()

      this.parse(
        text = jsonBody.string("text")!!,
        lang = request.params("lang")?.let { getLanguageByIso(it) },
        format = ResponseFormat.fromString(request.queryParams("format") ?: "JSON"),
        response = response,
        prettyPrint = request.booleanParam("pretty"))
    }
  }

  /**
   * Parse the given [text], eventually forcing on the language [lang].
   *
   * @param text the text to parse
   * @param lang the language to use to parse the [text] (default = unknown)
   * @param format the string format of the parsed sentences response (default = JSON)
   * @param response the response of the server
   * @param prettyPrint pretty print, used for JSON format (default = false)
   *
   * @return the parsed [text] in the given string [format]
   */
  private fun parse(text: String,
                    lang: Language?,
                    format: ResponseFormat,
                    response: Response,
                    prettyPrint: Boolean): String {

    this.checkText(text)

    val langDistribution: LanguageDistribution = this.getTextLanguageDistribution(text = text, forcedLang = lang)
    val textLang: Language = langDistribution.language
    val parsedSentences: List<MorphoSynSentence> = this.parse(text = text, langCode = textLang.isoCode)

    if (format == ResponseFormat.CoNLL) response.header("Content-Type", "text/plain")

    return when (format) {
      ResponseFormat.CoNLL -> this.parseToCoNLLFormat(parsedSentences)
      ResponseFormat.JSON -> this.parseToJSONFormat(
        sentences = parsedSentences,
        langDistribution = langDistribution,
        prettyPrint = prettyPrint)
    }
  }

  /**
   * @param sentences the parsed sentences
   *
   * @return the parsed sentences in CoNLL string format
   */
  private fun parseToCoNLLFormat(sentences: List<MorphoSynSentence>): String =
    sentences.joinToString(separator = "\n\n", postfix = "\n") {
      it.toCoNLL().toCoNLLString(writeComments = false)
    }

  /**
   * @param sentences the parsed sentences
   * @param langDistribution the language of the text with the distribution of the languages scores
   * @param prettyPrint pretty print (default = false)
   *
   * @return the parsed sentences in JSON string format
   */
  private fun parseToJSONFormat(sentences: List<MorphoSynSentence>,
                                langDistribution: LanguageDistribution,
                                prettyPrint: Boolean = false): String = json {
    obj(
      "language" to langDistribution.toJSON(),
      "sentences" to array(sentences.map { it.toJSON() })
    )
  }.toJsonString(prettyPrint = prettyPrint) + if (prettyPrint) "\n" else ""

  /**
   * Convert this [MorphoSynSentence] to a CoNLL Sentence.
   *
   * @return a CoNLL Sentence
   */
  private fun MorphoSynSentence.toCoNLL(): CoNLLSentence = CoNLLSentence(
    sentenceId = this.id.toString(),
    text = this.buildText(),
    tokens = this.tokens.mapIndexed { i, it ->
      it.toCoNLL(
        id = i + 1,
        headId = it.syntacticRelation.governor?.let { id -> this.tokens.indexOfFirst { it.id == id } + 1 } ?: 0)
    }
  )

  /**
   * @return the CoNLL object that represents this token
   */
  private fun MorphoSynToken.toCoNLL(id: Int, headId: Int) = CoNLLToken(
    id = id,
    form = (this as? RealToken)?.form ?: CoNLLToken.EMPTY_FILLER,
    lemma = CoNLLToken.EMPTY_FILLER,
    posList = this.flatPOS.let { if (it.isNotEmpty()) it else listOf(POSTag(CoNLLToken.EMPTY_FILLER)) },
    pos2List = listOf(POSTag(CoNLLToken.EMPTY_FILLER)),
    feats = emptyMap(),
    head = headId,
    syntacticDependencies = this.flatSyntacticRelations.map { it.dependency },
    multiWord = null
  )
}
