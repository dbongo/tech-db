package data

import scala.util.parsing.combinator.RegexParsers
import java.io.File
import scala.io.Source
import models.Technology

object CSV {

  def from(file: File): List[Technology] = from(Source.fromFile(file))

  def from(input: Source): List[Technology] = from(input.getLines.mkString("\n"))

  def from(input: String): List[Technology] = {
    CSVParser.parse(input).flatMap {
      case List(a,b,c,d,e,f) => Some(Technology(a,b,c,Seq(d.split(" "):_*), if(e.isEmpty) None else Some(e),f))
      case theRest => {
        println(s"For some reason we can't parse this: $theRest")
        None
      }
    }
  }

  def to(technologies: List[Technology]) = {
    val lines = technologies.map { technology =>
      s"""${technology.id},${technology.name},"${technology.description}",${technology.tags.mkString(" ")},${technology.homePage.map("\"" + _ + "\"").getOrElse("")},${technology.status}"""
    }

    lines.mkString("\n")
  }

  object CSVParser extends RegexParsers {
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
