/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver.routes.utils

import com.beust.klaxon.JsonObject
import com.beust.klaxon.json
import com.kotlinnlp.linguisticdescription.language.Language

/**
 * The language of a text with the related scores distribution.
 *
 * @property language the language of a text
 * @property distribution the distribution of languages scores (null if the language has not been predicted)
 */
internal data class LanguageDistribution(val language: Language, val distribution: List<Pair<Language, Double>>?) {

  /**
   * @return the JSON representation of this object
   */
  fun toJSON(): JsonObject {

    val jsonObj: JsonObject = json {
      obj(
        "id" to language.isoCode,
        "name" to language.name
      )
    }

    this.distribution?.let {
      jsonObj["distribution"] = json {
        array(it.map { (lang, score) ->
          obj("id" to lang.isoCode, "name" to lang.name, "score" to score)
        })
      }
    }

    return jsonObj
  }
}
