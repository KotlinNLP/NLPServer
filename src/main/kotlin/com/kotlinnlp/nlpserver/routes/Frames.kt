/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver.routes

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.json
import com.kotlinnlp.frameextractor.FramesExtractor
import com.kotlinnlp.frameextractor.TextFramesExtractor
import com.kotlinnlp.languagedetector.LanguageDetector
import com.kotlinnlp.linguisticdescription.InvalidLanguageCode
import com.kotlinnlp.linguisticdescription.language.Language
import com.kotlinnlp.linguisticdescription.language.getLanguageByIso
import com.kotlinnlp.linguisticdescription.sentence.token.FormToken
import com.kotlinnlp.linguisticdescription.sentence.Sentence
import com.kotlinnlp.neuraltokenizer.NeuralTokenizer
import com.kotlinnlp.neuraltokenizer.Sentence as TokenizerSentence
import com.kotlinnlp.nlpserver.InvalidDomain
import com.kotlinnlp.nlpserver.routes.utils.TokenizingCommand
import com.kotlinnlp.nlpserver.setAppender
import org.apache.log4j.Logger
import spark.Spark

/**
 * The command executed on the route '/frames'.

 * @param languageDetector a language detector (can be null)
 * @param tokenizers a map of neural tokenizers associated by language ISO 639-1 code
 * @param frameExtractors a map of text frames extractors associated by domain name
 */
class Frames(
  override val languageDetector: LanguageDetector?,
  override val tokenizers: Map<String, NeuralTokenizer>,
  private val frameExtractors: Map<String, TextFramesExtractor>
) : Route, TokenizingCommand {

  /**
   * The name of the command.
   */
  override val name: String = "frames"

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
      this.extractFrames(
        text = request.requiredQueryParam("text"),
        lang = request.queryParams("lang")?.let { getLanguageByIso(it) },
        domain = request.queryParams("domain"),
        distribution = request.booleanParam("distribution"),
        prettyPrint = request.booleanParam("pretty"))
    }

    Spark.get("/:domain") { request, _ ->
      this.extractFrames(
        text = request.requiredQueryParam("text"),
        lang = request.queryParams("lang")?.let { getLanguageByIso(it) },
        domain = request.params("domain"),
        distribution = request.booleanParam("distribution"),
        prettyPrint = request.booleanParam("pretty"))
    }

    Spark.post("") { request, _ ->

      val jsonBody: JsonObject = request.getJsonObject()

      this.extractFrames(
        text = jsonBody.string("text")!!,
        lang = request.queryParams("lang")?.let { getLanguageByIso(it) },
        distribution = request.booleanParam("distribution"),
        prettyPrint = request.booleanParam("pretty"))
    }

    Spark.post("/:domain") { request, _ ->

      val jsonBody: JsonObject = request.getJsonObject()

      this.extractFrames(
        text = jsonBody.string("text")!!,
        lang = request.queryParams("lang")?.let { getLanguageByIso(it) },
        domain = request.params("domain"),
        distribution = request.booleanParam("distribution"),
        prettyPrint = request.booleanParam("pretty"))
    }
  }

  /**
   * Extract frames from the given [text], eventually forcing on a given language and a given domain.
   *
   * @param text the text from which to extract frames
   * @param lang the language to use to analyze the [text] (default = unknown)
   * @param domain force to use the frame extractors associated to the given domain
   * @param distribution whether to include the distribution in the response (default = true)
   * @param prettyPrint pretty print, used for JSON format (default = false)
   *
   * @throws InvalidLanguageCode when the requested (or detected) language is not compatible
   * @throws InvalidDomain when the given domain is not valid
   *
   * @return the list of frames extracted, in a JSON string
   */
  private fun extractFrames(text: String,
                            lang: Language? = null,
                            domain: String? = null,
                            distribution: Boolean = true,
                            prettyPrint: Boolean = false): String {

    this.checkText(text)

    val sentences: List<TokenizerSentence> =
      this.tokenize(text = text, language = lang).filter { it.tokens.isNotEmpty() }

    val extractors: List<TextFramesExtractor> = domain?.let {
      listOf(this.frameExtractors[it] ?: throw InvalidDomain(domain))
    } ?: this.frameExtractors.values.toList()

    val jsonFrames: JsonArray<*> = json {
      array(extractors.map { extractor ->

        logger.debug("Extracting frames for domain '${extractor.model.name}' from text '${text.cutText(50)}'...")

        obj(
          "domain" to extractor.model.name,
          "sentences" to array(sentences.map { sentence ->

            @Suppress("UNCHECKED_CAST")
            val output: FramesExtractor.Output = extractor.extractFrames(sentence as Sentence<FormToken>)

            json {
              val jsonObj: JsonObject = obj("intent" to output.buildIntent().toJSON(sentence.tokens.map { it.form }))

              if (distribution) jsonObj["distribution"] = array(
                output.buildDistribution().map.entries
                  .asSequence()
                  .sortedByDescending { it.value }
                  .map { obj("intent" to it.key, "score" to it.value) }
                  .toList())

              jsonObj
            }
          })
        )
      })
    }

    return jsonFrames.toJsonString(prettyPrint) + if (prettyPrint) "\n" else ""
  }
}
