/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver.routes.utils

import com.kotlinnlp.nlpserver.EmptyText

/**
 * Defines a generic command that works on a text.
 */
internal interface Command {

  /**
   * Check that the text is not empty.
   */
  fun checkText(text: String) {

    if (text.isEmpty()) throw EmptyText()
  }
}
