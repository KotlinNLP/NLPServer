/* Copyright 2020-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package api

/**
 * Read strings from the standard input calling a given action every time.
 *
 * @param action an action called passing the string read
 */
internal fun inputLoop(action: (String) -> Unit) {

  while (true) {

    print("\nInput text (empty to exit): ")

    readInput()?.let { action(it) } ?: break
  }
}

/**
 * Read a text from the standard input.
 *
 * @return the string read or null if it was empty
 */
internal fun readInput(): String? {
  return readLine()!!.trim().ifEmpty { null }
}
