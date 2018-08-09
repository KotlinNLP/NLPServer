/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver

/**
 * Exception raised when a dependency of a command line argument is not satisfied.
 *
 * @param dependency the name of the not satisfied dependency
 */
class ArgumentDependencyNotSatisfied(argName: String, dependency: String)
  : RuntimeException("'$argName' requires '$dependency'")

/**
 * Exception raised when the given language is not supported.
 *
 * @param langCode the ISO 639-1 code of the language
 */
class LanguageNotSupported(langCode: String) : RuntimeException(langCode)

/**
 * Exception raised when the given required parameters are missing.
 *
 * @param params the list of missing parameters
 */
class MissingParameters(params: List<String>) : RuntimeException(params.joinToString(","))
