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
import com.kotlinnlp.conllio.Sentence as CoNLLSentence
import com.kotlinnlp.conllio.Token as CoNLLToken
import com.kotlinnlp.languagedetector.LanguageDetector
import com.kotlinnlp.linguisticdescription.InvalidLanguageCode
import com.kotlinnlp.linguisticdescription.language.Language
import com.kotlinnlp.neuralparser.NeuralParser
import com.kotlinnlp.neuralparser.helpers.preprocessors.BasePreprocessor
import com.kotlinnlp.neuralparser.helpers.preprocessors.MorphoPreprocessor
import com.kotlinnlp.neuralparser.parsers.lhrparser.LHRModel
import com.kotlinnlp.neuralparser.parsers.lhrparser.LHRParser
import com.kotlinnlp.neuralparser.parsers.lhrparser.LSSEncoder
import com.kotlinnlp.neuralparser.parsers.lhrparser.neuralmodules.contextencoder.ContextEncoder
import com.kotlinnlp.neuralparser.parsers.lhrparser.neuralmodules.headsencoder.HeadsEncoder
import com.kotlinnlp.neuralparser.parsers.lhrparser.utils.keyextractors.WordKeyExtractor
import com.kotlinnlp.neuraltokenizer.NeuralTokenizer
import com.kotlinnlp.neuraltokenizer.Sentence
import com.kotlinnlp.nlpserver.InvalidFrameExtractorDomain
import com.kotlinnlp.nlpserver.LanguageNotSupported
import com.kotlinnlp.nlpserver.MissingEmbeddingsMap
import com.kotlinnlp.nlpserver.commands.helpers.LSSEmbeddingsEncoder
import com.kotlinnlp.simplednn.core.embeddings.EmbeddingsMapByDictionary
import com.kotlinnlp.simplednn.simplemath.ndarray.dense.DenseNDArray
import com.kotlinnlp.tokensencoder.embeddings.EmbeddingsEncoder
import com.kotlinnlp.tokensencoder.embeddings.EmbeddingsEncoderModel

/**
 * The command executed on the route '/extract-frames'.

 * @param languageDetector a language detector (can be null)
 * @param tokenizers a map of neural tokenizers associated by language ISO 639-1 code
 * @param morphoPreprocessors a map of morpho-preprocessors associated by language ISO 639-1 code
 * @param parsers a map of neural parsers associated by language ISO 639-1 code
 * @param wordEmbeddings a map of pre-trained word embeddings maps associated by language ISO 639-1 code
 * @param frameExtractors a map of frame extractors associated by domain name
 */
class ExtractFrames(
  private val languageDetector: LanguageDetector?,
  private val tokenizers: Map<String, NeuralTokenizer>,
  private val morphoPreprocessors: Map<String, MorphoPreprocessor>,
  private val parsers: Map<String, NeuralParser<*>>,
  private val wordEmbeddings: Map<String, EmbeddingsMapByDictionary>,
  private val frameExtractors: Map<String, FrameExtractor>
) {

  /**
   * A base sentence preprocessor.
   */
  private val basePreprocessor = BasePreprocessor()

  /**
   * A map of LSS sentence encoders associated by ISO 639-1 language code.
   */
  private val sentenceEncoders: Map<String, LSSEmbeddingsEncoder> =
    this.parsers
      .filterValues { it is LHRParser }
      .mapValues { (langCode, parser) ->
        LSSEmbeddingsEncoder(
          preprocessor = this.morphoPreprocessors[langCode] ?: this.basePreprocessor,
          lssEncoder = (parser as LHRParser).model.buildLSSEncoder(),
          wordEmbeddingsEncoder = EmbeddingsEncoder(
            model = EmbeddingsEncoderModel(
              embeddingsMap = this.wordEmbeddings[langCode] ?: throw MissingEmbeddingsMap(langCode),
              embeddingKeyExtractor = WordKeyExtractor),
            useDropout = false))
      }

  /**
   * Extract frames from the given [text], eventually forcing on a given language and a given domain.
   *
   * @param text the text from which to extract frames
   * @param lang the language to use to analyze the [text] (default = unknown)
   * @param domain force to use the frame extractors associated to the given domain
   * @param prettyPrint pretty print, used for JSON format (default = false)
   *
   * @throws InvalidLanguageCode when the requested (or detected) language is not compatible
   * @throws InvalidFrameExtractorDomain when the given domain is not valid
   *
   * @return the list of frames extracted, in a JSON string
   */
  operator fun invoke(text: String,
                      lang: Language? = null,
                      domain: String? = null,
                      prettyPrint: Boolean = false): String {

    val textLanguage: Language = this.getTextLanguage(text = text, forcedLang = lang)
    val sentences: List<Sentence> = this.tokenizers.getValue(textLanguage.isoCode).tokenize(text)
    val sentenceEncoder: LSSEmbeddingsEncoder =
      this.sentenceEncoders[textLanguage.isoCode] ?: throw InvalidLanguageCode(textLanguage.isoCode)
    val extractors: List<FrameExtractor> = domain?.let {
      listOf(this.frameExtractors[it] ?: throw InvalidFrameExtractorDomain(domain))
    } ?: this.frameExtractors.values.toList()

    val jsonFrames = JsonObject(extractors.associate { extractor ->

      extractor.model.name to JsonArray(sentences.map {

        val tokensForms: List<String> = it.tokens.map { it.form }
        val tokenEncodings: List<DenseNDArray> = sentenceEncoder.encode(tokensForms)
        val output: FrameExtractor.Output = extractor.forward(tokenEncodings)

        json {
          obj(
            "intent" to output.buildIntent().toJSON(tokensForms),
            "distribution" to array(
              output.buildDistribution().map.entries
                .sortedByDescending { it.value }
                .map { obj("intent" to it.key, "score" to it.value) }
            )
          )
        }
      })
    })

    return jsonFrames.toJsonString(prettyPrint) + "\n"
  }

  /**
   * @param text the text to analyze (of which to detect the language if [forcedLang] is null)
   * @param forcedLang force this language to be returned (if it is supported)
   *
   * @throws LanguageNotSupported when the returning language is not supported
   * @throws RuntimeException when [forcedLang] is 'null' but the language detector is missing
   *
   * @return the language of the given [text]
   */
  private fun getTextLanguage(text: String, forcedLang: Language?): Language {

    return if (this.languageDetector == null) {

      if (forcedLang == null) {
        throw RuntimeException("Cannot determine language automatically (missing language detector)")

      } else {
        if (forcedLang.isoCode !in this.tokenizers) throw LanguageNotSupported(forcedLang.isoCode)

        forcedLang
      }

    } else {
      val lang: Language = forcedLang ?: this.languageDetector.detectLanguage(text)
      if (lang.isoCode !in this.tokenizers) throw LanguageNotSupported(lang.isoCode)

      lang
    }
  }

  /**
   * Build an [LSSEncoder] from this LHRModel.
   *
   * @return an encoder of Latent Syntactic Structures
   */
  private fun LHRModel.buildLSSEncoder() = LSSEncoder(
    tokensEncoderWrapper = this.tokensEncoderWrapperModel.buildWrapper(useDropout = false),
    contextEncoder = ContextEncoder(this.contextEncoderModel, useDropout = false),
    headsEncoder = HeadsEncoder(this.headsEncoderModel, useDropout = false),
    virtualRoot = this.rootEmbedding.array.values)
}
