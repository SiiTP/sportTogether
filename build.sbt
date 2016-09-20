name := "scAccountService"

version := "1.0"
scalaVersion := "2.11.8"

libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.1" % "test"
libraryDependencies += "org.scalamock" %% "scalamock-scalatest-support" % "3.2.2" % "test"

libraryDependencies ++= {
  val akkaV = "2.3.9"
  val sprayV = "1.3.3"
  Seq(
    "io.spray"            %%  "spray-can"             % sprayV,
    "io.spray"            %%  "spray-routing"         % sprayV,
    "io.spray"            %%  "spray-testkit"         % sprayV    % "test",
    "com.typesafe.akka"   %%  "akka-actor"            % akkaV,
    "com.typesafe.akka"   %%  "akka-testkit"          % akkaV     % "test",
    "org.specs2"          %%  "specs2-core"           % "2.3.11"  % "test",
    "org.jetbrains"       %   "annotations"           % "15.0",
    "mysql"               %   "mysql-connector-java"  % "5.1.36",
    "com.typesafe.slick"  %   "slick_2.11"            % "3.1.1",
    "org.slf4j"           %   "slf4j-nop"             % "1.6.4"
  )
}