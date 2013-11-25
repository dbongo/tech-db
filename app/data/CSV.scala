package data

import scala.util.parsing.combinator.RegexParsers
import java.io.File
import scala.io.Source
import models.Technology
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

object CSV {

  def from(file: File): List[Technology] = from(Source.fromFile(file))

  def from(input: Source): List[Technology] = from(input.getLines().mkString("\n"))

  def from(input: String): List[Technology] = {
    CSVParser.parse(input).flatMap {
      case List(id, name, description, clobbedTags, maybeHomePage, status, priority, archived, added, lastModified) => {
        Some(Technology(
          id,
          name,
          description,
          clobbedTags.split(" "),
          if(maybeHomePage.isEmpty) None else Some(maybeHomePage),
          status,
          priority.toBoolean,
          archived.toBoolean,
          DateTime.parse(added),
          DateTime.parse(lastModified)))
      }
      case theRest => {
        println(s"For some reason we can't parse this: $theRest")
        None
      }
    }
  }

  def to(technologies: List[Technology]) = {
    technologies.map { technology =>
      "%s,%s,\"%s\",%s,\"%s\",%s,%s,%s,%s,%s".format(
        technology.id,
        technology.name,
        technology.description,
        technology.tags.mkString(" "),
        technology.homePage.getOrElse(""),
        technology.status,
        technology.priority,
        technology.archived,
        technology.added.toString(ISODateTimeFormat.dateTime()),
        technology.lastModified.toString(ISODateTimeFormat.dateTime()))
    } mkString "\n"
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
