/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver

import com.kotlinnlp.frameextractor.TextFramesExtractor
import com.kotlinnlp.frameextractor.TextFramesExtractorModel
import com.kotlinnlp.geolocation.dictionary.LocationsDictionary
import com.kotlinnlp.hanclassifier.HANClassifier
import com.kotlinnlp.hanclassifier.HANClassifierModel
import com.kotlinnlp.languagedetector.LanguageDetector
import com.kotlinnlp.languagedetector.LanguageDetectorModel
import com.kotlinnlp.languagedetector.utils.FrequencyDictionary
import com.kotlinnlp.languagedetector.utils.TextTokenizer
import com.kotlinnlp.morphologicalanalyzer.dictionary.MorphologyDictionary
import com.kotlinnlp.neuralparser.parsers.lhrparser.LHRModel
import com.kotlinnlp.neuralparser.parsers.lhrparser.LHRParser
import com.kotlinnlp.neuraltokenizer.NeuralTokenizer
import com.kotlinnlp.neuraltokenizer.NeuralTokenizerModel
import com.kotlinnlp.simplednn.core.embeddings.EmbeddingsMap
import com.kotlinnlp.tokensencoder.embeddings.EmbeddingsEncoderModel
import com.kotlinnlp.tokensencoder.ensemble.EnsembleTokensEncoderModel
import com.kotlinnlp.tokensencoder.reduction.ReductionEncoderModel
import com.kotlinnlp.tokensencoder.wrapper.TokensEncoderWrapperModel
import com.kotlinnlp.utils.notEmptyOr
import java.io.File
import java.io.FileInputStream
import java.lang.RuntimeException
import java.util.logging.Logger

/**
 * Helper that builds various NLP components logging the loading steps.
 */
object NLPBuilder {

  /**
   * The logger of the [NLPBuilder].
   */
  private val logger = Logger.getLogger("NLP Builder")

  /**
   * Build a [LanguageDetector].
   *
   * @param languageDetectorModelFilename the filename of the language detector model
   * @param cjkModelFilename the filename of the CJK tokenizer used by the language detector
   * @param frequencyDictionaryFilename the filename of the frequency dictionary (can be null)
   *
   * @return a language detector
   */
  fun buildLanguageDetector(languageDetectorModelFilename: String,
                            cjkModelFilename: String,
                            frequencyDictionaryFilename: String?): LanguageDetector {

    this.logger.info("Loading language detector model from '$languageDetectorModelFilename'")
    val model = LanguageDetectorModel.load(FileInputStream(File(languageDetectorModelFilename)))

    this.logger.info("Loading CJK tokenizer model from '$cjkModelFilename'")
    val tokenizer = TextTokenizer(cjkModel = NeuralTokenizerModel.load(FileInputStream(File(cjkModelFilename))))

    val freqDictionary = if (frequencyDictionaryFilename != null) {
      this.logger.info("Loading frequency dictionary from '$frequencyDictionaryFilename'")
      FrequencyDictionary.load(FileInputStream(File(frequencyDictionaryFilename)))
    } else {
      this.logger.info("No frequency dictionary used to detect the language")
      null
    }

    return LanguageDetector(model = model, tokenizer = tokenizer, frequencyDictionary = freqDictionary)
  }

  /**
   * Build the map of languages ISO 639-1 codes to the related [NeuralTokenizer]s.
   *
   * @param tokenizerModelsDir the directory containing the tokenizers models
   *
   * @return a map of languages ISO 639-1 codes to the related [NeuralTokenizer]s
   */
  fun buildTokenizers(tokenizerModelsDir: String): Map<String, NeuralTokenizer> {

    this.logger.info("Loading tokenizer models from '$tokenizerModelsDir'")

    return File(tokenizerModelsDir).listFilesOrRaise().associate { modelFile ->

      this.logger.info("Loading '${modelFile.name}'...")
      val model = NeuralTokenizerModel.load(FileInputStream(modelFile))

      model.language.isoCode to NeuralTokenizer(model = model, useDropout = false)
    }
  }

