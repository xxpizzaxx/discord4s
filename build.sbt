name := "discord4s"

version := "1.0"

scalaVersion := "2.11.8"

resolvers += "jitpack" at "https://jitpack.io"

resolvers += Resolver.jcenterRepo

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.4")


libraryDependencies ++= Seq(
  "com.github.austinv11"        % "Discord4J"     % "2.7.0",
  "moe.pizza"                  %% "eveapi"        % "0.56",
  "eveapi"                     %% "esi-client"    % "1.419.0",
  "org.typelevel" %% "cats-effect" % "0.3-d37204d",
  "org.mockito" % "mockito-all" % "2.0.2-beta",
  "io.circe" %% "circe-core" % "0.8.0",
  "io.circe" %% "circe-generic" % "0.8.0",
  "io.circe" %% "circe-jawn" % "0.8.0",
  "io.circe" %% "circe-numbers" % "0.8.0",
  "io.circe" %% "circe-parser" % "0.8.0",
  "com.lihaoyi" %% "fastparse" % "0.4.3"
)

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest"   % "2.2.6"   % "test",
  "org.mockito"   % "mockito-core" % "1.10.19" % "test"
)
