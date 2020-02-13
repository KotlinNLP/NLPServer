/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver

import com.kotlinnlp.nlpserver.commands.*
import com.xenomachina.argparser.mainBody
import org.apache.log4j.*
import org.apache.log4j.spi.RootLogger

/**
 * Run the NLP Server.
 *
 * Launch with the '-h' option for help about the command line arguments.
 */
fun main(args: Array<String>) = mainBody {

  val parsedArgs = CommandLineArguments(args)
  val builder = NLPBuilder(parsedArgs)

  val server = NLPServer(
    port = parsedArgs.port,
    detectLanguage = builder.languageDetector?.let { DetectLanguage(it) },
    tokenize = builder.tokenizers?.let { Tokenize(tokenizers = it, languageDetector = builder.languageDetector) },
    parse = if (builder.parsers != null && builder.tokenizers != null)
      Parse(
        languageDetector = builder.languageDetector,
        tokenizers = builder.tokenizers,
        parsers = builder.parsers,
        morphoPreprocessors = builder.morphoPreprocessors)
    else
      null,
    findLocations = if (builder.locationsDictionary != null && builder.tokenizers != null)
      FindLocations(dictionary = builder.locationsDictionary, tokenizers = builder.tokenizers)
    else
      null,
    extractFrames = if (builder.tokenizers != null && builder.frameExtractors != null)
      ExtractFrames(
        languageDetector = builder.languageDetector,
        tokenizers = builder.tokenizers,
        frameExtractors = builder.frameExtractors)
    else
      null,
    categorize = if (builder.tokenizers != null && builder.hanClassifiers != null)
      Categorize(
        languageDetector = builder.languageDetector,
        tokenizers = builder.tokenizers,
        hanClassifiers = builder.hanClassifiers)
    else
      null,
    compare = if (builder.comparators != null)
      Compare(languageDetector = builder.languageDetector, comparators = builder.comparators)
    else
      null
  )

  initLogging(debugMode = parsedArgs.debug)

  server.start()
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
