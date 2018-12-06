/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver

import com.kotlinnlp.frameextractor.FrameExtractor
import com.kotlinnlp.frameextractor.FrameExtractorModel
import com.kotlinnlp.geolocation.dictionary.LocationsDictionary
import com.kotlinnlp.hanclassifier.HANClassifier
import com.kotlinnlp.hanclassifier.HANClassifierModel
import com.kotlinnlp.languagedetector.LanguageDetector
import com.kotlinnlp.languagedetector.LanguageDetectorModel
import com.kotlinnlp.languagedetector.utils.FrequencyDictionary
import com.kotlinnlp.languagedetector.utils.TextTokenizer
import com.kotlinnlp.linguisticdescription.language.getLanguageByIso
import com.kotlinnlp.morphologicalanalyzer.dictionary.MorphologyDictionary
import com.kotlinnlp.neuralparser.parsers.lhrparser.LHRModel
import com.kotlinnlp.neuralparser.parsers.lhrparser.LHRParser
import com.kotlinnlp.neuraltokenizer.NeuralTokenizer
import com.kotlinnlp.neuraltokenizer.NeuralTokenizerModel
import com.kotlinnlp.simplednn.core.embeddings.EMBDLoader
import com.kotlinnlp.simplednn.core.embeddings.EmbeddingsMapByDictionary
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
    val modelsDirectory = File(tokenizerModelsDir)

    require(modelsDirectory.isDirectory) { "$tokenizerModelsDir is not a directory" }

    return modelsDirectory.listFilesOrRaise().associate { modelFile ->

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
   * Build a list of [FrameExtractor]s.
   *
   * @param frameExtractorModelsDir the directory containing the frame extractors models
   *
   * @return a map of frame extractors associated by domain name
   */
  fun buildFrameExtractorsMap(frameExtractorModelsDir: String): Map<String, FrameExtractor> {

    this.logger.info("Loading frame extractor models from '$frameExtractorModelsDir'")
    val frameExtractorsDir = File(frameExtractorModelsDir)

    require(frameExtractorsDir.isDirectory) { "$frameExtractorModelsDir is not a directory" }

    return frameExtractorsDir.listFiles().associate { modelFile ->

      this.logger.info("Loading '${modelFile.name}'...")
      val extractor = FrameExtractor(model = FrameExtractorModel.load(FileInputStream(modelFile)))

      extractor.model.name to extractor
    }
  }

  /**
   * Build a list of [HANClassifier]s.
   *
   * @param hanClassifierModelsDir the directory containing the HAN classifier models
   *
   * @return a map of HAN classifiers associated by domain name
   */
  fun buildHANClassifiersMap(hanClassifierModelsDir: String): Map<String, HANClassifier> {

    this.logger.info("Loading classifiers models from '$hanClassifierModelsDir'")
    val hanClassifiersDir = File(hanClassifierModelsDir)

    require(hanClassifiersDir.isDirectory) { "$hanClassifierModelsDir is not a directory" }

    return hanClassifiersDir.listFiles().associate { modelFile ->

      this.logger.info("Loading '${modelFile.name}'...")
      val classifier = HANClassifier(model = HANClassifierModel.load(FileInputStream(modelFile)))

      classifier.model.name to classifier
    }
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
    val morphoDictDir = File(morphoDictionariesDir)

    require(morphoDictDir.isDirectory) { "$morphoDictionariesDir is not a directory" }

    return morphoDictDir.listFilesOrRaise().associate { dictionaryFile ->

      this.logger.info("Loading '${dictionaryFile.name}'...")
      val dictionary: MorphologyDictionary = MorphologyDictionary.load(FileInputStream(dictionaryFile))

      dictionary.language.isoCode to dictionary
    }
  }

  /**
   * Build the map of languages ISO 639-1 codes to the related [MorphologyDictionary]s.
   *
   * @param embeddingsDirname the directory containing the embeddings vectors files, one per language
   *
   * @return a map of languages ISO 639-1 codes to the related [MorphologyDictionary]
   */
  fun buildEmbeddingsMapsByLanguage(embeddingsDirname: String): Map<String, EmbeddingsMapByDictionary> {

    this.logger.info("Loading embeddings from '$embeddingsDirname'")
    val embeddingsDir = File(embeddingsDirname)

    require(embeddingsDir.isDirectory) { "$embeddingsDirname is not a directory" }

    return embeddingsDir.listFilesOrRaise().associate { embeddingsFile ->

      this.logger.info("Loading '${embeddingsFile.name}'...")
      val embeddings: EmbeddingsMapByDictionary =
        EMBDLoader(verbose = false).load(embeddingsFile.absolutePath.toString())

      val langCode: String =
        getLanguageByIso(with (embeddingsFile.nameWithoutExtension) { substring(length - 2) }.toLowerCase()).isoCode

      langCode to embeddings
    }
  }

  /**
   * Build the map of domain names to the related [MorphologyDictionary]s.
   * Each embeddings file must be named with the format 'embeddings_DOMAIN_NAME' (excluding the extension, that is not
   * considered).
   *
   * @param embeddingsDirname the directory containing the embeddings vectors files, one per domain
   *
   * @return a map of domain names to the related [MorphologyDictionary]
   */
  fun buildEmbeddingsMapsByDomain(embeddingsDirname: String): Map<String, EmbeddingsMapByDictionary> {

    this.logger.info("Loading domain-specific embeddings from '$embeddingsDirname'")
    val embeddingsDir = File(embeddingsDirname)

    require(embeddingsDir.isDirectory) { "$embeddingsDirname is not a directory" }

    return embeddingsDir.listFilesOrRaise().associate { embeddingsFile ->

      this.logger.info("Loading '${embeddingsFile.name}'...")
      val embeddings: EmbeddingsMapByDictionary =
        EMBDLoader(verbose = false).load(embeddingsFile.absolutePath.toString())

      val domainName: String = embeddingsFile.nameWithoutExtension.substringAfter("embeddings_")

      domainName to embeddings
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
   * @throws RuntimeException if the file of this directory is empty
   *
   * @return the list of files contained in this directory if it is not empty, otherwise an exception is raised
   */
  private fun File.listFilesOrRaise(): Array<File> = this.listFiles().let {
    if (it.isNotEmpty()) it else throw RuntimeException("Empty directory.")
  }
}
