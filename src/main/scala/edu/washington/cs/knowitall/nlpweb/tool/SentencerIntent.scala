package edu.washington.cs.knowitall
package nlpweb
package tool

import common.Timing
import unfiltered.request.HttpRequest
import edu.washington.cs.knowitall.tool.sentence.OpenNlpSentencer

object SentencerIntent extends ToolIntent("sentencer", List("opennlp")) {
  override val info = "Enter a single block of text (paragraph) to split into sentences."

  lazy val sentencers = Map(
    "opennlp" -> new OpenNlpSentencer())

  override def post[A](tool: String, text: String, params: Map[String, String]) = {
    val sentencer = sentencers(tool)

    val (sentencerTime, sentenced) = Timing.time(sentencer.segmentTexts(text))
    ("time: " + Timing.Milliseconds.format(sentencerTime),
    sentenced.map("<li>" + _).mkString("<ol>\n", "\n", "</ol>"))
  }
}
