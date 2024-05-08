import org.scalablytyped.converter.internal.scalajs.Dep
val V = new {
  val Scala      = "3.3.3"
  val ScalaGroup = "3.3"

  val catsEffect = "3.5.4"
  val tapir      = "1.10.6"
  val sttp       = "3.9.6"
  val circe      = "0.14.7"
  val refined    = "0.10.3"
  val scodecBits = "1.1.37"
  val shapeless  = "3.4.1"
  val fs2        = "3.10.2"

  val typesafeConfig = "1.4.3"
  val pureconfig     = "0.17.4"
  val bouncycastle   = "1.70"
  val sway           = "0.16.2"
  val lettuce        = "6.2.6.RELEASE"
  val jasync         = "2.1.24"

  val okhttp3LoggingInterceptor = "4.11.0"

  val web3J = "4.9.6"

  val awsSdk = "2.20.75"

  val scribe          = "3.13.4"
  val hedgehog        = "0.10.1"
  val organiseImports = "0.6.0"
  val munitCatsEffect = "2.0.0-M3"

  val tyrian = "0.9.0"

  val scalaJavaTime = "2.3.0"
  val jsSha3        = "0.8.0"
  val elliptic      = "6.5.4"
  val typesElliptic = "6.4.12"
  val pgEmbedded    = "1.0.3"
  val quill         = "4.6.0.1"
  val postgres      = "42.6.0"
  val flywayCore    = "9.19.1"
}

