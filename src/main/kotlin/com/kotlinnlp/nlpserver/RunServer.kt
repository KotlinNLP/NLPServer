/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver

import com.kotlinnlp.nlpserver.routes.*
import com.xenomachina.argparser.mainBody
import org.apache.log4j.*
import org.apache.log4j.spi.RootLogger

/**
 * Run the NLP Server.
 *
 * Launch with the '-h' option for help about the command line arguments.
 */
fun main(args: Array<String>): Unit = mainBody {

  val parsedArgs = CommandLineArguments(args)

  NLPServer(port = parsedArgs.port, enableCORS = parsedArgs.enableCORS, routes = buildRoutes(parsedArgs)).apply {

    initLogging(debugMode = parsedArgs.debug)

    start()
  }
}

/**
 * @param parsedArgs the parsed command line arguments
 *
 * @return the list of routes to make available in the server, depending on the given arguments
 */
private fun buildRoutes(parsedArgs: CommandLineArguments): List<Route> {

  val builder = NLPBuilder(parsedArgs)
  val routes: MutableList<Route> = mutableListOf()

  if (builder.languageDetector != null)
    routes.add(DetectLanguage(builder.languageDetector))

  if (builder.tokenizers != null)
    routes.add(Tokenize(tokenizers = builder.tokenizers, languageDetector = builder.languageDetector))

  if (builder.parsers != null && builder.tokenizers != null)
    routes.add(
      Parse(
        languageDetector = builder.languageDetector,
        tokenizers = builder.tokenizers,
        parsers = builder.parsers,
        morphoPreprocessors = builder.morphoPreprocessors))

  if (builder.locationsDictionary != null && builder.tokenizers != null)
    routes.add(FindLocations(dictionary = builder.locationsDictionary, tokenizers = builder.tokenizers))

  if (builder.tokenizers != null && builder.frameExtractors != null)
    routes.add(
      ExtractFrames(
        languageDetector = builder.languageDetector,
        tokenizers = builder.tokenizers,
        frameExtractors = builder.frameExtractors))

  if (builder.tokenizers != null && builder.hanClassifiers != null)
    routes.add(
      Categorize(
        languageDetector = builder.languageDetector,
        tokenizers = builder.tokenizers,
        hanClassifiers = builder.hanClassifiers))

  if (builder.comparators != null)
    routes.add(Compare(languageDetector = builder.languageDetector, comparators = builder.comparators))

  return routes
}

/**
 * Initialize logging.
 * This function should be called after the creation of all the loggers.
 *
 * @param debugMode whether to log in debug mode
 */
private fun initLogging(debugMode: Boolean) {

  val rootLogger = RootLogger.getRootLogger()
  val maxLoggerNameLen: Int = LogManager.getCurrentLoggers().asSequence().map { (it as Logger).name.length }.max()!!

  rootLogger.level = if (debugMode) Level.DEBUG else Level.INFO
  rootLogger.addAppender(ConsoleAppender(PatternLayout("(Thread %t) [%d] %-5p %-${maxLoggerNameLen}c - %m%n")))
}
