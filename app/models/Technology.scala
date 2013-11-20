package models

import tasks.ES


case class Technology(
  id: String,
  name: String,
  description: String,
  tags: Seq[String],
  homePage: Option[String],
  status: String = "New") // New, Incubation, Recommended, Rejected

object Technology {

  import play.api.data.Form
  import play.api.data.Forms._

  val form = Form(
    mapping(
      "id"          -> ignored(ES.id),
      "name"        -> nonEmptyText,
      "description" -> nonEmptyText,
      "tags"        -> nonEmptyText.transform(
        s => s.split(" ").toSeq,
        (s:Seq[String]) => s.mkString(" ")),
      "homePage"    -> optional(text),
      "status"      -> nonEmptyText
    )(Technology.apply)(Technology.unapply)
  )

  val statusOptions = Seq(
    "New"         -> "New",
    "Incubating"  -> "Incubating",
    "Recommended" -> "Recommended",
    "Rejected"    -> "Rejected"
  )
}