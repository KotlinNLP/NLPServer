/* Copyright 2020-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package api

import com.kotlinnlp.ApiClient
import com.kotlinnlp.api.NlpServiceApi
import com.kotlinnlp.api.model.InputText
import com.kotlinnlp.api.model.Location
import com.kotlinnlp.api.model.SingleLocation
import kotlin.Double.Companion.NaN

/**
 * Test the `locations` method of the KotlinNLP APIs.
 *
 * Launch with the '-h' option for help about the command line arguments.
 */
fun main(args: Array<String>) {

  val parsedArgs = CommandLineArguments(args)
  val client = NlpServiceApi(ApiClient().setBasePath("http://${parsedArgs.host}:${parsedArgs.port}"))

  inputLoop {

    val locations: List<Location> = client.findLocations(InputText().text(it), false)

    println("\nLocations found:")

    locations.forEach { loc ->
      println(" [%.2f, %.2f] ${loc.getNameEN()} (${loc.getCountryName()}) [score: %.1f %%, confidence: %.1f %%]"
        .format(
          loc.coords?.lat ?: NaN,
          loc.coords?.lon ?: NaN,
          100.0 * loc.stats.score.value,
          100.0 * loc.stats.confidence.value))
    }
  }
}

/**
 * @return the name of the country inside which this location is included
 */
private fun Location.getCountryName(): String = if (this.type == Location.TypeEnum.COUNTRY)
  this.name
else
  this.parents.country?.getNameEN() ?: "-"

/**
 * @return the name of this location preferring the English translation
 */
@Suppress("UNCHECKED_CAST")
private fun Location.getNameEN(): String = (this.translations as Map<String, String>)["en"] ?: this.name

/**
 * @return the name of this location preferring the English translation
 */
@Suppress("UNCHECKED_CAST")
private fun SingleLocation.getNameEN(): String = (this.translations as Map<String, String>)["en"] ?: this.name
