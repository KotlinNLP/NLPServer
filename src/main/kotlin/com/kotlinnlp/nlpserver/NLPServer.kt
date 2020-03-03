/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver

import com.kotlinnlp.nlpserver.routes.*
import org.apache.log4j.Logger
import spark.Filter
import spark.Spark

/**
 * The NLP Server class.
 *
 * @param port the port listened by the server
 * @param enableCORS whether to enable CORS requests
 * @param routes the list of routes that the server makes available
 */
class NLPServer(port: Int, enableCORS: Boolean, private val routes: List<Route>) {

  /**
   * The logger of the server.
   */
  private val logger = Logger.getLogger("NLP Server").setAppender()

  /**
   * Initialize Spark: set port and exceptions handling.
   */
  init {

    Spark.port(port)

    Spark.before(Filter { _, response ->
      response.header("Content-Type", "application/json")
    })

    this.initExceptions()

    if (enableCORS) this.enbaleCORS()
  }

  /**
   * Start the server.
   */
  fun start() {

    this.routes.forEach {

      Spark.path("/${it.name}") { it.initialize() }

      this.logger.info("Route '/${it.name}' enabled")
    }

    this.logger.info("NLP Server running on 'localhost:%d'".format(Spark.port()))
  }

  /**
   * Enable CORS requests.
   */
  private fun enbaleCORS() {

    Spark.options("/*") { request, response ->

      request.headers("Access-Control-Request-Headers")?.let {
        response.header("Access-Control-Allow-Headers", it)
      }

      request.headers("Access-Control-Request-Method")?.let {
        response.header("Access-Control-Allow-Methods", it)
      }

      "OK"
    }

    Spark.before("/*") { _, response ->
      response.header("Access-Control-Allow-Origin", "*")
    }

    this.logger.info("CORS enabled")
  }

  /**
   * Setup the exceptions handling.
   */
  private fun initExceptions() {

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
      this.logger.warn(exception.toString() + ". Stacktrace: \n  " + exception.stackTrace.joinToString("\n  "))
    }
  }
}
