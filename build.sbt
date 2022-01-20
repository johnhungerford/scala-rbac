import sbt._
import Dependencies._
import sbtassembly.AssemblyPlugin.assemblySettings

lazy val projectVersion = "1.2-SNAPSHOT"

/*
   ##############################################################################################
   ##                                                                                          ##
   ##                                  SETTINGS DEFINITIONS                                    ##
   ##                                                                                          ##
   ##############################################################################################
 */

// Supported versions for cross building
lazy val scala212 = "2.12.15"
lazy val scala213 = "2.13.8"
lazy val supportedScalaVersions = List(scala212, scala213)


// integrationConfig and wipConfig are used to define separate test configurations for integration testing
// and work-in-progress testing
lazy val IntegrationConfig = config( "integration" ) extend( Test )
lazy val WipConfig = config( "wip" ) extend( Test )

lazy val commonSettings =
    inConfig( IntegrationConfig )( Defaults.testTasks ) ++
    inConfig( WipConfig )( Defaults.testTasks ) ++
    Seq(
        version := projectVersion,
        description := "A flexible role-based access control library",
        licenses := List( "Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt" ) ),
        homepage := Some( url( "https://johnhungerford.github.io" ) ),
        startYear := Some( 2021 ),
        scalaVersion := scala213,
        resolvers ++= Seq( "Maven Central" at "https://repo1.maven.org/maven2/",
                           "JCenter" at "https://jcenter.bintray.com",
                           "Local Ivy Repository" at s"file://${System.getProperty( "user.home" )}/.ivy2/local/default" ),
        javacOptions ++= Seq( "-source", "1.8", "-target", "1.8" ),
        scalacOptions += "-target:jvm-1.8",
        useCoursier := false,
        libraryDependencies ++= logging ++ scalaTest,
        // `sbt test` should skip tests tagged IntegrationTest
        Test / testOptions := Seq( Tests.Argument( "-l", "org.hungerford.rbac.test.tags.annotations.IntegrationTest" ) ),
        // `sbt integration:test` should run only tests tagged IntegrationTest
        IntegrationConfig / parallelExecution := false,
        IntegrationConfig / testOptions := Seq( Tests.Argument( "-n", "org.hungerford.rbac.test.tags.IntegrationTest" ) ),
        // `sbt wip:test` should run only tests tagged WipTest
        WipConfig / testOptions := Seq( Tests.Argument( "-n", "org.hungerford.rbac.test.tags.WipTest" ) ),
    )

val Snapshot = "-SNAPSHOT".r

lazy val publishSettings = Seq(
    credentials += Credentials( Path.userHome / ".sbt" / "sonatype_credentials" ),
    organization := "io.github.johnhungerford.rbac",
    organizationName := "johnhungerford",
    organizationHomepage := Some( url( "https://johnhungerford.github.io" ) ),
    pomIncludeRepository := { _ => false },
    scmInfo := Some(
        ScmInfo(
            url("https://github.com/johnhungerford/scala-rbac"),
            "scm:git@github.com:johnhungerford/scala-rbac.git"
        )
    ),
    developers := List(
        Developer(
            id    = "johnhungerford",
            name  = "John Hungerford",
            email = "jiveshungerford@gmail.com",
            url   = url( "https://johnhungerford.github.io" )
        )
    ),
    publishTo := {
        val nexus = "https://s01.oss.sonatype.org/"
        if ( isSnapshot.value ) Some( "snapshots" at nexus + "content/repositories/snapshots" )
        else Some( "releases" at nexus + "service/local/staging/deploy/maven2" )
    },
    ThisBuild / publishMavenStyle := true,
)

lazy val disablePublish = Seq(
    publish := {}
)

lazy val disableBuild = Seq(
    Docker / publish := {}
)

lazy val buildSettings = Seq(
    assemblyMergeStrategy in assembly := {
        case PathList( "META-INF", "MANIFEST.MF" ) => MergeStrategy.discard
        case PathList( "reference.conf" ) => MergeStrategy.concat
        case x => MergeStrategy.last
    },
    test in assembly := {},
    mainClass in( Compile, run ) := Some( "Main" ),
    dockerBaseImage := "openjdk:8",
    dockerUpdateLatest := true,
    dockerUsername := Some( "johnhungerford" ),
)