val Dependencies = new {

  lazy val node = Seq(
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-armeria-server-cats" % V.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe"          % V.tapir,
      "com.outr"                    %% "scribe-slf4j"              % V.scribe,
      "com.typesafe" % "config" % V.typesafeConfig,
      ("io.swaydb"  %% "swaydb" % V.sway).cross(CrossVersion.for3Use2_13),
      "io.lettuce" % "lettuce-core" % V.lettuce,
    ),
    excludeDependencies ++= Seq(
      "org.scala-lang.modules" % "scala-collection-compat_2.13",
      "org.scala-lang.modules" % "scala-java8-compat_2.13",
    ),
  )

  lazy val jvmClient = Seq(
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client3" %% "armeria-backend-cats" % V.sttp,
      "com.softwaremill.sttp.tapir"   %% "tapir-sttp-client"    % V.tapir,
    ),
    excludeDependencies ++= Seq(
      "org.scala-lang.modules" % "scala-collection-compat_2.13",
      "org.scala-lang.modules" % "scala-java8-compat_2.13",
    ),
  )

  lazy val ethGateway = Seq(
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir"   %% "tapir-armeria-server-cats" % V.tapir,
      "com.softwaremill.sttp.tapir"   %% "tapir-json-circe"          % V.tapir,
      "com.softwaremill.sttp.client3" %% "armeria-backend-cats"      % V.sttp,
      "com.softwaremill.sttp.tapir"   %% "tapir-sttp-client"         % V.tapir,
      "com.outr"                      %% "scribe-slf4j"              % V.scribe,
      "com.github.pureconfig" %% "pureconfig-core" % V.pureconfig,
      "com.typesafe"           % "config"          % V.typesafeConfig,
      "org.web3j"              % "core"            % V.web3J,
      "org.web3j"              % "contracts"       % V.web3J,
      "com.squareup.okhttp3" % "logging-interceptor" % V.okhttp3LoggingInterceptor,
      "com.github.jasync-sql"  % "jasync-mysql" % V.jasync,
      "software.amazon.awssdk" % "kms"          % V.awsSdk,
    ),
  )

  lazy val archive = Seq(
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client3" %% "armeria-backend-cats" % V.sttp,
      "com.outr"                      %% "scribe-slf4j"         % V.scribe,
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
      "com.softwaremill.sttp.tapir"   %% "tapir-swagger-ui"          % V.tapir,
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
      "org.typelevel" %%% "shapeless3-typeable" % V.shapeless,
      "co.fs2"        %%% "fs2-core"            % V.fs2,
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

  lazy val catsEffectTests = Def.settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "munit-cats-effect" % V.munitCatsEffect % Test,
    ),
    Test / fork := true,
  )

  lazy val tests = Def.settings(
    libraryDependencies ++= Seq(
      "qa.hedgehog"            %%% "hedgehog-munit"  % V.hedgehog   % Test,
      "com.opentable.components" % "otj-pg-embedded" % V.pgEmbedded % Test,
      "org.flywaydb"             % "flyway-core"     % V.flywayCore,
    ),
    Test / fork := true,
  )

  lazy val lmscanCommon = Seq(
    libraryDependencies ++= Seq(
      "org.typelevel"               %%% "cats-effect"      % V.catsEffect,
      "io.circe"                    %%% "circe-generic"    % V.circe,
      "io.circe"                    %%% "circe-parser"     % V.circe,
      "io.circe"                    %%% "circe-refined"    % V.circe,
      "eu.timepit"                  %%% "refined"          % V.refined,
      "com.softwaremill.sttp.tapir" %%% "tapir-core"       % V.tapir,
      "com.softwaremill.sttp.tapir" %%% "tapir-json-circe" % V.tapir,
      "org.scodec"                  %%% "scodec-bits"      % V.scodecBits,
      "co.fs2"                      %%% "fs2-core"         % V.fs2,
      "io.getquill" %% "quill-jasync-postgres" % V.quill,
    ),
  )

  lazy val lmscanCommonJVM = Seq(
    libraryDependencies ++= Seq(
      "com.outr" %% "scribe-slf4j" % V.scribe,
    ),
  )

  lazy val lmscanCommonJS = Seq(
    libraryDependencies ++= Seq(
      "com.outr" %%% "scribe" % V.scribe,
    ),
  )

  lazy val lmscanFrontend = Seq(
    libraryDependencies ++= Seq(
      "io.indigoengine" %%% "tyrian-io"      % V.tyrian,
      "qa.hedgehog"     %%% "hedgehog-munit" % V.hedgehog % Test,
    ),
  )

  lazy val lmscanBackend = Seq(
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-armeria-server-cats" % V.tapir,
      "org.typelevel"                 %% "cats-effect"          % V.catsEffect,
      "com.softwaremill.sttp.tapir"   %% "tapir-json-circe"     % V.tapir,
      "com.softwaremill.sttp.tapir"   %% "tapir-core"           % V.tapir,
      "com.outr"                      %% "scribe-slf4j"         % V.scribe,
      "com.outr"                      %% "scribe-cats"          % V.scribe,
      "com.softwaremill.sttp.client3" %% "armeria-backend-cats" % V.sttp,
      "com.typesafe"             % "config"          % V.typesafeConfig,
      "com.outr"                %% "scribe-slf4j"    % V.scribe,
      "org.postgresql"           % "postgresql"      % V.postgres,
      "com.opentable.components" % "otj-pg-embedded" % V.pgEmbedded,
    ),
  )

  lazy val lmscanAgent = Seq(
    libraryDependencies ++= Seq(
      "com.outr"    %% "scribe-slf4j" % V.scribe,
      "com.typesafe" % "config"       % V.typesafeConfig,
      "com.softwaremill.sttp.client3" %% "armeria-backend-cats" % V.sttp,
      "io.circe"                      %% "circe-generic"        % V.circe,
      "io.circe"                      %% "circe-parser"         % V.circe,
      "io.circe"                      %% "circe-refined"        % V.circe,
      "com.squareup.okhttp3" % "logging-interceptor" % V.okhttp3LoggingInterceptor,
      "org.typelevel" %% "cats-effect"           % V.catsEffect,
      "io.getquill"   %% "quill-jasync-postgres" % V.quill,
      "org.postgresql" % "postgresql"            % V.postgres,
    ),
    excludeDependencies ++= Seq(
      // "org.scala-lang.modules" % "scala-compiler-2.13",
      // "org.scala-lang.modules" % "scala-asm",
    ),
  )

  lazy val nodeProxy = Seq(
    libraryDependencies ++= Seq(
      "com.outr"    %% "scribe-slf4j" % V.scribe,
      "com.typesafe" % "config"       % V.typesafeConfig,
      "com.softwaremill.sttp.tapir"   %% "tapir-armeria-server-cats" % V.tapir,
      "com.softwaremill.sttp.client3" %% "armeria-backend-cats" % V.sttp,
      "io.circe"                      %% "circe-generic"        % V.circe,
      "io.circe"                      %% "circe-parser"         % V.circe,
      "io.circe"                      %% "circe-refined"        % V.circe,
      "com.squareup.okhttp3" % "logging-interceptor" % V.okhttp3LoggingInterceptor,
      "org.typelevel"                 %% "cats-effect"          % V.catsEffect,
      "co.fs2"                        %%% "fs2-core"            % V.fs2,
    ),
  )
}

ThisBuild / organization := "org.leisuremeta"
ThisBuild / version      := "0.0.1-SNAPSHOT"
ThisBuild / scalaVersion := V.Scala
ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % V.organiseImports
ThisBuild / semanticdbEnabled := true

lazy val root = (project in file("."))
  .aggregate(
    node,
    api.jvm,
    api.js,
    lib.jvm,
    lib.js,
    archive,
    bulkInsert,
    ethGatewaySetup,
    ethGatewayCommon,
    ethGatewayDeposit,
    ethGatewayWithdraw,
    lmscanCommon.jvm,
    lmscanCommon.js,
    lmscanFrontend,
    lmscanBackend,
//    lmscanAgent,
    nodeProxy,
  )

