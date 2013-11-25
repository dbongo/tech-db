package models

import data.ES
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

case class Technology(
  id           : String,
  name         : String,
  description  : String,
  tags         : Seq[String],
  homePage     : Option[String],
  status       : String = "New",
  priority     : Boolean,
  archived     : Boolean,
  added        : DateTime,
  lastModified : DateTime = DateTime.now) {

  def withId(id: String) = copy(id = id)
}

object Technology {

  import play.api.data.Form
  import play.api.data.Forms._

  val form = Form(
    mapping(
      "id"           -> ignored(ES.id),
      "name"         -> nonEmptyText,
      "description"  -> nonEmptyText,
      "tags"         -> nonEmptyText.transform(
        s => s.split(" ").toSeq,
        (s:Seq[String]) => s.mkString(" ")),
      "homePage"     -> optional(text),
      "status"       -> Status.validator,
      "priority"     -> boolean,
      "archived"     -> boolean,
      "added"        -> ignored(DateTime.now),
      "lastModified" -> ignored(DateTime.now)
    )(Technology.apply)(Technology.unapply)
  )

  object Status {
    val New         = "New"
    val Incubating  = "Incubating"
    val Recommended = "Recommended"
    val Rejected    = "Rejected"
    val Deprecated  = "Deprecated"

    val options = Seq(New, Incubating, Recommended, Rejected, Deprecated).map(s => s -> s)

    def validator = nonEmptyText.verifying { value =>
      options.exists(status => status._1 == value)
    }
  }



}