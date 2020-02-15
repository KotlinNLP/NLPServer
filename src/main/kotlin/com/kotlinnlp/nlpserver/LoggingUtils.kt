/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver

import org.apache.log4j.*
import org.apache.log4j.spi.RootLogger

/**
 * Setup the logging.
 * This function should be called after the creation of all the loggers.
 *
 * @param debugMode whether to log in debug mode
 */
internal fun setupLogging(debugMode: Boolean) {

  RootLogger.getRootLogger().apply {

    level = if (debugMode) Level.DEBUG else Level.INFO

    setAppender(
      maxNameLength = LogManager.getCurrentLoggers().asSequence().map { (it as Logger).name.length }.max() ?: 5)
  }
}

/**
 * Add a custom console appender to this logger.
 *
 * @param maxNameLength the max length of the 'name' section in the pattern
 *
 * @return this logger
 */
internal fun Logger.setAppender(maxNameLength: Int = this.name.length): Logger = this.apply {
  removeAllAppenders()
  addAppender(ConsoleAppender(PatternLayout("(Thread %t) [%d] %-5p %-${maxNameLength}c - %m%n")))
}
