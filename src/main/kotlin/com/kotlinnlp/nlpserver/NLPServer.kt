/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver

import com.kotlinnlp.nlpserver.commands.DetectLanguage
import com.kotlinnlp.nlpserver.commands.Parse
import com.kotlinnlp.nlpserver.commands.Tokenize
import com.kotlinnlp.nlpserver.commands.exceptions.MissingParameters
import com.kotlinnlp.nlpserver.commands.exceptions.NotSupportedLanguage
import spark.Request
import spark.Spark
import java.util.logging.Logger

/**
 * The NLP Server class.
 *
 * @param port the port listened from the server (default = 3000)
 * @param tokenizerModelFilename the filename of the model of the tokenizer
 * @param languageDetectorModelFilename the filename of the model of the language detector
 * @param frequencyDictionaryFilename the filename of the frequency dictionary
 */
class NLPServer(
  port: Int = 3000,
  tokenizerModelFilename: String,
  languageDetectorModelFilename: String,
  frequencyDictionaryFilename: String?
) {

  /**
   * The logger of the server.
   */
  private val logger = Logger.getLogger("NLP Server")

  /**
   * The handler of the Parse command.
   */
  private val parse = Parse()

  /**
   * The handler of the Tokenize command.
   */
  private val tokenize = Tokenize(tokenizerModelFilename)

  /**
   * The handler of the DetectLanguage command.
   */
  private val detectLanguage = DetectLanguage(
    modelFilename = languageDetectorModelFilename,
    frequencyDictionaryFilename = frequencyDictionaryFilename)

  /**
   * Initialize Spark.
   * Set port and exceptions handling.
   */
  init {

    Spark.port(port)

    Spark.exception(MissingParameters::class.java) { exception, _, response ->
      response.status(400)
      response.body("Missing required parameters: %s\n".format((exception as MissingParameters).message))
    }

    Spark.exception(NotSupportedLanguage::class.java) { exception, _, response ->
      response.status(400)
      response.body("Not supported language: %s\n".format((exception as NotSupportedLanguage).message))
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

    Spark.path("/parse") {
      this.parseRoute()
    }

    Spark.path("/tokenize") {
      this.tokenizeRoute()
    }

    Spark.path("/detect-language") {
      this.detectLanguageRoute()
    }

    Spark.path("/classify-tokens-language") {
      this.classifyTokensLanguageRoute()
    }

    this.logger.info("NLP Server running on 'localhost:%d'\n".format(Spark.port()))
  }

  /**
   * Define the '/parse' route.
   */
  private fun parseRoute() {

    Spark.get("") { request, _ ->

      request.checkRequiredParams(requiredParams = listOf("text"))

      this.parse(text = request.queryParams("text"), lang = request.queryParams("lang"))
    }

    Spark.get("/:lang") { request, _ ->

      request.checkRequiredParams(requiredParams = listOf("text"))

      this.parse(text = request.queryParams("text"), lang = request.params("lang"))
    }

    Spark.post("") { request, _ ->
      this.parse(text = request.body())
    }

    Spark.post("/:lang") { request, _ ->
      this.parse(text = request.body(), lang = request.params("lang"))
    }
  }

  /**
   * Define the '/tokenize' route.
   */
  private fun tokenizeRoute() {

    Spark.get("") { request, _ ->

      request.checkRequiredParams(requiredParams = listOf("text"))

      this.tokenize(text = request.queryParams("text"))
    }

    Spark.post("") { request, _ ->
      this.tokenize(text = request.body())
    }
  }

  /**
   * Define the '/detect-language' route.
   */
  private fun detectLanguageRoute() {

    Spark.get("") { request, _ ->

      request.checkRequiredParams(requiredParams = listOf("text"))

      this.detectLanguage(text = request.queryParams("text"))
    }

    Spark.post("") { request, _ ->
      this.detectLanguage(text = request.body())
    }
  }

  /**
   * Define the '/classify-tokens-language' route.
   */
  private fun classifyTokensLanguageRoute() {

    Spark.get("") { request, _ ->

      request.checkRequiredParams(requiredParams = listOf("text"))

      this.detectLanguage.perToken(text = request.queryParams("text"))
    }

    Spark.post("") { request, _ ->
      this.detectLanguage.perToken(text = request.body())
    }
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
