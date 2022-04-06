package org.leisuremeta.lmchain.core
package node
package service

import java.time.Instant

import cats.Id
import cats.data.EitherT

import scodec.bits.ByteVector

import crypto.CryptoOps
import crypto.Hash.ops._
import crypto.MerkleTrie
import crypto.MerkleTrie.MerkleTrieState
import crypto.Sign.ops._
import datatype.{BigNat, UInt256Refine, Utf8}
import failure.DecodingFailure
import model.{
  Account,
  AccountSignature,
  Address,
  NetworkId,
  Signed,
  TokenState,
  Transaction,
}
import Transaction.Token.{DefinitionId, TokenId}
import repository.{StateRepository, TransactionRepository}
import repository.StateRepository._
import store.KeyValueStore

class StateServiceTest extends munit.FunSuite {

  implicit def idKvStore[K, V]: KeyValueStore[Id, K, V] =
    new KeyValueStore[Id, K, V] {

      private var map: Map[K, V] = Map.empty

      def get(key: K): EitherT[Id, DecodingFailure, Option[V]] =
        EitherT.rightT[Id, DecodingFailure](map.get(key))
      def put(key: K, value: V): Unit = {
        this.map = map.updated(key, value)
      }
      def remove(key: K): Unit = {
        this.map = map - key
      }
    }

  implicit def stateRepo[K, V] = StateRepository.fromStores[Id, K, V]

  implicit def txRepo = new TransactionRepository[Id] {

    private var map: Map[Signed.TxHash, Signed.Tx] = Map.empty

    def get(
        transactionHash: Signed.TxHash
    ): EitherT[Id, DecodingFailure, Option[Signed.Tx]] =
      EitherT.rightT[Id, DecodingFailure](map.get(transactionHash))
    def put(transaction: Signed.Tx): Unit = {
      this.map = map.updated(transaction.toHash, transaction)
    }
  }

  val key = CryptoOps.generate()

  def sign(tx: Transaction): Signed.Tx = {
    val sig = AccountSignature.UnnamedSignature(key.sign(tx).toOption.get)
    Signed(sig, tx)
  }

  val defId = UInt256Refine
    .from(
      ByteVector.fromValidHex(
        "4c90ac0e5afcba21156bab9dc77e14e16d1ee068770eb0cd81bc364ce7138509"
      )
    )
    .toOption
    .get

  val tx1: Transaction = Transaction.Token.DefineToken(
    networkId = NetworkId(BigNat.unsafeFromBigInt(102)),
    createdAt = Instant.now(),
    definitionId = Transaction.Token.DefinitionId(defId),
    name = Utf8.unsafeFrom("WONT"),
    symbol = Utf8.unsafeFrom("WONT"),
    divisionSize = BigNat.unsafeFromBigInt(4),
    data = ByteVector.empty,
  )

  def mint: Transaction = Transaction.Token.MintToken(
    networkId = NetworkId(BigNat.unsafeFromBigInt(102)),
    createdAt = Instant.now(),
    definitionId = DefinitionId(defId),
    tokenId = TokenId(defId),
    divisionIndex = Some(BigNat.unsafeFromBigInt(1)),
    outputs = Map(
      Account.Unnamed(Address.fromPublicKeyHash(key.publicKey.toHash)) ->
        BigNat.unsafeFromBigInt(1)
    ),
  )

  def getTokenState(
      state: StateService.MerkleState
  ): EitherT[Id, String, Option[TokenState]] = {
    MerkleTrie
      .get[Id, DefinitionId, TokenState](defId.bits)
      .runA(state.tokenState)
  }

  test("mint token") {
    val emptyState: StateService.MerkleState = StateService.MerkleState(
      namesState = MerkleTrieState.empty,
      tokenState = MerkleTrieState.empty,
      balanceState = MerkleTrieState.empty,
    )

    val result = for {
      state1 <- StateService.updateStateWithTx[Id](emptyState, sign(tx1))
      ts1    <- getTokenState(state1)
      state2 <- StateService.updateStateWithTx[Id](state1, sign(mint))
      ts2    <- getTokenState(state2)
      state3 <- StateService.updateStateWithTx[Id](state2, sign(mint))
      ts3    <- getTokenState(state3)
      state4 <- StateService.updateStateWithTx[Id](state3, sign(mint))
      ts4    <- getTokenState(state4)
    } yield {
      scribe.debug(s"state 1: $ts1")
      scribe.debug(s"state 2: $ts2")
      scribe.debug(s"state 3: $ts3")
      scribe.debug(s"state 4: $ts4")
      ts3
    }

    assertEquals(result.value.toOption.get.nonEmpty, true)

  }
}
