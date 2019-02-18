/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver.commands

import com.beust.klaxon.JsonObject
import com.beust.klaxon.json
import com.kotlinnlp.hanclassifier.HANClassifier
import com.kotlinnlp.conllio.Sentence as CoNLLSentence
import com.kotlinnlp.conllio.Token as CoNLLToken
import com.kotlinnlp.languagedetector.LanguageDetector
import com.kotlinnlp.linguisticdescription.InvalidLanguageCode
import com.kotlinnlp.linguisticdescription.language.Language
import com.kotlinnlp.linguisticdescription.sentence.token.FormToken
import com.kotlinnlp.linguisticdescription.sentence.Sentence
import com.kotlinnlp.neuraltokenizer.NeuralTokenizer
import com.kotlinnlp.neuraltokenizer.Sentence as TokenizerSentence
import com.kotlinnlp.nlpserver.InvalidDomain
import com.kotlinnlp.nlpserver.commands.utils.TokenizingCommand
import com.kotlinnlp.nlpserver.commands.utils.buildSentence
import com.kotlinnlp.simplednn.simplemath.ndarray.dense.DenseNDArray

/**
 * The command executed on the route '/categorize'.

 * @param languageDetector a language detector (can be null)
 * @param tokenizers a map of neural tokenizers associated by language ISO 639-1 code
 * @param hanClassifiers a map of HAN classifier associated by domain name
 */
class Categorize(
  override val languageDetector: LanguageDetector?,
  override val tokenizers: Map<String, NeuralTokenizer>,
  private val hanClassifiers: Map<String, HANClassifier>
) : TokenizingCommand {

  /**
   * Categorize a given [text], eventually forcing on a given language and a given domain.
   *
   * @param text the text to categorize
   * @param lang the language to use to analyze the [text] (default = unknown)
   * @param domain force to use the classifiers associated to the given domain
   * @param distribution whether to include the distribution in the response (default = true)
   * @param prettyPrint pretty print, used for JSON format (default = false)
   *
   * @throws InvalidLanguageCode when the requested (or detected) language is not compatible
   * @throws InvalidDomain when the given domain is not valid
   *
   * @return the category recognized, eventually with the distribution, in a JSON string
   */
  operator fun invoke(text: String,
                      lang: Language? = null,
                      domain: String? = null,
                      distribution: Boolean = true,
                      prettyPrint: Boolean = false): String {

    this.checkText(text)

    val textLanguage: Language = this.getTextLanguage(text = text, forcedLang = lang)
    val sentences: List<TokenizerSentence> =
      this.tokenizers.getValue(textLanguage.isoCode).tokenize(text).filter { it.tokens.isNotEmpty() }
    val classifiers: List<HANClassifier> =
      domain?.let { listOf(this.hanClassifiers[domain] ?: throw InvalidDomain(domain)) }
        ?: this.hanClassifiers.values.toList()

    val outputPerDomain = JsonObject(classifiers.associate { classifier ->

      classifier.model.name to json {

        val formSentences: List<Sentence<FormToken>> = sentences.map { s -> buildSentence(s.tokens.map { it.form }) }
        val predictions: List<DenseNDArray> = classifier.classify(formSentences)

        array(predictions.map {

          val jsonCat: JsonObject = obj("category" to it.argMaxIndex())

          if (distribution) jsonCat["distribution"] = array(it.toDoubleArray().toList())

          jsonCat
        })
      }
    })

    return outputPerDomain.toJsonString(prettyPrint) + if (prettyPrint) "\n" else ""
  }
}
