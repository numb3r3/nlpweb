package edu.washington.cs.knowitall.nlpweb

import unfiltered.request.Path
import unfiltered.request.Seg
import unfiltered.request.GET
import unfiltered.response.ResponseString
import unfiltered.filter.Intent
import unfiltered.jetty.Http
import unfiltered.response.Ok
import unfiltered.scalate.Scalate
import unfiltered.jetty.ContextBuilder
import edu.washington.cs.knowitall.nlpweb.tool._

object NlpWeb extends App {
  val tools = Iterable(
    new StemmerIntent,
    new TokenizerIntent,
    new PostaggerIntent,
    new ChunkerIntent,
    new ParserIntent,
    new SentencerIntent,
    new ExtractorIntent,
    new ConstituencyParserIntent
  ).map(intent => (intent.path, intent)).toMap

  def first = Intent {
    case GET(Path("/")) => ResponseString("slash")
    case GET(Path("/foo")) => ResponseString("foo")
    case GET(Path("/bar")) => ResponseString("bar")
  }

  val intent = tools.values.map(_.intent).reduce(_ orElse _) orElse first

  val plan = new unfiltered.filter.Planify(intent)

  println("starting...")
  Http(8089).context("/public") { ctx: ContextBuilder =>
    ctx.resources(this.getClass.getResource("/pub"))
  }.filter(plan).run()
}
