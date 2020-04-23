/* Copyright 2020-present Simone Cangialosi. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver.routes.utils

import com.kotlinnlp.linguisticdescription.sentence.MorphoSynSentence
import com.kotlinnlp.neuralparser.NeuralParser
import com.kotlinnlp.neuralparser.helpers.preprocessors.BasePreprocessor
import com.kotlinnlp.neuralparser.helpers.preprocessors.MorphoPreprocessor
import com.kotlinnlp.neuralparser.helpers.preprocessors.SentencePreprocessor
import com.kotlinnlp.neuralparser.language.BaseSentence
import com.kotlinnlp.neuralparser.language.BaseToken
import com.kotlinnlp.neuraltokenizer.Sentence
import com.kotlinnlp.neuraltokenizer.Token
import com.kotlinnlp.nlpserver.LanguageNotSupported

/**
 * Defines a command that uses morpho-syntactic parsers.
 */
internal interface ParsingCommand : TokenizingCommand {

  companion object {

    /**
     * A base sentence preprocessor.
     */
    private val basePreprocessor = BasePreprocessor()
  }

  /**
   * A map of morpho-syntactic parsers associated by language ISO 639-1 code.
   */
  val parsers: Map<String, NeuralParser<*>>

  /**
   * A map of morpho-preprocessors associated by language ISO 639-1 code.
   */
  val morphoPreprocessors: Map<String, MorphoPreprocessor>

  /**
   * Parse a text morpho-syntactically.
   *
   * @param text the input text
   * @param langCode the language iso code
   *
   * @return the parsed sentences
   */
  fun parse(text: String, langCode: String): List<MorphoSynSentence> = this.parse(
    text = text,
    sentences = this.tokenizers.getValue(langCode).tokenize(text).filter { it.tokens.isNotEmpty() },
    langCode = langCode)

  /**
   * Parse a text morpho-syntactically.
   *
   * @param text the input text
   * @param sentences the tokenized sentences of the given text
   * @param langCode the language iso code
   *
   * @return the parsed sentences
   */
  fun parse(text: String, sentences: List<Sentence>, langCode: String): List<MorphoSynSentence> {

    this.logger.debug("Parsing text with ${sentences.size} sentences: '${text.cutText(50)}'...")

    val preprocessor: SentencePreprocessor = this.morphoPreprocessors[langCode] ?: basePreprocessor
    val parser: NeuralParser<*> = this.parsers[langCode] ?: throw LanguageNotSupported(langCode)

    return sentences.map { parser.parse(preprocessor.convert(it.toBaseSentence())) }
  }

  /**
   * @return a new base sentence built from this tokenizer sentence
   */
  private fun Sentence.toBaseSentence() = BaseSentence(
    id = this.position.index,
    tokens = this.tokens.mapIndexed { i, it -> it.toBaseToken(id = i) },
    position = this.position)

  /**
   * @param id the token ID
   *
   * @return a new base token built from this tokenizer token
   */
  private fun Token.toBaseToken(id: Int) = BaseToken(id = id, position = this.position, form = this.form)
}
