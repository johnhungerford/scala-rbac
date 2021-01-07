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
        dependencyOverrides ++= Seq( "com.google.guava" % "guava" % "15.0",
                                     "com.fasterxml.jackson.core" % "jackson-core" % jacksonOverrideVersion,
                                     "com.fasterxml.jackson.core" % "jackson-annotation" % jacksonOverrideVersion,
                                     "com.fasterxml.jackson.core" % "jackson-databind" % jacksonOverrideVersion ),
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

lazy val assemblySettings = Seq(
    libraryDependencies ++= scalatra ++ jackson,
    assemblyMergeStrategy in assembly := {
        case PathList( "META-INF", "MANIFEST.MF" ) => MergeStrategy.discard
        case PathList( "reference.conf" ) => MergeStrategy.concat
        case x => MergeStrategy.last
    },
    Compile / unmanagedResourceDirectories += baseDirectory.value / "src/main/webapp",
    test in assembly := {},
    mainClass in( Compile, run ) := Some( "Main" ),
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
  .aggregate( rbacCore, rbacHttp )
  .settings(
      name := "scala-rbac",
      disablePublish
  )

lazy val rbacCore = ( project in file( "scala-rbac-core" ) )
  .configs( IntegrationConfig, WipConfig )
  .disablePlugins( sbtassembly.AssemblyPlugin )
  .settings(
      commonSettings,
      publishSettings,
  )

lazy val rbacHttp = ( project in file( "scala-rbac-http" ) )
  .dependsOn( rbacCore )
  .configs( IntegrationConfig, WipConfig )
  .disablePlugins( sbtassembly.AssemblyPlugin )
  .settings(
      commonSettings,
      libraryDependencies ++= jackson,
      publishSettings,
  )

lazy val rbacScalatra = ( project in file( "scala-rbac-scalatra" ) )
  .dependsOn( rbacCore, rbacHttp )
  .configs( IntegrationConfig, WipConfig )
  .disablePlugins( sbtassembly.AssemblyPlugin )
  .settings(
      commonSettings,
      libraryDependencies ++= scalatra ++ jackson,
      publishSettings,
  )

lazy val rbacExample = ( project in file( "scala-rbac-example" ) )
  .dependsOn( rbacCore, rbacHttp, rbacScalatra )
  .configs( IntegrationConfig, WipConfig )
  .enablePlugins( JavaAppPackaging )
  .settings(
      commonSettings,
      libraryDependencies ++= typesafeConfig ++ scalatra ++ jackson,
      assemblySettings,
      disablePublish,
  )
