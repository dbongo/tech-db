package data

import models.Technology
import org.elasticsearch.search.SearchHit
import java.util

object TechnologyReaders {

  import ES._
  import Implicits._

  implicit object TechnologyHitReader extends SearchHitReader[Technology]{
    def read(searchHit: SearchHit): Technology = {
      Technology(
        searchHit.sourceAs[String]("id").get,
        searchHit.sourceAs[String]("name").get,
        searchHit.sourceAs[String]("description").get,
        searchHit.sourceAs[Seq[String]]("tags").get,
        searchHit.sourceAs[String]("homePage").flatMap(Option(_)),
        searchHit.sourceAs[String]("status").get
      )
    }
  }

  implicit object TechnologySourceReader extends SourceReader[Technology]{
    def read(source: util.Map[String, Object]): Technology = {
      Technology(
        source.getAs[String]("id").get,
        source.getAs[String]("name").get,
        source.getAs[String]("description").get,
        source.getAs[Seq[String]]("tags").get,
        source.getAs[String]("homePage").flatMap(Option(_)),
        source.getAs[String]("status").get
      )
    }
  }
}