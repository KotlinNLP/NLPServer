/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver

import com.beust.klaxon.*
import com.kotlinnlp.geolocation.structures.CandidateEntity
import com.kotlinnlp.linguisticdescription.language.Language
import com.kotlinnlp.linguisticdescription.language.getLanguageByIso
import com.kotlinnlp.nlpserver.commands.*
import spark.Request
import spark.Spark
import java.util.logging.Logger

/**
 * The NLP Server class.
 *
 * @param port the port listened from the server
 * @param detectLanguage the handler of the 'DetectLanguage' command
 * @param tokenize the handler of the 'Tokenize' command
 * @param parse the handler of the 'Parse' command
 * @param findLocations the handler of the 'FindLocations' command
 * @param extractFrames the handler of the 'ExtractFrames' command
 * @param categorize the handler of the 'Categorize' command
 */
class NLPServer(
  port: Int,
  private val detectLanguage: DetectLanguage?,
  private val tokenize: Tokenize?,
  private val parse: Parse?,
  private val findLocations: FindLocations?,
  private val extractFrames: ExtractFrames?,
  private val categorize: Categorize?
) {

  /**
   * The logger of the server.
   */
  private val logger = Logger.getLogger("NLP Server")

  /**
   * Initialize Spark: set port and exceptions handling.
   */
  init {

    Spark.port(port)

    Spark.exception(MissingParameters::class.java) { exception, _, response ->
      response.status(400)
      response.body("Missing required parameters: %s\n".format((exception as MissingParameters).message))
    }

    Spark.exception(EmptyText::class.java) { _, _, response ->
      response.status(400)
      response.body("Text is empty\n")
    }

    Spark.exception(LanguageNotSupported::class.java) { exception, _, response ->
      response.status(400)
      response.body("Language not supported: %s\n".format((exception as LanguageNotSupported).langCode))
    }

    Spark.exception(InvalidDomain::class.java) { exception, _, response ->
      response.status(400)
      response.body("Invalid domain: %s\n".format((exception as InvalidDomain).domain))
    }

    Spark.exception(RuntimeException::class.java) { exception, _, response ->
      response.status(500)
      response.body("500 Server error\n")
      this.logger.warning(exception.toString() + ". Stacktrace: \n  " + exception.stackTrace.joinToString("\n  "))
    }
  }

  /**
   * Start the server.
   */
  fun start() {

    this.enbaleCORS()

    this.detectLanguage?.let {
      Spark.path("/detect-language") { this.detectLanguageRoute() }
      Spark.path("/classify-tokens-language") { this.classifyTokensLanguageRoute() }
    }

    this.tokenize?.let {

      Spark.path("/tokenize") { this.tokenizeRoute() }

      this.parse?.let { Spark.path("/parse") { this.parseRoute() } }
      this.extractFrames?.let { Spark.path("/extract-frames") { this.extractFramesRoute() } }
      this.categorize?.let { Spark.path("/categorize") { this.categorizeRoute() } }
    }

    this.findLocations?.let { Spark.path("/find-locations") { this.findLocationsRoute() } }

    this.logger.info("NLP Server running on 'localhost:%d'".format(Spark.port()))
  }

  /**
   * Enable CORS requests.
   */
  private fun enbaleCORS() {

    Spark.before("/*") { _, response ->
      response.header("Access-Control-Allow-Origin", "*")
    }
  }

  /**
   * Define the '/parse' route.
   */
  private fun parseRoute() {

    Spark.get("") { request, _ ->

      request.checkRequiredParams(requiredParams = listOf("text"))

      this.parse!!(
        text = request.queryParams("text"),
        lang = request.queryParams("lang")?.let { getLanguageByIso(it) },
        format = this.getParsedFormat(request.queryParams("format") ?: "JSON"),
        prettyPrint = request.queryParams("pretty") != null)
    }

    Spark.get("/:lang") { request, _ ->

      request.checkRequiredParams(requiredParams = listOf("text"))

      this.parse!!(
        text = request.queryParams("text"),
        lang = request.params("lang")?.let { getLanguageByIso(it) },
        format = this.getParsedFormat(request.queryParams("format") ?: "JSON"),
        prettyPrint = request.queryParams("pretty") != null)
    }

    Spark.post("") { request, _ ->
      this.parse!!(
        text = request.body(),
        lang = null,
        format = this.getParsedFormat(request.queryParams("format") ?: "JSON"),
        prettyPrint = request.queryParams("pretty") != null)
    }

    Spark.post("/:lang") { request, _ ->
      this.parse!!(
        text = request.body(),
        lang = request.params("lang")?.let { getLanguageByIso(it) },
        format = this.getParsedFormat(request.queryParams("format") ?: "JSON"),
        prettyPrint = request.queryParams("pretty") != null)
    }
  }

  /**
   * @param formatString a string representing a parsing response format ('JSON', 'CoNLL')
   *
   * @return the parse response format related to the given [formatString]
   */
  private fun getParsedFormat(formatString: String): Parse.ResponseFormat = when (formatString.toLowerCase()) {
    "json" -> Parse.ResponseFormat.JSON
    "conll" -> Parse.ResponseFormat.CoNLL
    else -> throw RuntimeException("Invalid parsing response format: $formatString.")
  }

  /**
   * Define the '/tokenize' route.
   */
  private fun tokenizeRoute() {

    Spark.get("") { request, _ ->

      request.checkRequiredParams(requiredParams = listOf("text"))

      this.tokenize!!(text = request.queryParams("text"), prettyPrint = request.queryParams("pretty") != null)
    }

    Spark.get("/:lang") { request, _ ->

      request.checkRequiredParams(requiredParams = listOf("text"))

      this.tokenize!!(
        text = request.queryParams("text"),
        language = request.params("lang")?.let { getLanguageByIso(it) },
        prettyPrint = request.queryParams("pretty") != null)
    }

    Spark.post("") { request, _ ->
      this.tokenize!!(text = request.body(), prettyPrint = request.queryParams("pretty") != null)
    }

    Spark.post("/:lang") { request, _ ->
      this.tokenize!!(
        text = request.body(),
        language = request.params("lang")?.let { getLanguageByIso(it) },
        prettyPrint = request.queryParams("pretty") != null)
    }
  }

  /**
   * Define the '/detect-language' route.
   */
  private fun detectLanguageRoute() {

    Spark.get("") { request, _ ->

      request.checkRequiredParams(requiredParams = listOf("text"))

      this.detectLanguage!!(
        text = request.queryParams("text"),
        distribution = request.queryParams("distribution") != null,
        prettyPrint = request.queryParams("pretty") != null)
    }

    Spark.post("") { request, _ ->
      this.detectLanguage!!(
        text = request.body(),
        distribution = request.queryParams("distribution") != null,
        prettyPrint = request.queryParams("pretty") != null)
    }
  }

  /**
   * Define the '/classify-tokens-language' route.
   */
  private fun classifyTokensLanguageRoute() {

    Spark.get("") { request, _ ->

      request.checkRequiredParams(requiredParams = listOf("text"))

      this.detectLanguage!!.perToken(
        text = request.queryParams("text"),
        distribution = request.queryParams("distribution") != null,
        prettyPrint = request.queryParams("pretty") != null)
    }

    Spark.post("") { request, _ ->
      this.detectLanguage!!.perToken(
        text = request.body(),
        distribution = request.queryParams("distribution") != null,
        prettyPrint = request.queryParams("pretty") != null)
    }
  }

  /**
   * Define the '/find-locations' route.
   */
  private fun findLocationsRoute() {

    Spark.post("") { request, _ ->

      val jsonBody: JsonObject = Parser().parse(StringBuilder(request.body())) as JsonObject

      this.execFindLocations(
        jsonBody = jsonBody,
        language = getLanguageByIso(jsonBody.string("lang")!!),
        prettyPrint = request.queryParams("pretty") != null)
    }

    Spark.post("/:lang") { request, _ ->

      val jsonBody: JsonObject = Parser().parse(StringBuilder(request.body())) as JsonObject

      this.execFindLocations(
        jsonBody = jsonBody,
        language = getLanguageByIso(request.params("lang")!!),
        prettyPrint = request.queryParams("pretty") != null)
    }
  }

  /**
   * Define the '/extract-frames' route.
   */
  private fun extractFramesRoute() {

    Spark.get("") { request, _ ->

      request.checkRequiredParams(requiredParams = listOf("text"))

      this.extractFrames!!(
        text = request.queryParams("text"),
        lang = request.queryParams("lang")?.let { getLanguageByIso(it) },
        domain = request.queryParams("domain"),
        distribution = request.queryParams("distribution") != null,
        prettyPrint = request.queryParams("pretty") != null)
    }

    Spark.get("/:domain") { request, _ ->

      request.checkRequiredParams(requiredParams = listOf("text"))

      this.extractFrames!!(
        text = request.queryParams("text"),
        lang = request.queryParams("lang")?.let { getLanguageByIso(it) },
        domain = request.params("domain"),
        distribution = request.queryParams("distribution") != null,
        prettyPrint = request.queryParams("pretty") != null)
    }

    Spark.post("") { request, _ ->
      this.extractFrames!!(
        text = request.body(),
        lang = request.queryParams("lang")?.let { getLanguageByIso(it) },
        distribution = request.queryParams("distribution") != null,
        prettyPrint = request.queryParams("pretty") != null)
    }

    Spark.post("/:domain") { request, _ ->
      this.extractFrames!!(
        text = request.body(),
        lang = request.queryParams("lang")?.let { getLanguageByIso(it) },
        domain = request.params("domain"),
        distribution = request.queryParams("distribution") != null,
        prettyPrint = request.queryParams("pretty") != null)
    }
  }

  /**
   * Define the '/categorize' route.
   */
  private fun categorizeRoute() {

    Spark.get("") { request, _ ->

      request.checkRequiredParams(requiredParams = listOf("text"))

      this.categorize!!(
        text = request.queryParams("text"),
        lang = request.queryParams("lang")?.let { getLanguageByIso(it) },
        domain = request.queryParams("domain"),
        distribution = request.queryParams("distribution") != null,
        prettyPrint = request.queryParams("pretty") != null)
    }

    Spark.get("/:domain") { request, _ ->

      request.checkRequiredParams(requiredParams = listOf("text"))

      this.categorize!!(
        text = request.queryParams("text"),
        lang = request.queryParams("lang")?.let { getLanguageByIso(it) },
        domain = request.params("domain"),
        distribution = request.queryParams("distribution") != null,
        prettyPrint = request.queryParams("pretty") != null)
    }

    Spark.get("/:lang/:domain") { request, _ ->

      request.checkRequiredParams(requiredParams = listOf("text"))

      this.categorize!!(
        text = request.queryParams("text"),
        lang = getLanguageByIso(request.params("lang")),
        domain = request.params("domain"),
        distribution = request.queryParams("distribution") != null,
        prettyPrint = request.queryParams("pretty") != null)
    }

    Spark.post("") { request, _ ->
      this.categorize!!(
        text = request.body(),
        lang = request.queryParams("lang")?.let { getLanguageByIso(it) },
        distribution = request.queryParams("distribution") != null,
        prettyPrint = request.queryParams("pretty") != null)
    }

    Spark.post("/:domain") { request, _ ->
      this.categorize!!(
        text = request.body(),
        lang = request.queryParams("lang")?.let { getLanguageByIso(it) },
        domain = request.params("domain"),
        distribution = request.queryParams("distribution") != null,
        prettyPrint = request.queryParams("pretty") != null)
    }

    Spark.post("/:lang/:domain") { request, _ ->
      this.categorize!!(
        text = request.body(),
        lang = getLanguageByIso(request.params("lang")),
        domain = request.params("domain"),
        distribution = request.queryParams("distribution") != null,
        prettyPrint = request.queryParams("pretty") != null)
    }
  }

  /**
   * Execute the command 'FindLocations'.
   *
   * @param jsonBody the JSON object containing the body of the request
   * @param language the language of the input text
   * @param prettyPrint whether to pretty print the result
   *
   * @return the result of the command
   */
  private fun execFindLocations(jsonBody: JsonObject, language: Language, prettyPrint: Boolean): String {

    return this.findLocations!!(
      text = jsonBody.string("text")!!,
      language = language,
      candidates = jsonBody.array<JsonObject>("candidates")!!.map { candidate ->
        CandidateEntity(
          name = candidate.string("name")!!,
          score = candidate.double("score")!!,
          occurrences = candidate.array<JsonArray<Int>>("occurrences")?.map { range -> IntRange(range[0], range[1]) }
            ?: listOf()
        )
      },
      prettyPrint = prettyPrint
    )
  }

  /**
   * Check if all [requiredParams] are present in this [Request].
   *
   * @param requiredParams the list of required parameters to check
   *
   * @throws MissingParameters if at least one parameter is missing
   */
  private fun Request.checkRequiredParams(requiredParams: List<String>) {

    val missingParams: List<String> = this.getMissingParams(requiredParams)

    if (missingParams.isNotEmpty()) {
      throw MissingParameters(missingParams)
    }
  }

  /**
   * @param requiredParams a list of required parameters
   *
   * @return a list of required parameters that are missing in this [Request]
   */
  private fun Request.getMissingParams(requiredParams: List<String>): List<String> {

    val requestParams = this.queryParams()

    return requiredParams.filter { !requestParams.contains(it) }
  }
}
