val V = new {
  val Scala      = "2.13.6"
  val ScalaGroup = "2.13"

  val shapeless        = "2.3.7"
  val cats             = "2.4.2"
  val catsEffect       = "2.5.4"
  val kittens          = "2.3.2"
  val laminar          = "0.14.2"
  val sttp             = "2.2.10"
  val finch            = "0.32.1"
  val finagle          = "20.9.0"
  val pureconfig       = "0.17.0"
  val circe            = "0.13.0"
  val organiseImports  = "0.5.0"
  val betterMonadicFor = "0.3.1"
  val munit            = "0.7.29"
  val munitCatsEffect2 = "1.0.6"
  val scalacheck       = "1.15.4"
  val scalaJavaTime    = "2.3.0"
  val refined          = "0.9.27"
  val scodecBits       = "1.1.29"
  val scribe           = "3.6.1"
  val acyclic          = "0.2.1"
  val kindProjector    = "0.13.2"
  val splain           = "0.5.8"
  val scalaTypedHoles  = "0.1.9"
  val bouncycastle     = "1.69"
  val monix            = "3.4.0"
  val swaydb           = "0.16.2"
  val libthrift        = "0.10.0"
  val scrooge          = "20.9.0"
  val catbirdEffect    = "21.8.0"

  val scalaJsDom    = "2.0.0"
  val jsSha3        = "0.8.0"
  val elliptic      = "6.5.4"
  val typesElliptic = "6.4.12"
  val idbKeyval     = "5.0.5"
}

val Dependencies = new {
  private val finchModules =
    Seq("core", "circe").map("finchx-" + _)

//  private val sttpModules = Seq("core", "circe")

  private val refinedModules =
    "refined" +: Seq("cats", "scodec").map("refined-" + _)

  private val swaydbModules = Seq("swaydb", "cats-effect")

  lazy val client = Seq(
    libraryDependencies ++=
//      sttpModules.map("com.softwaremill.sttp.client" %%% _ % V.sttp) ++
        Seq(
          "com.raquo"    %%% "laminar"     % V.laminar,
          "org.scala-js" %%% "scalajs-dom" % V.scalaJsDom,
        ),
    Compile / npmDependencies ++= Seq(
      "idb-keyval" -> V.idbKeyval
    ),
  )

  lazy val node = Seq(
    libraryDependencies ++=
      finchModules.map("com.github.finagle" %% _ % V.finch) ++
        swaydbModules.map("io.swaydb"       %% _ % V.swaydb) ++
        Seq(
          "com.twitter"           %% "finagle-core"        % V.finagle,
          "com.twitter"           %% "finagle-http"        % V.finagle,
          "com.twitter"           %% "finagle-netty4-http" % V.finagle,
          "io.monix"              %% "monix-reactive"      % V.monix,
          "com.github.pureconfig" %% "pureconfig"          % V.pureconfig,
          "eu.timepit"            %% "refined-pureconfig"  % V.refined,
          "com.outr"              %% "scribe-slf4j"        % V.scribe,
        )
  )

  lazy val gossip = Seq(
    libraryDependencies ++=
      Seq(
        "com.outr"         %% "scribe-slf4j"   % V.scribe,
        "org.apache.thrift" % "libthrift"      % V.libthrift,
        "com.twitter"      %% "scrooge-core"   % V.scrooge,
        "com.twitter"      %% "finagle-thrift" % V.scrooge,
        "org.scodec"       %% "scodec-bits"    % V.scodecBits,
        "io.catbird"       %% "catbird-effect" % V.catbirdEffect,
      )
  )

  lazy val shared = Def.settings(
    libraryDependencies ++= refinedModules.map(
      "eu.timepit" %%% _ % V.refined
    ) ++
      Seq(
        "com.chuusai"       %%% "shapeless"       % V.shapeless,
        "org.typelevel"     %%% "kittens"         % V.kittens,
        "org.typelevel"     %%% "cats-effect"     % V.catsEffect,
        "io.circe"          %%% "circe-generic"   % V.circe,
        "io.circe"          %%% "circe-parser"    % V.circe,
        "io.circe"          %%% "circe-refined"   % V.circe,
        "org.scodec"        %%% "scodec-bits"     % V.scodecBits,
        "io.monix"          %%% "monix-tail"      % V.monix,
        "com.outr"          %%% "scribe"          % V.scribe,
        "io.github.cquiroz" %%% "scala-java-time" % V.scalaJavaTime,
      )
  )

  lazy val sharedJVM = Seq(
    libraryDependencies ++=
      Seq(
        "org.bouncycastle" % "bcprov-jdk15on" % V.bouncycastle
      )
  )

  lazy val sharedJS = Seq(
    Compile / npmDependencies ++= Seq(
      "js-sha3"         -> V.jsSha3,
      "elliptic"        -> V.elliptic,
      "@types/elliptic" -> V.typesElliptic,
    )
  )

  lazy val tests = Def.settings(
    libraryDependencies ++= Seq(
      "org.scalameta"  %%% "munit"            % V.munit      % Test,
      "org.scalameta"  %%% "munit-scalacheck" % V.munit      % Test,
      "org.scalacheck" %%% "scalacheck"       % V.scalacheck % Test,
    ),
    testFrameworks += new TestFramework("munit.Framework"),
  )

  lazy val testsJVM = Seq(
    libraryDependencies ++= Seq(
      "org.typelevel"  %%% "munit-cats-effect-2" % V.munitCatsEffect2 % Test,
    ),
  )
}

