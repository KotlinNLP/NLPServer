/* Copyright 2020-present Simone Cangialosi. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package api

import com.kotlinnlp.ApiClient
import com.kotlinnlp.api.NlpServiceApi
import com.kotlinnlp.api.model.ExtractFramesResponse
import com.kotlinnlp.api.model.InputText
import com.kotlinnlp.api.model.NLPSlot
import com.kotlinnlp.api.model.TokenizedSentence

/**
 * Test the `extract-frames` method of the KotlinNLP APIs.
 *
 * Launch with the '-h' option for help about the command line arguments.
 */
fun main(args: Array<String>) {

  val parsedArgs = CommandLineArguments(args)
  val client = NlpServiceApi(ApiClient().setBasePath("http://${parsedArgs.host}:${parsedArgs.port}"))

  inputLoop {

    val sentences: List<TokenizedSentence> = client.tokenize(InputText().text(it), false)
    val framesPerDomain: List<ExtractFramesResponse> = client.extractFrames(InputText().text(it), null, false, false)

    framesPerDomain.forEach { res ->

      println("\nDomain: ${res.domain}")

      res.sentences.zip(sentences).forEachIndexed { i, (frame, sentence) ->
        println()
        println("\tSentence #$i")
        println("\tIntent: ${frame.intent.name}")
        println("\tSlots: ${buildSlotsStr(slots = frame.intent.slots, sentence = sentence)}")
      }
    }
  }
}

/**
 * @param slots the slots of a frame
 * @param sentence the tokenized sentence that contains the frame
 *
 * @return the string representation of the given slots
 */
private fun buildSlotsStr(slots: List<NLPSlot>, sentence: TokenizedSentence): String = if (slots.isNotEmpty())
  slots.joinToString(" || ") { slot -> buildSlotStr(slot = slot, sentence = sentence) }
else
  "None"

/**
 * @param slot a frame slot
 * @param sentence the tokenized sentence that contains the slot
 *
 * @return the string representation of the given slot
 */
private fun buildSlotStr(slot: NLPSlot, sentence: TokenizedSentence): String {

  val form: String = slot.tokens.joinToString(" ") { tk -> sentence.tokens[tk.index].form }

  return "${slot.name} `$form` (%.1f %%)".format(100.0 * slot.tokens.map { it.score }.average())
}
