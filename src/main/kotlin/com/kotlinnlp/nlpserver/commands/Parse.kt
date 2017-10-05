/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver.commands

import com.kotlinnlp.nlpserver.commands.exceptions.NotSupportedLanguage

/**
 * The command executed on the route '/parse'.
 */
class Parse {

  /**
   * Parse the given [text], eventually forcing on the language [lang].
   *
   * @param text the text to parse
   * @param lang the language to use to parse the [text] (default = null)
   *
   * @return the parsed [text] in JSON format
   */
  operator fun invoke(text: String, lang: String? = null): String {

    if (lang != null) {
      throw NotSupportedLanguage(lang)
    }

    return "{\"parsed\": [], \"text\": \"$text\", \"lang\": \"$lang\"}\n"
  }
}
