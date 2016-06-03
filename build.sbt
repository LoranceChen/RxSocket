name := "RxSocket"

version := "0.9.3"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "io.reactivex" %% "rxscala" % "0.26.1",
  "net.liftweb" %% "lift-json" % "3.0-M8"
)

exportJars := true

//addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")

//useGpg := true