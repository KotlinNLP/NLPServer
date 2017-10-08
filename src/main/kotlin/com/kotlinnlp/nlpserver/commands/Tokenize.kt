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
 * @param modelsDir the directory containing the tokenizer models
 * @property detectLanguageCmd the [DetectLanguage] command to detect the language before tokenizing
 */
class Tokenize(modelsDir: String, private val detectLanguageCmd: DetectLanguage) {

  /**
   * The logger of this command.
   */
  private val logger = Logger.getLogger("NLP Server - Tokenize")

  /**
   * Maps each supported language iso-code to its [NeuralTokenizer].
   */
  private val tokenizers: Map<String, NeuralTokenizer>

  /**
   * Load the models and initialize the tokenizers.
   */
  init {
    this.logger.info("Loading tokenizer models from '$modelsDir'")

    val modelsDirectory = File(modelsDir)

    require(modelsDirectory.isDirectory) { "$modelsDir is not a directory" }

    val tokenizersMap = mutableMapOf<String, NeuralTokenizer>()

    modelsDirectory.listFiles().forEach { modelFile ->

      this.logger.info("Loading '${modelFile.name}'...")
      val model = NeuralTokenizerModel.load(FileInputStream(modelFile))

      tokenizersMap[model.language] = NeuralTokenizer(model)
    }

    this.tokenizers = tokenizersMap.toMap()
  }

  /**
   * Tokenize the given [text].
   * If a [language] is given the related tokenizer is forced to be used, otherwise the [detectLanguageCmd] is used to
   * choose the right tokenizer.
   *
   * @param text the text to tokenize
   * @param language the isoA2-code of the language with which to force the tokenization (default = null)
   *
   * @return the tokenized [text] in JSON format
   */
  operator fun invoke(text: String, language: String? = null): String {

    val tokenizerLang: String = language?.toLowerCase() ?: this.detectLanguageCmd(text)

    require(tokenizerLang in this.tokenizers) { "Language $tokenizerLang not supported." }

    return this.tokenizers[tokenizerLang]!!.tokenize(text).toJsonSentences().toJsonString() + "\n"
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
