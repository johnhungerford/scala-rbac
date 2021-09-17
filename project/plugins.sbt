
logLevel := Level.Warn

//resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

addSbtPlugin( "com.eed3si9n" % "sbt-assembly" % "0.14.6" )
addSbtPlugin( "com.typesafe.sbt" % "sbt-native-packager" % "1.3.2" )
addSbtPlugin( "org.jetbrains" % "sbt-ide-settings" % "1.1.0")
addSbtPlugin( "com.typesafe.play" % "sbt-plugin" % "2.8.7" )
addSbtPlugin( "com.github.sbt" % "sbt-pgp" % "2.1.2" )
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.7.0")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.0.0")

libraryDependencies += "com.spotify" % "docker-client" % "8.9.0"
