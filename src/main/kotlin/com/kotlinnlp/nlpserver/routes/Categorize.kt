/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver.routes

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonBase
import com.beust.klaxon.JsonObject
import com.beust.klaxon.json
import com.kotlinnlp.hanclassifier.HANClassifier
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
import com.kotlinnlp.simplednn.simplemath.ndarray.dense.DenseNDArray
import org.apache.log4j.Logger
import spark.Spark

/**
 * The command executed on the route '/categorize'.

 * @param languageDetector a language detector (can be null)
 * @param tokenizers a map of neural tokenizers associated by language ISO 639-1 code
 * @param hanClassifiers a map of HAN classifier associated by domain name
 */
class Categorize(
  override val languageDetector: LanguageDetector?,
  override val tokenizers: Map<String, NeuralTokenizer>,
  private val hanClassifiers: Map<String, HANClassifier>
) : Route, TokenizingCommand {

  /**
   * The name of the command.
   */
  override val name: String = "categorize"

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
      this.categorize(
        text = request.requiredQueryParam("text"),
        lang = request.queryParams("lang")?.let { getLanguageByIso(it) },
        domain = request.queryParams("domain"),
        distribution = request.booleanParam("distribution"),
        prettyPrint = request.booleanParam("pretty"))
    }

    Spark.get("/:domain") { request, _ ->
      this.categorize(
        text = request.requiredQueryParam("text"),
        lang = request.queryParams("lang")?.let { getLanguageByIso(it) },
        domain = request.params("domain"),
        distribution = request.booleanParam("distribution"),
        prettyPrint = request.booleanParam("pretty"))
    }

    Spark.get("/:lang/:domain") { request, _ ->
      this.categorize(
        text = request.requiredQueryParam("text"),
        lang = getLanguageByIso(request.params("lang")),
        domain = request.params("domain"),
        distribution = request.booleanParam("distribution"),
        prettyPrint = request.booleanParam("pretty"))
    }

    Spark.post("") { request, _ ->
      this.categorize(
        text = request.getJsonObject().string("text")!!,
        lang = request.queryParams("lang")?.let { getLanguageByIso(it) },
        distribution = request.booleanParam("distribution"),
        prettyPrint = request.booleanParam("pretty"))
    }

    Spark.post("/:domain") { request, _ ->
      this.categorize(
        text = request.getJsonObject().string("text")!!,
        lang = request.queryParams("lang")?.let { getLanguageByIso(it) },
        domain = request.params("domain"),
        distribution = request.booleanParam("distribution"),
        prettyPrint = request.booleanParam("pretty"))
    }

    Spark.post("/:lang/:domain") { request, _ ->
      this.categorize(
        text = request.getJsonObject().string("text")!!,
        lang = getLanguageByIso(request.params("lang")),
        domain = request.params("domain"),
        distribution = request.booleanParam("distribution"),
        prettyPrint = request.booleanParam("pretty"))
    }
  }

  /**
   * Categorize a given [text], eventually forcing on a given language and a given domain.
   *
   * @param text the text to categorize
   * @param lang the language to use to analyze the [text] (default = unknown)
   * @param domain force to use the classifiers associated to the given domain
   * @param distribution whether to include the distribution in the response (default = true)
   * @param prettyPrint pretty print, used for JSON format (default = false)
   *
   * @throws InvalidLanguageCode when the requested (or detected) language is not compatible
   * @throws InvalidDomain when the given domain is not valid
   *
   * @return the category recognized, eventually with the distribution, in a JSON string
   */
  private fun categorize(text: String,
                         lang: Language? = null,
                         domain: String? = null,
                         distribution: Boolean = true,
                         prettyPrint: Boolean = false): String {

    this.checkText(text)

    val sentences: List<TokenizerSentence> =
      this.tokenize(text = text, language = lang).filter { it.tokens.isNotEmpty() }

    val classifiers: List<HANClassifier> =
      domain?.let { listOf(this.hanClassifiers[domain] ?: throw InvalidDomain(domain)) }
        ?: this.hanClassifiers.values.toList()

    val outputPerDomain: JsonArray<*> = json {
      array(classifiers.map { classifier ->

        logger.debug("Classifying domain '$domain' of text '${text.cutText(50)}'...")

        @Suppress("UNCHECKED_CAST")
        val predictions: List<DenseNDArray> = classifier.classify(sentences.map { it as Sentence<FormToken> })

        logger.debug("$domain categories: " +
          predictions.joinToString(" | ") { "%d (%.2f %%)".format(it.argMaxIndex(), 100.0 * it.max()) })

        obj(
          "domain" to classifier.model.name,
          "categories" to array(predictions.map { prediction ->
            obj(
              "id" to prediction.argMaxIndex(),
              "score" to prediction.max()
            ).also {
              if (distribution)
                it["distribution"] = array(prediction.toDoubleArray().toList())
            }
          })
        )
      })
    }

    val jsonRes: JsonBase = domain?.let { outputPerDomain.single() as JsonObject } ?: outputPerDomain

    return jsonRes.toJsonString(prettyPrint) + if (prettyPrint) "\n" else ""
  }
}
