package data

import scala.util.parsing.combinator.RegexParsers
import java.io.File
import scala.io.Source
import models.Technology
import tasks.ES
import com.sksamuel.elastic4s.ElasticDsl._
import ES.DefaultReaders._
import ES.Implicits._
import com.sksamuel.elastic4s.source.ObjectSource
import scala.concurrent.Await
import scala.concurrent.duration._

object Exporter {

  def performExport = {
    val lines = ES.client.sync.execute(search in "technologies").resultsAs[Technology].map { technology =>
      s"${technology.id},${technology.name},${technology.description},${technology.tags.mkString(" ")},${technology.homePage.getOrElse("")}, ${technology.status}"
    }

    lines.mkString("\n")
  }
}

object Importer {

  def performImport(file: File) = {
    val source = Source.fromFile(file).getLines.mkString("\n")
    val technologies = CSV.parse(source).map {
      case List(a,b,c,d,e,f) => Technology(a,b,c,d.split(" "), if(e.isEmpty) None else Some(e),f)
    }
    val inserts = technologies.map(t => index into "technologies/technology" id t.id source ObjectSource(t))

    Await.ready(ES.client.bulk(inserts:_*), Duration.Inf)
  }

  object CSV extends RegexParsers {
    override val skipWhitespace = false   // meaningful spaces in CSV

    def COMMA   = ","
    def DQUOTE  = "\""
    def DQUOTE2 = "\"\"" ^^ { case _ => "\"" }  // combine 2 dquotes into 1
    def CRLF    = "\r\n" | "\n"
    def TXT     = "[^\",\r\n]".r
    def SPACES  = "[ \t]+".r

    def file: Parser[List[List[String]]] = repsep(record, CRLF) <~ (CRLF?)

    def record: Parser[List[String]] = repsep(field, COMMA)

    def field: Parser[String] = escaped|nonescaped

    def escaped: Parser[String] = {
      ((SPACES?)~>DQUOTE~>((TXT|COMMA|CRLF|DQUOTE2)*)<~DQUOTE<~(SPACES?)) ^^ {
        case ls => ls.mkString("")
      }
    }

    def nonescaped: Parser[String] = (TXT*) ^^ { case ls => ls.mkString("") }

    def parse(s: String) = parseAll(file, s) match {
      case Success(res, _) => res
      case e => throw new Exception(e.toString)
    }
  }
}
