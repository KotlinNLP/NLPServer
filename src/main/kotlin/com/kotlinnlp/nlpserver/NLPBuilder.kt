/* Copyright 2016-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.nlpserver

import com.kotlinnlp.correlator.helpers.TextComparator
import com.kotlinnlp.frameextractor.TextFramesExtractor
import com.kotlinnlp.frameextractor.TextFramesExtractorModel
import com.kotlinnlp.geolocation.dictionary.LocationsDictionary
import com.kotlinnlp.hanclassifier.HANClassifier
import com.kotlinnlp.hanclassifier.HANClassifierModel
import com.kotlinnlp.languagedetector.LanguageDetector
import com.kotlinnlp.languagedetector.LanguageDetectorModel
import com.kotlinnlp.languagedetector.utils.FrequencyDictionary
import com.kotlinnlp.languagedetector.utils.TextTokenizer
import com.kotlinnlp.morphologicalanalyzer.MorphologicalAnalyzer
import com.kotlinnlp.morphologicalanalyzer.dictionary.MorphologyDictionary
import com.kotlinnlp.neuralparser.helpers.preprocessors.MorphoPreprocessor
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
import org.apache.log4j.*
import java.io.File
import java.io.FileInputStream
import java.lang.RuntimeException

/**
 * Helper that builds the NLP components logging the loading steps.
 *
 * @param parsedArgs the parsed command line arguments
 */
internal class NLPBuilder(parsedArgs: CommandLineArguments) {

  /**
   * The logger of the [NLPBuilder].
   */
  private val logger = Logger.getLogger("NLP Builder").setAppender()

  /**
   * A language detector or null if the required arguments are not present.
   */
  val languageDetector: LanguageDetector? =
    if (parsedArgs.langDetectorModel != null && parsedArgs.cjkTokenizerModel != null)
      buildLanguageDetector(
        languageDetectorModelFilename = parsedArgs.langDetectorModel!!,
        cjkModelFilename = parsedArgs.cjkTokenizerModel!!,
        frequencyDictionaryFilename = parsedArgs.freqDictionary)
    else
      null

  /**
   * Tokenizers associated by language ISO 639-1 code or null if the required arguments are not present.
   */
  val tokenizers: Map<String, NeuralTokenizer>? = parsedArgs.tokenizerModelsDir?.let { buildTokenizers(it) }

  /**
   * LHR parsers associated by language ISO 639-1 code or null if the required arguments are not present.
   */
  val parsers: Map<String, LHRParser>? = parsedArgs.lhrParserModelsDir?.let { buildLHRParsers(it) }

  /**
   * Morphology dictionaries associated by language ISO 639-1 code (empty if the required arguments are not present).
   */
  private val morphoDicts: Map<String, MorphologyDictionary> =
    parsedArgs.morphoDictionaryDir?.let { buildMorphoDictionaries(it) } ?: mapOf()

  /**
   * Morphological preprocessors associated by language ISO 639-1 code (empty if the required arguments are not
   * present).
   */
  val morphoPreprocessors: Map<String, MorphoPreprocessor> = this.morphoDicts.mapValues { MorphoPreprocessor(it.value) }

  /**
   * Morphological analyzers associated by language ISO 639-1 code (empty if the required arguments are not
   * present).
   */
  val morphoAnalyzers: Map<String, MorphologicalAnalyzer> =
    this.morphoDicts.mapValues { MorphologicalAnalyzer(it.value) }

  /**
   * A locations dictionary or null if the required arguments are not present.
   */
  val locationsDictionary: LocationsDictionary? = parsedArgs.locationsDictionary?.let { buildLocationsDictionary(it) }

  /**
   * Frames extractors associated by domain name or null if the required arguments are not present.
   */
  val frameExtractors: Map<String, TextFramesExtractor>? = parsedArgs.frameExtractorModelsDir?.let {
    buildFrameExtractorsMap(frameExtractorModelsDir = it, embeddingsDir = parsedArgs.framesExtractorEmbeddingsDir)
  }

  /**
   * HAN classifiers associated by domain name or null if the required arguments are not present.
   */
  val hanClassifiers: Map<String, HANClassifier>? = parsedArgs.hanClassifierModelsDir?.let {
    buildHANClassifiersMap(hanClassifierModelsDir = it, embeddingsDir = parsedArgs.hanClassifierEmbeddingsDir)
  }

  /**
   * Generic word embeddings associated by language ISO 639-1 code or null if the required arguments are not present.
   */
  private val wordEmbeddings: Map<String, EmbeddingsMap<String>>? =
    parsedArgs.wordEmbeddingsDir?.let { buildWordEmbeddings(it) }

