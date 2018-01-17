version := "0.10.1"

name := "rxsocket"

organization := "com.scalachan"

scalaVersion := "2.12.4"

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-language:postfixOps",
  "-language:implicitConversions"
)

resolvers ++= Seq(
  "main" at "http://repo1.maven.org/maven2",

  "Sonatype Nexus" at "https://oss.sonatype.org/content/repositories/snapshots"
)

parallelExecution in Test := false

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}


libraryDependencies ++= Seq(
  "io.reactivex" %% "rxscala" % "0.26.5",
  "org.json4s" %% "json4s-native" % "3.6.0-M1",
  "org.slf4j" % "slf4j-api" % "1.7.25",
  "ch.qos.logback" % "logback-classic" % "1.2.3" % Test,
  "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",
  "junit" % "junit" % "4.12" % Test,

)


lazy val commonPublishSettings = Seq (
  pgpSecretRing := file(Path.userHome + "/.sbt/gpg/secring.asc"),
  pgpPublicRing := file(Path.userHome + "/.sbt/gpg/pubring.asc"),
  publishArtifact in (Compile, packageDoc) := true,
  publishArtifact in Test := false,
  publishMavenStyle := true,
  credentials += Credentials(Path.userHome / ".ivy2" / ".new_card"),
  publishArtifact in (Compile, packageDoc) := true,
  publishArtifact in Test := false,
  sonatypeProfileName := "com.scalachan",

)

lazy val root = (project in file(".")).
  settings(commonPublishSettings).
  settings(
    resolvers ++= Seq(
      "main" at "http://repo1.maven.org/maven2",
      "Sonatype Nexus" at "https://oss.sonatype.org/content/repositories/snapshots"
    ),
    publishArtifact in (Compile, packageDoc) := true,
    publishArtifact in Test := false
  ).
  settings(
    pomExtra in Global :=
      <url>https://github.com/LoranceChen/RxSocket</url>
        <licenses>
          <license>
            <name>Apache License, Version 2.0</name>
            <url>http://www.apache.org/licenses/LICENSE-2.0</url>
          </license>
        </licenses>
        <scm>
          <url>git@github.com/LoranceChen/RxSocket.git</url>
          <connection>scm:git:git@github.com/LoranceChen/RxSocket.git</connection>
        </scm>
        <developers>
          <developer>
            <id>lorancechen</id>
            <name>UnlimitedCode Inc.</name>
            <url>http://www.scalachan.com/</url>
          </developer>
        </developers>

  )
