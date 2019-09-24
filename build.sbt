name := "ergo-utility"

organization := "org.ergoplatform"

version := "0.1"

scalaVersion := "2.12.10"

resolvers ++= Seq(
  "Sonatype Public" at "https://oss.sonatype.org/content/groups/public/",
  "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/",
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
  "Typesafe maven releases" at "http://repo.typesafe.com/typesafe/maven-releases/"
)

val ergoWalletVersion = "master-5449e2f4-SNAPSHOT"

libraryDependencies ++= Seq(
  "org.ergoplatform" %% "ergo-wallet" % ergoWalletVersion,

  "org.typelevel" %% "cats-effect" % "2.0.0-RC2",

  "dev.zio" %% "zio" % "1.0.0-RC13",
  "dev.zio" %% "zio-interop-cats" % "2.0.0.0-RC3",

  "com.joefkelley" %% "argyle" % "1.0.0",
)

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-language:experimental.macros",
  "-language:higherKinds",
  "-language:postfixOps",
  "-feature",
  "-unchecked",
  "-Xfuture",
  "-Yno-adapted-args",
  "-Ywarn-numeric-widen",
  "-Ypartial-unification"
)

test in assembly := {}

assemblyJarName in assembly := s"${name.value}-${version.value}.jar"

assemblyMergeStrategy in assembly := {
  case "logback.xml" => MergeStrategy.first
  case "module-info.class" => MergeStrategy.discard
  case other => (assemblyMergeStrategy in assembly).value(other)
}
