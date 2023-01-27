package io.leisuremeta.chain.lmscan.frontend
import io.circe.*, io.circe.generic.semiauto.*
import io.circe.syntax.*
import io.circe.{Decoder, Encoder}

case class Tx(
    hash: String,
    txType: String,
    fromAddr: List[String],
    toAddr: List[String],
    amount: Long,
    blockHash: String,
    eventTime: Int,
    createdAt: Int,
)
case class Payload(tx_list: List[Tx])
case class TxList(totalCount: Int, totalPages: Int, payload: Payload)

object Parser:
  implicit val txDecoder: Decoder[Tx] = deriveDecoder
  implicit val txEncoder: Encoder[Tx] = deriveEncoder

object CustomEncoder:
  implicit val txListEncoder: Encoder[TxList] = txList =>
    Json.obj(
      "totalCount" -> txList.totalCount.asJson,
      "totalPages" -> txList.totalPages.asJson,
      //   "payload"    -> txList.payload.asJson,
    )

// object CustomDecoder:
//   implicit val txListDecoder: Decoder[TxList] =
//     Decoder.forProduct3("totalCount", "totalPages", "payload")(TxList.apply)

case class Author(name: String, bio: Option[String])
case class Article(title: String, content: String, author: Author)
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
