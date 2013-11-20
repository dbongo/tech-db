package tasks

import com.sksamuel.elastic4s.ElasticClient
import org.elasticsearch.common.settings.{Settings, ImmutableSettings}
import org.elasticsearch.search.SearchHit
import org.elasticsearch.action.search.SearchResponse
import models.Technology
import collection.JavaConversions._
import collection.JavaConverters._
import java.util
import org.elasticsearch.action.get.GetResponse
import scala.util.Random

object ES {

  def id = s"${math.abs(Random.nextInt)}-${System.currentTimeMillis}"

  // this is here because of the names.txt not found issue in elasticsearch
  val settings = ImmutableSettings.settingsBuilder()
    .classLoader(classOf[Settings].getClassLoader()).build()

  lazy val client = ElasticClient.remote(settings, "localhost" -> 9300)

  trait SearchHitReader[T] {
    def read(searchHit: SearchHit): T
  }

  trait SourceReader[T] {
    def read(source: util.Map[String, Object]): T
  }

  object Implicits {
    implicit class RichSearchResponse(response: SearchResponse) {
      def resultsAs[T](implicit reader: SearchHitReader[T]): List[T] = {
        response.getHits.hits.map(reader.read(_)).toList
      }
    }

    implicit class RichGetResponse(response: GetResponse) {
      def sourceAs[T](implicit reader: SourceReader[T]): Option[T] = {
        Option(response.getSource).map(reader.read(_))
      }
    }

    implicit class RichSearchHit(hit: SearchHit) {
      def sourceAs[T](key: String) = {
        hit.getSource().getAs[T](key)
      }
    }

    implicit class RichSource(hit: util.Map[String, Object]) {
      def getAs[T](key: String) = {
        hit.toMap.get(key).flatMap(Option(_)).map { value =>
          if(classOf[util.List[String]].isAssignableFrom(value.getClass)) {
            // handle thing that can be iterated
            value.asInstanceOf[util.List[T]].asScala.asInstanceOf[T]
          } else {
            value.asInstanceOf[T]
          }
        }
      }
    }
  }

  object DefaultReaders {

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
}