lazy val node = (project in file("modules/node"))
  .settings(Dependencies.node)
  .settings(Dependencies.tests)
  .settings(
    name := "leisuremeta-chain-node",
    assemblyMergeStrategy := {
      case x if x `contains` "reactor/core" =>
        MergeStrategy.first
      case x if x `contains` "io.netty.versions.properties" =>
        MergeStrategy.first
      case x if x `contains` "module-info.class" => MergeStrategy.discard
      case x =>
        val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
        oldStrategy(x)
    },
  )
  .dependsOn(api.jvm)

lazy val ethGatewayCommon = (project in file("modules/eth-gateway-common"))
  .settings(Dependencies.ethGateway)
  .settings(Dependencies.catsEffectTests)
  .settings(
    name := "leisuremeta-chain-eth-gateway-common",
    Compile / compile / wartremoverErrors ++= Warts
      .allBut(Wart.Any, Wart.AsInstanceOf, Wart.Nothing, Wart.Recursion),
  )
  .dependsOn(api.jvm)

lazy val ethGatewaySetup = (project in file("modules/eth-gateway-setup"))
  .settings(Dependencies.ethGateway)
  .settings(Dependencies.catsEffectTests)
  .settings(
    name := "leisuremeta-chain-eth-gateway-setup",
  )
  .dependsOn(ethGatewayCommon)

lazy val ethGatewayDeposit = (project in file("modules/eth-gateway-deposit"))
  .settings(Dependencies.ethGateway)
  .settings(Dependencies.tests)
  .settings(
    name := "leisuremeta-chain-eth-gateway-deposit",
    assemblyMergeStrategy := {
      case x if x `contains` "okio.kotlin_module" => MergeStrategy.first
      case x if x `contains` "io.netty.versions.properties" =>
        MergeStrategy.first
      case x if x `contains` "module-info.class" => MergeStrategy.discard
      case x =>
        val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
        oldStrategy(x)
    },
    Compile / compile / wartremoverErrors ++= Warts
      .allBut(
        Wart.Any,
        Wart.AsInstanceOf,
        Wart.Nothing,
        Wart.Recursion,
        Wart.SeqApply,
      ),
  )
  .dependsOn(ethGatewayCommon)

lazy val ethGatewayWithdraw = (project in file("modules/eth-gateway-withdraw"))
  .settings(Dependencies.ethGateway)
  .settings(Dependencies.tests)
  .settings(
    name := "leisuremeta-chain-eth-gateway-withdraw",
    assemblyMergeStrategy := {
      case x if x `contains` "okio.kotlin_module" => MergeStrategy.first
      case x if x `contains` "io.netty.versions.properties" =>
        MergeStrategy.first
      case x if x `contains` "module-info.class" => MergeStrategy.discard
      case x =>
        val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
        oldStrategy(x)
    },
    Compile / compile / wartremoverErrors ++= Warts
      .allBut(
        Wart.Any,
        Wart.AsInstanceOf,
        Wart.Nothing,
        Wart.Recursion,
        Wart.TripleQuestionMark,
      ),
  )
  .dependsOn(ethGatewayCommon)

lazy val archive = (project in file("modules/archive"))
  .settings(Dependencies.archive)
  .settings(Dependencies.tests)
  .settings(
    name := "leisuremeta-chain-archive",
    assemblyMergeStrategy := {
      case x if x `contains` "io.netty.versions.properties" =>
        MergeStrategy.first
      case x if x `contains` "module-info.class" => MergeStrategy.discard
      case x =>
        val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
        oldStrategy(x)
    },
  )
  .dependsOn(api.jvm)

lazy val bulkInsert = (project in file("modules/bulk-insert"))
  .dependsOn(node)
  .settings(
    name := "leisuremeta-chain-bulk-insert",
    excludeDependencies ++= Seq(
      "org.scala-lang.modules" % "scala-collection-compat_2.13",
      "org.scala-lang.modules" % "scala-java8-compat_2.13",
    ),
    assemblyMergeStrategy := {
      case x if x `contains` "io.netty.versions.properties" =>
        MergeStrategy.first
      case x if x `contains` "module-info.class" => MergeStrategy.discard
      case x =>
        val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
        oldStrategy(x)
    },
  )

lazy val jvmClient = (project in file("modules/jvm-client"))
  .settings(Dependencies.jvmClient)
  .dependsOn(node)
  .settings(
    name := "leisuremeta-chain-jvm-client",
  )

