package io.leisuremeta.chain
package api

import io.circe.KeyEncoder
import io.circe.generic.auto.*
import io.circe.refined.*
import sttp.client3.*
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.{*, given}

import lib.crypto.{Hash, Signature}
import lib.datatype.{BigNat, UInt256BigInt, UInt256Bytes, Utf8}
import api.model.{AccountSignature, NodeStatus, Signed, Transaction}
import io.leisuremeta.chain.api.model.AccountSignature

object LeisureMetaChainApi:

  given Schema[UInt256Bytes] = Schema.string
  given Schema[UInt256BigInt] = Schema(SchemaType.SInteger())
  given Schema[BigNat] = Schema.schemaForBigInt.map[BigNat] {
    (bigint: BigInt) => BigNat.fromBigInt(bigint).toOption
  } { (bignat: BigNat) => bignat.toBigInt }
  given Schema[Utf8] = Schema.string
  given [K: KeyEncoder, V: Schema]: Schema[Map[K, V]] =
    Schema.schemaForMap[K, V](KeyEncoder[K].apply)
  given [A]: Schema[Hash.Value[A]] = Schema.string
  given Schema[Signature.Header] = Schema(SchemaType.SInteger())

//  import UInt256Bytes.given
  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val postTxEndpoint =
    endpoint.post
      .in("tx")
      .in(jsonBody[Signed.Tx])
      .out(jsonBody[Signed.TxHash])
      .errorOut(plainBody[String])

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getStatusEndpoint = endpoint.get.in("status").out(jsonBody[NodeStatus])
