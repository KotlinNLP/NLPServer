/* Copyright 2016-present Simone Cangialosi. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver.routes

import com.beust.klaxon.json
import com.kotlinnlp.languagedetector.LanguageDetector
import com.kotlinnlp.linguisticdescription.InvalidLanguageCode
import com.kotlinnlp.linguisticdescription.language.Language
import com.kotlinnlp.linguisticdescription.language.getLanguageByIso
import com.kotlinnlp.linguisticdescription.sentence.MorphoSynSentence
import com.kotlinnlp.neuralparser.NeuralParser
import com.kotlinnlp.neuralparser.helpers.preprocessors.MorphoPreprocessor
import com.kotlinnlp.neuraltokenizer.NeuralTokenizer
import com.kotlinnlp.nlpserver.routes.utils.ParsingCommand
import com.kotlinnlp.summarizer.Summarizer
import com.kotlinnlp.summarizer.Summary
import spark.Spark

/**
 * The command executed on the route '/summarize'.

 * @param languageDetector a language detector (can be null)
 * @param tokenizers a map of tokenizers associated by language ISO 639-1 code
 * @param parsers a map of morpho-syntactic parsers associated by language ISO 639-1 code
 * @param morphoPreprocessors a map of morpho-preprocessors associated by language ISO 639-1 code
 * @param lemmasBlacklist lemmas ignored for the summary associated by language ISO 639-1 code
 */
class Summarize(
  override val languageDetector: LanguageDetector?,
  override val tokenizers: Map<String, NeuralTokenizer>,
  override val parsers: Map<String, NeuralParser<*>>,
  override val morphoPreprocessors: Map<String, MorphoPreprocessor>,
  private val lemmasBlacklist: Map<String, Set<String>>
) : Route, ParsingCommand {

  /**
   * The name of the command.
   */
  override val name: String = "summarize"

  /**
   * Initialize the route.
   * Define the paths handled.
   */
  override fun initialize() {

    Spark.post("") { request, _ ->
      this.summarize(
        text = request.getJsonObject().string("text")!!,
        lang = request.queryParams("lang")?.let { getLanguageByIso(it) },
        prettyPrint = request.booleanParam("pretty"))
    }

    Spark.post("/:lang") { request, _ ->
      this.summarize(
        text = request.getJsonObject().string("text")!!,
        lang = getLanguageByIso(request.params("lang")),
        prettyPrint = request.booleanParam("pretty"))
    }
  }

  /**
   * Summarize a given [text], eventually forcing on a given language.
   *
   * @param text the text to categorize
   * @param lang the language to use to analyze the [text] (default = unknown)
   * @param prettyPrint pretty print, used for JSON format (default = false)
   *
   * @throws InvalidLanguageCode when the requested (or detected) language is not compatible
   *
   * @return the text summary, in a JSON string
   */
  private fun summarize(text: String, lang: Language? = null, prettyPrint: Boolean = false): String {

    this.checkText(text)

    val textLang: Language = this.getTextLanguage(text = text, forcedLang = lang)
    val parsedSentences: List<MorphoSynSentence> = this.parse(text = text, langCode = textLang.isoCode)

    val summary: Summary = Summarizer(
      sentences = parsedSentences,
      ignoreLemmas = this.lemmasBlacklist[textLang.isoCode] ?: setOf(),
      minLCMSupport = if (text.length > 10000) 0.001 else 0.01
    ).getSummary()

    return json {
      obj(
        "salience_distribution" to array(summary.getSalienceDistribution()),
        "sentences" to array(parsedSentences.zip(summary.salienceScores).map { (sentence, score) ->
          obj(
            "text" to sentence.buildText(),
            "score" to score
          )
        }),
        "itemsets" to array(summary.relevantItemsets.map {
          obj(
            "text" to it.text,
            "score" to it.score
          )
        }),
        "keywords" to array(summary.relevantKeywords.map {
          obj(
            "keyword" to it.keyword,
            "score" to it.score
          )
        })
      )
    }.toJsonString(prettyPrint) + if (prettyPrint) "\n" else ""
  }
}
