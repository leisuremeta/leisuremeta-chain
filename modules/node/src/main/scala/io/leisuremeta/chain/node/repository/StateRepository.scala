package io.leisuremeta.chain
package node
package repository

import java.time.Instant

import cats.{Functor, Monad}
import cats.data.{EitherT, Kleisli, OptionT}
import cats.implicits.*

import api.model.{
  Account,
  AccountData,
  GroupId,
  GroupData,
  PublicKeySummary,
  TransactionWithResult,
}
import api.model.account.EthAddress
import api.model.reward.{DaoActivity, DaoInfo}
import api.model.token.{
  NftState,
  Rarity,
  TokenDefinition,
  TokenDefinitionId,
  TokenId,
}
import lib.crypto.Hash
import lib.datatype.BigNat
import lib.merkle.{GenericMerkleTrie, GenericMerkleTrieNode, GenericMerkleTrieState}
import lib.merkle.GenericMerkleTrie.NodeStore
import lib.merkle.GenericMerkleTrieNode.{MerkleHash, MerkleRoot}
import lib.failure.DecodingFailure
import store.KeyValueStore

trait StateRepository[F[_], K, V]:
  def get(
      merkleRoot: MerkleRoot[K, V],
  ): EitherT[F, DecodingFailure, Option[GenericMerkleTrieNode[K, V]]]

  def put(state: GenericMerkleTrieState[K, V]): F[Unit]

