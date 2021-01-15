import sbt._
import Dependencies._
import sbtassembly.AssemblyPlugin.assemblySettings

val projectVersion = "1.0-SNAPSHOT"


/*
   ##############################################################################################
   ##                                                                                          ##
   ##                                  SETTINGS DEFINITIONS                                    ##
   ##                                                                                          ##
   ##############################################################################################
 */

// integrationConfig and wipConfig are used to define separate test configurations for integration testing
// and work-in-progress testing
lazy val IntegrationConfig = config( "integration" ) extend( Test )
lazy val WipConfig = config( "wip" ) extend( Test )

lazy val commonSettings =
    inConfig( IntegrationConfig )( Defaults.testTasks ) ++
    inConfig( WipConfig )( Defaults.testTasks ) ++
    Seq(
        organization := "org.hungerford",
        version := projectVersion,
        organizationName := "John Hungerford",
        description := "A flexible role-based access control library",
        licenses += "GPLv2" -> url("https://www.gnu.org/licenses/gpl-2.0.html"),
        startYear := Some( 2021 ),
        scalaVersion := "2.12.7",
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
    externalResolvers += "GitHub johnhungerford Apache Maven Packages" at "https://maven.pkg.github.com/johnhungerford/scala-rbac",
    publishTo := Some( "GitHub johnhungerford Apache Maven Packages" at "https://maven.pkg.github.com/johnhungerford/scala-rbac" ),
) ++ projectVersion match {
    case Snapshot => Seq(
        publishArtifact in (Compile, packageDoc) := false,
        publishArtifact in (Compile, packageSrc) := false,
    )
    case _ => Seq()
}

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
  .disablePlugins( sbtassembly.AssemblyPlugin )
  .aggregate( rbacCore, rbacHttp, rbacScalatra, rbacPlay, rbacServicesExample, rbacScalatraExample, rbacPlayExample )
  .settings(
      name := "scala-rbac",
      disablePublish,
  )

lazy val rbacCore = ( project in file( "scala-rbac-core" ) )
  .configs( IntegrationConfig, WipConfig )
  .disablePlugins( sbtassembly.AssemblyPlugin )
  .settings(
      commonSettings,
      publishSettings,
      disableBuild,
  )

lazy val rbacHttp = ( project in file( "scala-rbac-http" ) )
  .dependsOn( rbacCore )
  .configs( IntegrationConfig, WipConfig )
  .disablePlugins( sbtassembly.AssemblyPlugin )
  .settings(
      commonSettings,
      libraryDependencies ++= jackson,
      publishSettings,
      disableBuild,
  )

lazy val rbacScalatra = ( project in file( "scala-rbac-scalatra" ) )
  .dependsOn( rbacCore, rbacHttp )
  .configs( IntegrationConfig, WipConfig )
  .disablePlugins( sbtassembly.AssemblyPlugin )
  .settings(
      commonSettings,
      libraryDependencies ++= scalatra ++ jackson,
      publishSettings,
      disableBuild,
  )

lazy val rbacPlay = ( project in file( "scala-rbac-play" ) )
  .dependsOn( rbacCore, rbacHttp )
  .configs( IntegrationConfig, WipConfig )
  .disablePlugins( sbtassembly.AssemblyPlugin )
  .settings(
      commonSettings,
      libraryDependencies ++=  play ++ jackson,
      publishSettings,
      disableBuild,
  )

lazy val rbacServicesExample = ( project in file( "scala-rbac-examples/services-example" ) )
  .dependsOn( rbacCore )
  .configs( IntegrationConfig, WipConfig )
  .disablePlugins( sbtassembly.AssemblyPlugin )
  .settings(
      commonSettings,
      disablePublish,
      disableBuild,
  )

lazy val rbacScalatraExample = ( project in file( "scala-rbac-examples/scalatra-example" ) )
  .dependsOn( rbacCore, rbacScalatra, rbacServicesExample )
  .configs( IntegrationConfig, WipConfig )
  .enablePlugins( JavaAppPackaging )
  .settings(
      commonSettings,
      libraryDependencies ++= typesafeConfig ++ scalatra ++ jackson,
      buildSettings,
      Docker / packageName := "rbac-scalatra-example",
  )

lazy val rbacPlayExample = ( project in file( "scala-rbac-examples/play-example" ) )
  .dependsOn( rbacCore, rbacPlay, rbacServicesExample )
  .configs( IntegrationConfig, WipConfig )
  .enablePlugins( PlayScala, JavaAppPackaging, DockerPlugin, DockerSpotifyClientPlugin )
  .settings(
      commonSettings,
      libraryDependencies ++= typesafeConfig ++ play ++ jackson :+ guice,
      buildSettings,
      Compile / unmanagedResourceDirectories += baseDirectory.value / "app" / "conf", // For fatjar to work
      mainClass in assembly := Some("play.core.server.ProdServerStart"), // For fatjar to work
      Docker / packageName := "rbac-play-example",
  )
