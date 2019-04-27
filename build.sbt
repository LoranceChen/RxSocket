version := "0.13.1"

name := "rxsocket"

organization := "com.scalachan"

scalaVersion := "2.12.8"
crossScalaVersions := Seq("2.12.8", "2.11.12")

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-language:postfixOps",
  "-language:implicitConversions",
  "-language:higherKinds",
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
  "io.monix" %% "monix" % "3.0.0-RC1",
  "org.json4s" %% "json4s-native" % "3.6.0-M3",
  "org.slf4j" % "slf4j-api" % "1.7.25",
  "com.typesafe.akka" %% "akka-actor" % "2.5.20",
  "ch.qos.logback" % "logback-classic" % "1.2.3" % Test,
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

lazy val example = (project in file("example"))
  .settings(
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.2.3" % Test,
    )
  )
  .dependsOn(root)

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

fork := true
updateOptions := updateOptions.value.withCachedResolution(true)
