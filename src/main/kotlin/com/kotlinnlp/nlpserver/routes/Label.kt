/* Copyright 2020-present Simone Cangialosi. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver.routes

import com.beust.klaxon.JsonBase
import com.beust.klaxon.JsonObject
import com.beust.klaxon.json
import com.kotlinnlp.languagedetector.LanguageDetector
import com.kotlinnlp.linguisticdescription.InvalidLanguageCode
import com.kotlinnlp.linguisticdescription.language.Language
import com.kotlinnlp.linguisticdescription.language.getLanguageByIso
import com.kotlinnlp.linguisticdescription.sentence.RealSentence
import com.kotlinnlp.linguisticdescription.sentence.token.RealToken
import com.kotlinnlp.neuraltokenizer.NeuralTokenizer
import com.kotlinnlp.neuraltokenizer.Sentence as TokenizerSentence
import com.kotlinnlp.nlpserver.InvalidDomain
import com.kotlinnlp.nlpserver.routes.utils.TokenizingCommand
import com.kotlinnlp.nlpserver.setAppender
import com.kotlinnlp.tokenslabeler.TokensLabeler
import org.apache.log4j.Logger
import spark.Spark

/**
 * The command executed on the route '/label'.

 * @param languageDetector a language detector (can be null)
 * @param tokenizers a map of neural tokenizers associated by language ISO 639-1 code
 * @param labelers a map of tokens labelers associated by domain name
 */
class Label(
  override val languageDetector: LanguageDetector?,
  override val tokenizers: Map<String, NeuralTokenizer>,
  private val labelers: Map<String, TokensLabeler>
) : Route, TokenizingCommand {

  /**
   * The name of the command.
   */
  override val name: String = "label"

  /**
   * The logger of the command.
   */
  override val logger = Logger.getLogger(this::class.simpleName).setAppender()

  /**
   * Initialize the route.
   * Define the paths handled.
   */
  override fun initialize() {

    Spark.post("") { request, _ ->
      this.labelText(
        text = request.getJsonObject().string("text")!!,
        lang = request.queryParams("lang")?.let { getLanguageByIso(it) },
        prettyPrint = request.booleanParam("pretty"))
    }

    Spark.post("/:domain") { request, _ ->
      this.labelText(
        text = request.getJsonObject().string("text")!!,
        lang = request.queryParams("lang")?.let { getLanguageByIso(it) },
        domain = request.params("domain"),
        prettyPrint = request.booleanParam("pretty"))
    }

    Spark.post("/:lang/:domain") { request, _ ->
      this.labelText(
        text = request.getJsonObject().string("text")!!,
        lang = getLanguageByIso(request.params("lang")),
        domain = request.params("domain"),
        prettyPrint = request.booleanParam("pretty"))
    }
  }

  /**
   * Label the tokens of a given [text], eventually forcing on a given language and a given domain.
   *
   * @param text the text to label
   * @param lang the language to use to analyze the [text] (default = unknown)
   * @param domain force to use the tokens labeler associated to the given domain
   * @param prettyPrint pretty print, used for JSON format (default = false)
   *
   * @throws InvalidLanguageCode when the requested (or detected) language is not compatible
   * @throws InvalidDomain when the given domain is not valid
   *
   * @return the labeled text, eventually with the distribution, in a JSON string
   */
  private fun labelText(text: String,
                        lang: Language? = null,
                        domain: String? = null,
                        prettyPrint: Boolean = false): String {

    this.checkText(text)

    val textLang: Language = this.getTextLanguage(text = text, forcedLang = lang)
    val sentences: List<TokenizerSentence> =
      this.tokenize(text = text, language = textLang).filter { it.tokens.isNotEmpty() }

    val jsonRes: JsonBase = domain?.let {
      labelByDomain(domain = it, sentences = sentences, text = text)
    } ?: json {
      array(labelers.keys.map { labelByDomain(domain = it, sentences = sentences, text = text) })
    }

    return jsonRes.toJsonString(prettyPrint) + if (prettyPrint) "\n" else ""
  }

  /**
   * Label a text respect to a given domain.
   *
   * @param domain the labeler domain
   * @param sentences the tokenized sentences
   * @param text the input text
   *
   * @return the text labeling as JSON object
   */
  private fun labelByDomain(domain: String, sentences: List<TokenizerSentence>, text: String): JsonObject {

    logger.debug("$domain labeling of text '${text.cutText(50)}'...")

    val labeler = this.labelers.getValue(domain)

    return json {
      obj(
        "domain" to domain,
        "sentences" to array(sentences.map { sentence ->
          obj(
            "tokens" to array(
              @Suppress("UNCHECKED_CAST")
              labeler.predict(sentence as RealSentence<RealToken>).mapIndexed { i, label ->
                obj(
                  "form" to sentence.tokens[i].form,
                  "iob" to label.type.annotation,
                  "label" to label.value,
                  "score" to label.score
                )
              }
            )
          )
        })
      )
    }
  }
}
