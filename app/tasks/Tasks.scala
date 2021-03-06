package tasks

import play.core.StaticApplication
import scala.io.Source
import data.{ES, TechnologyApi, CSV}
import TechnologyApi._
import models.Technology

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

