logLevel := Level.Warn

//resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

addSbtPlugin( "com.eed3si9n" % "sbt-assembly" % "0.14.6" )
addSbtPlugin( "com.typesafe.sbt" % "sbt-native-packager" % "1.3.2" )
addSbtPlugin("no.arktekk.sbt" % "aether-deploy" % "0.23.0")
addSbtPlugin("com.dwijnand" % "sbt-dynver" % "4.0.0")
addSbtPlugin("org.jetbrains" % "sbt-ide-settings" % "1.1.0")