inThisBuild(
  Seq(
    organization := "org.leisuremeta",
    version      := "0.1.0-SNAPSHOT",
    scalaVersion := V.Scala,
    scalafixDependencies += "com.github.liancheng" %% "organize-imports" % V.organiseImports,
    semanticdbEnabled := true, // enable SemanticDB
    semanticdbVersion := "4.4.27", //scalafixSemanticdb.revision, // use Scalafix compatible version
    autoCompilerPlugins := true,
    //addCompilerPlugin("com.lihaoyi" %% "acyclic" % V.acyclic),
    addCompilerPlugin(
      "com.olegpy" %% "better-monadic-for" % V.betterMonadicFor
    ),
    addCompilerPlugin(
      "org.typelevel" %% "kind-projector" % V.kindProjector cross CrossVersion.full
    ),
    addCompilerPlugin("io.tryp" % "splain" % V.splain cross CrossVersion.patch),
    //addCompilerPlugin(
    //  "com.github.cb372" % "scala-typed-holes" % V.scalaTypedHoles cross CrossVersion.full
    //),
    scalacOptions ++= Seq(
      "-deprecation", // Emit warning and location for usages of deprecated APIs.
      "-explaintypes", // Explain type errors in more detail.
      "-feature", // Emit warning and location for usages of features that should be imported explicitly.
      "-language:existentials", // Existential types (besides wildcard types) can be written and inferred
      "-language:experimental.macros", // Allow macro definition (besides implementation and application)
      "-language:higherKinds", // Allow higher-kinded types
      "-language:implicitConversions", // Allow definition of implicit functions called views
      "-unchecked", // Enable additional warnings where generated code depends on assumptions.
      "-Vimplicits", // the compiler print implicit resolution chains when no implicit value can be found
      "-Vtype-diffs", // type error messages (found: X, required: Y) into colored diffs between the two types
      "-Xcheckinit", // Wrap field accessors to throw an exception on uninitialized access.
      "-Xfatal-warnings", // Fail the compilation if there are any warnings.
      "-Xlint:adapted-args", // Warn if an argument list is modified to match the receiver.
      "-Xlint:constant", // Evaluation of a constant arithmetic expression results in an error.
      "-Xlint:delayedinit-select", // Selecting member of DelayedInit.
      "-Xlint:doc-detached", // A Scaladoc comment appears to be detached from its element.
      "-Xlint:inaccessible", // Warn about inaccessible types in method signatures.
      "-Xlint:infer-any", // Warn when a type argument is inferred to be `Any`.
      "-Xlint:missing-interpolator", // A string literal appears to be missing an interpolator id.
      "-Xlint:nullary-unit",    // Warn when nullary methods return Unit.
      "-Xlint:option-implicit", // Option.apply used implicit view.
      "-Xlint:package-object-classes", // Class or object defined in package object.
      "-Xlint:poly-implicit-overload", // Parameterized overloaded implicit methods are not visible as view bounds.
      "-Xlint:private-shadow", // A private field (or class parameter) shadows a superclass field.
      "-Xlint:stars-align", // Pattern sequence wildcard must align with sequence component.
      "-Xlint:type-parameter-shadow", // A local type parameter shadows a type already in scope.
      "-Ywarn-dead-code", // Warn when dead code is identified.
      "-Ywarn-extra-implicit", // Warn when more than one implicit parameter section is defined.
      "-Ywarn-macros:after",
      "-Ywarn-numeric-widen",    // Warn when numerics are widened.
      "-Ywarn-unused:implicits", // Warn if an implicit parameter is unused.
      "-Ywarn-unused:imports", // Warn if an import selector is not referenced.
      "-Ywarn-unused:locals",  // Warn if a local definition is unused.
      "-Ywarn-unused:params",  // Warn if a value parameter is unused.
      "-Ywarn-unused:patvars", // Warn if a variable bound in a pattern is unused.
      "-Ywarn-unused:privates", // Warn if a private member is unused.
      "-Ywarn-value-discard", // Warn when non-Unit expression results are unused.
      "-Ybackend-parallelism",
      "8", // Enable paralellisation — change to desired number!
      "-Ycache-plugin-class-loader:last-modified", // Enables caching of classloaders for compiler plugins
      "-Ycache-macro-class-loader:last-modified", // and macro definitions. This can lead to performance improvements.
    ),
    //scalacOptions += "-P:typed-holes:log-level:info",
    //scalacOptions += "-P:acyclic:force",
    Compile / console / scalacOptions ~= (_.filterNot(
      Set(
        "-Ywarn-unused:imports",
        "-Xfatal-warnings",
      )
    )),
    //libraryDependencies += "com.lihaoyi" %%% "acyclic" % V.acyclic % "provided",
  )
)

