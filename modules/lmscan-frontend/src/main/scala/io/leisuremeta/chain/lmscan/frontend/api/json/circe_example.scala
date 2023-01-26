// 디코더 => Json => _.spaces2
// [scala 객체] =>[scala]  => [ io.circe.Json ]  => [json 객체]
// [json] => io.circe.parser => [ io.circe.Json ] => Decoder[Author] <= [scala]
package io.leisuremeta.chain.lmscan.frontend
import io.circe.{Decoder, Encoder}
import io.circe.syntax.*
import io.circe.*, io.circe.parser.*

case class Author(name: String, bio: Option[String])
case class Article(title: String, content: String, author: Author)

object CirceTest:

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

object EncoderTest:
  val huet = Author("휴 작가", None)
  val article = Article(
    title = "Article title",
    content = "Article content ...",
    author = huet,
  )
  implicit val authorEncoder: Encoder[Author] = author =>
    Json.obj(
      "name" -> author.name.asJson,
      "bio"  -> author.bio.asJson,
    )

  implicit val articleEncoder: Encoder[Article] = article =>
    Json.obj(
      "title"   -> article.title.asJson,
      "content" -> article.content.asJson,
      "author"  -> article.author.asJson,
    )

object DecoderTest:
  val validAuthor1  = Json.obj("name" -> "paola".asJson)
  val invalidAuthor = Json.obj("bio" -> 42.asJson)
  val validArticle = Json.obj(
    "title"   -> "how to make add".asJson,
    "content" -> "...".asJson,
    "author"  -> Json.obj("name" -> "phiol asdda".asJson),
  )

  implicit val authorDecoder: Decoder[Author] =
    Decoder.forProduct2("name", "bio")(Author.apply)

  implicit val articleDecoder: Decoder[Article] = json =>
    for
      title   <- json.get[String]("title")
      content <- json.get[String]("content")
      author  <- json.get[Author]("author")
    yield Article(title, content, author)

  val jsonString = validArticle.noSpaces
  val t          = io.circe.parser.parse(jsonString).flatMap(_.as[Article])

  val test1 = authorDecoder(validAuthor1.hcursor)
  val test2 =
    validAuthor1.as[Author] // test1,2 같은결과 // Right(Author(paola,None))
  val test3 = authorDecoder.decodeAccumulating(invalidAuthor.hcursor)
  val test4 = authorDecoder(invalidAuthor.hcursor)

object SampleJson:
  val tx_list = """{
    "sample_url": "http://localhost:8081/tx/list?useDataNav=true&pageNo=0&sizePerRequest=10",
    "totalCount": 21,
    "totalPages": 3,
    "payload": [
        {
            "hash": "7913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2dc",
            "txType": "account",
            "fromAddr": "26A463A0ED56A4A97D673A47C254728409C7B002",
            "toAddr": [
                "b775871c85faae7eb5f6bcebfd28b1e1b412235c"
            ],
            "amount": 1.2345678912345679E8,
            "blockHash": "6913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da",
            "eventTime": 1673939878,
            "createdAt": 21312412
        },
        {
            "hash": "7913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2dc",
            "txType": "account",
            "fromAddr": "26A463A0ED56A4A97D673A47C254728409C7B002",
            "toAddr": [
                "b775871c85faae7eb5f6bcebfd28b1e1b412235c"
            ],
            "amount": 1.2345678912345679E8,
            "blockHash": "6913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da",
            "eventTime": 1673939878,
            "createdAt": 21312412
        }]
    }
"""

  val parseResult = parse(tx_list) match
    case Left(failure) =>
      println("Invalid JSON :(")
    case Right(json) =>
      println("Yay, got some JSON!")
      parse(tx_list)
