package tasks

import play.core.StaticApplication
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.source.ObjectSource
import models.Technology

trait PlayTask extends Runnable {
  val application = new StaticApplication(new java.io.File("."))
}

class ResetDB extends PlayTask {
  def run() {
    ES.client.deleteIndex("_all")
  }
}

class SeedDB extends PlayTask {
  def run() {
    Seed.data.map { t =>
      ES.client.sync.execute(index into "technologies/technology" id t.id source ObjectSource(t))
    }
  }
}

object Seed {
  val data = Seq(
    Technology(ES.id, "Play! (Scala)", "A Web framework for scala", "scala" :: "web" :: "jvm" :: Nil, Some("http://playframework.com")),
    Technology(ES.id, "Play! (Java)", "A Web framework for java", Seq("java", "web", "jvm"),  Some("http://playframework.com")),
    Technology(ES.id, "ElasticSearch", "A Searchy thing like google", Seq("search", "persistence", "database"),  Some("http://elasticsearch.org"))
  )
}