package io.leisuremeta.chain
package node
package dapp

import api.model.*
import api.model.token.*
import lib.crypto.{CryptoOps, KeyPair}
import lib.crypto.Hash.ops.*
import lib.crypto.Sign.ops.*
import lib.datatype.{BigNat, Utf8}
import lib.failure.DecodingFailure
import lib.merkle.MerkleTrie.NodeStore
import lib.merkle.MerkleTrieState
import repository.TransactionRepository
import store.KeyValueStore

import cats.data.{EitherT, Kleisli}
import cats.effect.IO
import cats.syntax.all.*
import munit.CatsEffectSuite

class PlayNommDAppTest extends CatsEffectSuite:
  given emptyNodeStore: NodeStore[IO] = Kleisli.liftF(EitherT.pure(None))
  given inMemoryStore[K, V]: KeyValueStore[IO, K, V] =
    new KeyValueStore[IO, K, V]:
      val store: collection.mutable.Map[K, V] = collection.mutable.Map.empty
      def get(key: K): EitherT[IO, DecodingFailure, Option[V]] =
        store.get(key).pure[EitherT[IO, DecodingFailure, *]]
      def put(key: K, value: V): IO[Unit] =
        IO.delay:
          store.put(key, value)
          ()
      def remove(key: K): IO[Unit] =
        IO.delay:
          store.remove(key)
          ()
  given txRepo: TransactionRepository[IO] = TransactionRepository.fromStores[IO]
  given initialPlayNommState: PlayNommState[IO] = PlayNommState.build[IO]

  val aliceKey = CryptoOps.fromPrivate:
    BigInt(
      "b229e76b742616db3ac2c5c2418f44063fcc5fcc52a08e05d4285bdb31acba06",
      16,
    )

  val alicePKS  = PublicKeySummary.fromPublicKeyHash(aliceKey.publicKey.toHash)
  val alice     = Account(Utf8.unsafeFrom("alice"))
  val bob       = Account(Utf8.unsafeFrom("bob"))
  val carol     = Account(Utf8.unsafeFrom("carol"))
  val networkId = NetworkId(BigNat.unsafeFromLong(2021L))
  val mintGroup = GroupId(Utf8.unsafeFrom("mint-group"))
  val testToken = TokenDefinitionId(Utf8.unsafeFrom("test-token"))

  def sign(account: Account, key: KeyPair)(tx: Transaction): Signed.Tx =
    key.sign(tx).map(sig => Signed(AccountSignature(sig, account), tx)) match
      case Right(signedTx) => signedTx
      case Left(msg)       => throw Exception(msg)

  def signAlice = sign(alice, aliceKey)

  val fixture: IO[MerkleTrieState] =
    val txs: Seq[Transaction] = IndexedSeq(
      Transaction.AccountTx.CreateAccount(
        networkId = networkId,
        createdAt = java.time.Instant.parse("2023-01-11T19:01:00.00Z"),
        account = alice,
        ethAddress = None,
        guardian = None,
      ),
      Transaction.GroupTx.CreateGroup(
        networkId = networkId,
        createdAt = java.time.Instant.parse("2023-01-11T19:02:00.00Z"),
        groupId = mintGroup,
        name = Utf8.unsafeFrom("Mint Group"),
        coordinator = alice,
      ),
      Transaction.GroupTx.AddAccounts(
        networkId = networkId,
        createdAt = java.time.Instant.parse("2023-01-11T19:03:00.00Z"),
        groupId = mintGroup,
        accounts = Set(alice),
      ),
      Transaction.TokenTx.DefineToken(
        networkId = networkId,
        createdAt = java.time.Instant.parse("2023-01-11T19:04:00.00Z"),
        definitionId = testToken,
        name = Utf8.unsafeFrom("Test Token"),
        symbol = Some(Utf8.unsafeFrom("TST")),
        minterGroup = Some(mintGroup),
        nftInfo = None,
      ),
    )

    txs
      .map(signAlice(_))
      .traverse(PlayNommDApp[IO](_))
      .runS(MerkleTrieState.empty)
      .value
      .flatMap:
        case Right(state)  => IO.pure(state)
        case Left(failure) => IO.raiseError(new Exception(failure.toString))

  test("Account is added to group"):
    val program =
      for findOption <- PlayNommState[IO].group.groupAccount
          .get((mintGroup, alice))
      yield findOption.nonEmpty

    for
      state  <- fixture
      result <- program.runA(state).value
    yield assertEquals(result, Right(true))

  test("Snapshot is created successfully"):

    val txs = Seq(
      Transaction.TokenTx.CreateSnapshot(
        networkId = networkId,
        createdAt = java.time.Instant.parse("2023-01-11T19:05:00.00Z"),
        definitionId = testToken,
        memo = Some(Utf8.unsafeFrom("Snapshot #0")),
      ),
    )

    val program = for
      _ <- txs.map(signAlice(_)).traverse(PlayNommDApp[IO](_))
      snapshotStateOption <- PlayNommState[IO].token.snapshotState
        .get(testToken)
        .mapK:
          PlayNommDAppFailure.mapExternal:
            "Failed to get snapshot state"
    yield snapshotStateOption

    for
      state  <- fixture
      result <- program.runA(state).value
      snapshotStateOption <- IO.fromEither:
        result.leftMap(failure => new Exception(failure.msg))
    yield assertEquals(
      snapshotStateOption.map(_.snapshotId),
      Some(SnapshotState.SnapshotId.Zero),
    )