  /**
   * Build the map of languages ISO 639-1 codes to the related [LHRParser]s.
   *
   * @param lhrModelsDir the directory containing the LHR models
   *
   * @return a map of languages ISO 639-1 codes to the related [LHRParser]s
   */
  fun buildLHRParsers(lhrModelsDir: String): Map<String, LHRParser> {

    this.logger.info("Loading LHR models from '$lhrModelsDir'")
    val modelsDirectory = File(lhrModelsDir)

    require(modelsDirectory.isDirectory) { "$lhrModelsDir is not a directory" }

    return modelsDirectory.listFiles().associate { modelFile ->

      this.logger.info("Loading '${modelFile.name}'...")
      val model: LHRModel = LHRModel.load(FileInputStream(modelFile))

      model.language.isoCode to LHRParser(model)
    }
  }

  /**
   * Build a map of domain names to the related [TextFramesExtractor]s.
   *
   * @param frameExtractorModelsDir the directory containing the frame extractors models
   * @param embeddingsDir the directory containing the embeddings for the frames extractors (null if they are already
   *                      included in the models)
   *
   * @return a map of text frame extractors associated by domain name
   */
  fun buildFrameExtractorsMap(frameExtractorModelsDir: String,
                              embeddingsDir: String?): Map<String, TextFramesExtractor> {

    this.logger.info("Loading frame extractor models from '$frameExtractorModelsDir'")
    val frameExtractorsDir = File(frameExtractorModelsDir)

    require(frameExtractorsDir.isDirectory) { "$frameExtractorModelsDir is not a directory" }

    val embeddings: Map<String, EmbeddingsMap<String>>? = embeddingsDir?.let {
      this.logger.info("Loading frames extractor embeddings from '$embeddingsDir'")
      this.buildDomainEmbeddingsMap(it)
    }

    return frameExtractorsDir.listFiles().associate { modelFile ->

      this.logger.info("Loading '${modelFile.name}'...")
      val extractor = TextFramesExtractor(model = TextFramesExtractorModel.load(FileInputStream(modelFile)))
      val domainName: String = extractor.model.name

      embeddings
        ?.getOrElse(domainName) { throw RuntimeException("Missing frames extractor embeddings for '$domainName'") }
        ?.let { extractor.setEmbeddings(it) }

      extractor.model.name to extractor
    }
  }

  /**
   * Build a map of domain names to the related [HANClassifier]s.
   *
   * @param hanClassifierModelsDir the directory containing the HAN classifier models
   * @param embeddingsDir the directory containing the embeddings for the HAN classifiers (null if they are already
   *                      included in the classifiers models)
   *
   * @return a map of HAN classifiers associated by domain name
   */
  fun buildHANClassifiersMap(hanClassifierModelsDir: String,
                             embeddingsDir: String?): Map<String, HANClassifier> {

    this.logger.info("Loading classifiers models from '$hanClassifierModelsDir'")
    val hanClassifiersDir = File(hanClassifierModelsDir).also {
      require(it.isDirectory) { "$hanClassifierModelsDir is not a directory" }
    }

    val embeddings: Map<String, EmbeddingsMap<String>>? = embeddingsDir?.let {
      this.logger.info("Loading classifiers embeddings from '$embeddingsDir'")
      this.buildDomainEmbeddingsMap(it)
    }

    return hanClassifiersDir.listFilesOrRaise().associate { modelFile ->

      this.logger.info("Loading '${modelFile.name}'...")
      val classifier = HANClassifier(model = HANClassifierModel.load(FileInputStream(modelFile)))
      val domainName: String = classifier.model.name

      embeddings
        ?.getOrElse(domainName) { throw RuntimeException("Missing classifier embeddings for '$domainName'") }
        ?.let { classifier.setEmbeddings(it) }

      domainName to classifier
    }
  }

  /**
   * Build a map of generic word embeddings, associated by language ISO 639-1 code.
   *
   * @param dirname the name of the directory containing the embeddings
   *
   * @return a map of generic word embeddings, associated by language ISO 639-1 code
   */
  fun buildWordEmbeddings(dirname: String): Map<String, EmbeddingsMap<String>> =

