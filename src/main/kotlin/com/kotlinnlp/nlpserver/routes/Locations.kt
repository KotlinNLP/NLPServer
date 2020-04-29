/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver.routes

import com.beust.klaxon.JsonObject
import com.beust.klaxon.json
import com.kotlinnlp.geolocation.LocationsFinder
import com.kotlinnlp.geolocation.dictionary.LocationsDictionary
import com.kotlinnlp.geolocation.structures.CandidateEntity
import com.kotlinnlp.languagedetector.LanguageDetector
import com.kotlinnlp.linguisticdescription.language.Language
import com.kotlinnlp.linguisticdescription.language.getLanguageByIso
import com.kotlinnlp.linguisticdescription.sentence.RealSentence
import com.kotlinnlp.linguisticdescription.sentence.properties.AnnotatedSegment
import com.kotlinnlp.linguisticdescription.sentence.properties.Entity
import com.kotlinnlp.linguisticdescription.sentence.token.RealToken
import com.kotlinnlp.neuraltokenizer.NeuralTokenizer
import com.kotlinnlp.neuraltokenizer.Sentence
import com.kotlinnlp.nlpserver.LanguageNotSupported
import com.kotlinnlp.nlpserver.routes.utils.TokenizingCommand
import com.kotlinnlp.nlpserver.setAppender
import com.kotlinnlp.tokenslabeler.TokensLabeler
import org.apache.log4j.Logger
import spark.Spark
import java.lang.RuntimeException

/**
 * The command executed on the route '/locations'.
 *
 * @param languageDetector a language detector (can be null)
 * @param tokenizers a map of tokenizers associated by language ISO 639-1 code
 * @param dictionary a locations dictionary
 * @param nerLabelers a map of named-entity labelers to find candidates automatically, associated by language ISO 639-1
 *                    code (optional)
 */
class Locations(
  override val languageDetector: LanguageDetector?,
  override val tokenizers: Map<String, NeuralTokenizer>,
  private val dictionary: LocationsDictionary,
  private val nerLabelers: Map<String, TokensLabeler>?
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
        language = request.queryParams("lang")?.let { getLanguageByIso(it) },
        jsonBody = jsonBody,
        prettyPrint = request.booleanParam("pretty"))
    }

    Spark.post("/:lang") { request, _ ->

      val jsonBody: JsonObject = request.getJsonObject()

      this.findLocations(
        language = request.params("lang")?.let { getLanguageByIso(it) },
        jsonBody = jsonBody,
        prettyPrint = request.booleanParam("pretty"))
    }
  }

  /**
   * Find locations in the input text.
   *
   * @param language the text language or null to detect it automatically
   * @param jsonBody the JSON request body
   * @param prettyPrint pretty print
   *
   * @return the locations formatted into a JSON string
   */
  private fun findLocations(language: Language?, jsonBody: JsonObject, prettyPrint: Boolean): String {

    val text: String = this.checkText(jsonBody.string("text")!!)

    val textLang: Language = this.getTextLanguage(text = text, forcedLang = language)
    val sentences: List<Sentence> = this.tokenize(text = text, language = textLang)

    val candidates: List<CandidateEntity> =
      jsonBody.array<JsonObject>("candidates")?.map { it.toCandidateEntity() }
        ?: this.findCandidates(sentences = sentences, lang = textLang)

    this.logger.debug("Searching for locations mentioned in the text '${text.cutText(50)}'...")

    return this.findLocations(sentences = sentences, candidates = candidates, prettyPrint = prettyPrint)
  }

  /**
   * Find locations in the given sentences based on given candidate entities.
   *
   * @param sentences the input sentences tokenized
   * @param candidates the entities candidate as locations
   * @param prettyPrint pretty print
   *
   * @return the locations formatted into a JSON string
   */
  private fun findLocations(sentences: List<Sentence>,
                            candidates: List<CandidateEntity>,
                            prettyPrint: Boolean): String {

    val finder = LocationsFinder(
      dictionary = this.dictionary,
      textTokens = sentences.flatMap { sentence -> sentence.tokens.map { it.form } },
      candidateEntities = candidates.toSet(),
      coordinateEntitiesGroups = listOf(),
      ambiguityGroups = listOf())

    this.logger.debug("Locations found (${finder.bestLocations.size}: " +
      finder.bestLocations.joinToString(", ") { "${it.location.name} (%.1f %%)".format(100.0 * it.confidence) })

    return json {
      array(finder.bestLocations.map {
        it.toJSON(dictionary)
      })
    }.toJsonString(prettyPrint) + if (prettyPrint) "\n" else ""
  }

  /**
   * Find entities candidate as locations using the proper named-entity labeler.
   *
   * @param sentences the tokenized input sentences
   * @param lang the text language
   *
   * @throws LanguageNotSupported if the language is not supported for NER labeling
   *
   * @return the entities candidate as locations
   */
  private fun findCandidates(sentences: List<Sentence>, lang: Language): List<CandidateEntity> {

    val labeler: TokensLabeler = this.nerLabelers
      ?.let { it[lang.isoCode] ?: throw LanguageNotSupported(lang.isoCode) }
      ?: throw RuntimeException("Cannot find candidates: not present in the request, NER labelers not loaded.")

    this.logger.debug("Searching for candidate locations with the NER labeler...")

    val candidates: List<CandidateEntity> = sentences.flatMap { sentence ->

      @Suppress("UNCHECKED_CAST")
      val segments: List<AnnotatedSegment> = labeler.predictAsSegments(sentence as RealSentence<RealToken>)

      val segmentsByForm: Map<String, List<AnnotatedSegment>> = segments
        .filter { it.annotation == Entity.Type.Location.annotation }
        .groupBy { it.getRefTokens(sentence.tokens).joinToString(" ") { tk -> tk.form }.toTitleCase() }

      segmentsByForm.map { (form, segments) ->
        CandidateEntity(name = form, score = segments.asSequence().map { it.score }.average())
      }
    }

    this.logger.debug("Location candidates found (${candidates.size}): " +
      candidates.joinToString(", ") { "${it.name} (%.1f %%)".format(100.0 * it.score) }.ifEmpty { "none" })

    return candidates
  }

  /**
   * @return a new [CandidateEntity] built from this JSON representation
   */
  private fun JsonObject.toCandidateEntity() = CandidateEntity(
    name = this.string("name")!!,
    score = this.double("score")!!
  )

  /**
   * Tokenize this string by spaces and convert each word to title case (only the first char in upper case).
   *
   * @return a copy of this string with each word in title case
   */
  private fun String.toTitleCase(): String = this.trim().split(" ").joinToString(" ") {
    it[0].toUpperCase() + it.substring(1, it.length).toLowerCase()
  }
}
