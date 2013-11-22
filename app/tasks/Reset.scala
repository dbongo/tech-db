package tasks

import play.core.StaticApplication
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.source.ObjectSource
import models.Technology
import scala.io.Source
import data.Importer

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
    Importer.convert(Source.fromFile("./data/seed.csv")).map { t =>
      ES.client.sync.execute(index into "technologies/technology" id t.id source ObjectSource(t))
    }
  }
}