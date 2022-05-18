package io.leisuremeta.chain
package api.model


import lib.crypto.Signature

final case class AccountSignature(
    sig: Signature,
    name: Account,
)
