addSbtPlugin("org.scala-js"       % "sbt-scalajs"              % "1.7.1")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.1.0")
addSbtPlugin("io.spray"           % "sbt-revolver"             % "0.9.1")

addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.20.0")
addSbtPlugin("org.scalablytyped.converter" % "sbt-converter" % "1.0.0-beta37")

addSbtPlugin("com.eed3si9n"     % "sbt-assembly" % "1.1.0")
addSbtPlugin("org.scalameta"    % "sbt-scalafmt" % "2.4.3")
addSbtPlugin("ch.epfl.scala"    % "sbt-scalafix" % "0.9.31")
addSbtPlugin("com.timushev.sbt" % "sbt-updates"  % "0.5.3")

addSbtPlugin("org.wartremover" % "sbt-wartremover" % "2.4.16")

libraryDependencies += "org.scala-js" %% "scalajs-env-nodejs" % "1.2.1"

addSbtPlugin("com.twitter" % "scrooge-sbt-plugin" % "20.9.0")
