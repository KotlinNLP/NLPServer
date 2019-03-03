/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver

import com.kotlinnlp.frameextractor.FrameExtractor
import com.kotlinnlp.geolocation.dictionary.LocationsDictionary
import com.kotlinnlp.hanclassifier.HANClassifier
import com.kotlinnlp.languagedetector.LanguageDetector
import com.kotlinnlp.morphologicalanalyzer.MorphologicalAnalyzer
import com.kotlinnlp.neuralparser.helpers.preprocessors.MorphoPreprocessor
import com.kotlinnlp.neuralparser.parsers.lhrparser.LHRParser
import com.kotlinnlp.neuraltokenizer.NeuralTokenizer
import com.kotlinnlp.nlpserver.commands.*
import com.kotlinnlp.simplednn.core.embeddings.EmbeddingsMap
import com.xenomachina.argparser.mainBody

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
  val morphoPreprocessors: Map<String, MorphoPreprocessor> = buildMorphoPreprocessors(parsedArgs)
  val locationsDictionary: LocationsDictionary? = buildLocationsDictionary(parsedArgs)
  val frameExtractors: Map<String, FrameExtractor>? = buildFrameExtractors(parsedArgs)
  val hanClassifiers: Map<String, HANClassifier>? = buildHANClassifiers(parsedArgs)
  val embeddingsMapByLang: Map<String, EmbeddingsMap<String>>? = buildEmbeddingsMapsByLanguage(parsedArgs)

  NLPServer(
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
    extractFrames = if (tokenizers != null && parsers != null && embeddingsMapByLang != null && frameExtractors != null)
      ExtractFrames(
        languageDetector = languageDetector,
        tokenizers = tokenizers,
        lssModels = parsers.mapValues { it.value.model.lssModel },
        morphoPreprocessors = morphoPreprocessors,
        wordEmbeddings = embeddingsMapByLang,
        frameExtractors = frameExtractors)
    else
      null,
    categorize = if (tokenizers != null && hanClassifiers != null)
      Categorize(languageDetector = languageDetector, tokenizers = tokenizers, hanClassifiers = hanClassifiers)
    else
      null
  ).start()
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
 * @return a map of frame extractors associated by domain name or null if the required arguments are not present
 */
private fun buildFrameExtractors(parsedArgs: CommandLineArguments): Map<String, FrameExtractor>? =
  parsedArgs.frameExtractorModelsDir?.let { NLPBuilder.buildFrameExtractorsMap(it) }

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
 * @return a map of embeddings maps associated by language ISO code or null if the required arguments are not present
 */
private fun buildEmbeddingsMapsByLanguage(parsedArgs: CommandLineArguments): Map<String, EmbeddingsMap<String>>? =
  parsedArgs.embeddingsDir?.let { NLPBuilder.buildEmbeddingsMapsByLanguage(it) }

/**
 * @param parsedArgs the parsed command line arguments
 *
 * @return a map of morpho preprocessors associated by language ISO code (empty if the required arguments are not
 *         present)
 */
private fun buildMorphoPreprocessors(parsedArgs: CommandLineArguments): Map<String, MorphoPreprocessor> =
  parsedArgs.morphoDictionaryDir?.let {
    NLPBuilder.buildMorphoDictionaries(it).mapValues { (_, dictionary) ->
      MorphoPreprocessor(MorphologicalAnalyzer(language = dictionary.language, dictionary = dictionary))
    }
  } ?: mapOf()

/**
 * @param parsedArgs the parsed command line arguments
 *
 * @return a locations dictionary or null if the required arguments are not present
 */
private fun buildLocationsDictionary(parsedArgs: CommandLineArguments): LocationsDictionary? =
  parsedArgs.locationsDictionary?.let { NLPBuilder.buildLocationsDictionary(it) }
