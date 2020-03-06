/* Copyright 2020-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package api

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default

/**
 * The interpreter of command line arguments.
 *
 * @param args the array of command line arguments
 */
internal class CommandLineArguments(args: Array<String>) {

  /**
   * The parser of the string arguments.
   */
  private val parser = ArgParser(args)

  /**
   * The host of the NLPServer (default = localhost).
   */
  val host: String by parser.storing(
    "-t",
    "--host",
    help="the host of the NLPServer"
  ).default("localhost")

  /**
   * The port listened by the NLPServer (default = 3000).
   */
  val port: Int by parser.storing(
    "-p",
    "--port",
    help="the port listened by the NLPServer"
  ) { toInt() }.default(3000)

  /**
   * Force parsing all arguments (only read ones are parsed by default).
   */
  init {
    parser.force()
  }
}
