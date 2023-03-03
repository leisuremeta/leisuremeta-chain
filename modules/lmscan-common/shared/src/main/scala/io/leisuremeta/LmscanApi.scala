package io.leisuremeta.chain.lmscan.common

import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import io.circe.generic.auto.*

sealed trait ExplorerFailure
object ExplorerFailure:
  final case class InternalFailure(msg: String) extends ExplorerFailure
  final case class ExternalFailure(msg: String) extends ExplorerFailure


object LmscanApi:
  opaque type Utf8 = String
  object Utf8:
    def apply(s: String): Utf8 = s
  extension (u: Utf8)
    def asString: String = u

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val helloEndpoint: PublicEndpoint[Unit, ExplorerFailure, Utf8, Any] = endpoint
    .get
    .in("hello")
    .errorOut(jsonBody[ExplorerFailure])
    .out(jsonBody[Utf8])
