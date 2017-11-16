/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver

import com.kotlinnlp.nlpserver.commands.exceptions.ArgumentDependenciesNotSatisfied
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
   * The filename of the MorphologyDictionary.
   */
  val morphoDictionary: String? by parser.storing(
    "-m",
    "--morphology-dictionary",
    help="the filename of the morphology dictionary used by the parser"
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
   * @throws ArgumentDependenciesNotSatisfied if at least one dependency of an argument is not satisfied
   */
  private fun checkDependencies() {

    if (this.langDetectorModel != null && this.cjkTokenizerModel == null)
      throw ArgumentDependenciesNotSatisfied(
        argName = "language detector model",
        dependencies = listOf("cjk tokenizer model"))

    if (this.cjkTokenizerModel != null && this.langDetectorModel == null)
      throw ArgumentDependenciesNotSatisfied(
        argName = "cjk tokenizer model",
        dependencies = listOf("language detector model"))

    if (this.tokenizerModelsDir != null && this.langDetectorModel == null)
      throw ArgumentDependenciesNotSatisfied(
        argName = "tokenizer models dir",
        dependencies = listOf("language detector model", "cjk tokenizer model"))

  }
}
