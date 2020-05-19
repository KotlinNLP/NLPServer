/* Copyright 2020-present Simone Cangialosi. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package api

import com.jakewharton.picnic.Table
import com.jakewharton.picnic.TextAlignment
import com.jakewharton.picnic.table
import com.kotlinnlp.ApiClient
import com.kotlinnlp.api.NlpServiceApi
import com.kotlinnlp.api.model.*
import com.xenomachina.argparser.mainBody
import java.lang.RuntimeException

/**
 * Test the `parse` method of the KotlinNLP APIs.
 *
 * Launch with the '-h' option for help about the command line arguments.
 */
fun main(args: Array<String>) = mainBody {

  val parsedArgs = CommandLineArguments(args)
  val client = NlpServiceApi(ApiClient().setBasePath("http://${parsedArgs.host}:${parsedArgs.port}"))

  inputLoop {

    val analysis: LinguisticAnalysis = client.parse(InputText().text(it), true, false)

    println("\nLanguage detected: ${analysis.language.name} (%.1f %%)"
      .format(100.0 * analysis.language.distribution!!.first().score))

    analysis.sentences.forEach { sentence ->

      println("\n[${sentence.position.start} - ${sentence.position.end}] Sentence #${sentence.id}:"
        .format(sentence.score))

      println(buildTable(sentence))
    }
  }
}

/**
 * @param sentence a linguistic sentence
 *
 * @return a table with the tokens of the given sentence as rows
 */
private fun buildTable(sentence: LinguisticSentence): Table = table {

  cellStyle {
    border = true
  }

  header {
    row("ID", "Form", "Head", "Dependency", "POS")
    cellStyle {
      alignment = TextAlignment.MiddleCenter
    }
  }

  sentence.tokens
    .flatMap { tk -> if (tk is TokenComposite) tk.components.map { true to it } else listOf(false to tk) }
    .forEach { (isComponent, tk) ->
      row(*(tk as LinguisticToken).toFields(sentence = sentence, isComponent = isComponent))
    }
}

/**
 * @param sentence the sentence containing this token
 * @param isComponent whether this token is a component of a composite token
 *
 * @return the table fields representing this token
 */
private fun LinguisticToken.toFields(sentence: LinguisticSentence, isComponent: Boolean): Array<Any> = when (this) {
  is TokenWord -> buildFields(id = this.id, form = this.buildForm(isComponent), morpho = this.morphology.first(),
    dependency = this.dependency, sentence = sentence)
  is TokenTrace -> buildFields(id = this.id, form = this.buildForm(isComponent), morpho = this.morphology.first(),
    dependency = this.dependency, sentence = sentence)
  is TokenWordTrace -> buildFields(id = this.id, form = this.buildForm(isComponent), morpho = this.morphology.first(),
    dependency = this.dependency, sentence = sentence)
  else -> throw RuntimeException("Invalid token")
}

/**
 * Build the table fields representing the given single token (that is not composite).
 *
 * @param id the token ID
 * @param form the token form
 * @param morpho the token morphology
 * @param dependency the token dependency
 * @param sentence the sentence containing the token
 *
 * @return the table fields representing the given token
 */
private fun buildFields(
  id: Int,
  form: String,
  morpho: SingleMorphology,
  dependency: Dependency,
  sentence: LinguisticSentence
): Array<Any> {

  val headStr: String = dependency.head?.let { head ->
    "${sentence.tokens.first { it.id == head }.buildForm(false)} [$head]"
  } ?: "ROOT"

  return arrayOf(
    id,
    form,
    "$headStr (%.1f %%)".format(100.0 * dependency.attachmentScore),
    "${dependency.relation.value} (%.1f %%)".format(100.0 * dependency.relationScore),
    "${morpho.pos.value} (%.1f %%)".format(100.0 * morpho.score)
  )
}

/**
 * @param isComponent whether this token is a component of a composite token
 *
 * @return this token form
 */
private fun LinguisticToken.buildForm(isComponent: Boolean): String = if (isComponent) {

  when(this) {
    is TokenWord -> this.morphology.first().lemma
    is TokenWordTrace -> this.morphology.first().lemma
    is TokenTrace -> this.morphology.first().lemma
    else -> throw RuntimeException("Invalid token")
  }

} else {

  when(this) {
    is TokenWord -> this.form
    is TokenWordTrace -> this.form
    is TokenTrace -> "(-)"
    else -> throw RuntimeException("Invalid token")
  }
}
