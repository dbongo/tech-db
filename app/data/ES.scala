package data

import com.sksamuel.elastic4s.ElasticClient
import org.elasticsearch.common.settings.{Settings, ImmutableSettings}
import org.elasticsearch.search.SearchHit
import org.elasticsearch.action.search.SearchResponse
import collection.JavaConversions._
import collection.JavaConverters._
import java.util.{List => JavaList}
import java.util.{Map => JavaMap}
import org.elasticsearch.action.get.GetResponse
import scala.util.Random
import org.joda.time.DateTime
import scala.reflect.ClassTag

object ES {

  def id = s"${math.abs(Random.nextInt())}-${System.currentTimeMillis}"

  // this is here because of the names.txt not found issue in elasticsearch
  val settings = ImmutableSettings.settingsBuilder()
    .classLoader(classOf[Settings].getClassLoader).build()

  lazy val client = ElasticClient.remote(settings, "localhost" -> 9300)

  trait SearchHitReader[T] {
    def read(searchHit: SearchHit): T
  }

  trait SourceReader[T] {
    def read(source: JavaMap[String, Object]): T
  }

  object Implicits {
    implicit class RichSearchResponse(response: SearchResponse) {
      def resultsAs[T](implicit reader: SearchHitReader[T]): List[T] = {
        response.getHits.hits.map(reader.read).toList
      }
    }

    implicit class RichGetResponse(response: GetResponse) {
      def sourceAs[T](implicit reader: SourceReader[T]): Option[T] = {
        Option(response.getSource).map(reader.read)
      }
    }

    implicit class RichSearchHit(hit: SearchHit) {
      def sourceAs[T <: AnyRef : ClassTag](key: String) = {
        hit.getSource.getAs[T](key)
      }
    }

    implicit class RichSource(hit: JavaMap[String, Object]) {
      def getAs[T <: Any : ClassTag](key: String): Option[T] = {
        val targetClass = scala.reflect.classTag[T].runtimeClass
        hit.toMap.get(key).flatMap(Option(_)).map {
          case value: JavaList[_]                                => value.asScala.asInstanceOf[T]
          case value: String if targetClass == classOf[DateTime] => DateTime.parse(value).asInstanceOf[T]
          case value                                             => value.asInstanceOf[T]
        }
      }
    }
  }
}
