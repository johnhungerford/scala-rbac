import sbt._

object Dependencies {

    val slf4jVersion = "1.7.20"
    val logbackVersion = "1.1.7"
    val scalaTestVersion = "3.2.3"
    val jacksonVersion = "2.9.9"
    val scalatraVersion = "2.5.4"
    val jettyWebappVersion = "9.4.18.v20190429"
    val servletApiVersion = "3.1.0"
    val jacksonOverrideVersion = "2.9.10"
    val typesafeConfigVersion = "1.4.1"
    val playVersion = "2.8.7"
    val playTestVersion = "5.1.0"

    val logging = Seq( "org.slf4j" % "slf4j-api" % slf4jVersion,
                       "ch.qos.logback" % "logback-classic" % logbackVersion )

    val scalaTest = Seq( "org.scalatest" %% "scalatest" % scalaTestVersion % "test" )

    val jackson = Seq( "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion,
                       "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % jacksonVersion,
                       "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion )

    val scalatra = Seq( "org.scalatra" %% "scalatra" % scalatraVersion,
                        "org.scalatra" %% "scalatra-scalate" % scalatraVersion,
                        "org.scalatra" %% "scalatra-scalatest" % scalatraVersion % "test",
                        "org.eclipse.jetty" % "jetty-webapp" % jettyWebappVersion,
                        "javax.servlet" % "javax.servlet-api" % servletApiVersion )

    val typesafeConfig = Seq( "com.typesafe" % "config" % typesafeConfigVersion )

    val play = Seq( "com.typesafe.play" %% "play" % playVersion,
                    "org.scalatestplus.play" %% "scalatestplus-play" % playTestVersion % "test" )
}