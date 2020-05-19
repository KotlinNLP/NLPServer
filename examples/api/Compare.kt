/* Copyright 2020-present Simone Cangialosi. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package api

import com.kotlinnlp.ApiClient
import com.kotlinnlp.api.NlpServiceApi
import com.kotlinnlp.api.model.ComparingTexts
import com.kotlinnlp.api.model.InputText
import com.kotlinnlp.api.model.ScoredId
import com.xenomachina.argparser.mainBody
import java.lang.RuntimeException

/**
 * Test the `compare` method of the KotlinNLP APIs.
 *
 * Launch with the '-h' option for help about the command line arguments.
 */
fun main(args: Array<String>) = mainBody {

  val parsedArgs = CommandLineArguments(args)
  val client = NlpServiceApi(ApiClient().setBasePath("http://${parsedArgs.host}:${parsedArgs.port}"))

  inputLoop { text ->

    print("Now the comparing text: ")
    val comparingText: String = readInput() ?: throw RuntimeException("Empty comparing text")

    val apiInput = ComparingTexts().text(text).comparing(listOf(InputText().text(comparingText)))
    val comparison: List<ScoredId> = client.compare(apiInput, false)

    println("\nComparison score: %.2f %%".format(100.0 * comparison.single().score))
  }
}
