/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver.commands

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.json
import com.kotlinnlp.frameextractor.FrameExtractor
import com.kotlinnlp.languagedetector.LanguageDetector
import com.kotlinnlp.linguisticdescription.InvalidLanguageCode
import com.kotlinnlp.linguisticdescription.language.Language
import com.kotlinnlp.linguisticdescription.sentence.token.FormToken
import com.kotlinnlp.linguisticdescription.sentence.Sentence
import com.kotlinnlp.lssencoder.LSSModel
import com.kotlinnlp.neuralparser.helpers.preprocessors.BasePreprocessor
import com.kotlinnlp.neuralparser.helpers.preprocessors.MorphoPreprocessor
import com.kotlinnlp.neuralparser.language.ParsingSentence
import com.kotlinnlp.neuralparser.language.ParsingToken
import com.kotlinnlp.neuraltokenizer.NeuralTokenizer
import com.kotlinnlp.neuraltokenizer.Sentence as TokenizerSentence
import com.kotlinnlp.nlpserver.InvalidDomain
import com.kotlinnlp.nlpserver.MissingEmbeddingsMapByLanguage
import com.kotlinnlp.nlpserver.commands.utils.TokenizingCommand
import com.kotlinnlp.nlpserver.commands.utils.buildSentence
import com.kotlinnlp.nlpserver.commands.utils.buildTokensEncoder
import com.kotlinnlp.simplednn.core.embeddings.EmbeddingsMapByDictionary
import com.kotlinnlp.simplednn.simplemath.ndarray.dense.DenseNDArray
import com.kotlinnlp.tokensencoder.TokensEncoder

/**
 * The command executed on the route '/extract-frames'.

 * @param languageDetector a language detector (can be null)
 * @param tokenizers a map of neural tokenizers associated by language ISO 639-1 code
 * @param morphoPreprocessors a map of morpho-preprocessors associated by language ISO 639-1 code
 * @param lssModels a map of LSS encoders models associated by language ISO 639-1 code
 * @param wordEmbeddings a map of pre-trained word embeddings maps associated by language ISO 639-1 code
 * @param frameExtractors a map of frame extractors associated by domain name
 */
class ExtractFrames(
  override val languageDetector: LanguageDetector?,
  override val tokenizers: Map<String, NeuralTokenizer>,
  private val morphoPreprocessors: Map<String, MorphoPreprocessor>,
  private val lssModels: Map<String, LSSModel<ParsingToken, ParsingSentence>>,
  private val wordEmbeddings: Map<String, EmbeddingsMapByDictionary>,
  private val frameExtractors: Map<String, FrameExtractor>
) : TokenizingCommand {

  /**
   * A base sentence preprocessor.
   */
  private val basePreprocessor = BasePreprocessor()

  /**
   * A map of LSS sentence encoders associated by ISO 639-1 language code.
   */
  private val tokensEncoders: Map<String, TokensEncoder<FormToken, Sentence<FormToken>>> =
    this.lssModels
      .mapValues { (langCode, lssModel) ->
        buildTokensEncoder(
          preprocessor = this.morphoPreprocessors[langCode] ?: this.basePreprocessor,
          embeddingsMap = this.wordEmbeddings[langCode] ?: throw MissingEmbeddingsMapByLanguage(langCode),
          lssModel = lssModel)
      }

  /**
   * Extract frames from the given [text], eventually forcing on a given language and a given domain.
   *
   * @param text the text from which to extract frames
   * @param lang the language to use to analyze the [text] (default = unknown)
   * @param domain force to use the frame extractors associated to the given domain
   * @param distribution whether to include the distribution in the response (default = true)
   * @param prettyPrint pretty print, used for JSON format (default = false)
   *
   * @throws InvalidLanguageCode when the requested (or detected) language is not compatible
   * @throws InvalidDomain when the given domain is not valid
   *
   * @return the list of frames extracted, in a JSON string
   */
  operator fun invoke(text: String,
                      lang: Language? = null,
                      domain: String? = null,
                      distribution: Boolean = true,
                      prettyPrint: Boolean = false): String {

    this.checkText(text)

    val textLanguage: Language = this.getTextLanguage(text = text, forcedLang = lang)
    val sentences: List<TokenizerSentence> = this.tokenizers.getValue(textLanguage.isoCode).tokenize(text)
    val tokensEncoder: TokensEncoder<FormToken, Sentence<FormToken>> =
      this.tokensEncoders[textLanguage.isoCode] ?: throw InvalidLanguageCode(textLanguage.isoCode)
    val extractors: List<FrameExtractor> = domain?.let {
      listOf(this.frameExtractors[it] ?: throw InvalidDomain(domain))
    } ?: this.frameExtractors.values.toList()

    val jsonFrames = JsonObject(extractors.associate { extractor ->

      extractor.model.name to JsonArray(sentences.map { sentence ->

        val tokensForms: List<String> = sentence.tokens.map { it.form }
        val tokenEncodings: List<DenseNDArray> = tokensEncoder.forward(buildSentence(tokensForms))
        val output: FrameExtractor.Output = extractor.forward(tokenEncodings)

        json {
          val jsonObj: JsonObject = obj("intent" to output.buildIntent().toJSON(tokensForms))

          if (distribution) jsonObj["distribution"] = array(
            output.buildDistribution().map.entries
              .asSequence()
              .sortedByDescending { it.value }
              .map { obj("intent" to it.key, "score" to it.value) }
              .toList())

          jsonObj
        }
      })
    })

    return jsonFrames.toJsonString(prettyPrint) + if (prettyPrint) "\n" else ""
  }
}
