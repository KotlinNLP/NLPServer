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
import com.kotlinnlp.correlator.helpers.TextComparator
import com.kotlinnlp.languagedetector.LanguageDetector
import com.kotlinnlp.linguisticdescription.language.Language
import com.kotlinnlp.linguisticdescription.language.getLanguageByIso
import com.kotlinnlp.nlpserver.LanguageNotSupported
import com.kotlinnlp.nlpserver.routes.utils.LanguageDetectingCommand
import com.kotlinnlp.nlpserver.routes.utils.Progress
import com.kotlinnlp.nlpserver.setAppender
import com.kotlinnlp.utils.pmap
import org.apache.log4j.Logger
import spark.Spark

/**
 * The command executed on the route '/compare'.
 *
 * @param languageDetector a language detector (can be null)
 * @param comparators a map of text comparators associated by language ISO 639-1 code
 * @param parallelization the number of threads used to parallelize operations
 */
class Compare(
  override val languageDetector: LanguageDetector?,
  private val comparators: Map<String, TextComparator>,
  private val parallelization: Int
) : Route, LanguageDetectingCommand {

  /**
   * The name of the command.
   */
  override val name: String = "compare"

  /**
   * The logger of the command.
   */
  private val logger = Logger.getLogger(this::class.simpleName).setAppender()

  /**
   * Check requirements.
   */
  init {
    require(this.parallelization >= 1)
  }

  /**
   * Initialize the route.
   * Define the paths handled.
   */
  override fun initialize() {

    Spark.post("") { request, _ ->

      val jsonBody: JsonObject = request.getJsonObject()

      this.compare(
        baseText = jsonBody.string("text")!!,
        comparingTexts = jsonBody.array<JsonObject>("comparing")!!.associate { it.int("id")!! to it.string("text")!! },
        lang = request.queryParams("lang")?.let { getLanguageByIso(it) },
        prettyPrint = request.booleanParam("pretty"))
    }

    Spark.post("/:lang") { request, _ ->

      val jsonBody: JsonObject = request.getJsonObject()

      this.compare(
        baseText = jsonBody.string("text")!!,
        comparingTexts = jsonBody.array<JsonObject>("comparing")!!.associate { it.int("id")!! to it.string("text")!! },
        lang = getLanguageByIso(request.params("lang")),
        prettyPrint = request.booleanParam("pretty"))
    }
  }

  /**
   * Compare a text with others, giving a similarity score for each couple.
   *
   * @param baseText the base text
   * @param comparingTexts the comparing texts
   * @param lang the language of the text or null if it is unknown
   * @param prettyPrint pretty print, used for JSON format
   *
   * @return the results of the comparison, as objects with the text ID and the comparison score
   */
  private fun compare(baseText: String,
                      comparingTexts: Map<Int, String>,
                      lang: Language?,
                      prettyPrint: Boolean): String {

    (comparingTexts.values + baseText).forEach { this.checkText(it) }

    this.logger.debug("Comparing text `$baseText` with other ${comparingTexts.size}")

    val textLang: Language = this.getTextLanguage(text = baseText, forcedLang = lang)

    val results: JsonArray<*> = this.compare(
      baseText = baseText,
      comparingTexts = comparingTexts,
      comparator = this.comparators[textLang.isoCode] ?: throw LanguageNotSupported(textLang.isoCode))

    this.logger.debug("Texts compared")

    return results.toJsonString(prettyPrint) + if (prettyPrint) "\n" else ""
  }

  /**
   * Compare a text with others.
   *
   * @param baseText the base text
   * @param comparingTexts the comparing texts
   * @param comparator a texts comparator
   *
   * @return the results of the comparison, as objects with the text ID and the comparison score
   */
  private fun compare(baseText: String, comparingTexts: Map<Int, String>, comparator: TextComparator): JsonArray<*> {

    val textTokens: List<TextComparator.ComparingToken> = comparator.parse(baseText)
    val chunkSize: Int = Math.ceil(comparingTexts.size.toDouble() / this.parallelization).toInt()

    return json {

      array(
        comparingTexts
          .entries
          .chunked(chunkSize)
          .withIndex()
          .pmap { compareChunk(chunk = it, textTokens = textTokens, comparator = comparator) }
          .asSequence()
          .flatten()
          .sortedByDescending { it.second }
          .map {
            obj(
              "id" to it.first,
              "score" to it.second
            )
          }
          .toList()
      )
    }
  }

  /**
   * Compare a chunk of texts with the base text.
   *
   * @param chunk a chunk of comparing texts, with its index
   * @param textTokens the base text tokens
   * @param comparator a texts comparator
   *
   * @return the comparison results of the given texts
   */
  private fun compareChunk(chunk: IndexedValue<List<Map.Entry<Int, String>>>,
                           textTokens: List<TextComparator.ComparingToken>,
                           comparator: TextComparator): List<Pair<Int, Double>> {

    val progress = Progress(
      total = chunk.value.size,
      logger = this.logger,
      description = "Chunk #${chunk.index + 1} progress")

    return chunk.value.map { (id, text) ->

      progress.tick()

      id to comparator.compare(parsedTokensA = textTokens, parsedTokensB = comparator.parse(text)).score
    }
  }
}
