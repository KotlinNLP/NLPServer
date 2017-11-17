/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver.commands

import com.kotlinnlp.languagedetector.LanguageDetector
import com.kotlinnlp.linguisticdescription.morphology.MorphologyDictionary
import com.kotlinnlp.neuralparser.NeuralParser
import com.kotlinnlp.neuralparser.language.Token
import com.kotlinnlp.neuralparser.parsers.GenericNeuralParser
import com.kotlinnlp.neuraltokenizer.NeuralTokenizer
import com.kotlinnlp.neuraltokenizer.Sentence
import com.kotlinnlp.nlpserver.commands.exceptions.NotSupportedLanguage

/**
 * The command executed on the route '/parse'.
 *
 * @param morphologyDictionary a morphology dictionary
 * @param parser a neural parser
 */
class Parse(
  private val morphologyDictionary: MorphologyDictionary,
  private val parser: GenericNeuralParser,
  private val tokenizers: Map<String, NeuralTokenizer>,
  private val languageDetector: LanguageDetector?
) {

  /**
   * Parse the given [text], eventually forcing on the language [lang].
   *
   * @param text the text to parse
   * @param lang the language to use to parse the [text] (default = null)
   *
   * @return the parsed [text] in JSON format
   */
  operator fun invoke(text: String, lang: String? = null): String {

    val tokenizerLang: String = this.getTokenizerLanguage(text = text, forcedLang = lang)
    val sentences: ArrayList<Sentence> = this.tokenizers[tokenizerLang]!!.tokenize(text)

    return sentences.joinToString(separator = "\n\n") {
      val parserSentence = it.toParserSentence()
      parserSentence.toCoNLL(dependencyTree = this.parser.parse(parserSentence)).toCoNLL(writeComments = false)
    }
  }

  /**
   *
   */
  private fun getTokenizerLanguage(text: String, forcedLang: String?): String {

    return if (this.languageDetector == null) {
      if (forcedLang == null) throw RuntimeException("Cannot determine language automatically (missing language detector)")
      if (forcedLang !in this.tokenizers) throw NotSupportedLanguage(forcedLang)

      forcedLang

    } else {
      val tokenizerLang: String = forcedLang?.toLowerCase() ?: this.languageDetector.detectLanguage(text).isoCode
      if (tokenizerLang !in this.tokenizers) throw NotSupportedLanguage(tokenizerLang)

      tokenizerLang
    }
  }

  /**
   *
   */
  private fun Sentence.toParserSentence() = com.kotlinnlp.neuralparser.language.Sentence(
    tokens = this.tokens.filter { !it.isSpace }.mapIndexed { i, it ->

      val firstPos: String?
        = this@Parse.morphologyDictionary[it.form]?.morphologies?.first()?.list?.first()?.type?.annotation

      Token(id = i, word = it.form, pos = firstPos ?: "UNKNOWN")
    }
  )
}
