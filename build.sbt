name := "RxSocket"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "com.netflix.rxjava" % "rxjava-scala" % "0.20.7"
)

exportJars := true

//addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")

//useGpg := true