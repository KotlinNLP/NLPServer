/* Copyright 2020-present Simone Cangialosi. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package api

import com.kotlinnlp.ApiClient
import com.kotlinnlp.api.NlpServiceApi
import com.kotlinnlp.api.model.InputText
import com.kotlinnlp.api.model.Summary

/**
 * Test the `summarize` method of the KotlinNLP APIs.
 *
 * Launch with the '-h' option for help about the command line arguments.
 */
fun main(args: Array<String>) {

  val parsedArgs = CommandLineArguments(args)
  val client = NlpServiceApi(ApiClient().setBasePath("http://${parsedArgs.host}:${parsedArgs.port}"))

  inputLoop { text ->

    val summary: Summary = client.summarize(InputText().text(text), false)

    println("\nSalience distribution:")
    println(formatSalience(summary.salience))

    println("\nRelevant itemsets:")
    summary.itemsets.forEach {
      println("\t" + formatScoredText(text = it.text, score = it.score))
    }

    println("\nRelevant keywords:")
    summary.keywords.forEach {
      println("\t" + formatScoredText(text = it.keyword, score = it.score))
    }

    println("\nSentences relevance:")
    summary.sentences.forEach {
      println("\t" + formatScoredText(text = it.text, score = it.score))
    }
  }
}

/**
 * @param salience the distribution of the salience scores in buckets with interval 0.1
 *
 * @return the scores distribution formatted to a string
 */
private fun formatSalience(salience: List<Double>): String =
  salience.indices.joinToString(separator = "\n\t", prefix = "\t") { i ->
    "[>= %.1f] %.1f%%".format(i / 10.0, 100.0 * salience.takeLast(10 - i).sum())
  }

/**
 * @param text a text
 * @param score the text score
 *
 * @return the scored text formatted to a string
 */
private fun formatScoredText(text: String, score: Double): String = "[%.1f%%] $text".format(100.0 * score)
