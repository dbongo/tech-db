package tasks

import play.core.StaticApplication
import scala.io.Source
import data.{ES, TechnologyApi, CSV}
import TechnologyApi._

trait PlayTask extends Runnable {
  val application = new StaticApplication(new java.io.File("."))
}

trait TaskWithDependencies extends Runnable {

  val dependants: List[Class[_ <: Runnable]]

  def run() {
    dependants.map(_.newInstance().run())
    execute()
  }

  def execute() : Unit
}

class ResetDB extends Runnable {
  def run() {
    deleteAll
  }
}


class SeedDB extends Runnable {
  def run() {
    insertMany(CSV.from(Source.fromFile("./data/seed.csv")))
  }
}

class Prioritize extends Runnable {
  def run() {

    import com.sksamuel.elastic4s.ElasticDsl._
    import concurrent.ExecutionContext.Implicits.global

    TechnologyApi.all.map { technologies =>
      println(technologies)
      ES.client.bulk(
        technologies
          .map(_.id)
          .map(
            update id _ in "technologies/technology" script "ctx._source.priority = false"
          ):_*
        )
    }
  }
}