lazy val api = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/api"))
  .settings(Dependencies.api)
  .settings(
    scalacOptions ++= Seq(
      "-Xmax-inlines:64",
    ),
    Compile / compile / wartremoverErrors ++= Warts.allBut(Wart.NoNeedImport),
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
    Compile / compile / wartremoverErrors ++= Warts
      .allBut(Wart.SeqApply, Wart.SeqUpdated),
  )
  .jsConfigure { project =>
    project
      .enablePlugins(ScalaJSBundlerPlugin)
      .enablePlugins(ScalablyTypedConverterPlugin)
  }

lazy val lmscanCommon = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .in(file("modules/lmscan-common"))
  .settings(Dependencies.lmscanCommon)
  .settings(Dependencies.tests)
  .jvmSettings(Dependencies.lmscanCommonJVM)
  .jvmSettings(
    Test / fork := true,
  )
  .jsSettings(Dependencies.lmscanCommonJS)
  .jsSettings(
    useYarn := true,
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.CommonJSModule)
    },
    scalacOptions ++= Seq(
      "-scalajs",
    ),
    externalNpm := {
      scala.sys.process.Process("yarn", baseDirectory.value).!
      baseDirectory.value
    },
    Test / fork := false,
    // Compile / compile / wartremoverErrors ++= Warts.all,
  )
  .jsConfigure { project =>
    project
      .enablePlugins(ScalaJSBundlerPlugin)
      .enablePlugins(ScalablyTypedConverterExternalNpmPlugin)
  }

lazy val lmscanFrontend = (project in file("modules/lmscan-frontend"))
  .enablePlugins(ScalaJSPlugin)
  .enablePlugins(ScalablyTypedConverterExternalNpmPlugin)
  .settings(Dependencies.lmscanFrontend)
  .settings(
    name := "leisuremeta-chain-lmscan-frontend",
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) },
    externalNpm := {
      scala.sys.process.Process("yarn", baseDirectory.value).!
      baseDirectory.value
    },
    scalacOptions ++= Seq(
      "-scalajs",
    ), // sbt-tpolecat bug: https://github.com/typelevel/sbt-tpolecat/issues/102
//    scalaJSUseMainModuleInitializer := true,
  )
  .dependsOn(lmscanCommon.js, api.js)

lazy val lmscanBackend = (project in file("modules/lmscan-backend"))
  .enablePlugins(FlywayPlugin)
  .settings(Dependencies.lmscanBackend)
  .settings(Dependencies.tests)
  .settings(
    name := "leisuremeta-chain-lmscan-backend",
    assemblyMergeStrategy := {
      case PathList("scala", "tools", "asm", xs @ _*) => MergeStrategy.first
      case PathList("io", "getquill", xs @ _*)        => MergeStrategy.first
      case x if x `contains` "io.netty.versions.properties" =>
        MergeStrategy.first
      case x if x `contains` "scala-asm.properties" =>
        MergeStrategy.first
      case x if x `contains` "compiler.properties" =>
        MergeStrategy.first
      case x if x `contains` "module-info.class" => MergeStrategy.discard
      case x =>
        val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
        oldStrategy(x)
    },
  )
  .settings(
    flywayUrl      := Settings.flywaySettings.url,
    flywayUser     := Settings.flywaySettings.user,
    flywayPassword := Settings.flywaySettings.pwd,
    flywaySchemas  := Settings.flywaySettings.schemas,
    flywayLocations ++= Settings.flywaySettings.locations,
  )
  .dependsOn(lmscanCommon.jvm)

lazy val lmscanAgent = (project in file("modules/lmscan-agent"))
  .settings(Dependencies.lmscanAgent)
  .settings(Dependencies.tests)
  .settings(
    name := "leisuremeta-chain-lmscan-agent",
    assemblyMergeStrategy := {
      case PathList("scala", "tools", "asm", xs @ _*) => MergeStrategy.first
      case PathList("io", "getquill", xs @ _*)        => MergeStrategy.first
      case x if x `contains` "io.netty.versions.properties" =>
        MergeStrategy.first
      case x if x `contains` "scala-asm.properties" =>
        MergeStrategy.first
      case x if x `contains` "compiler.properties" =>
        MergeStrategy.first
      case x if x `contains` "module-info.class" => MergeStrategy.discard
      case x =>
        val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
        oldStrategy(x)
    },
  )
  .dependsOn(api.jvm)


lazy val nodeProxy = (project in file("modules/node-proxy"))
  .settings(Dependencies.nodeProxy)
  .settings(Dependencies.tests)
  .settings(
    name := "leisuremeta-chain-node-proxy",
    assemblyMergeStrategy := {
      case x if x `contains` "okio.kotlin_module" => MergeStrategy.first
      case x if x `contains` "io.netty.versions.properties" =>
        MergeStrategy.first
      case x if x `contains` "module-info.class" => MergeStrategy.discard
      case x =>
        val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
        oldStrategy(x)
    },
  )
  .dependsOn(api.jvm)
