import se.yobriefca.SbtTasks._

name := "tech-db"

version := "0.2.0"

libraryDependencies ++= Seq(
  "org.elasticsearch" % "elasticsearch" % "0.90.6",
  "com.sksamuel.elastic4s" % "elastic4s_2.10" % "0.90.6.0",
  "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % "2.1.1"
)

installTask("seedDB", "seeds the database")

installTask("resetDB", "resets the database")

installTask("prioritize", "Runs a migration")

play.Project.playScalaSettings
