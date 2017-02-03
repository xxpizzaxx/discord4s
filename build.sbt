name := "discord4s"

version := "1.0"

scalaVersion := "2.11.8"

resolvers += "jitpack" at "https://jitpack.io"

resolvers += Resolver.jcenterRepo

libraryDependencies ++= Seq(
  "com.github.austinv11"        % "Discord4J"     % "2.7.0",
  "moe.pizza"                  %% "eveapi"        % "0.56",
  "eveapi"                     %% "esi-client"    % "1.230.0"
)

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest"   % "2.2.6"   % "test",
  "org.mockito"   % "mockito-core" % "1.10.19" % "test"
)