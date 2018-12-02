/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver.commands

import com.beust.klaxon.JsonObject
import com.beust.klaxon.json
import com.kotlinnlp.hanclassifier.EncodedSentence
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
import com.kotlinnlp.nlpserver.MissingEmbeddingsMapByDomain
import com.kotlinnlp.nlpserver.commands.utils.TokenizingCommand
import com.kotlinnlp.nlpserver.commands.utils.buildSentence
import com.kotlinnlp.simplednn.core.embeddings.EmbeddingsMapByDictionary
import com.kotlinnlp.simplednn.core.functionalities.activations.Tanh
import com.kotlinnlp.simplednn.simplemath.ndarray.dense.DenseNDArray
import com.kotlinnlp.tokensencoder.embeddings.EmbeddingsEncoderModel
import com.kotlinnlp.tokensencoder.embeddings.keyextractor.NormWordKeyExtractor
import com.kotlinnlp.tokensencoder.reduction.ReductionEncoder
import com.kotlinnlp.tokensencoder.reduction.ReductionEncoderModel

/**
 * The command executed on the route '/categorize'.

 * @param languageDetector a language detector (can be null)
 * @param tokenizers a map of neural tokenizers associated by language ISO 639-1 code
 * @param domainEmbeddings a map of domain-specific pre-trained word embeddings maps associated by domain name
 * @param hanClassifiers a map of HAN classifier associated by domain name
 */
class Categorize(
  override val languageDetector: LanguageDetector?,
  override val tokenizers: Map<String, NeuralTokenizer>,
  private val domainEmbeddings: Map<String, EmbeddingsMapByDictionary>,
  private val hanClassifiers: Map<String, HANClassifier>
) : TokenizingCommand {

  /**
   * A HAN classifier associated to the related tokens encoder.
   *
   * @property classifier a HAN classifier of a certain domain
   * @property encoder the reduction tokens encoder for the same domain
   */
  private data class ClassifierEncoder(
    val classifier: HANClassifier,
    val encoder: ReductionEncoder<FormToken, Sentence<FormToken>>
  )

  /**
   * A map of pairs <classifier, encoder> associated by domain.
   */
  private val classifierEncoders: Map<String, ClassifierEncoder> =
    this.hanClassifiers.mapValues { (domain, classifier) ->

      val embeddingsMap = this.domainEmbeddings[domain] ?: throw MissingEmbeddingsMapByDomain(domain)
      val tokensEncoder = ReductionEncoder<FormToken, Sentence<FormToken>>(
        model = ReductionEncoderModel(
          inputEncoderModel = EmbeddingsEncoderModel(
            embeddingsMap = embeddingsMap,
            embeddingKeyExtractor = NormWordKeyExtractor()),
          tokenEncodingSize = 50,
          activationFunction = Tanh()),
        useDropout = false)


      ClassifierEncoder(classifier = classifier, encoder = tokensEncoder)
    }

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

    val textLanguage: Language = this.getTextLanguage(text = text, forcedLang = lang)
    val sentences: List<TokenizerSentence> = this.tokenizers.getValue(textLanguage.isoCode).tokenize(text)
    val classifiersEncoders: List<ClassifierEncoder> = domain?.let {
      listOf(this.classifierEncoders[domain] ?: throw InvalidDomain(domain))
    } ?: this.classifierEncoders.values.toList()

    val outputPerDomain = JsonObject(classifiersEncoders.associate { classifierEncoder ->

      classifierEncoder.classifier.model.name to json {

        val textEncodings: List<EncodedSentence> = sentences.map { sentence ->
          EncodedSentence(
            tokens = classifierEncoder.encoder.forward(buildSentence(forms = sentence.tokens.map { it.form })))
        }
        val predictions: List<DenseNDArray> = classifierEncoder.classifier.classify(textEncodings)

        array(predictions.map {

          val jsonCat: JsonObject = obj("category" to it.argMaxIndex())

          if (distribution) jsonCat["distribution"] = array(it.toDoubleArray().toList())

          jsonCat
        })
      }
    })

    return outputPerDomain.toJsonString(prettyPrint) + "\n"
  }
}
