name := "RxSocket"

version := "0.6-SNAPSHOT"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "com.netflix.rxjava" % "rxjava-scala" % "0.20.7",
  "net.liftweb" %% "lift-json" % "3.0-M8"
)

exportJars := true

//addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")

//useGpg := true