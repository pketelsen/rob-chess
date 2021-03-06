libraryDependencies  ++= Seq(
  "org.scalanlp" %% "breeze" % "0.11.2",
  "org.scalanlp" %% "breeze-natives" % "0.11.2",
  "com.kitfox.svg" % "svg-salamander" % "1.0"
)

resolvers ++= Seq(
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
  "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/"
)

lazy val root = (project in file(".")).
  settings(
    name := "rob-chess",
    version := "1.0",
    scalaVersion := "2.11.7"
  )

mainClass in assembly := Some("controller.Application")
assemblyJarName in assembly := "RobChess.jar"