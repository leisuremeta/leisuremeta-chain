package io.leisuremeta.chain
package api.model

import java.time.Instant

import cats.Eq

import lib.crypto.Hash
import lib.merkle.MerkleTrieNode.MerkleRoot
import account.EthAddress
import reward.*
import token.*

final case class StateRoot(
    account: StateRoot.AccountStateRoot,
    group: StateRoot.GroupStateRoot,
    token: StateRoot.TokenStateRoot,
    reward: StateRoot.RewardStateRoot,
)

object StateRoot:

  def empty: StateRoot = StateRoot(
    account = StateRoot.AccountStateRoot.empty,
    group = StateRoot.GroupStateRoot.empty,
    token = StateRoot.TokenStateRoot.empty,
    reward = StateRoot.RewardStateRoot.empty,
  )
  case class AccountStateRoot(
      namesRoot: Option[MerkleRoot[Account, AccountData]],
      keyRoot: Option[
        MerkleRoot[(Account, PublicKeySummary), PublicKeySummary.Info],
      ],
      ethRoot: Option[MerkleRoot[EthAddress, Account]]
  )
  object AccountStateRoot:
    def empty: AccountStateRoot = AccountStateRoot(None, None, None)

  case class GroupStateRoot(
      groupRoot: Option[MerkleRoot[GroupId, GroupData]],
      groupAccountRoot: Option[MerkleRoot[(GroupId, Account), Unit]],
  )
  object GroupStateRoot:
    def empty: GroupStateRoot = GroupStateRoot(None, None)

  case class TokenStateRoot(
      tokenDefinitionRoot: Option[
        MerkleRoot[TokenDefinitionId, TokenDefinition],
      ],
      fungibleBalanceRoot: Option[MerkleRoot[
        (Account, TokenDefinitionId, Hash.Value[TransactionWithResult]),
        Unit,
      ]],
      nftBalanceRoot: Option[
        MerkleRoot[(Account, TokenId, Hash.Value[TransactionWithResult]), Unit],
      ],
      nftRoot: Option[MerkleRoot[TokenId, NftState]],
      rarityRoot: Option[MerkleRoot[(TokenDefinitionId, Rarity, TokenId), Unit]],
      entrustFungibleBalanceRoot: Option[MerkleRoot[
        (Account, Account, TokenDefinitionId, Hash.Value[TransactionWithResult]),
        Unit,
      ]],
      entrustNftBalanceRoot: Option[
        MerkleRoot[(Account, Account, TokenId, Hash.Value[TransactionWithResult]), Unit],
      ],
  )
  object TokenStateRoot:
    def empty: TokenStateRoot = TokenStateRoot(None, None, None, None, None, None, None)

  case class RewardStateRoot(
      dao: Option[MerkleRoot[GroupId, DaoInfo]],
      userActivity: Option[MerkleRoot[(Instant, Account), DaoActivity]],
      tokenReceived: Option[MerkleRoot[(Instant, TokenId), DaoActivity]],
  )
  object RewardStateRoot:
    def empty: RewardStateRoot = RewardStateRoot(None, None, None)

  given eqStateRoot: Eq[StateRoot] = Eq.fromUniversalEquals
