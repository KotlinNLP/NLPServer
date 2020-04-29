/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver

/**
 * Raised when a dependency of a command line argument is not satisfied.
 *
 * @param dependency the name of the not satisfied dependency
 */
class ArgumentDependencyNotSatisfied(argName: String, dependency: String)
  : RuntimeException("'$argName' requires '$dependency'")

/**
 * Raised when the input text is blank.
 */
class BlankText : RuntimeException()

/**
 * Raised when the given language is not supported.
 *
 * @property langCode the ISO 639-1 code of the language
 */
class LanguageNotSupported(val langCode: String) : RuntimeException(langCode)

/**
 * Raised when the given required query parameters are missing.
 *
 * @param params the list of missing parameters
 */
class MissingQueryParameters(params: List<String>) : RuntimeException(params.joinToString(","))

/**
 * Raised when there is no model associated to a given domain.
 *
 * @property domain the domain name
 */
class InvalidDomain(val domain: String): RuntimeException(domain)

/**
 * Raised when the given body does not contain the expected JSON object.
 */
class InvalidJSONBody : RuntimeException()

/**
 * Raised when the Content-Type header of the request is not the expected one.
 *
 * @property expected the expected Content-Type
 * @property given the given Content-Type
 */
class InvalidContentType(val expected: String, val given: String)
  : RuntimeException("Expected '$expected', given '$given'")