lazy val root =
  (project in file(".")).aggregate(client, node, gossip, shared.js, shared.jvm)

lazy val client = (project in file("modules/client"))
  .dependsOn(shared.js)
  .enablePlugins(ScalaJSPlugin)
  .enablePlugins(ScalaJSBundlerPlugin)
  .enablePlugins(ScalablyTypedConverterPlugin)
  .settings(scalaJSUseMainModuleInitializer := true)
  .settings(
    Dependencies.client,
    Dependencies.sharedJS,
    Dependencies.tests,
    webpackBundlingMode := BundlingMode.LibraryAndApplication(),
    Test / scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.CommonJSModule)
    },
    Test / jsEnv := new org.scalajs.jsenv.nodejs.NodeJSEnv(),
  )

lazy val node = (project in file("modules/node"))
  .dependsOn(shared.jvm % "test->test;compile->compile")
  .dependsOn(gossip % "compile->compile")
  .settings(Dependencies.node)
  .settings(Dependencies.tests)
  .settings(Dependencies.testsJVM)
  .settings(
    name := "lmchain-core-node",
    Compile / console / scalacOptions ~= (_.filterNot(
      Set(
        "-Ywarn-unused:imports",
        "-Xfatal-warnings",
      )
    )),
    // assembly plugin related
    assembly / assemblyMergeStrategy := {
      case "BUILD"                       => MergeStrategy.discard
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case PathList(ps @ _*) if ps.last endsWith ".properties" =>
        MergeStrategy.first
      case PathList("module-info.class")         => MergeStrategy.discard
      case x if x.endsWith("/module-info.class") => MergeStrategy.discard
      case other => MergeStrategy.defaultMergeStrategy(other)
    },
    Test / fork := true,
  )

