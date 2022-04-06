package org.leisuremeta.lmchain.core.datatype

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.NonNegative
import eu.timepit.refined.refineV

trait BigNatDefinition {
  type BigNat = BigInt Refined NonNegative

  object BigNat {
    val Zero: BigNat = refineV[NonNegative](BigInt(0)).toOption.get
    val One: BigNat  = refineV[NonNegative](BigInt(1)).toOption.get

    def fromBigInt(bigint: BigInt): Either[String, BigNat] =
      refineV[NonNegative](bigint)

    @SuppressWarnings(Array("org.wartremover.warts.Throw"))
    def unsafeFromBigInt(n: BigInt): BigNat = fromBigInt(n) match {
      case Right(nat) => nat
      case Left(e)    => throw new Exception(e)
    }

    @SuppressWarnings(Array("org.wartremover.warts.Throw"))
    def unsafeFromLong(l: Long): BigNat =
      refineV[NonNegative](BigInt(l)) match {
        case Right(nat) => nat
        case Left(e)    => throw new Exception(e)
      }

    @SuppressWarnings(Array("org.wartremover.warts.Throw"))
    def unsafeFromInt(n: Int): BigNat =
      refineV[NonNegative](BigInt(n)) match {
        case Right(nat) => nat
        case Left(e)    => throw new Exception(e)
      }

    @SuppressWarnings(Array("org.wartremover.warts.Throw"))
    def add(x: BigNat, y: BigNat): BigNat =
      refineV[NonNegative](x.value + y.value) match {
        case Right(nat) => nat
        case Left(e)    => throw new Exception(e)
      }
  }
}
