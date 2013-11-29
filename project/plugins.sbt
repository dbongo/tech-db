logLevel := Level.Warn

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "yobriefca.se-repo" at "http://yobriefca.se/maven"

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.2.1")

addSbtPlugin("se.yobriefca" % "sbt-tasks" % "0.3.16")