lazy val gossip = (project in file("modules/gossip"))
  .dependsOn(shared.jvm % "test->test;compile->compile")
  .settings(Dependencies.gossip)
  .settings(Dependencies.tests)
  .settings(
    name := "lmchain-gossip",
    //scroogeThriftIncludeRoot := false,
    Compile / scalacOptions ~= (_.filterNot(
      Set(
        "-Ywarn-unused:imports",
        "-Xfatal-warnings",
      )
    )),
  )

lazy val shared = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .in(file("modules/shared"))
  .settings(Dependencies.shared)
  .settings(Dependencies.tests)
  //.settings(
  //  Compile / compile / wartremoverWarnings ++= Warts
  //    .allBut(Wart.Any, Wart.Nothing, Wart.Serializable, Wart.StringPlusAny)
  //)
  .jvmSettings(Dependencies.sharedJVM)
  .jsSettings(Dependencies.sharedJS)
  .jsSettings(
    useYarn := true,
    Test / scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.CommonJSModule)
    },
  )
  .jsConfigure { project =>
    project
      .enablePlugins(ScalaJSBundlerPlugin)
      .enablePlugins(ScalablyTypedConverterPlugin)
  }

lazy val consoleClient = (project in file("modules/console"))
  .dependsOn(shared.jvm)
  .settings(Dependencies.node)
  .settings(Dependencies.tests)
  .settings(
    name := "lmchain-console-client",
    Compile / console / scalacOptions ~= (_.filterNot(
      Set(
        "-Ywarn-unused:imports",
        "-Xfatal-warnings",
      )
    )),
  )

lazy val fastOptCompileCopy = taskKey[Unit]("")

val jsPath = "modules/node/src/main/resources"

fastOptCompileCopy := {
  val files = (client / Compile / fastOptJS / webpack).value
  val f = files
    .find(
      _.metadata
        .get(BundlerFileTypeAttr)
        .exists(_ == BundlerFileType.ApplicationBundle)
    )
    .get
    .data
  val fmap = f.getParentFile / (f.getName + ".map")
  IO.copyFile(
    f,
    baseDirectory.value / jsPath / "client-fastopt-bundle.js",
  )
  IO.copyFile(
    fmap,
    baseDirectory.value / jsPath / "client-fastopt-bundle.js.map",
  )
}

lazy val fullOptCompileCopy = taskKey[Unit]("")

fullOptCompileCopy := {
  val files = (client / Compile / fullOptJS / webpack).value
  val f = files
    .find(
      _.metadata
        .get(BundlerFileTypeAttr)
        .exists(_ == BundlerFileType.ApplicationBundle)
    )
    .get
    .data
  IO.copyFile(
    f,
    baseDirectory.value / jsPath / "lmchain.js",
  )
}

addCommandAlias("assembly", ";fullOptCompileCopy; node/assembly")
addCommandAlias("runDev", ";fastOptCompileCopy; node/reStart --mode dev")
addCommandAlias("runProd", ";fullOptCompileCopy; node/reStart --mode prod")

val scalafixRules = Seq(
  "OrganizeImports",
  "DisableSyntax",
  "LeakingImplicitClassVal",
  "ProcedureSyntax",
  "NoValInForComprehension",
).mkString(" ")

val CICommands = Seq(
  "clean",
  "node/compile",
  "node/test",
  "client/compile",
  "client/fastOptJS",
  "client/test",
  "scalafmtCheckAll",
  s"scalafix --check $scalafixRules",
).mkString(";")

val PrepareCICommands = Seq(
  s"compile:scalafix --rules $scalafixRules",
  s"test:scalafix --rules $scalafixRules",
  "test:scalafmtAll",
  "compile:scalafmtAll",
  "scalafmtSbt",
).mkString(";")

addCommandAlias("ci", CICommands)

addCommandAlias("preCI", PrepareCICommands)
