val V = new {
  val Scala      = "3.1.2"
  val ScalaGroup = "3.1"

  val catsEffect = "3.3.11"
  val tapir      = "1.0.0-M8"
  val sttp       = "3.5.2"
  val circe      = "0.15.0-M1"
  val refined    = "0.9.28"
  val scodecBits = "1.1.30"
  val shapeless  = "3.0.4"

  val typesafeConfig = "1.4.2"
  val bouncycastle   = "1.70"

  val scribe          = "3.8.2"
  val hedgehog        = "0.8.0"
  val organiseImports = "0.6.0"

  val scalaJavaTime = "2.3.0"
  val jsSha3        = "0.8.0"
  val elliptic      = "6.5.4"
  val typesElliptic = "6.4.12"
}

val Dependencies = new {

  lazy val node = Seq(
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-armeria-server-cats" % V.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe"          % V.tapir,
      "com.outr"                    %% "scribe-slf4j"              % V.scribe,
      "com.typesafe" % "config" % V.typesafeConfig,
    ),
  )

  lazy val api = Seq(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "shapeless3-deriving" % V.shapeless,
      "org.typelevel" %% "cats-effect"         % V.catsEffect,
      "com.softwaremill.sttp.tapir"   %% "tapir-armeria-server-cats" % V.tapir,
      "com.softwaremill.sttp.tapir"   %% "tapir-json-circe"          % V.tapir,
      "com.softwaremill.sttp.tapir"   %% "tapir-swagger-ui-bundle"   % V.tapir,
      "com.softwaremill.sttp.client3" %% "core"                      % V.sttp,
    ),
  )

  lazy val lib = Seq(
    libraryDependencies ++= Seq(
      "io.circe"      %%% "circe-generic"       % V.circe,
      "io.circe"      %%% "circe-parser"        % V.circe,
      "io.circe"      %%% "circe-refined"       % V.circe,
      "eu.timepit"    %%% "refined"             % V.refined,
      "org.scodec"    %%% "scodec-bits"         % V.scodecBits,
      "org.typelevel" %%% "shapeless3-deriving" % V.shapeless,
      "org.typelevel" %%% "shapeless3-typeable" % V.shapeless,
    ),
  )

  lazy val libJVM = Seq(
    libraryDependencies ++= Seq(
      "org.bouncycastle" % "bcprov-jdk15on" % V.bouncycastle,
      "com.outr"        %% "scribe-slf4j"   % V.scribe,
    ),
  )

  lazy val libJS = Seq(
    libraryDependencies ++= Seq(
      "com.outr" %%% "scribe" % V.scribe,
    ),
    Compile / npmDependencies ++= Seq(
      "js-sha3"         -> V.jsSha3,
      "elliptic"        -> V.elliptic,
      "@types/elliptic" -> V.typesElliptic,
    ),
  )

  lazy val tests = Def.settings(
    libraryDependencies ++= Seq(
      "qa.hedgehog" %% "hedgehog-minitest" % V.hedgehog % Test,
    ),
    testFrameworks += TestFramework("minitest.runner.Framework"),
    Test / fork := true,
  )
}

ThisBuild / organization := "org.leisuremeta"
ThisBuild / version      := "0.0.1-SNAPSHOT"
ThisBuild / scalaVersion := V.Scala
ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % V.organiseImports
ThisBuild / semanticdbEnabled := true

lazy val root = (project in file("."))
  .aggregate(node, api.jvm, api.js)

lazy val node = (project in file("modules/node"))
  .settings(Dependencies.node)
  .settings(
    name := "leisuremeta-chain-node",
  )
  .dependsOn(api.jvm)

lazy val api = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/api"))
  .settings(Dependencies.api)
  .settings(
    scalacOptions ++= Seq(
      "-Xmax-inlines:64",
    ),
    Compile / compile / wartremoverErrors ++= Warts.all,
  )
  .dependsOn(lib)

lazy val lib = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .in(file("modules/lib"))
  .settings(Dependencies.lib)
  .settings(Dependencies.tests)
  .jvmSettings(Dependencies.libJVM)
  .jsSettings(Dependencies.libJS)
  .jsSettings(
    useYarn := true,
    Test / scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.CommonJSModule)
    },
    scalacOptions ++= Seq(
      "-scalajs",
    ),
    Test / fork := false,
    Compile / compile / wartremoverErrors ++= Warts.all,
  )
  .jsConfigure { project =>
    project
      .enablePlugins(ScalaJSBundlerPlugin)
      .enablePlugins(ScalablyTypedConverterPlugin)
  }
