import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import scalariform.formatter.preferences._

lazy val rsc = (project in file("."))
  .settings(scalariformSettings: _*)
  .settings(
    version := "1.0-SNAPSHOT",
    scalaVersion := "2.11.6",
    organization := "swave.io",
    description := "Comparison of Reactive Streams implementations",
    startYear := Some(2015),
    licenses := Seq("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt")),
    javacOptions ++= Seq(
      "-encoding", "UTF-8",
      "-source", "1.8",
      "-target", "1.8",
      "-Xlint:unchecked",
      "-Xlint:deprecation"),
    scalacOptions ++= List(
      "-encoding", "UTF-8",
      "-feature",
      "-unchecked",
      "-deprecation",
      "-Xlint",
      "-language:_",
      "-target:jvm-1.8",
      "-Xlog-reflective-calls"),
    ScalariformKeys.preferences := ScalariformKeys.preferences.value
      .setPreference(RewriteArrowSymbols, true)
      .setPreference(AlignParameters, true)
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(DoubleIndentClassDeclaration, true)
      .setPreference(PreserveDanglingCloseParenthesis, true),
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % "2.3.11",
      "com.typesafe.akka" %% "akka-stream-experimental" % "1.0-RC3",
      "io.reactivex"	%% "rxscala"	% "0.24.1",
      "io.projectreactor"	% "reactor-core"	% "2.0.3.RELEASE",
      "io.projectreactor"	% "reactor-stream"	% "2.0.3.RELEASE",
      "ch.qos.logback" % "logback-classic" % "1.1.2"))