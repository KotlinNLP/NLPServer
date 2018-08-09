/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver

import com.kotlinnlp.geolocation.dictionary.LocationsDictionary
import com.kotlinnlp.languagedetector.LanguageDetector
import com.kotlinnlp.morphologicalanalyzer.MorphologicalAnalyzer
import com.kotlinnlp.neuralparser.NeuralParser
import com.kotlinnlp.neuralparser.helpers.preprocessors.MorphoPreprocessor
import com.kotlinnlp.neuraltokenizer.NeuralTokenizer
import com.kotlinnlp.nlpserver.commands.DetectLanguage
import com.kotlinnlp.nlpserver.commands.FindLocations
import com.kotlinnlp.nlpserver.commands.Parse
import com.kotlinnlp.nlpserver.commands.Tokenize
import com.xenomachina.argparser.mainBody

/**
 * Run the NLP Server.
 */
fun main(args: Array<String>) = mainBody {

  val parsedArgs = CommandLineArguments(args)

  val languageDetector: LanguageDetector? = buildLanguageDetector(parsedArgs)
  val tokenizers: Map<String, NeuralTokenizer>? = buildTokenizers(parsedArgs)
  val parsers: Map<String, NeuralParser<*>>? = buildParsers(parsedArgs)
  val morphoPreprocessors: Map<String, MorphoPreprocessor> = buildMorphoPreprocessors(parsedArgs)
  val locationsDictionary: LocationsDictionary? = buildLocationsDictionary(parsedArgs)

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
 * @return a map of neural parsers associated by language ISO code or null if the required arguments are not present
 */
private fun buildParsers(parsedArgs: CommandLineArguments): Map<String, NeuralParser<*>>? =
  parsedArgs.neuralParserModelsDir?.let { NLPBuilder.buildNeuralParsers(it) }

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
