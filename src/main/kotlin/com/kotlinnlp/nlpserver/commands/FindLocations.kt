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
import com.kotlinnlp.linguisticdescription.language.Language
import com.kotlinnlp.neuraltokenizer.NeuralTokenizer
import com.kotlinnlp.nlpserver.LanguageNotSupported
import com.kotlinnlp.nlpserver.commands.utils.Command

/**
 * The command executed on the route '/find-locations'.
 *
 * @param dictionary a locations dictionary
 * @param tokenizers a map of languages ISO 3166-1 alpha-2 codes to neural tokenizers
 */
class FindLocations(
  private val dictionary: LocationsDictionary,
  private val tokenizers: Map<String, NeuralTokenizer>
) : Command {

  /**
   * Find locations in the given [text].
   *
   * @param text the input text
   * @param language the language to use to tokenize the [text]
   * @param candidates the list of candidate locations as pairs of <name, score>
   * @param prettyPrint pretty print (default = false)
   *
   * @return the parsed [text] in the given string [format]
   */
  operator fun invoke(text: String,
                      language: Language,
                      candidates: List<Pair<String, Double>>,
                      prettyPrint: Boolean = false): String {

    this.checkText(text)

    val tokenizer: NeuralTokenizer = this.tokenizers[language.isoCode] ?: throw LanguageNotSupported(language.isoCode)

    val finder = LocationsFinder(
      dictionary = this.dictionary,
      textTokens = tokenizer.tokenize(text).flatMap { sentence -> sentence.tokens.map { it.form } },
      candidateEntities = candidates.map { CandidateEntity(name = it.first, score = it.second) }.toSet(),
      coordinateEntitiesGroups = listOf(),
      ambiguityGroups = listOf()
    )

    return json {
      array(finder.bestLocations.map {
        JsonObject(it.toJSON() + it.location.getParentsInfo())
      })
    }.toJsonString(prettyPrint) + "\n"
  }

  /**
   * @return a map of parents info of this location (only actual parents are present)
   */
  private fun Location.getParentsInfo(): Map<String, String?> = mapOf(
    "adminArea1" to this.adminArea1Id?.let { this@FindLocations.dictionary[it]!!.name },
    "adminArea2" to this.adminArea2Id?.let { this@FindLocations.dictionary[it]!!.name },
    "countryIso" to this.countryId?.let { this@FindLocations.dictionary[it]!!.isoA2 },
    "country" to this.countryId?.let { this@FindLocations.dictionary[it]!!.name },
    "continent" to this.continentId?.let { this@FindLocations.dictionary[it]!!.name }
  )
}
