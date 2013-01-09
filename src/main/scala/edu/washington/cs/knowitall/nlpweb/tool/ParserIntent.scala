package edu.washington.cs.knowitall
package nlpweb
package tool

import scala.collection.JavaConversions.asJavaCollection
import common.Timing
import edu.washington.cs.knowitall.tool.parse.{DependencyParser, MaltParser, StanfordParser}
import edu.washington.cs.knowitall.tool.parse.graph.{DependencyGraph, DependencyPattern}
import edu.washington.cs.knowitall.tool.stem.MorphaStemmer
import unfiltered.request.HttpRequest

object ParserIntent extends ToolIntent("parser", List("malt", "stanford", "deserialize")) {
  implicit def stemmer = MorphaStemmer
  override val info = "Enter a single sentence to be parsed."

  lazy val stanfordParser = new StanfordParser()
  lazy val maltParser = new MaltParser()
  lazy val deserializeParser = new DependencyParser {
    override def dependencyGraph(pickled: String) =
      DependencyGraph.deserialize(pickled)

    override def dependencies(pickled: String) =
      DependencyGraph.deserialize(pickled).dependencies
  }

  val parsers = tools
  def getParser(parser: String): DependencyParser =
    parser match {
      case "stanford" => stanfordParser
      case "malt" => maltParser
      case "deserialize" => deserializeParser
    }

  override def config[A](req: unfiltered.request.HttpRequest[A], tool: String) = {
    val pattern =
      if (req.parameterNames contains "pattern") req.parameterValues("pattern").headOption
      else None
    config(
      pattern,
      req.parameterNames.contains("collapsed"),
      req.parameterNames.contains("collapseNounGroups"),
      req.parameterNames.contains("collapsePrepOf"),
      req.parameterNames.contains("collapseWeakLeaves"))
  }

  def config(pattern: Option[String], collapsed: Boolean, collapseNounGroups: Boolean, collapsePrepOf: Boolean, collapseWeakLeaves: Boolean): String = """
    pattern: <input name="pattern" type="input" size="60" value="""" + pattern.getOrElse("") + """" /><br />
    <input name="collapsed" type="checkbox" value="true" """ + (if (true) """checked="true" """ else "") + """ /> Collapsed<br />
    <input name="collapseNounGroups" type="checkbox" value="true" """ + (if (true) """checked="true" """ else "") + """/> Collapse Noun Groups<br />
    <input name="collapsePrepOf" type="checkbox" value="true" """ + (if (true) """checked="true" """ else "") + """/> Collapse Prep Of<br />
    <input name="collapseWeakLeaves" type="checkbox" value="true" """ + (if (true) """checked="true" """ else "") + """/> Collapse Weak Leaves<br />
    <br />"""

  override def post[A](tool: String, text: String, params: Map[String, String]) = {
    val parser = getParser(tool)
    val pattern = ""
    var (parseTime, graph) = parser.synchronized {
      Timing.time(
        parser match {
          case parser: DependencyParser =>
            val graph = parser.dependencyGraph(text)
            if (/*params.getOrElse("collapsed", "")*/"true" == "true")
              graph.collapse
            else
              graph
        })
    }

    if (params.get("collapseNounGroups").getOrElse("") == "true") {
      graph = graph.collapseNounGroups()
    }

    if (params.get("collapsePrepOf").getOrElse("") == "true") {
      graph = graph.collapseNNPOf
    }

    if (params.get("collapseWeakLeaves").getOrElse("") == "true") {
      graph = graph.collapseWeakLeaves
    }

    val (nodes, edges) = if ((params.keys contains "pattern") && !params("pattern").isEmpty) {
      val pattern = DependencyPattern.deserialize(params("pattern").trim)
      val matches = pattern(graph.graph)
      (for (m <- matches; v <- m.bipath.nodes) yield v,
        for (m <- matches; e <- m.bipath.edges) yield e)
    }
    else (List(), List())

    val rawDot = graph.dotWithHighlights(if (text.length > 100) text.substring(0, 100) + "..." else text, Set.empty, Set.empty)
    val dot = rawDot
      .replaceAll("\n", " ")
      .replaceAll("""\?|#|%|^|~|`|@|&|\$""", "")
      .replaceAll("""\s+""", " ")
      .replaceAll("\"", """%22""")
      .replaceAll(" ", "%20")

    ("parse time: " + Timing.Milliseconds.format(parseTime),
      "<img src=\"/dot/png/" + dot + "\" /><br><pre>serialized: " + graph.serialize + "\n\n" + rawDot + "</pre>")
  }
}
