name := "discord4s"

version := "1.0"

scalaVersion := "2.11.8"

resolvers += "jitpack" at "https://jitpack.io"

resolvers += Resolver.jcenterRepo

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

libraryDependencies ++= Seq(
  "com.github.austinv11"        % "Discord4J"     % "2.7.0",
  "moe.pizza"                  %% "eveapi"        % "0.56",
  "eveapi"                     %% "esi-client"    % "1.419.0",
  "io.frees" %% "freestyle"              % "0.1.1",
  "io.frees" %% "freestyle-effects"      % "0.1.1",
  "io.frees" %% "freestyle-tagless"      % "0.1.1",
  "io.frees" %% "freestyle-async"        % "0.1.1",
  "io.frees" %% "freestyle-async-fs2"    % "0.1.1",
  "org.typelevel" %% "cats-effect" % "0.3-d37204d",
  "org.mockito" % "mockito-all" % "2.0.2-beta"
)

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest"   % "2.2.6"   % "test",
  "org.mockito"   % "mockito-core" % "1.10.19" % "test"
)