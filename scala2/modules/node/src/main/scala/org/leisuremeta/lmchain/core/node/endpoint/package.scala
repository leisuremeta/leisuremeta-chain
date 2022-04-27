package org.leisuremeta.lmchain.core
package node

import scala.util.Try

import io.finch.{DecodeEntity, DecodePath}
import scodec.bits.ByteVector
import shapeless.tag.@@

import datatype.{UInt256Bytes, UInt256Refine}
import model.{Account, Address}
import model.Account.Name

package object endpoint {

  implicit def taggedUint256Path[A]: DecodePath[UInt256Bytes @@ A] =
    uint256DecodePath.asInstanceOf[DecodePath[UInt256Bytes @@ A]]

  implicit val uint256DecodePath: DecodePath[UInt256Bytes] = { s =>
    (for {
      bytes   <- ByteVector.fromHexDescriptive(s)
      refined <- UInt256Refine.from(bytes)
    } yield refined).toOption
  }

  implicit val addressDecodePath: DecodePath[Address] =
    Address.fromHex(_).toOption

  implicit val addressDecodeEntity: DecodeEntity[Address] =
    Address.fromHex(_).left.map(new Exception(_))

  implicit val bigintDecodePath: DecodePath[BigInt] = { s =>
    Try(BigInt(s)).toOption
  }

  implicit val bigintDecodeEntity: DecodeEntity[BigInt] = { s =>
    Try(BigInt(s)).toEither
  }

  implicit val nameDecodePath: DecodePath[Name] = Name.from(_).toOption

  implicit val accountDecodePath: DecodePath[Account] = { s =>
    val addressOption: Option[Account] = for {
      sWithoutPrefix <- Option.when(s.startsWith("0x"))(s `drop` 2)
      address        <- Address.fromHex(sWithoutPrefix).toOption
    } yield Account.Unnamed(address)

    addressOption.orElse(Account.Name.from(s).map(Account.Named(_)).toOption)
  }
}
