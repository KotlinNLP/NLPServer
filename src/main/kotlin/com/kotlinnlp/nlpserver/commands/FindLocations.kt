/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver.commands

import com.beust.klaxon.JsonObject
import com.beust.klaxon.json
import com.kotlinnlp.geolocation.LocationsFinder
import com.kotlinnlp.geolocation.dictionary.LocationsDictionary
import com.kotlinnlp.geolocation.structures.CandidateEntity
import com.kotlinnlp.geolocation.structures.Location
import com.kotlinnlp.neuraltokenizer.NeuralTokenizer
import com.kotlinnlp.nlpserver.commands.exceptions.LanguageNotSupported

/**
 * The command executed on the route '/find-locations'.
 *
 * @param dictionary a locations dictionary
 * @param tokenizers a map of languages iso-a2 codes to neural tokenizers
 */
class FindLocations(
  private val dictionary: LocationsDictionary,
  private val tokenizers: Map<String, NeuralTokenizer>
) {

  /**
   * Find locations in the given [text].
   *
   * @param text the input text
   * @param lang the iso-a2 code of the language to use to tokenize the [text]
   * @param candidates the list of candidate locations as pairs of <name, score>
   * @param prettyPrint pretty print (default = false)
   *
   * @return the parsed [text] in the given string [format]
   */
  operator fun invoke(text: String,
                      lang: String,
                      candidates: List<Pair<String, Double>>,
                      prettyPrint: Boolean = false): String {

    if (lang !in this.tokenizers) throw LanguageNotSupported(lang)

    val finder = LocationsFinder(
      dictionary = this.dictionary,
      textTokens = this.tokenizers.getValue(lang).tokenize(text).flatMap { it.tokens.map { it.form } },
      candidateEntities = candidates.map { CandidateEntity(name = it.first, score = it.second) }.toSet(),
      coordinateEntitiesGroups = listOf(),
      ambiguityGroups = listOf()
    )

    return json {
      array(finder.bestLocations.map {
        JsonObject(it.location.toJSON() + this@FindLocations.getParentsObj(it.location))
      })
    }.toJsonString(prettyPrint) + "\n"
  }

  /**
   *
   */
  private fun getParentsObj(location: Location): JsonObject = JsonObject(
    mapOf(
      "region" to location.regionId?.let { this@FindLocations.dictionary[it]!!.name },
      "continent" to location.continentId?.let { this@FindLocations.dictionary[it]!!.name },
      "country" to location.countryId?.let { this@FindLocations.dictionary[it]!!.name },
      "adminArea2" to location.adminArea2Id?.let { this@FindLocations.dictionary[it]!!.name },
      "adminArea1" to location.adminArea1Id?.let { this@FindLocations.dictionary[it]!!.name }
    ).filter { it.value != null }
  )
}
