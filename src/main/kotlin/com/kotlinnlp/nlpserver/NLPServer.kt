/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver

import com.kotlinnlp.languagedetector.LanguageDetector
import com.kotlinnlp.languagedetector.LanguageDetectorModel
import com.kotlinnlp.languagedetector.utils.FrequencyDictionary
import com.kotlinnlp.languagedetector.utils.TextTokenizer
import com.kotlinnlp.neuraltokenizer.NeuralTokenizer
import com.kotlinnlp.neuraltokenizer.NeuralTokenizerModel
import com.kotlinnlp.nlpserver.commands.DetectLanguage
import com.kotlinnlp.nlpserver.commands.Parse
import com.kotlinnlp.nlpserver.commands.Tokenize
import com.kotlinnlp.nlpserver.commands.exceptions.MissingParameters
import com.kotlinnlp.nlpserver.commands.exceptions.NotSupportedLanguage
import spark.Request
import spark.Spark
import java.io.File
import java.io.FileInputStream
import java.util.logging.Logger

/**
 * The NLP Server class.
 *
 * @param port the port listened from the server (default = 3000)
 * @param tokenizerModelsDir the directory containing the tokenizer models (default = null)
 * @param languageDetectorModelFilename the filename of the language detector model (default = null)
 * @param cjkModelFilename the filename of the CJK tokenizer used by the language detector (default = null)
 * @param frequencyDictionaryFilename the filename of the frequency dictionary (default = null)
 */
class NLPServer(
  port: Int = 3000,
  tokenizerModelsDir: String? = null,
  languageDetectorModelFilename: String? = null,
  cjkModelFilename: String? = null,
  frequencyDictionaryFilename: String? = null
) {

  /**
   * The logger of the server.
   */
  private val logger = Logger.getLogger("NLP Server")

  /**
   * A [LanguageDetector].
   */
  private val languageDetector: LanguageDetector? = this.buildLanguageDetector(
    languageDetectorModelFilename = languageDetectorModelFilename,
    cjkModelFilename = cjkModelFilename,
    frequencyDictionaryFilename = frequencyDictionaryFilename)

  /**
   * A [Map] of languages iso-a2 codes to the related [NeuralTokenizer]s.
   */
  private val tokenizers: Map<String, NeuralTokenizer>? = this.buildTokenizers(tokenizerModelsDir)

  /**
   * The handler of the Parse command.
   */
  private val parse = Parse()

  /**
   * The handler of the DetectLanguage command.
   */
  private val detectLanguage: DetectLanguage? =
    if (this.languageDetector != null)
      DetectLanguage(this.languageDetector)
    else
      null

  /**
   * The handler of the Tokenize command.
   */
  private val tokenize: Tokenize? =
    if (this.tokenizers != null && this.languageDetector != null)
      Tokenize(
        tokenizers = this.tokenizers,
        languageDetector = this.languageDetector)
    else
      null

  /**
   * Initialize Spark: set port and exceptions handling.
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

    if (this.detectLanguage != null) {
      Spark.path("/detect-language") {
        this.detectLanguageRoute()
      }
      Spark.path("/classify-tokens-language") {
        this.classifyTokensLanguageRoute()
      }
    }

    if (this.tokenize != null) {
      Spark.path("/tokenize") {
        this.tokenizeRoute()
      }
    }

    this.logger.info("NLP Server running on 'localhost:%d'\n".format(Spark.port()))
  }

  /**
   * Build a [LanguageDetector].
   * The [languageDetectorModelFilename] and the [cjkModelFilename] arguments are required to be not null to build it,
   * otherwise null is returned.
   *
   * @param languageDetectorModelFilename the filename of the language detector model
   * @param cjkModelFilename the filename of the CJK tokenizer used by the language detector
   * @param frequencyDictionaryFilename the filename of the frequency dictionary
   *
   * @return a [LanguageDetector] with the given models
   */
  private fun buildLanguageDetector(languageDetectorModelFilename: String?,
                                    cjkModelFilename: String?,
                                    frequencyDictionaryFilename: String?): LanguageDetector? {

    return if (languageDetectorModelFilename == null || cjkModelFilename == null) {

      this.logger.info("No language detector loaded\n")

      null

    } else {

      this.logger.info("Loading language detector model from '$languageDetectorModelFilename'\n")
      val model = LanguageDetectorModel.load(FileInputStream(File(languageDetectorModelFilename)))

      this.logger.info("Loading CJK tokenizer model from '$cjkModelFilename'\n")
      val tokenizer = TextTokenizer(cjkModel = NeuralTokenizerModel.load(FileInputStream(File(cjkModelFilename))))

      val freqDictionary = if (frequencyDictionaryFilename != null) {
        this.logger.info("Loading frequency dictionary from '$frequencyDictionaryFilename'\n")
        FrequencyDictionary.load(FileInputStream(File(frequencyDictionaryFilename)))
      } else {
        this.logger.info("No frequency dictionary used to detect the language\n")
        null
      }

      return LanguageDetector(model = model, tokenizer = tokenizer, frequencyDictionary = freqDictionary)
    }
  }

  /**
   * Build the [Map] of languages iso-a2 codes to the related [NeuralTokenizer]s.
   * The [tokenizerModelsDir] argument and the [detectLanguage] command are required to be not null to build it,
   * otherwise null is returned.
   *
   * @param tokenizerModelsDir the directory containing the tokenizers models
   *
   * @return a [Map] of languages iso-a2 codes to the related [NeuralTokenizer]s
   */
  private fun buildTokenizers(tokenizerModelsDir: String?): Map<String, NeuralTokenizer>? {

    return if (this.languageDetector == null || tokenizerModelsDir == null) {

      this.logger.info("No tokenizer loaded")

      null

    } else {

      this.logger.info("Loading tokenizer models from '$tokenizerModelsDir'")
      val modelsDirectory = File(tokenizerModelsDir)

      require(modelsDirectory.isDirectory) { "$tokenizerModelsDir is not a directory" }

      val tokenizersMap = mutableMapOf<String, NeuralTokenizer>()

      modelsDirectory.listFiles().forEach { modelFile ->

        this.logger.info("Loading '${modelFile.name}'...")
        val model = NeuralTokenizerModel.load(FileInputStream(modelFile))

        tokenizersMap[model.language] = NeuralTokenizer(model)
      }

      tokenizersMap.toMap()
    }
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

      this.tokenize!!(text = request.queryParams("text"))
    }

    Spark.get("/:lang") { request, _ ->

      request.checkRequiredParams(requiredParams = listOf("text"))

      this.tokenize!!(text = request.queryParams("text"), language = request.params("lang"))
    }

    Spark.post("") { request, _ ->
      this.tokenize!!(text = request.body())
    }

    Spark.post("/:lang") { request, _ ->
      this.tokenize!!(text = request.body(), language = request.params("lang"))
    }
  }

  /**
   * Define the '/detect-language' route.
   */
  private fun detectLanguageRoute() {

    Spark.get("") { request, _ ->

      request.checkRequiredParams(requiredParams = listOf("text"))

      this.detectLanguage!!(text = request.queryParams("text"))
    }

    Spark.post("") { request, _ ->
      this.detectLanguage!!(text = request.body())
    }
  }

  /**
   * Define the '/classify-tokens-language' route.
   */
  private fun classifyTokensLanguageRoute() {

    Spark.get("") { request, _ ->

      request.checkRequiredParams(requiredParams = listOf("text"))

      this.detectLanguage!!.perToken(text = request.queryParams("text"))
    }

    Spark.post("") { request, _ ->
      this.detectLanguage!!.perToken(text = request.body())
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
