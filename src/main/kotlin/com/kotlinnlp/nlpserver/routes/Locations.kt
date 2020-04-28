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
import com.kotlinnlp.geolocation.LocationsFinder
import com.kotlinnlp.geolocation.dictionary.LocationsDictionary
import com.kotlinnlp.geolocation.structures.CandidateEntity
import com.kotlinnlp.geolocation.structures.Location
import com.kotlinnlp.languagedetector.LanguageDetector
import com.kotlinnlp.linguisticdescription.language.Language
import com.kotlinnlp.linguisticdescription.language.getLanguageByIso
import com.kotlinnlp.neuraltokenizer.NeuralTokenizer
import com.kotlinnlp.neuraltokenizer.Sentence
import com.kotlinnlp.nlpserver.routes.utils.TokenizingCommand
import com.kotlinnlp.nlpserver.setAppender
import org.apache.log4j.Logger
import spark.Spark

/**
 * The command executed on the route '/locations'.
 *
 * @param languageDetector a language detector (can be null)
 * @param tokenizers a map of tokenizers associated by language ISO 639-1 code
 * @param dictionary a locations dictionary
 */
class Locations(
  override val languageDetector: LanguageDetector?,
  override val tokenizers: Map<String, NeuralTokenizer>,
  private val dictionary: LocationsDictionary
) : Route, TokenizingCommand {

  /**
   * The name of the command.
   */
  override val name: String = "locations"

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

      val jsonBody: JsonObject = request.getJsonObject()

      this.findLocations(
        text = jsonBody.string("text")!!,
        language = getLanguageByIso(jsonBody.string("lang")!!),
        candidates = jsonBody.array<JsonObject>("candidates")!!.map { it.toCandidateEntity() },
        prettyPrint = request.booleanParam("pretty"))
    }

    Spark.post("/:lang") { request, _ ->

      val jsonBody: JsonObject = request.getJsonObject()

      this.findLocations(
        text = jsonBody.string("text")!!,
        language = getLanguageByIso(jsonBody.string("lang")!!),
        candidates = jsonBody.array<JsonObject>("candidates")!!.map { it.toCandidateEntity() },
        prettyPrint = request.booleanParam("pretty"))
    }
  }

  /**
   * Find locations in the given [text].
   *
   * @param text the input text
   * @param language the language to use to tokenize the [text]
   * @param candidates the list of candidate locations
   * @param prettyPrint pretty print (default = false)
   *
   * @return the parsed [text] in the given string [format]
   */
  private fun findLocations(text: String,
                            language: Language,
                            candidates: List<CandidateEntity>,
                            prettyPrint: Boolean = false): String {

    this.checkText(text)

    val sentences: List<Sentence> = this.tokenize(text = text, language = language)

    logger.debug("Searching for locations mentioned in the text '${text.cutText(50)}'...")

    val finder = LocationsFinder(
      dictionary = this.dictionary,
      textTokens = sentences.flatMap { sentence -> sentence.tokens.map { it.form } },
      candidateEntities = candidates.toSet(),
      coordinateEntitiesGroups = listOf(),
      ambiguityGroups = listOf()
    )

    return json {
      array(finder.bestLocations.map {
        JsonObject(it.toJSON() + it.location.getParentsInfo())
      })
    }.toJsonString(prettyPrint) + if (prettyPrint) "\n" else ""
  }

  /**
   * @return a new [CandidateEntity] built from this JSON representation
   */
  private fun JsonObject.toCandidateEntity() = CandidateEntity(
    name = this.string("name")!!,
    score = this.double("score")!!,
    occurrences = this.array<JsonArray<Int>>("occurrences")?.map { range -> IntRange(range[0], range[1]) } ?: listOf()
  )

  /**
   * @return a map of parents info of this location (only actual parents are present)
   */
  private fun Location.getParentsInfo(): Map<String, String?> = mapOf(
    "adminArea1" to this.adminArea1Id?.let { this@Locations.dictionary[it]!!.name },
    "adminArea2" to this.adminArea2Id?.let { this@Locations.dictionary[it]!!.name },
    "countryIso" to this.countryId?.let { this@Locations.dictionary[it]!!.isoA2 },
    "country" to this.countryId?.let { this@Locations.dictionary[it]!!.name },
    "continent" to this.continentId?.let { this@Locations.dictionary[it]!!.name }
  )
}
