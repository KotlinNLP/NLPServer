/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver.commands

import com.beust.klaxon.json
import com.kotlinnlp.conllio.Sentence as CoNLLSentence
import com.kotlinnlp.conllio.Token as CoNLLToken
import com.kotlinnlp.languagedetector.LanguageDetector
import com.kotlinnlp.linguisticdescription.sentence.MorphoSyntacticSentence
import com.kotlinnlp.linguisticdescription.sentence.token.MorphoSyntacticToken
import com.kotlinnlp.linguisticdescription.sentence.token.RealToken
import com.kotlinnlp.neuralparser.NeuralParser
import com.kotlinnlp.neuralparser.language.ParsingSentence
import com.kotlinnlp.neuralparser.language.ParsingToken
import com.kotlinnlp.neuraltokenizer.NeuralTokenizer
import com.kotlinnlp.neuraltokenizer.Sentence
import com.kotlinnlp.nlpserver.LanguageNotSupported

/**
 * The command executed on the route '/parse'.

 * @param parser a neural parser
 * @param tokenizers a map of languages iso-a2 codes to neural tokenizers
 * @param languageDetector a language detector (can be null)
 */
class Parse(
  private val parser: NeuralParser<*>,
  private val tokenizers: Map<String, NeuralTokenizer>,
  private val languageDetector: LanguageDetector?
) {

  /**
   * The format of the parsing response.
   *
   * @property CoNLL the response will be written in CoNLL format
   * @property JSON the response will be written in JSON format
   */
  enum class ResponseFormat { CoNLL, JSON }

  /**
   * Parse the given [text], eventually forcing on the language [lang].
   *
   * @param text the text to parse
   * @param lang the language to use to parse the [text] (default = null)
   * @param format the string format of the parsed sentences response (default = JSON)
   * @param prettyPrint pretty print, used for JSON format (default = false)
   *
   * @return the parsed [text] in the given string [format]
   */
  operator fun invoke(text: String,
                      lang: String? = null,
                      format: ResponseFormat = ResponseFormat.JSON,
                      prettyPrint: Boolean = false): String {

    val textLanguage: String = this.getTextLanguage(text = text, forcedLang = lang)
    val sentences: List<Sentence> = this.tokenizers.getValue(textLanguage).tokenize(text)

    return when (format) {
      ResponseFormat.CoNLL -> this.parseToCoNLLFormat(sentences)
      ResponseFormat.JSON -> this.parseToJSONFormat(sentences, lang = textLanguage, prettyPrint = prettyPrint)
    } + "\n"
  }

  /**
   * @param text the text to parse (of which to detect the language if [forcedLang] is null)
   * @param forcedLang force this language to be returned (if it is supported)
   *
   * @throws LanguageNotSupported when the returning language is not supported
   * @throws RuntimeException when [forcedLang] is 'null' but the language detector is missing
   *
   * @return the language iso-a2 code of the given [text]
   */
  private fun getTextLanguage(text: String, forcedLang: String?): String {

    return if (this.languageDetector == null) {

      if (forcedLang == null) {
        throw RuntimeException("Cannot determine language automatically (missing language detector)")

      } else {
        if (forcedLang !in this.tokenizers) throw LanguageNotSupported(forcedLang)

        forcedLang
      }

    } else {
      val lang: String = forcedLang?.toLowerCase() ?: this.languageDetector.detectLanguage(text).isoCode
      if (lang !in this.tokenizers) throw LanguageNotSupported(lang)

      lang
    }
  }

  /**
   * Parse the given [sentences] and return the response in CoNLL format.
   *
   * @param sentences the list of sentences to parse
   *
   * @return the parsed sentences in CoNLL string format
   */
  private fun parseToCoNLLFormat(sentences: List<Sentence>): String =
    sentences.joinToString(separator = "\n\n") { sentence ->
      this.parser.parse(sentence.toParsingSentence()).toCoNLL().toCoNLLString(writeComments = false)
    }

  /**
   * Parse the given [sentences] and return the response in JSON format.
   *
   * @param sentences the list of sentences to parse
   * @param lang the text language
   * @param prettyPrint pretty print (default = false)
   *
   * @return the parsed sentences in JSON string format
   */
  private fun parseToJSONFormat(sentences: List<Sentence>, lang: String, prettyPrint: Boolean = false): String = json {
    obj (
      "lang" to lang,
      "sentences" to array(sentences.map { this@Parse.parser.parse(it.toParsingSentence()).toJSON() })
    )
  }.toJsonString(prettyPrint = prettyPrint)

  /**
   * Convert this tokenizer Sentence object into the [ParsingSentence] object of the NeuralParser.
   *
   * @return a NeuralParser Parsing Sentence
   */
  private fun Sentence.toParsingSentence() = ParsingSentence(
    tokens = this.tokens.mapIndexed { i, it ->
      ParsingToken(id = i, form = it.form, position = it.position, morphologies = emptyList(), posTag = null)
    }
  )

  /**
   * Convert this [MorphoSyntacticSentence] to a CoNLL Sentence.
   *
   * @return a CoNLL Sentence
   */
  private fun MorphoSyntacticSentence.toCoNLL(): CoNLLSentence = CoNLLSentence(
    sentenceId = this.id.toString(),
    text = this.buildText(),
    tokens = this.tokens.map { it.toCoNLL() }
  )

  /**
   * Note:
   * This extension is specific for MorphoSyntacticTokens that come from the Neural Parser and they are built from
   * ParsingTokens.
   * ParsingTokens ids are sequential and start from 0. They start from 1 in the CoNLL format instead.
   *
   * @return the CoNLL object that represents this token
   */
  private fun MorphoSyntacticToken.toCoNLL() = CoNLLToken(
    id = this.id + 1, //
    form = (this as? RealToken)?.form ?: CoNLLToken.emptyFiller,
    lemma = CoNLLToken.emptyFiller,
    pos = if (this.morphologies.isNotEmpty())
      this.morphologies.first().list.joinToString("-") { it.type.annotation }
    else
      CoNLLToken.emptyFiller,
    pos2 = CoNLLToken.emptyFiller,
    feats = emptyMap(),
    head = this.dependencyRelation.governor?.plus(1) ?: 0, // id starts from 1 in the CoNLL format
    deprel = this.dependencyRelation.deprel,
    multiWord = null
  )
}
