/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver.commands

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.kotlinnlp.nlpserver.InvalidContentType
import com.kotlinnlp.nlpserver.InvalidJSONBody
import com.kotlinnlp.nlpserver.MissingQueryParameters
import spark.Request
import java.lang.StringBuilder

/**
 * Handle a command of the API.
 */
interface Route {

  /**
   * The name of the command.
   */
  val name: String

  /**
   * Initialize the route.
   * Define the paths handled.
   */
  fun initialize()

  /**
   * @throws InvalidJSONBody if the body of this request is not a JSON object
   *
   * @return the body of this request parsed as JSON object
   */
  fun Request.getJsonObject(): JsonObject {

    this.assertJsonApplication()

    return Parser().parse(StringBuilder(this.body())) as? JsonObject ?: throw InvalidJSONBody()
  }

  /**
   * Assert that the Content-Type header of this request is 'application/json'.
   *
   * @throws InvalidContentType if the Content-Type header of this request is not 'application/json'
   */
  fun Request.assertJsonApplication() {

    if (!this.contentType().startsWith("application/json"))
      throw InvalidContentType(expected = "application/json", given = this.contentType())
  }

  /**
   * Check if a required parameter is present in the query of this [Request] and return it.
   *
   * @param requiredParam the required parameter to check
   *
   * @throws MissingQueryParameters if the given parameter is missing
   *
   * @return the required parameter
   */
  fun Request.requiredQueryParam(requiredParam: String): String {

    this.checkRequiredQueryParams(listOf(requiredParam))

    return this.queryParams(requiredParam)
  }

  /**
   * Check if all the required parameters are present in the query of in this [Request].
   *
   * @param requiredParams the list of required query parameters to check
   *
   * @throws MissingQueryParameters if at least one query parameter is missing
   */
  fun Request.checkRequiredQueryParams(requiredParams: List<String>) {

    val missingParams: List<String> = this.getMissingQueryParams(requiredParams)

    if (missingParams.isNotEmpty()) {
      throw MissingQueryParameters(missingParams)
    }
  }

  /**
   * @param requiredParams a list of required query parameters
   *
   * @return a list of required parameters that are missing in this [Request]
   */
  fun Request.getMissingQueryParams(requiredParams: List<String>): List<String> {

    val requestParams = this.queryParams()

    return requiredParams.filter { !requestParams.contains(it) }
  }

  /**
   * Get a boolean query parameter.
   * A boolean query parameter is true if it is present with an empty value or containing 'true'.
   *
   * @param paramName the name of the query parameter
   *
   * @return true if the parameters is 'true' or empty, otherwise false
   */
  fun Request.booleanParam(paramName: String): Boolean =
    this.queryParams(paramName)?.let { it.isEmpty() || it.toBoolean() } ?: false
}
