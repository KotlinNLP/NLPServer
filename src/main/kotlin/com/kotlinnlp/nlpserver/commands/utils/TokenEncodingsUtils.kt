/* Copyright 2018-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver.commands.utils

import com.kotlinnlp.linguisticdescription.sentence.Sentence
import com.kotlinnlp.linguisticdescription.sentence.token.FormToken
import com.kotlinnlp.linguisticdescription.sentence.token.properties.Position
import com.kotlinnlp.lssencoder.LSSModel
import com.kotlinnlp.neuralparser.helpers.preprocessors.SentencePreprocessor
import com.kotlinnlp.neuralparser.language.BaseSentence
import com.kotlinnlp.neuralparser.language.BaseToken
import com.kotlinnlp.neuralparser.language.ParsingSentence
import com.kotlinnlp.neuralparser.language.ParsingToken
import com.kotlinnlp.tokensencoder.embeddings.keyextractor.NormWordKeyExtractor
import com.kotlinnlp.simplednn.core.embeddings.EmbeddingsMapByDictionary
import com.kotlinnlp.tokensencoder.embeddings.EmbeddingsEncoderModel
import com.kotlinnlp.tokensencoder.ensemble.EnsembleTokensEncoder
import com.kotlinnlp.tokensencoder.ensemble.EnsembleTokensEncoderModel
import com.kotlinnlp.tokensencoder.lss.LSSTokensEncoderModel
import com.kotlinnlp.tokensencoder.wrapper.SentenceConverter
import com.kotlinnlp.tokensencoder.wrapper.TokensEncoderWrapperModel

/**
 * A token with a form.
 *
 * @property form the form of the token
 */
private class BaseToken(override val form: String) : FormToken

/**
 * A sentence of form tokens.
 *
 * @property tokens the list of tokens that compose the sentence
 */
private class BaseSentence(override val tokens: List<FormToken>) : Sentence<FormToken>

/**
 * @param forms a list of tokens forms
 *
 * @return a new form sentence with the given forms
 */
internal fun buildSentence(forms: List<String>): Sentence<FormToken> =
  BaseSentence(tokens = forms.map { BaseToken(it) })

/**
 * Build an [EnsembleTokensEncoder] composed by an embeddings encoder and an LSS encoder.
 *
 * @param preprocessor a sentence preprocessor
 * @param embeddingsMap an embeddings map by dictionary
 * @param lssModel the model of an LSS encoder
 *
 * @return a new tokens encoder
 */
internal fun buildTokensEncoder(preprocessor: SentencePreprocessor,
                                embeddingsMap: EmbeddingsMapByDictionary,
                                lssModel: LSSModel<ParsingToken, ParsingSentence>) = EnsembleTokensEncoder(
  model = EnsembleTokensEncoderModel(
    components = listOf(
      EnsembleTokensEncoderModel.ComponentModel(
        TokensEncoderWrapperModel(
          model = EmbeddingsEncoderModel(embeddingsMap = embeddingsMap, embeddingKeyExtractor = NormWordKeyExtractor()),
          converter = FormSentenceConverter(preprocessor))),
      EnsembleTokensEncoderModel.ComponentModel(
        TokensEncoderWrapperModel(
          model = LSSTokensEncoderModel(lssModel = lssModel),
          converter = FormSentenceConverter(preprocessor))))
  ),
  useDropout = false)

/**
 * The [SentenceConverter] from a sentence of form tokens.
 *
 * @param preprocessor a sentence preprocessor
 */
private class FormSentenceConverter(
  private val preprocessor: SentencePreprocessor
) : SentenceConverter<FormToken, Sentence<FormToken>, ParsingToken, ParsingSentence> {

  /**
   * @param sentence an input sentence
   *
   * @return a parsing sentence built with the given input sentence
   */
  override fun convert(sentence: Sentence<FormToken>): ParsingSentence =
    this.preprocessor.convert(this.buildBaseSentence(sentence))

  /**
   * @param sentence an input sentence
   *
   * @return a new base sentence built from the given input sentence
   */
  private fun buildBaseSentence(sentence: Sentence<FormToken>): BaseSentence {

    var tokenPosition = 0

    fun nextPosition(token: FormToken): Int {
      tokenPosition += token.form.length + 1
      return tokenPosition
    }

    return BaseSentence(
      id = 0,
      position = Position(index = 0, start = 0, end = sentence.tokens.sumBy { it.form.length + 1 }),
      tokens = sentence.tokens.mapIndexed { i, it ->
        BaseToken(
          id = i,
          form = it.form,
          position = Position(index = i, start = tokenPosition, end = nextPosition(it) - 1))
      }
    )
  }
}
