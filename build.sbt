name := "play-action-step"

organization := "mrks"

scalaVersion := "2.13.1"

licenses += ("Apache-2.0", url("https://github.com/markussammallahti/play-action-step/blob/master/LICENSE"))

crossScalaVersions := Seq("2.12.10", "2.13.1")

scalacOptions ++= Seq(
  "-encoding", "utf-8",
  "-deprecation",
  "-unchecked",
  "-feature",
  "-Xfatal-warnings",
)

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play" % "2.7.+" % Provided
)

libraryDependencies ++= Seq(
  "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3" % Test
)
