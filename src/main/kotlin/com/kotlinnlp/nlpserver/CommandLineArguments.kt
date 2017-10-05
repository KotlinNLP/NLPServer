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
   * The port listened by the server.
   */
  val port: Int by parser.storing(
    "-p",
    "--port",
    help="the port listened from the server"
  ) { toInt() }.default(3000)

  /**
   * The filename of the model of the NeuralTokenizer.
   */
  val tokenizerModel: String by parser.storing(
    "-t",
    "--tokenizer-model",
    help="the filename of the model of the neural tokenizer"
  )

  /**
   * The filename of the model of the LanguageDetector.
   */
  val langDetectorModel: String by parser.storing(
    "-l",
    "--language-detector-model",
    help="the filename of the model of the language detector"
  )

  /**
   * The filename of the FrequencyDictionary.
   */
  val freqDictionary: String by parser.storing(
    "-f",
    "--frequency-dictionary",
    help="the filename of the frequency dictionary used by the language detector"
  )

  /**
   * Force parsing all arguments (only read ones are parsed by default).
   */
  init {
    parser.force()
  }
}