/*
   ##############################################################################################
   ##                                                                                          ##
   ##                                  PROJECT DEFINITIONS                                     ##
   ##                                                                                          ##
   ##############################################################################################
 */

lazy val root = ( project in file( "." ) )
  .disablePlugins( sbtassembly.AssemblyPlugin, SbtPgp )
  .aggregate(
      rbacCore.projects( JVMPlatform ),
      rbacCore.projects( JSPlatform ),
      rbacHttp,
      rbacScalatra,
      rbacPlay,
  )
  .settings(
      name := "scala-rbac",
      disablePublish,
      disableBuild,
      crossScalaVersions := Nil,
  )

lazy val rbacCore = ( crossProject( JSPlatform, JVMPlatform ) in file( "scala-rbac-core" ) )
  .configs( IntegrationConfig, WipConfig )
  .disablePlugins( sbtassembly.AssemblyPlugin )
  .settings(
      name := "scala-rbac-core",
      commonSettings,
      publishSettings,
      disableBuild,
      crossScalaVersions := supportedScalaVersions,
  )

lazy val rbacHttp = ( project in file( "scala-rbac-http" ) )
  .dependsOn( rbacCore.projects( JVMPlatform ) )
  .configs( IntegrationConfig, WipConfig )
  .disablePlugins( sbtassembly.AssemblyPlugin )
  .settings(
      commonSettings,
      libraryDependencies ++= jackson,
      publishSettings,
      disableBuild,
      crossScalaVersions := supportedScalaVersions,
  )

lazy val rbacScalatra = ( project in file( "scala-rbac-scalatra" ) )
  .dependsOn( rbacCore.projects( JVMPlatform ), rbacHttp )
  .configs( IntegrationConfig, WipConfig )
  .disablePlugins( sbtassembly.AssemblyPlugin )
  .settings(
      commonSettings,
      libraryDependencies ++= scalatra ++ jackson,
      publishSettings,
      disableBuild,
      crossScalaVersions := supportedScalaVersions,
  )

lazy val rbacPlay = ( project in file( "scala-rbac-play" ) )
  .dependsOn( rbacCore.projects( JVMPlatform ), rbacHttp )
  .configs( IntegrationConfig, WipConfig )
  .disablePlugins( sbtassembly.AssemblyPlugin )
  .settings(
      commonSettings,
      libraryDependencies ++=  play ++ jackson,
      publishSettings,
      disableBuild,
      crossScalaVersions := supportedScalaVersions,
  )

lazy val rbacServicesExample = ( project in file( "scala-rbac-examples/services-example" ) )
  .dependsOn( rbacCore.projects( JVMPlatform ) )
  .configs( IntegrationConfig, WipConfig )
  .disablePlugins( sbtassembly.AssemblyPlugin, SbtPgp  )
  .settings(
      commonSettings,
      disablePublish,
      disableBuild,
      crossScalaVersions := Nil,
  )

lazy val rbacScalatraExample = ( project in file( "scala-rbac-examples/scalatra-example" ) )
  .dependsOn( rbacCore.projects( JVMPlatform ), rbacScalatra, rbacServicesExample )
  .configs( IntegrationConfig, WipConfig )
  .enablePlugins( JavaAppPackaging )
  .disablePlugins( SbtPgp )
  .settings(
      commonSettings,
      libraryDependencies ++= typesafeConfig ++ scalatra ++ jackson,
      buildSettings,
      Docker / packageName := "rbac-scalatra-example",
      disablePublish,
      crossScalaVersions := Nil,
  )

lazy val rbacPlayExample = ( project in file( "scala-rbac-examples/play-example" ) )
  .dependsOn( rbacCore.projects( JVMPlatform ), rbacPlay, rbacServicesExample )
  .configs( IntegrationConfig, WipConfig )
  .enablePlugins( PlayScala, JavaAppPackaging, DockerPlugin, DockerSpotifyClientPlugin )
  .disablePlugins( SbtPgp )
  .settings(
      commonSettings,
      libraryDependencies ++= typesafeConfig ++ play ++ jackson :+ guice,
      buildSettings,
      Compile / unmanagedResourceDirectories += baseDirectory.value / "app" / "conf", // For fatjar to work
      mainClass in assembly := Some("play.core.server.ProdServerStart"), // For fatjar to work
      Docker / packageName := "rbac-play-example",
      disablePublish,
      crossScalaVersions := Nil,
  )
