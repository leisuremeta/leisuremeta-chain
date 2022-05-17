package io.leisuremeta.chain
package api

import io.circe.KeyEncoder
import io.circe.generic.auto.*
import sttp.client3.*
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.*

import lib.datatype.{BigNat, UInt256Bytes}
import api.model.Transaction

object LeisureMetaChainApi:

  given Schema[UInt256Bytes] = Schema.string
  given Schema[BigNat] = Schema.schemaForBigInt.map[BigNat] {
    (bigint: BigInt) => BigNat.fromBigInt(bigint).toOption
  } { (bignat: BigNat) => bignat.toBigInt }
  given [K: KeyEncoder, V: Schema]: Schema[Map[K, V]] =
    Schema.schemaForMap[K, V](KeyEncoder[K].apply)

//  import UInt256Bytes.given
  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val postTxEndpoint =
    endpoint.post
      .in("tx")
      .in(jsonBody[Transaction])
      .out(jsonBody[UInt256Bytes])

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getStatusEndpoint = endpoint.get.in("status").out(jsonBody[String])
