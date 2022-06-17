package io.leisuremeta.chain
package api.model

import java.time.Instant

import cats.Eq

import lib.crypto.Hash
import lib.merkle.MerkleTrieNode.MerkleRoot
import token.*

final case class StateRoot(
    account: StateRoot.AccountStateRoot,
    group: StateRoot.GroupStateRoot,
    token: StateRoot.TokenStateRoot,
    offering: StateRoot.RandomOfferingStateRoot,
)

object StateRoot:

  def empty: StateRoot = StateRoot(
    account = StateRoot.AccountStateRoot.empty,
    group = StateRoot.GroupStateRoot.empty,
    token = StateRoot.TokenStateRoot.empty,
    offering = StateRoot.RandomOfferingStateRoot.empty,
  )
  case class AccountStateRoot(
      namesRoot: Option[MerkleRoot[Account, AccountData]],
      keyRoot: Option[
        MerkleRoot[(Account, PublicKeySummary), PublicKeySummary.Info],
      ],
  )
  object AccountStateRoot:
    def empty: AccountStateRoot = AccountStateRoot(None, None)

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
      lockRoot: Option[MerkleRoot[(Account, Hash.Value[TransactionWithResult]), Unit]],
      deadlineRoot: Option[MerkleRoot[(Instant, Hash.Value[TransactionWithResult]), Unit]],
  )
  object TokenStateRoot:
    def empty: TokenStateRoot = TokenStateRoot(None, None, None, None, None, None, None)

  case class RandomOfferingStateRoot(
      offeringRoot: Option[MerkleRoot[TokenDefinitionId, Hash.Value[TransactionWithResult]]],
  )
  object RandomOfferingStateRoot:
    def empty: RandomOfferingStateRoot = RandomOfferingStateRoot(None)

  given eqStateRoot: Eq[StateRoot] = Eq.fromUniversalEquals