  /**
   * Terms blacklists for the comparison, associated by language ISO 639-1 code (empty if no blacklist is present).
   */
  private val comparisonBlacklists: Map<String, Set<String>> =
    parsedArgs.comparisonBlacklistsDir?.let { buildComparisonBlacklists(it) } ?: mapOf()

  /**
   * Text comparators associated by language ISO-639-1 code or null if the required arguments are not present.
   */
  val comparators: Map<String, TextComparator>? = buildComparators()

  /**
   * Build a [LanguageDetector].
   *
   * @param languageDetectorModelFilename the filename of the language detector model
   * @param cjkModelFilename the filename of the CJK tokenizer used by the language detector
   * @param frequencyDictionaryFilename the filename of the frequency dictionary (can be null)
   *
   * @return a language detector
   */
  private fun buildLanguageDetector(languageDetectorModelFilename: String,
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
   * Build a map of [NeuralTokenizer]s associated by language ISO 639-1 code.
   *
   * @param tokenizerModelsDir the directory containing the tokenizers models
   *
   * @return a map of tokenizers associated by language ISO 639-1 code
   */
  private fun buildTokenizers(tokenizerModelsDir: String): Map<String, NeuralTokenizer> =
    File(tokenizerModelsDir)
      .listFilesOrRaise()
      .also { this.logger.info("Loading tokenizers models from '$tokenizerModelsDir':") }
      .associate { modelFile ->

        this.logger.info("  loading '${modelFile.name}'...")
        val model = NeuralTokenizerModel.load(FileInputStream(modelFile))

        model.language.isoCode to NeuralTokenizer(model = model, useDropout = false)
      }

  /**
   * Build a map of [LHRParser]s associated by language ISO 639-1 code.
   *
   * @param lhrModelsDir the directory containing the LHR models
   *
   * @return a map of neural parsers associated by language ISO 639-1 code
   */
  private fun buildLHRParsers(lhrModelsDir: String): Map<String, LHRParser> =
    File(lhrModelsDir)
      .listFilesOrRaise()
      .also { this.logger.info("Loading parsers models from '$lhrModelsDir':") }
      .associate { modelFile ->

        this.logger.info("  loading '${modelFile.name}'...")
        val model: LHRModel = LHRModel.load(FileInputStream(modelFile))

        model.language.isoCode to LHRParser(model)
      }

  /**
   * Build a map of [TextFramesExtractor]s associated by domain name.
   *
   * @param frameExtractorModelsDir the directory containing the frame extractors models
   * @param embeddingsDir the directory containing the embeddings for the frames extractors (null if they are already
   *                      included in the models)
   *
   * @return a map of text frame extractors associated by domain name
   */
  private fun buildFrameExtractorsMap(frameExtractorModelsDir: String,
                                      embeddingsDir: String?): Map<String, TextFramesExtractor> {

    val embeddings: Map<String, EmbeddingsMap<String>>? = embeddingsDir?.let {
      this.logger.info("Loading frames extractors embeddings from '$embeddingsDir':")
      this.buildDomainEmbeddingsMap(it)
    }

    return File(frameExtractorModelsDir)
      .listFilesOrRaise()
      .also { this.logger.info("Loading frame extractors models from '$frameExtractorModelsDir':") }
      .associate { modelFile ->

        this.logger.info("  loading '${modelFile.name}'...")
        val extractor = TextFramesExtractor(model = TextFramesExtractorModel.load(FileInputStream(modelFile)))
        val domainName: String = extractor.model.name

        embeddings
          ?.getOrElse(domainName) { throw RuntimeException("Missing frames extractor embeddings for '$domainName'") }
          ?.let { extractor.setEmbeddings(it) }

        extractor.model.name to extractor
      }
  }

  /**
   * Build a map [HANClassifier]s associated by domain name.
   *
   * @param hanClassifierModelsDir the directory containing the HAN classifier models
   * @param embeddingsDir the directory containing the embeddings for the HAN classifiers (null if they are already
   *                      included in the classifiers models)
   *
   * @return a map of HAN classifiers associated by domain name
   */
  private fun buildHANClassifiersMap(hanClassifierModelsDir: String,
                                     embeddingsDir: String?): Map<String, HANClassifier> {

    val embeddings: Map<String, EmbeddingsMap<String>>? = embeddingsDir?.let {
      this.logger.info("Loading classifiers embeddings from '$embeddingsDir':")
      this.buildDomainEmbeddingsMap(it)
    }

    return File(hanClassifierModelsDir)
      .listFilesOrRaise()
      .also { this.logger.info("Loading classifiers models from '$hanClassifierModelsDir':") }
      .associate { modelFile ->

        this.logger.info("  loading '${modelFile.name}'...")
        val classifier = HANClassifier(model = HANClassifierModel.load(FileInputStream(modelFile)))
        val domainName: String = classifier.model.name

        embeddings
          ?.getOrElse(domainName) { throw RuntimeException("Missing classifier embeddings for '$domainName'") }
          ?.let { classifier.setEmbeddings(it) }

        domainName to classifier
      }
  }

  /**
   * Build a map of [MorphologyDictionary]s associated by language ISO 639-1 code.
   *
   * @param morphoDictionariesDir the directory containing the morphology dictionaries
   *
   * @return a map morphology dictionaries associated by language ISO 639-1 code
   */
  private fun buildMorphoDictionaries(morphoDictionariesDir: String): Map<String, MorphologyDictionary> =
    File(morphoDictionariesDir)
      .listFilesOrRaise()
      .also { this.logger.info("Loading morphology dictionaries from '$morphoDictionariesDir':") }
      .associate { dictionaryFile ->

        this.logger.info("  loading '${dictionaryFile.name}'...")
        val dictionary: MorphologyDictionary = MorphologyDictionary.load(FileInputStream(dictionaryFile))

        dictionary.language.isoCode to dictionary
      }

  /**
   * Load a serialized [LocationsDictionary] from file.
   *
   * @param locationsDictionaryFilename the filename of the serialized locations dictionary
   *
   * @return a locations dictionary
   */
  private fun buildLocationsDictionary(locationsDictionaryFilename: String): LocationsDictionary {

    this.logger.info("Loading locations dictionary from '$locationsDictionaryFilename'")

    return LocationsDictionary.load(FileInputStream(File(locationsDictionaryFilename)))
  }

  /**
   * Build a map of generic word embeddings, associated by language ISO 639-1 code.
   *
   * @param dirname the name of the directory containing the embeddings
   *
   * @return a map of generic word embeddings, associated by language ISO 639-1 code
   */
  private fun buildWordEmbeddings(dirname: String): Map<String, EmbeddingsMap<String>> =
    File(dirname)
      .listFilesOrRaise()
      .also { this.logger.info("Loading word embeddings from '$dirname':") }
      .associate { embeddingsFile ->

        this.logger.info("  loading '${embeddingsFile.name}'...")
        val embeddingsMap: EmbeddingsMap<String> =
          EmbeddingsMap.load(embeddingsFile.absolutePath.toString(), verbose = false)

        val language: String = embeddingsFile.nameWithoutExtension.substringAfterLast("__").toLowerCase()

        language to embeddingsMap
      }

  /**
   * Build a map of terms blacklists for the comparison, associated by language ISO 639-1 code.
   *
   * @param dirname the name of the directory containing the comparison blacklists
   *
   * @return a map of terms blacklists for the comparison, associated by language ISO 639-1 code
   */
  private fun buildComparisonBlacklists(dirname: String): Map<String, Set<String>> =
    File(dirname)
      .listFilesOrRaise()
      .also { this.logger.info("Loading comparison blacklists from '$dirname':") }
      .associate { file ->

        this.logger.info("  loading '${file.name}'...")

        val language: String = file.nameWithoutExtension.substringAfterLast("__").toLowerCase()

        language to file.readLines().toSet()
      }

  /**
   * @return a map of text comparators associated by language ISO-639-1 code or null if the required components are not
   *         present for any language
   */
  private fun buildComparators(): Map<String, TextComparator>? {

    if (this.parsers == null || this.wordEmbeddings == null || this.tokenizers == null) return null

    val languages: Set<String> = this.parsers.keys.intersect(this.morphoDicts.keys).intersect(this.wordEmbeddings.keys)

    return languages
      .associate {
        it to TextComparator(
          embeddings = this.wordEmbeddings.getValue(it),
          tokenizerModel = this.tokenizers.getValue(it).model,
          morphoDictionary = this.morphoDicts.getValue(it),
          parserModel = this.parsers.getValue(it).model,
          lemmasBlacklist = this.comparisonBlacklists[it] ?: setOf(),
          cacheEnabled = true
        )
      }
      .ifEmpty { null }
  }

  /**
   * Build a map of embeddings maps associated by domain name.
   *
   * @param embeddingsDir the directory containing the embeddings
   *
   * @return a map of embeddings maps associated by domain name
   */
  private fun buildDomainEmbeddingsMap(embeddingsDir: String): Map<String, EmbeddingsMap<String>> =
    File(embeddingsDir)
      .listFilesOrRaise()
      .associate { embeddingsFile ->

        this.logger.info("  loading '${embeddingsFile.name}'...")
        val embeddingsMap: EmbeddingsMap<String> =
          EmbeddingsMap.load(embeddingsFile.absolutePath.toString(), verbose = false)

        val domainName: String = embeddingsFile.nameWithoutExtension.substringAfterLast("__")

        domainName to embeddingsMap
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
      (this.model.tokensEncoder as EnsembleTokensEncoderModel)
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