    File(dirname)
      .also { require(it.isDirectory) { "$dirname is not a directory" } }
      .listFilesOrRaise()
      .associate { embeddingsFile ->

        this.logger.info("Loading word embeddings from '${embeddingsFile.name}'...")
        val embeddingsMap: EmbeddingsMap<String> =
          EmbeddingsMap.load(embeddingsFile.absolutePath.toString(), verbose = false)

        val language: String = embeddingsFile.nameWithoutExtension.substringAfterLast("__").toLowerCase()

        language to embeddingsMap
      }

  /**
   * Build the map of languages ISO 639-1 codes to the related [MorphologyDictionary]s.
   *
   * @param morphoDictionariesDir the directory containing the morphology dictionaries
   *
   * @return a map of languages ISO 639-1 codes to the related [MorphologyDictionary]
   */
  fun buildMorphoDictionaries(morphoDictionariesDir: String): Map<String, MorphologyDictionary> {

    this.logger.info("Loading morphology dictionaries from '$morphoDictionariesDir'")

    return File(morphoDictionariesDir).listFilesOrRaise().associate { dictionaryFile ->

      this.logger.info("Loading '${dictionaryFile.name}'...")
      val dictionary: MorphologyDictionary = MorphologyDictionary.load(FileInputStream(dictionaryFile))

      dictionary.language.isoCode to dictionary
    }
  }

  /**
   * Load a serialized [LocationsDictionary] from file.
   *
   * @param locationsDictionaryFilename the filename of the serialized locations dictionary
   *
   * @return a locations dictionary
   */
  fun buildLocationsDictionary(locationsDictionaryFilename: String): LocationsDictionary {

    this.logger.info("Loading locations dictionary from '$locationsDictionaryFilename'")

    return LocationsDictionary.load(FileInputStream(File(locationsDictionaryFilename)))
  }

  /**
   * Build a map of domain names to the related embeddings map.
   *
   * @param embeddingsDir the directory containing the embeddings
   *
   * @return a map of embeddings maps associated by domain name
   */
  private fun buildDomainEmbeddingsMap(embeddingsDir: String): Map<String, EmbeddingsMap<String>> {

    return File(embeddingsDir).listFilesOrRaise().associate { embeddingsFile ->

      this.logger.info("Loading '${embeddingsFile.name}'...")
      val embeddingsMap: EmbeddingsMap<String> =
        EmbeddingsMap.load(embeddingsFile.absolutePath.toString(), verbose = false)

      val domainName: String = embeddingsFile.nameWithoutExtension.substringAfterLast("__")

      domainName to embeddingsMap
    }
  }

  /**
   * Set a given embeddings map into this HAN classifier.
   * The classifier must use a reduction tokens encoder with a transient embeddings encoder.
   *
   * @param embeddingsMap an embeddings map
   */
  private fun HANClassifier.setEmbeddings(embeddingsMap: EmbeddingsMap<String>) {

    val inputTokensEncoder: EmbeddingsEncoderModel.Transient<*, *> =
      (this.model.tokensEncoder as ReductionEncoderModel).inputEncoderModel as EmbeddingsEncoderModel.Transient

    inputTokensEncoder.setEmbeddingsMap(embeddingsMap)
  }

  /**
   * Set a given embeddings map into this Text Frames Extractor.
   * The extractor must use an ensemble tokens encoder with an embeddings encoder with a transient embeddings encoder as
   * first component.
   *
   * @param embeddingsMap an embeddings map
   */
  private fun TextFramesExtractor.setEmbeddings(embeddingsMap: EmbeddingsMap<String>) {

    val firstEncoder: TokensEncoderWrapperModel<*, *, *, *> =
      (model.tokensEncoder as EnsembleTokensEncoderModel)
        .components.first().model as TokensEncoderWrapperModel<*, *, *, *>

    (firstEncoder.model as EmbeddingsEncoderModel.Transient).setEmbeddingsMap(embeddingsMap)
  }

  /**
   * @throws IllegalArgumentException if this is not a directory
   * @throws RuntimeException if the file of this directory is empty
   *
   * @return the list of files contained in this directory if it is not empty, otherwise an exception is raised
   */
  private fun File.listFilesOrRaise(): List<File> {

    require(this.isDirectory) { "${this.name} is not a directory" }

    return this.listFiles().toList().notEmptyOr { throw RuntimeException("Empty directory.") }
  }
}
