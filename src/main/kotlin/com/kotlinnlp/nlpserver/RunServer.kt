/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver

import com.kotlinnlp.correlator.helpers.TextComparator
import com.kotlinnlp.frameextractor.TextFramesExtractor
import com.kotlinnlp.geolocation.dictionary.LocationsDictionary
import com.kotlinnlp.hanclassifier.HANClassifier
import com.kotlinnlp.languagedetector.LanguageDetector
import com.kotlinnlp.morphologicalanalyzer.dictionary.MorphologyDictionary
import com.kotlinnlp.neuralparser.helpers.preprocessors.MorphoPreprocessor
import com.kotlinnlp.neuralparser.parsers.lhrparser.LHRParser
import com.kotlinnlp.neuraltokenizer.NeuralTokenizer
import com.kotlinnlp.nlpserver.commands.*
import com.kotlinnlp.simplednn.core.embeddings.EmbeddingsMap
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

  val languageDetector: LanguageDetector? = buildLanguageDetector(parsedArgs)
  val tokenizers: Map<String, NeuralTokenizer>? = buildTokenizers(parsedArgs)
  val parsers: Map<String, LHRParser>? = buildParsers(parsedArgs)
  val morphoDicts: Map<String, MorphologyDictionary> = buildMorphoDictionaries(parsedArgs)
  val morphoPreprocessors: Map<String, MorphoPreprocessor> = morphoDicts.mapValues { MorphoPreprocessor(it.value) }
  val locationsDictionary: LocationsDictionary? = buildLocationsDictionary(parsedArgs)
  val frameExtractors: Map<String, TextFramesExtractor>? = buildFrameExtractors(parsedArgs)
  val hanClassifiers: Map<String, HANClassifier>? = buildHANClassifiers(parsedArgs)
  val comparators: Map<String, TextComparator>? = buildComparators(
    parsedArgs = parsedArgs,
    tokenizers = tokenizers,
    morphoDicts = morphoDicts,
    parsers = parsers,
    wordEmbeddings = buildWordEmbeddings(parsedArgs))

  val server = NLPServer(
    port = parsedArgs.port,
    detectLanguage = languageDetector?.let { DetectLanguage(it) },
    tokenize = tokenizers?.let { Tokenize(tokenizers = it, languageDetector = languageDetector) },
    parse = if (parsers != null && tokenizers != null)
      Parse(
        languageDetector = languageDetector,
        tokenizers = tokenizers,
        parsers = parsers,
        morphoPreprocessors = morphoPreprocessors)
    else
      null,
    findLocations = if (locationsDictionary != null && tokenizers != null)
      FindLocations(dictionary = locationsDictionary, tokenizers = tokenizers)
    else
      null,
    extractFrames = if (tokenizers != null && frameExtractors != null)
      ExtractFrames(languageDetector = languageDetector, tokenizers = tokenizers, frameExtractors = frameExtractors)
    else
      null,
    categorize = if (tokenizers != null && hanClassifiers != null)
      Categorize(languageDetector = languageDetector, tokenizers = tokenizers, hanClassifiers = hanClassifiers)
    else
      null,
    compare = if (comparators != null)
      Compare(languageDetector = languageDetector, comparators = comparators)
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



/**
 * @param parsedArgs the parsed command line arguments
 *
 * @return a language detector or null if the required arguments are not present
 */
private fun buildLanguageDetector(parsedArgs: CommandLineArguments): LanguageDetector? =
  if (parsedArgs.langDetectorModel != null && parsedArgs.cjkTokenizerModel != null)
    NLPBuilder.buildLanguageDetector(
      languageDetectorModelFilename = parsedArgs.langDetectorModel!!,
      cjkModelFilename = parsedArgs.cjkTokenizerModel!!,
      frequencyDictionaryFilename = parsedArgs.freqDictionary)
  else
    null

/**
 * @param parsedArgs the parsed command line arguments
 *
 * @return a map of neural tokenizers associated by language ISO code or null if the required arguments are not present
 */
private fun buildTokenizers(parsedArgs: CommandLineArguments): Map<String, NeuralTokenizer>? =
  parsedArgs.tokenizerModelsDir?.let { NLPBuilder.buildTokenizers(it) }

/**
 * @param parsedArgs the parsed command line arguments
 *
 * @return a map of LHR parsers associated by language ISO code or null if the required arguments are not present
 */
private fun buildParsers(parsedArgs: CommandLineArguments): Map<String, LHRParser>? =
  parsedArgs.lhrParserModelsDir?.let { NLPBuilder.buildLHRParsers(it) }

/**
 * @param parsedArgs the parsed command line arguments
 *
 * @return a map of text frames extractors associated by domain name or null if the required arguments are not present
 */
private fun buildFrameExtractors(parsedArgs: CommandLineArguments): Map<String, TextFramesExtractor>? =
  parsedArgs.frameExtractorModelsDir?.let {
    NLPBuilder.buildFrameExtractorsMap(
      frameExtractorModelsDir = it,
      embeddingsDir = parsedArgs.framesExtractorEmbeddingsDir)
  }

/**
 * @param parsedArgs the parsed command line arguments
 *
 * @return a map of HAN classifiers associated by domain name or null if the required arguments are not present
 */
private fun buildHANClassifiers(parsedArgs: CommandLineArguments): Map<String, HANClassifier>? =
  parsedArgs.hanClassifierModelsDir?.let {
    NLPBuilder.buildHANClassifiersMap(
      hanClassifierModelsDir = it,
      embeddingsDir = parsedArgs.hanClassifierEmbeddingsDir)
  }

/**
 * @param parsedArgs the parsed command line arguments
 *
 * @return a map of generic word embeddings associated by language ISO 639-1 code or null if the required arguments are
 *         not present
 */
private fun buildWordEmbeddings(parsedArgs: CommandLineArguments): Map<String, EmbeddingsMap<String>>? =
  parsedArgs.wordEmbeddingsDir?.let { NLPBuilder.buildWordEmbeddings(it) }

/**
 * @param parsedArgs the parsed command line arguments
 * @param tokenizers
 * @param morphoDicts
 * @param parsers
 * @param wordEmbeddings
 *
 * @return a map of text comparators associated by language ISO-639-1 code or null if the required arguments are not
 *         present
 */
private fun buildComparators(parsedArgs: CommandLineArguments,
                             tokenizers: Map<String, NeuralTokenizer>?,
                             morphoDicts: Map<String, MorphologyDictionary>,
                             parsers: Map<String, LHRParser>?,
                             wordEmbeddings: Map<String, EmbeddingsMap<String>>?): Map<String, TextComparator>? {

  if (parsers == null || wordEmbeddings == null || tokenizers == null) return mapOf()

  val languages: Set<String> = parsers.keys.intersect(morphoDicts.keys).intersect(wordEmbeddings.keys)
  val blacklists: Map<String, Set<String>> = parsedArgs.comparisonBlacklistsDir?.let {
    NLPBuilder.buildComparisonBlacklists(it)
  } ?: mapOf()

  return languages.associate {
    it to TextComparator(
      embeddings = wordEmbeddings.getValue(it),
      tokenizer = tokenizers.getValue(it),
      morphoDictionary = morphoDicts.getValue(it),
      parser = parsers.getValue(it),
      lemmasBlacklist = blacklists[it] ?: setOf()
    )
  }
}

/**
 * @param parsedArgs the parsed command line arguments
 *
 * @return a map of morpho dictionaries associated by language ISO code (empty if the required arguments are not
 *         present)
 */
private fun buildMorphoDictionaries(parsedArgs: CommandLineArguments): Map<String, MorphologyDictionary> =
  parsedArgs.morphoDictionaryDir?.let { NLPBuilder.buildMorphoDictionaries(it) } ?: mapOf()

/**
 * @param parsedArgs the parsed command line arguments
 *
 * @return a locations dictionary or null if the required arguments are not present
 */
private fun buildLocationsDictionary(parsedArgs: CommandLineArguments): LocationsDictionary? =
  parsedArgs.locationsDictionary?.let { NLPBuilder.buildLocationsDictionary(it) }
