package org.leisuremeta.lmchain.core
package model

import datatype.BigNat

final case class NameState(
    addressess: Map[Address, BigNat],
    threshold: BigNat,
    guardian: Option[Account],
)
