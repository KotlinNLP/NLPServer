/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver.commands.exceptions

/**
 * Exception raised when one or more dependencies of a command line argument are not satisfied.
 *
 * @param dependencies the list of not satisfied dependencies
 */
class ArgumentDependenciesNotSatisfied(
  argName: String,
  dependencies: List<String>
) : RuntimeException("'$argName' requires: '${dependencies.joinToString("', '")}'")
