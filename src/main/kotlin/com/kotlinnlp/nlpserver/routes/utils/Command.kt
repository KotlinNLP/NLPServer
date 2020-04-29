/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver.routes.utils

import com.kotlinnlp.nlpserver.BlankText
import org.apache.log4j.Logger

/**
 * Defines a generic command that works on a text.
 */
internal interface Command {

  companion object {

    /**
     * The suffix added when cutting of a text.
     */
    const val CUT_TEXT_SUFFIX = "[...]"
  }

  /**
   * The logger of the command.
   */
  val logger: Logger

  /**
   * Check that the text is not blank.
   *
   * @param text a text
   *
   * @throws BlankText if the given text is blank
   *
   * @return the given text
   */
  fun checkText(text: String): String = text.ifBlank { throw BlankText() }

  /**
   * Cutoff this text in case its length is greater then [maxChars], adding [CUT_TEXT_SUFFIX] at the end.
   *
   * @param maxChars the max number of chars of the returned string
   *
   * @return the cut text
   */
  fun String.cutText(maxChars: Int): String = if (this.length > maxChars)
    this.take(maxChars - CUT_TEXT_SUFFIX.length) + CUT_TEXT_SUFFIX
  else
    this
}
