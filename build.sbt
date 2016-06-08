name := "SentimentAnalyse"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "org.apache.spark" % "spark-mllib_2.11" % "1.5.2" ,
  "org.clulab" %% "processors" % "5.7.1" excludeAll(
    ExclusionRule(organization = "org.json4s")
    ),
  "org.clulab" %% "processors" % "5.7.1" classifier "models",
  "net.ruippeixotog" %% "scala-scraper" % "0.1.2",
  "com.github.wookietreiber" %% "scala-chart" % "latest.integration",
  "org.scala-lang.modules" %% "scala-swing" % "1.0.1",
  "com.novocode" % "junit-interface" % "0.8" % "test->default"
)