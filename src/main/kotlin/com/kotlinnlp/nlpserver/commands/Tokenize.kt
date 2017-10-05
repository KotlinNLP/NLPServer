/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver.commands

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.kotlinnlp.neuraltokenizer.NeuralTokenizer
import com.kotlinnlp.neuraltokenizer.NeuralTokenizerModel
import com.kotlinnlp.neuraltokenizer.Sentence
import com.kotlinnlp.neuraltokenizer.Token
import java.io.File
import java.io.FileInputStream
import java.util.logging.Logger

/**
 * The command executed on the route '/tokenize'.
 *
 * @param modelFilename the filename of the model of the tokenizer
 */
class Tokenize(modelFilename: String) {

  /**
   * The logger of this command.
   */
  private val logger = Logger.getLogger("NLP Server - Tokenize")

  /**
   * A [NeuralTokenizer].
   */
  private val tokenizer: NeuralTokenizer

  /**
   * Load the model and initialize the tokenizer.
   */
  init {
    this.logger.info("Loading tokenizer model from '$modelFilename'\n")
    this.tokenizer = NeuralTokenizer(model = NeuralTokenizerModel.load(FileInputStream(File(modelFilename))))
  }

  /**
   * Tokenize the given [text].
   *
   * @param text the text to tokenize
   *
   * @return the tokenized [text] in JSON format
   */
  operator fun invoke(text: String): String {
    return this.tokenizer.tokenize(text).toJsonSentences().toJsonString() + "\n"
  }

  /**
   * @return this list of sentences converted to a nested JsonArray of token forms
   */
  private fun ArrayList<Sentence>.toJsonSentences(): JsonArray<JsonObject> {

    return JsonArray(*Array(
      size = this.size,
      init = { i ->
        JsonObject(mapOf(
          Pair("id", this[i].id),
          Pair("text", this[i].text),
          Pair("startAt", this[i].startAt),
          Pair("endAt", this[i].endAt),
          Pair("tokens", this[i].tokens.toJsonTokens())
        ))
      }
    ))
  }

  /**
   * @return this list of sentences converted to a nested JsonArray of token forms
   */
  private fun ArrayList<Token>.toJsonTokens(): JsonArray<JsonObject> {

    return JsonArray(*Array(
      size = this.size,
      init = { i ->
        JsonObject(mapOf(
          Pair("id", this[i].id),
          Pair("form", this[i].form),
          Pair("startAt", this[i].startAt),
          Pair("endAt", this[i].endAt),
          Pair("isSpace", this[i].isSpace)
        ))
      }
    ))
  }
}
