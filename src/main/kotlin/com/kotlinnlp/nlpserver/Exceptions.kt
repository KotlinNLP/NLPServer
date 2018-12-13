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
 * Exception raised when the input text is empty.
 */
class EmptyText : RuntimeException()

/**
 * Exception raised when the given language is not supported.
 *
 * @property langCode the ISO 639-1 code of the language
 */
class LanguageNotSupported(val langCode: String) : RuntimeException(langCode)

/**
 * Exception raised when the given required parameters are missing.
 *
 * @param params the list of missing parameters
 */
class MissingParameters(params: List<String>) : RuntimeException(params.joinToString(","))

/**
 * Raised when an embeddings map of a given language is missing.
 *
 * @param langCode the ISO 639-1 language code of the missing embeddings map
 */
class MissingEmbeddingsMapByLanguage(langCode: String): RuntimeException("Missing embeddings for language '$langCode'")

/**
 * Raised when an embeddings map of a given domain is missing.
 *
 * @param domain the domain of the missing embeddings map
 */
class MissingEmbeddingsMapByDomain(domain: String): RuntimeException("Missing embeddings for domain '$domain'")

/**
 * Raised when there is no model associated to a given domain.
 *
 * @property domain the domain name
 */
class InvalidDomain(val domain: String): RuntimeException(domain)
