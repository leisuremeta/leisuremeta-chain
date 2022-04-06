package org.leisuremeta.lmchain.core
package crypto

import datatype.UInt256BigInt

final case class KeyPair(privateKey: UInt256BigInt, publicKey: PublicKey) {
  override lazy val toString: String =
    s"KeyPair(${privateKey.toBytes.toHex}, $publicKey)"
}
