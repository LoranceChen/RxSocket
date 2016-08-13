import sbt._

version := "0.9.4"

name := "rxsocket"

organization := "com.lorancechen"

scalaVersion := "2.11.7"

ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) }

overridePublishSettings

sonatypeProfileName := "com.github.lorancechen"

//exportJars := true
//enablePlugins(SignedAetherPlugin)
//
//disablePlugins(AetherPlugin)
//

//useGpg := true
//
//pgpReadOnly := false

//Configuration: Key Pair Locations
//pgpSecretRing := file(Path.userHome + ".sbt/gpg/secring.asc")
//
//pgpPublicRing := file(Path.userHome + ".sbt/gpg/pubring.asc")

//credentials += Credentials("Sonatype Nexus",
//  "localhost:7070",
//  "admin",
//  "admin123")

publishArtifact in (Compile, packageDoc) := false

resolvers ++= Seq(
  "main" at "http://repo1.maven.org/maven2",

//  "Sonatype Nexus" at "http://localhost:7070/nexus/repository/maven-releases/"//"https://oss.sonatype.org/content/repositories/snapshots"
  "Sonatype Nexus" at "https://oss.sonatype.org/content/repositories/snapshots"
)

externalResolvers := Resolver.withDefaultResolvers(resolvers.value, mavenCentral = false)

parallelExecution in Test := false

publishTo <<= version { (v: String) =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
//    Some("releases"  at "http://localhost:7070/nexus/repository/maven-releases/")

}

libraryDependencies ++= Seq(
  "io.reactivex" %% "rxscala" % "0.26.2",
  "net.liftweb" %% "lift-json" % "3.0-RC3"
)

//
//publishArtifact in Test := false
//
//pomIncludeRepository := { _ => false }
//
////credentials += Credentials(Path.userHome / ".ivy2" / ".nexus_cred")

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
        <url>https://www.scalachan.com/</url>
      </developer>
    </developers>

