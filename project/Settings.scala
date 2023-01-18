import sbt._

object Settings {
    val flywaySettings = new {
        lazy val url = "jdbc:postgresql://127.0.0.1:5432/postgres"
        lazy val user = "postgres"
        lazy val pwd = ""
        lazy val schemas = Seq("")
        lazy val locations = Seq("", "")
    }
}