object StateRepository:

  /** AccountState
    */
  trait AccountState[F[_]]:
    def name: StateRepository[F, Account, AccountData]
    def key
        : StateRepository[F, (Account, PublicKeySummary), PublicKeySummary.Info]
    def eth: StateRepository[F, EthAddress, Account]
  object AccountState:
    def apply[F[_]: AccountState]: AccountState[F] = summon
    given from[F[_]: Monad](using
        nameKVStroe: MerkleHashStore[F, Account, AccountData],
        keyKVStroe: MerkleHashStore[
          F,
          (Account, PublicKeySummary),
          PublicKeySummary.Info,
        ],
        ethKVStore: MerkleHashStore[F, EthAddress, Account],
    ): AccountState[F] = new AccountState[F]:
      def name: StateRepository[F, Account, AccountData] = fromStores
      def key: StateRepository[
        F,
        (Account, PublicKeySummary),
        PublicKeySummary.Info,
      ] = fromStores
      def eth: StateRepository[F, EthAddress, Account] = fromStores

  given nodeStoreFromAccount[F[_]: Functor: AccountState]
      : NodeStore[F, Account, AccountData] =
    Kleisli(AccountState[F].name.get(_).leftMap(_.msg))
  given nodeStoreFromAccountKey[F[_]: Functor: AccountState]
      : NodeStore[F, (Account, PublicKeySummary), PublicKeySummary.Info] =
    Kleisli(AccountState[F].key.get(_).leftMap(_.msg))
  given nodeStoreFromEth[F[_]: Functor: AccountState]
      : NodeStore[F, EthAddress, Account] =
    Kleisli(AccountState[F].eth.get(_).leftMap(_.msg))

  /** GroupState
    */
  trait GroupState[F[_]]:
    def group: StateRepository[F, GroupId, GroupData]
    def groupAccount: StateRepository[F, (GroupId, Account), Unit]
  object GroupState:
    def apply[F[_]: GroupState]: GroupState[F] = summon
    given from[F[_]: Monad](using
        groupKVStroe: MerkleHashStore[F, GroupId, GroupData],
        groupAccountKVStroe: MerkleHashStore[
          F,
          (GroupId, Account),
          Unit,
        ],
    ): GroupState[F] = new GroupState[F]:
      def group: StateRepository[F, GroupId, GroupData] = fromStores
      def groupAccount: StateRepository[F, (GroupId, Account), Unit] =
        fromStores

  given nodeStoreFromGroup[F[_]: Functor: GroupState]
      : NodeStore[F, GroupId, GroupData] =
    Kleisli(GroupState[F].group.get(_).leftMap(_.msg))
  given nodeStoreFromGroupAccount[F[_]: Functor: GroupState]
      : NodeStore[F, (GroupId, Account), Unit] =
    Kleisli(GroupState[F].groupAccount.get(_).leftMap(_.msg))

  /** TokenState
    */
  trait TokenState[F[_]]:
    def definition: StateRepository[F, TokenDefinitionId, TokenDefinition]
    def fungibleBalance: StateRepository[
      F,
      (Account, TokenDefinitionId, Hash.Value[TransactionWithResult]),
      Unit,
    ]
    def nftBalance: StateRepository[
      F,
      (Account, TokenId, Hash.Value[TransactionWithResult]),
      Unit,
    ]
    def nft: StateRepository[F, TokenId, NftState]
    def rarity: StateRepository[F, (TokenDefinitionId, Rarity, TokenId), Unit]
    def entrustFungibleBalance: StateRepository[
      F,
      (Account, Account, TokenDefinitionId, Hash.Value[TransactionWithResult]),
      Unit,
    ]
    def entrustNftBalance: StateRepository[
      F,
      (Account, Account, TokenId, Hash.Value[TransactionWithResult]),
      Unit,
    ]

  object TokenState:
    def apply[F[_]: TokenState]: TokenState[F] = summon
    given from[F[_]: Monad](using
        difinitionKVStroe: MerkleHashStore[
          F,
          TokenDefinitionId,
          TokenDefinition,
        ],
        fungibleBalanceKVStroe: MerkleHashStore[
          F,
          (Account, TokenDefinitionId, Hash.Value[TransactionWithResult]),
          Unit,
        ],
        nftBalanceKVStore: MerkleHashStore[
          F,
          (Account, TokenId, Hash.Value[TransactionWithResult]),
          Unit,
        ],
        nftKVStore: MerkleHashStore[F, TokenId, NftState],
        rarityKVStore: MerkleHashStore[
          F,
          (TokenDefinitionId, Rarity, TokenId),
          Unit,
        ],
        entrustFungibleBalanceKVStroe: MerkleHashStore[
          F,
          (
              Account,
              Account,
              TokenDefinitionId,
              Hash.Value[TransactionWithResult],
          ),
          Unit,
        ],
        entrustNftBalanceKVStore: MerkleHashStore[
          F,
          (Account, Account, TokenId, Hash.Value[TransactionWithResult]),
          Unit,
        ],
    ): TokenState[F] = new TokenState[F]:
      def definition: StateRepository[F, TokenDefinitionId, TokenDefinition] =
        fromStores
      def fungibleBalance: StateRepository[
        F,
        (Account, TokenDefinitionId, Hash.Value[TransactionWithResult]),
        Unit,
      ] = fromStores
      def nftBalance: StateRepository[
        F,
        (Account, TokenId, Hash.Value[TransactionWithResult]),
        Unit,
      ] = fromStores
      def nft: StateRepository[F, TokenId, NftState] = fromStores
      def rarity
          : StateRepository[F, (TokenDefinitionId, Rarity, TokenId), Unit] =
        fromStores
      def entrustFungibleBalance: StateRepository[
        F,
        (
            Account,
            Account,
            TokenDefinitionId,
            Hash.Value[TransactionWithResult],
        ),
        Unit,
      ] = fromStores
      def entrustNftBalance: StateRepository[
        F,
        (Account, Account, TokenId, Hash.Value[TransactionWithResult]),
        Unit,
      ] = fromStores

  given nodeStoreFromDefinition[F[_]: Functor: TokenState]
      : NodeStore[F, TokenDefinitionId, TokenDefinition] =
    Kleisli(TokenState[F].definition.get(_).leftMap(_.msg))

  given nodeStoreFromFungibleBalance[F[_]: Functor: TokenState]: NodeStore[
    F,
    (Account, TokenDefinitionId, Hash.Value[TransactionWithResult]),
    Unit,
  ] =
    Kleisli(TokenState[F].fungibleBalance.get(_).leftMap(_.msg))

  given nodeStoreFromNftBalance[F[_]: Functor: TokenState]: NodeStore[
    F,
    (Account, TokenId, Hash.Value[TransactionWithResult]),
    Unit,
  ] =
    Kleisli(TokenState[F].nftBalance.get(_).leftMap(_.msg))

  given nodeStoreFromNft[F[_]: Functor: TokenState]
      : NodeStore[F, TokenId, NftState] =
    Kleisli(TokenState[F].nft.get(_).leftMap(_.msg))

  given nodeStoreFromRarity[F[_]: Functor: TokenState]
      : NodeStore[F, (TokenDefinitionId, Rarity, TokenId), Unit] =
    Kleisli(TokenState[F].rarity.get(_).leftMap(_.msg))

  given nodeStoreFromEntrustFungibleBalance[F[_]: Functor: TokenState]
      : NodeStore[
        F,
        (
            Account,
            Account,
            TokenDefinitionId,
            Hash.Value[TransactionWithResult],
        ),
        Unit,
      ] =
    Kleisli(TokenState[F].entrustFungibleBalance.get(_).leftMap(_.msg))

  given nodeStoreFromEntrustNftBalance[F[_]: Functor: TokenState]: NodeStore[
    F,
    (Account, Account, TokenId, Hash.Value[TransactionWithResult]),
    Unit,
  ] =
    Kleisli(TokenState[F].entrustNftBalance.get(_).leftMap(_.msg))

  /** RewardState
    */
  trait RewardState[F[_]]:
    def daoState: StateRepository[F, GroupId, DaoInfo]
    def userActivityState: StateRepository[F, (Instant, Account), DaoActivity]
    def tokenReceivedState: StateRepository[F, (Instant, TokenId), DaoActivity]
  object RewardState:
    def apply[F[_]: RewardState]: RewardState[F] = summon

    given from[F[_]: Monad](using
        daoKVStroe: MerkleHashStore[F, GroupId, DaoInfo],
        userActivityKVStroe: MerkleHashStore[
          F,
          (Instant, Account),
          DaoActivity,
        ],
        tokenReceivedKVStore: MerkleHashStore[
          F,
          (Instant, TokenId),
          DaoActivity,
        ],
    ): RewardState[F] = new RewardState[F]:
      def daoState: StateRepository[F, GroupId, DaoInfo] = fromStores
      def userActivityState
          : StateRepository[F, (Instant, Account), DaoActivity] = fromStores
      def tokenReceivedState
          : StateRepository[F, (Instant, TokenId), DaoActivity] = fromStores

  end RewardState

  given nodeStoreFromDaoState[F[_]: Functor: RewardState]
      : NodeStore[F, GroupId, DaoInfo] =
    Kleisli(RewardState[F].daoState.get(_).leftMap(_.msg))

  given nodeStoreFromUserActivityState[F[_]: Functor: RewardState]
      : NodeStore[F, (Instant, Account), DaoActivity] =
    Kleisli(RewardState[F].userActivityState.get(_).leftMap(_.msg))

  given nodeStoreFromTokenReceivedState[F[_]: Functor: RewardState]
      : NodeStore[F, (Instant, TokenId), DaoActivity] =
    Kleisli(RewardState[F].tokenReceivedState.get(_).leftMap(_.msg))

  /** General
    */
  given nodeStore[F[_]: Functor, K, V](using
      sr: StateRepository[F, K, V],
  ): NodeStore[F, K, V] = Kleisli(sr.get(_).leftMap(_.msg))

  type MerkleHashStore[F[_], K, V] =
    KeyValueStore[F, MerkleHash[K, V], GenericMerkleTrieNode[K, V]]

  def fromStores[F[_]: Monad, K, V](using
      stateKvStore: MerkleHashStore[F, K, V],
  ): StateRepository[F, K, V] = new StateRepository[F, K, V]:

    def get(
        merkleRoot: MerkleRoot[K, V],
    ): EitherT[F, DecodingFailure, Option[GenericMerkleTrieNode[K, V]]] =
      stateKvStore.get(merkleRoot)

    def put(state: GenericMerkleTrieState[K, V]): F[Unit] = for
      _ <- Monad[F].pure(scribe.debug(s"Putting state: $state"))
      _ <- state.diff.toList.traverse { case (hash, (node, count)) =>
        stateKvStore.get(hash).value.flatMap {
          case Right(None) if count > 0 => stateKvStore.put(hash, node)
          case Right(_) => Monad[F].unit
          case Left(err) => throw new Exception(s"Error: $err")
        }
      }
      _ <- Monad[F].pure(scribe.debug(s"Putting completed: $state"))
    yield ()
