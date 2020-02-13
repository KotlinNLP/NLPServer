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
import spark.Filter
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
 * @param compare the handler of the 'Compare' command
 */
class NLPServer(
  port: Int,
  private val detectLanguage: DetectLanguage?,
  private val tokenize: Tokenize?,
  private val parse: Parse?,
  private val findLocations: FindLocations?,
  private val extractFrames: ExtractFrames?,
  private val categorize: Categorize?,
  private val compare: Compare?
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

    Spark.exception(MissingQueryParameters::class.java) { exception, _, response ->
      response.status(400)
      response.body("Missing required query parameters: %s\n".format((exception as MissingQueryParameters).message))
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

    Spark.before(Filter { _, response ->
      response.header("Content-Type", "application/json")
    })
  }

  /**
   * Start the server.
   */
  fun start() {

    this.enbaleCORS()

    this.detectLanguage?.let {

      Spark.path("/detect-language") { this.detectLanguageRoute() }
      this.logger.info("Route '/detect-language' enabled")

      Spark.path("/classify-tokens-language") { this.classifyTokensLanguageRoute() }
      this.logger.info("Route '/classify-tokens-language' enabled")
    }

    this.tokenize?.let {

      Spark.path("/tokenize") { this.tokenizeRoute() }
      this.logger.info("Route '/tokenize' enabled")

      this.parse?.let {

        Spark.path("/parse") { this.parseRoute() }
        this.logger.info("Route '/parse' enabled")

        Spark.path("/compare") { this.compareRoute() }
        this.logger.info("Route '/compare' enabled")
      }

      this.extractFrames?.let {
        Spark.path("/extract-frames") { this.extractFramesRoute() }
        this.logger.info("Route '/extract-frames' enabled")
      }

      this.categorize?.let {
        Spark.path("/categorize") { this.categorizeRoute() }
        this.logger.info("Route '/categorize' enabled")
      }
    }

    this.findLocations?.let {
      Spark.path("/find-locations") { this.findLocationsRoute() }
      this.logger.info("Route '/find-locations' enabled")
    }

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

    Spark.get("") { request, response ->

      this.parse!!(
        text = request.requiredQueryParam("text"),
        lang = request.queryParams("lang")?.let { getLanguageByIso(it) },
        format = this.getParsedFormat(request.queryParams("format") ?: "JSON"),
        response = response,
        prettyPrint = request.booleanParam("pretty"))
    }

    Spark.get("/:lang") { request, response ->

      this.parse!!(
        text = request.requiredQueryParam("text"),
        lang = request.params("lang")?.let { getLanguageByIso(it) },
        format = this.getParsedFormat(request.queryParams("format") ?: "JSON"),
        response = response,
        prettyPrint = request.booleanParam("pretty"))
    }

    Spark.post("") { request, response ->

      val jsonBody: JsonObject = request.getJsonObject()

      this.parse!!(
        text = jsonBody.string("text")!!,
        lang = null,
        format = this.getParsedFormat(request.queryParams("format") ?: "JSON"),
        response = response,
        prettyPrint = request.booleanParam("pretty"))
    }

    Spark.post("/:lang") { request, response ->

      val jsonBody: JsonObject = request.getJsonObject()

      this.parse!!(
        text = jsonBody.string("text")!!,
        lang = request.params("lang")?.let { getLanguageByIso(it) },
        format = this.getParsedFormat(request.queryParams("format") ?: "JSON"),
        response = response,
        prettyPrint = request.booleanParam("pretty"))
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

      this.tokenize!!(text = request.requiredQueryParam("text"), prettyPrint = request.booleanParam("pretty"))
    }

    Spark.get("/:lang") { request, _ ->

      this.tokenize!!(
        text = request.requiredQueryParam("text"),
        language = request.params("lang")?.let { getLanguageByIso(it) },
        prettyPrint = request.booleanParam("pretty"))
    }

    Spark.post("") { request, _ ->
      this.tokenize!!(text = request.body(), prettyPrint = request.booleanParam("pretty"))
    }

    Spark.post("/:lang") { request, _ ->
      this.tokenize!!(
        text = request.body(),
        language = request.params("lang")?.let { getLanguageByIso(it) },
        prettyPrint = request.booleanParam("pretty"))
    }
  }

  /**
   * Define the '/detect-language' route.
   */
  private fun detectLanguageRoute() {

    Spark.get("") { request, _ ->

      this.detectLanguage!!(
        text = request.requiredQueryParam("text"),
        distribution = request.booleanParam("distribution"),
        prettyPrint = request.booleanParam("pretty"))
    }

    Spark.post("") { request, _ ->
      this.detectLanguage!!(
        text = request.body(),
        distribution = request.booleanParam("distribution"),
        prettyPrint = request.booleanParam("pretty"))
    }
  }

  /**
   * Define the '/classify-tokens-language' route.
   */
  private fun classifyTokensLanguageRoute() {

    Spark.get("") { request, _ ->

      this.detectLanguage!!.perToken(
        text = request.requiredQueryParam("text"),
        distribution = request.booleanParam("distribution"),
        prettyPrint = request.booleanParam("pretty"))
    }

    Spark.post("") { request, _ ->
      this.detectLanguage!!.perToken(
        text = request.body(),
        distribution = request.booleanParam("distribution"),
        prettyPrint = request.booleanParam("pretty"))
    }
  }

  /**
   * Define the '/find-locations' route.
   */
  private fun findLocationsRoute() {

    Spark.post("") { request, _ ->

      val jsonBody: JsonObject = request.getJsonObject()

      this.execFindLocations(
        jsonBody = jsonBody,
        language = getLanguageByIso(jsonBody.string("lang")!!),
        prettyPrint = request.booleanParam("pretty"))
    }

    Spark.post("/:lang") { request, _ ->

      val jsonBody: JsonObject = request.getJsonObject()

      this.execFindLocations(
        jsonBody = jsonBody,
        language = getLanguageByIso(request.params("lang")!!),
        prettyPrint = request.booleanParam("pretty"))
    }
  }

  /**
   * Define the '/extract-frames' route.
   */
  private fun extractFramesRoute() {

    Spark.get("") { request, _ ->

      this.extractFrames!!(
        text = request.requiredQueryParam("text"),
        lang = request.queryParams("lang")?.let { getLanguageByIso(it) },
        domain = request.queryParams("domain"),
        distribution = request.booleanParam("distribution"),
        prettyPrint = request.booleanParam("pretty"))
    }

    Spark.get("/:domain") { request, _ ->

      this.extractFrames!!(
        text = request.requiredQueryParam("text"),
        lang = request.queryParams("lang")?.let { getLanguageByIso(it) },
        domain = request.params("domain"),
        distribution = request.booleanParam("distribution"),
        prettyPrint = request.booleanParam("pretty"))
    }

    Spark.post("") { request, _ ->

      val jsonBody: JsonObject = request.getJsonObject()

      this.extractFrames!!(
        text = jsonBody.string("text")!!,
        lang = request.queryParams("lang")?.let { getLanguageByIso(it) },
        distribution = request.booleanParam("distribution"),
        prettyPrint = request.booleanParam("pretty"))
    }

    Spark.post("/:domain") { request, _ ->

      val jsonBody: JsonObject = request.getJsonObject()

      this.extractFrames!!(
        text = jsonBody.string("text")!!,
        lang = request.queryParams("lang")?.let { getLanguageByIso(it) },
        domain = request.params("domain"),
        distribution = request.booleanParam("distribution"),
        prettyPrint = request.booleanParam("pretty"))
    }
  }

  /**
   * Define the '/categorize' route.
   */
  private fun categorizeRoute() {

    Spark.get("") { request, _ ->

      this.categorize!!(
        text = request.requiredQueryParam("text"),
        lang = request.queryParams("lang")?.let { getLanguageByIso(it) },
        domain = request.queryParams("domain"),
        distribution = request.booleanParam("distribution"),
        prettyPrint = request.booleanParam("pretty"))
    }

    Spark.get("/:domain") { request, _ ->

      this.categorize!!(
        text = request.requiredQueryParam("text"),
        lang = request.queryParams("lang")?.let { getLanguageByIso(it) },
        domain = request.params("domain"),
        distribution = request.booleanParam("distribution"),
        prettyPrint = request.booleanParam("pretty"))
    }

    Spark.get("/:lang/:domain") { request, _ ->

      this.categorize!!(
        text = request.requiredQueryParam("text"),
        lang = getLanguageByIso(request.params("lang")),
        domain = request.params("domain"),
        distribution = request.booleanParam("distribution"),
        prettyPrint = request.booleanParam("pretty"))
    }

    Spark.post("") { request, _ ->
      this.categorize!!(
        text = request.body(),
        lang = request.queryParams("lang")?.let { getLanguageByIso(it) },
        distribution = request.booleanParam("distribution"),
        prettyPrint = request.booleanParam("pretty"))
    }

    Spark.post("/:domain") { request, _ ->
      this.categorize!!(
        text = request.body(),
        lang = request.queryParams("lang")?.let { getLanguageByIso(it) },
        domain = request.params("domain"),
        distribution = request.booleanParam("distribution"),
        prettyPrint = request.booleanParam("pretty"))
    }

    Spark.post("/:lang/:domain") { request, _ ->
      this.categorize!!(
        text = request.body(),
        lang = getLanguageByIso(request.params("lang")),
        domain = request.params("domain"),
        distribution = request.booleanParam("distribution"),
        prettyPrint = request.booleanParam("pretty"))
    }
  }

  /**
   * Define the '/compare' route.
   */
  private fun compareRoute() {

    Spark.post("") { request, _ ->

      val jsonBody: JsonObject = request.getJsonObject()

      this.compare!!(
        baseText = jsonBody.string("text")!!,
        comparingTexts = jsonBody.array<JsonObject>("comparing")!!.associate { it.int("id")!! to it.string("text")!! },
        lang = request.queryParams("lang")?.let { getLanguageByIso(it) },
        prettyPrint = request.booleanParam("pretty"))
    }

    Spark.post("/:lang") { request, _ ->

      val jsonBody: JsonObject = request.getJsonObject()

      this.compare!!(
        baseText = jsonBody.string("text")!!,
        comparingTexts = jsonBody.array<JsonObject>("comparing")!!.associate { it.int("id")!! to it.string("text")!! },
        lang = getLanguageByIso(request.params("lang")),
        prettyPrint = request.booleanParam("pretty"))
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
   * @throws InvalidJSONBody if the body of this request is not a JSON object
   *
   * @return the body of this request parsed as JSON object
   */
  private fun Request.getJsonObject(): JsonObject {

    this.assertJsonApplication()

    return Parser().parse(java.lang.StringBuilder(this.body())) as? JsonObject ?: throw InvalidJSONBody()
  }

  /**
   * Assert that the Content-Type header of this request is 'application/json'.
   *
   * @throws InvalidContentType if the Content-Type header of this request is not 'application/json'
   */
  private fun Request.assertJsonApplication() {

    if (!this.contentType().startsWith("application/json"))
      throw InvalidContentType(expected = "application/json", given = this.contentType())
  }

  /**
   * Check if a required parameter is present in the query of this [Request] and return it.
   *
   * @param requiredParam the required parameter to check
   *
   * @throws MissingQueryParameters if the given parameter is missing
   *
   * @return the required parameter
   */
  private fun Request.requiredQueryParam(requiredParam: String): String {

    this.checkRequiredParams(listOf(requiredParam))

    return this.queryParams(requiredParam)
  }

  /**
   * Check if all [requiredParams] are present in this [Request].
   *
   * @param requiredParams the list of required parameters to check
   *
   * @throws MissingQueryParameters if at least one parameter is missing
   */
  private fun Request.checkRequiredParams(requiredParams: List<String>) {

    val missingParams: List<String> = this.getMissingParams(requiredParams)

    if (missingParams.isNotEmpty()) {
      throw MissingQueryParameters(missingParams)
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

  /**
   * Get a boolean query parameter.
   * A boolean query parameter is true if it is present with an empty value or containing 'true'.
   *
   * @param paramName the name of the query parameter
   *
   * @return true if the parameters is 'true' or empty, otherwise false
   */
  private fun Request.booleanParam(paramName: String): Boolean =
    this.queryParams(paramName)?.let { it.isEmpty() || it.toBoolean() } ?: false
}
