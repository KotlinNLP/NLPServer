/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default

/**
 * The interpreter of command line arguments.
 *
 * @param args the array of command line arguments
 */
class CommandLineArguments(args: Array<String>) {

  /**
   * The parser of the string arguments.
   */
  private val parser = ArgParser(args)

  /**
   * The port listened by the server (default = 3000).
   */
  val port: Int by parser.storing(
    "-p",
    "--port",
    help="the port listened from the server"
  ) { toInt() }.default(3000)

  /**
   * The directory containing the serialized models of the NeuralTokenizer, one per language.
   */
  val tokenizerModelsDir: String? by parser.storing(
    "-t",
    "--tokenizer-models-directory",
    help="the directory containing the serialized models of the neural tokenizers (one per language)"
  ).default(null)

  /**
   * The filename of the LanguageDetector serialized model.
   */
  val langDetectorModel: String? by parser.storing(
    "-l",
    "--language-detector-model",
    help="the filename of the language detector serialized model"
  ).default(null)

  /**
   * The filename of the CJK NeuralTokenizer serialized model.
   */
  val cjkTokenizerModel: String? by parser.storing(
    "-c",
    "--cjk-tokenizer-model",
    help="the filename of the CJK neural tokenizer model used by the language detector"
  ).default(null)

  /**
   * The filename of the FrequencyDictionary.
   */
  val freqDictionary: String? by parser.storing(
    "-f",
    "--frequency-dictionary",
    help="the filename of the frequency dictionary used by the language detector"
  ).default(null)

  /**
   * The directory containing the morphology dictionaries used by the parser (one per language).
   */
  val morphoDictionaryDir: String? by parser.storing(
    "-m",
    "--morphology-dictionary",
    help="the directory containing the morphology dictionaries used by the parser (one per language)"
  ).default(null)

  /**
   * The file path of the pre-trained word embeddings, one per language (the file name must end with the ISO 639-1
   * language code).
   */
  val embeddingsDir: String? by parser.storing(
    "-e",
    "--pre-trained-word-emb",
    help="the directory containing the pre-trained word embeddings files, one per language (the file name must end " +
      "with the ISO 639-1 language code)"
  ).default(null)

  /**
   * The file path of the domain-specific word embeddings, one per domain (the file name must be in the format
   * 'embeddings_DOMAIN_NAME.xxx' - the extension is not considered).
   */
  val domainEmbeddingsDir: String? by parser.storing(
    "-w",
    "--domain-word-emb",
    help="the file path of the domain-specific word embeddings, one per domain (the file name must be in the format " +
      "'embeddings_DOMAIN_NAME.xxx' - the extension is not considered)"
  ).default(null)

  /**
   * The directory containing the serialized models of the LHRParser, one per language.
   */
  val lhrParserModelsDir: String? by parser.storing(
    "-n",
    "--neural-parser",
    help="the directory containing the serialized models of the neural parsers (one per language)"
  ).default(null)

  /**
   * The directory containing the serialized models of the frame extractors, one per domain.
   */
  val frameExtractorModelsDir: String? by parser.storing(
    "-x",
    "--frame-extractor",
    help="the directory containing the serialized models of the frame extractors, one per domain"
  ).default(null)

  /**
   * The directory containing the serialized models of the HAN classifier, one per domain.
   */
  val hanClassifierModelsDir: String? by parser.storing(
    "-s",
    "--han-classifier",
    help="the directory containing the serialized models of the HAN classifier, one per domain"
  ).default(null)

  /**
   * The filename of the LocationsDictionary.
   */
  val locationsDictionary: String? by parser.storing(
    "-d",
    "--locations-dictionary",
    help="the filename of the serialized locations dictionary"
  ).default(null)

  /**
   * Force parsing all arguments (only read ones are parsed by default).
   * Check dependencies.
   */
  init {

    parser.force()

    this.checkDependencies()
  }

  /**
   * Check dependencies of all arguments.
   *
   * @throws ArgumentDependencyNotSatisfied if at least one dependency of an argument is not satisfied
   */
  private fun checkDependencies() {

    this.checkDependency(
      arg = this.langDetectorModel, argName = "language detector model",
      dep = this.cjkTokenizerModel, depName = "cjk tokenizer model",
      checkReverse = true)

    this.checkDependency(
      arg = this.lhrParserModelsDir, argName = "neural parser models directory",
      dep = this.tokenizerModelsDir, depName = "tokenizer models directory")

    this.checkDependency(
      arg = this.locationsDictionary, argName = "locations dictionary",
      dep = this.tokenizerModelsDir, depName = "tokenizer models directory")

    this.checkDependency(
      arg = this.morphoDictionaryDir, argName = "morphology dictionary directory",
      dep = this.lhrParserModelsDir, depName = "neural parser models directory")

    this.checkDependency(
      arg = this.frameExtractorModelsDir, argName = "frame extractor models directory",
      dep = this.lhrParserModelsDir, depName = "neural parser models directory")

    this.checkDependency(
      arg = this.frameExtractorModelsDir, argName = "frame extractor models directory",
      dep = this.tokenizerModelsDir, depName = "tokenizer models directory")

    this.checkDependency(
      arg = this.frameExtractorModelsDir, argName = "frame extractor models directory",
      dep = this.embeddingsDir, depName = "embeddings directory")

    this.checkDependency(
      arg = this.hanClassifierModelsDir, argName = "HAN classifier models directory",
      dep = this.domainEmbeddingsDir, depName = "domains specific embeddings directory")
  }

  /**
   * Check the dependency of an argument.
   *
   * @param arg the argument
   * @param argName the argument name
   * @param dep the dependency to check
   * @param depName the dependency name
   * @param checkReverse whether to check the dependency in the reverse order
   *
   * @throws ArgumentDependencyNotSatisfied if at least one dependency of an argument is not satisfied
   */
  private fun checkDependency(arg: Any?, argName: String, dep: Any?, depName: String, checkReverse: Boolean = false) {

    if (arg != null && dep == null)
      throw ArgumentDependencyNotSatisfied(argName = argName, dependency = depName)

    if (checkReverse && dep != null && arg == null)
      throw ArgumentDependencyNotSatisfied(argName = depName, dependency = argName)
  }
}
