import AssemblyKeys._  // put this at the top of the file
name := "sportTogether"

version := "1.4.19"

scalaVersion := "2.11.8"

assemblySettings

lazy val buildSettings = Seq(
  version := "0.1-SNAPSHOT",
  organization := "com.example",
  scalaVersion := "2.10.1"
)

val app = (project in file("app")).
  settings(buildSettings: _*).
  settings(assemblySettings: _*).
  settings(

  )
test in assembly := {}
libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.1" % "test"
libraryDependencies += "org.scalamock" %% "scalamock-scalatest-support" % "3.2.2" % "test"
libraryDependencies += "net.databinder.dispatch" %% "dispatch-core" % "0.11.2"

libraryDependencies ++= {
  val akkaV = "2.3.9"
  val sprayV = "1.3.3"
  Seq(
    "io.spray"                      %%  "spray-can"             % sprayV,
    "io.spray"                      %%  "spray-routing"         % sprayV,
    "io.spray"                      %%  "spray-testkit"         % sprayV    % "test",
    "io.spray"                      %%  "spray-httpx"           % sprayV,
    "io.spray"                      %%  "spray-json"            % "1.3.2",
    "io.spray"                      %%  "spray-client"          % sprayV,
    "io.spray"                      %%  "spray-http"            % sprayV,
    "com.typesafe.akka"             %%  "akka-actor"            % akkaV,
    "com.typesafe.akka"             %%  "akka-testkit"          % akkaV     % "test",
    "com.typesafe.scala-logging"    %%  "scala-logging"         % "3.5.0",
    "ch.qos.logback"                %   "logback-classic"       % "1.1.7",
    "org.specs2"                    %%  "specs2-core"           % "2.3.11"  % "test",
    "org.jetbrains"                 %   "annotations"           % "15.0",
    "mysql"                         %   "mysql-connector-java"  % "5.1.36",
    "com.typesafe.slick"            %   "slick_2.11"            % "3.1.1",
    "com.typesafe.slick"            %   "slick-hikaricp_2.11"   % "3.1.1",
    "net.debasishg"                 %%  "redisclient"           % "3.3"
  )
}
