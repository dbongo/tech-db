package tasks

import play.core.StaticApplication
import scala.io.Source
import data.{TechnologyApi, CSV}
import TechnologyApi._

trait PlayTask extends Runnable {
  val application = new StaticApplication(new java.io.File("."))
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