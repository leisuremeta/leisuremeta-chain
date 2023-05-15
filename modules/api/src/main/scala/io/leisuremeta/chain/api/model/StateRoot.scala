package io.leisuremeta.chain
package api.model

import java.time.Instant

import cats.Eq

import lib.crypto.Hash
import lib.merkle.GenericMerkleTrieNode.{MerkleRoot => GenericMerkleRoot}
import lib.merkle.MerkleTrieNode.MerkleRoot
import account.EthAddress
import reward.*
import token.*

final case class StateRoot(
    main: Option[MerkleRoot],
//    account: StateRoot.AccountStateRoot,
    group: StateRoot.GroupStateRoot,
    token: StateRoot.TokenStateRoot,
    reward: StateRoot.RewardStateRoot,
)

object StateRoot:

  def empty: StateRoot = StateRoot(
    main = None,
//    account = StateRoot.AccountStateRoot.empty,
    group = StateRoot.GroupStateRoot.empty,
    token = StateRoot.TokenStateRoot.empty,
    reward = StateRoot.RewardStateRoot.empty,
  )
//  case class AccountStateRoot(
//      namesRoot: Option[GenericMerkleRoot[Account, AccountData]],
//      keyRoot: Option[
//        GenericMerkleRoot[(Account, PublicKeySummary), PublicKeySummary.Info],
//      ],
//      ethRoot: Option[GenericMerkleRoot[EthAddress, Account]]
//  )
//  object AccountStateRoot:
//    def empty: AccountStateRoot = AccountStateRoot(None, None, None)

  case class GroupStateRoot(
      groupRoot: Option[GenericMerkleRoot[GroupId, GroupData]],
      groupAccountRoot: Option[GenericMerkleRoot[(GroupId, Account), Unit]],
  )
  object GroupStateRoot:
    def empty: GroupStateRoot = GroupStateRoot(None, None)

  case class TokenStateRoot(
      tokenDefinitionRoot: Option[
        GenericMerkleRoot[TokenDefinitionId, TokenDefinition],
      ],
      fungibleBalanceRoot: Option[GenericMerkleRoot[
        (Account, TokenDefinitionId, Hash.Value[TransactionWithResult]),
        Unit,
      ]],
      nftBalanceRoot: Option[
        GenericMerkleRoot[(Account, TokenId, Hash.Value[TransactionWithResult]), Unit],
      ],
      nftRoot: Option[GenericMerkleRoot[TokenId, NftState]],
      rarityRoot: Option[GenericMerkleRoot[(TokenDefinitionId, Rarity, TokenId), Unit]],
      entrustFungibleBalanceRoot: Option[GenericMerkleRoot[
        (Account, Account, TokenDefinitionId, Hash.Value[TransactionWithResult]),
        Unit,
      ]],
      entrustNftBalanceRoot: Option[
        GenericMerkleRoot[(Account, Account, TokenId, Hash.Value[TransactionWithResult]), Unit],
      ],
  )
  object TokenStateRoot:
    def empty: TokenStateRoot = TokenStateRoot(None, None, None, None, None, None, None)

  case class RewardStateRoot(
      dao: Option[GenericMerkleRoot[GroupId, DaoInfo]],
      userActivity: Option[GenericMerkleRoot[(Instant, Account), DaoActivity]],
      tokenReceived: Option[GenericMerkleRoot[(Instant, TokenId), DaoActivity]],
  )
  object RewardStateRoot:
    def empty: RewardStateRoot = RewardStateRoot(None, None, None)

  given eqStateRoot: Eq[StateRoot] = Eq.fromUniversalEquals
