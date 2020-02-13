/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver.commands.utils

import org.apache.log4j.Logger

/**
 * Track an elaboration progress logging it with a logger.
 *
 * @param total the total amount of items to elaborate
 * @param logger a logger
 * @param description the log description
 */
internal class Progress(
  private val total: Int,
  private val logger: Logger,
  private val description: String = "Progress"
) {

  /**
   * The threshold over which to print a progress log.
   */
  private var progressTh = 0.1

  /**
   * The current amount of items elaborated.
   */
  private var current = 0

  /**
   * Increment the progress of a unit.
   */
  fun tick() {

    this.current += 1

    val progress: Double = this.current.toDouble() / total

    if (progress >= this.progressTh) {
      this.logger.debug("$description: %.0f %%".format(100 * this.progressTh))
      this.progressTh += 0.1
    }
  }
}
