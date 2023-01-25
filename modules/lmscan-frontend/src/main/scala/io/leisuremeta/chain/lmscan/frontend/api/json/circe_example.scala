package io.leisuremeta.chain.lmscan.frontend
import java.util.UUID
import io.circe.Json
import io.circe.*
import io.circe.syntax.*

object Circe:

  val jsonString  = Json.fromString("Hello there")
  val jsonString2 = "Hello there".asJson

  val jsonNumber  = Json.fromInt(42)
  val jsonNumber2 = 42.asJson

  val jsonArray = Json.arr(jsonString, jsonNumber)

  val jsonObj = Json.obj(
    "foo" -> "bar".asJson,
  )
  def mapString(json: Json) = json.mapString(_.toUpperCase())
  def mapArray(json: Json)  = json.mapArray(_.map(_.mapString(_.toUpperCase())))

  val complexObj = Json.obj("nested" -> jsonObj)
  val transFormedObject = complexObj.hcursor
    .downField("nested")
    .downField("foo")
    .withFocus(_.mapString(_.reverse))
    .top
    .map(_.noSpaces)

case class Author(name: String, bio: Option[String])
case class Article(id: UUID, title: String, content: String, author: Author)

object Encoder:
  val huet = Author("asfas", None)
//   val author = Author("asfas", None)
  val article = Article(
    UUID.randomUUID(),
    title = "the zipper",
    content = "the main draw ...",
    author = huet,
  )
  implicit val authorEncoder: Encoder[Author] = author =>
    Json.obj(
      "name" -> author.name.asJson,
      "bio"  -> author.bio.asJson,
    )

  implicitly[Encoder[Option[Author]]]
