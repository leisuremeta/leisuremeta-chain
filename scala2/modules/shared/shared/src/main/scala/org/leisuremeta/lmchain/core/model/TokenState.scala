package org.leisuremeta.lmchain.core
package model

import scodec.bits.ByteVector

import datatype.{BigNat, Utf8}
import Transaction.Token.DefinitionId

final case class TokenState(
    networkId: NetworkId,
    definitionId: DefinitionId,
    name: Utf8,
    symbol: Utf8,
    divisionSize: BigNat,
    data: ByteVector,
    admin: Option[Account],
    totalAmount: BigNat,
    divisionAmount: Vector[BigNat],
)
