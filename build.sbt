//import sbt._

version := "0.10.1"

name := "rxsocket"

organization := "com.scalachan"

scalaVersion := "2.12.2"

ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) }

overridePublishSettings

sonatypeProfileName := "com.scalachan"

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-language:postfixOps",
  "-language:implicitConversions"
)

//exportJars := true
enablePlugins(SignedAetherPlugin)
//
disablePlugins(AetherPlugin)

//useGpg := true
//
//pgpReadOnly := false

//Configuration: Key Pair Locations
pgpSecretRing := file(Path.userHome + "/.sbt/gpg/secring.asc")

pgpPublicRing := file(Path.userHome + "/.sbt/gpg/pubring.asc")

publishArtifact in (Compile, packageDoc) := true

resolvers ++= Seq(
  "main" at "http://repo1.maven.org/maven2",

//  "Sonatype Nexus" at "http://localhost:7070/nexus/repository/maven-releases/"//"https://oss.sonatype.org/content/repositories/snapshots"
  "Sonatype Nexus" at "https://oss.sonatype.org/content/repositories/snapshots"
)

//externalResolvers := Resolver.withDefaultResolvers(resolvers.value, mavenCentral = false)

parallelExecution in Test := false

publishTo <<= version { (v: String) =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
//  Some("Sonatype Nexus Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots")
  else
    Some("Sonatype Nexus Staging"  at nexus + "service/local/staging/deploy/maven2")
//    Some("releases"  at "http://localhost:7070/nexus/repository/maven-releases/")

}

libraryDependencies ++= Seq(
  "io.reactivex" %% "rxscala" % "0.26.5",
  "net.liftweb" %% "lift-json" % "3.0.1"
)

//
publishArtifact in Test := false

//pomIncludeRepository := { _ => false }
//
credentials += Credentials(Path.userHome / ".ivy2" / ".nexus_cred")

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

