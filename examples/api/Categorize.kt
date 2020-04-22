/* Copyright 2020-present Simone Cangialosi. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package api

import com.kotlinnlp.ApiClient
import com.kotlinnlp.api.NlpServiceApi
import com.kotlinnlp.api.model.Categorization
import com.kotlinnlp.api.model.InputText

/**
 * Test the `categorize` method of the KotlinNLP APIs.
 *
 * Launch with the '-h' option for help about the command line arguments.
 */
fun main(args: Array<String>) {

  val parsedArgs = CommandLineArguments(args)
  val client = NlpServiceApi(ApiClient().setBasePath("http://${parsedArgs.host}:${parsedArgs.port}"))

  inputLoop { text ->

    val categorizations: List<Categorization> = client.categorize(InputText().text(text), false, false)

    println()

    categorizations.forEach {

      println("Domain: ${it.domain}")

      it.categories.forEachIndexed { i, cat ->
        println("\t- level $i: category index ${cat.id} [%.2f %%]".format(100.0 * cat.score))
      }
    }
  }
}
