package io.leisuremeta.chain
package node
package dapp

import cats.Monad

import api.model.*
import api.model.{Account as AccountM}
import api.model.account.*
import api.model.token.*
import api.model.reward.*
import api.model.voting.*
import api.model.creator_dao.*
import lib.crypto.Hash
import lib.datatype.{BigNat, Utf8}
import lib.merkle.MerkleTrie.NodeStore
import lib.application.DAppState
import java.time.Instant

trait PlayNommState[F[_]]:
  def account: PlayNommState.Account[F]
  def group: PlayNommState.Group[F]
  def token: PlayNommState.Token[F]
  def reward: PlayNommState.Reward[F]
  def voting: PlayNommState.Voting[F]
  def creatorDao: PlayNommState.CreatorDao[F]

object PlayNommState:

  def apply[F[_]: PlayNommState]: PlayNommState[F] = summon

  case class Account[F[_]](
      name: DAppState[F, AccountM, AccountData],
      key: DAppState[F, (AccountM, PublicKeySummary), PublicKeySummary.Info],
      externalChainAddresses: DAppState[F, (ExternalChain, ExternalChainAddress), AccountM],
  )

  case class Reward[F[_]](
      dao: DAppState[F, GroupId, DaoInfo],
      accountActivity: DAppState[F, (AccountM, Instant), Seq[ActivityLog]],
      tokenReceived: DAppState[F, (TokenId, Instant), Seq[ActivityLog]],
      accountSnapshot: DAppState[F, AccountM, ActivitySnapshot],
      tokenSnapshot: DAppState[F, TokenId, ActivitySnapshot],
      ownershipSnapshot: DAppState[F, TokenId, OwnershipSnapshot],
      accountRewarded: DAppState[F, AccountM, ActivityRewardLog],
      tokenRewarded: DAppState[F, TokenId, ActivityRewardLog],
      ownershipRewarded: DAppState[F, TokenId, OwnershipRewardLog],
  )

  case class Group[F[_]](
      group: DAppState[F, GroupId, GroupData],
      groupAccount: DAppState[F, (GroupId, AccountM), Unit],
  )

  type TxHash = Hash.Value[TransactionWithResult]

  type BalanceAmount = BigNat

  case class Token[F[_]](
      definition: DAppState[F, TokenDefinitionId, TokenDefinition],
      fungibleBalance: DAppState[
        F,
        (AccountM, TokenDefinitionId, TxHash),
        Unit,
      ],
      nftBalance: DAppState[F, (AccountM, TokenId, TxHash), Unit],
      nftState: DAppState[F, TokenId, NftState],
      nftHistory: DAppState[F, TxHash, NftState],
      rarityState: DAppState[F, (TokenDefinitionId, Rarity, TokenId), Unit],
      entrustFungibleBalance: DAppState[
        F,
        (AccountM, AccountM, TokenDefinitionId, TxHash),
        Unit,
      ],
      entrustNftBalance: DAppState[
        F,
        (AccountM, AccountM, TokenId, TxHash),
        Unit,
      ],
      snapshotState: DAppState[F, TokenDefinitionId, SnapshotState],
      fungibleSnapshot: DAppState[
        F,
        (AccountM, TokenDefinitionId, SnapshotState.SnapshotId),
        Map[TxHash, BalanceAmount],
      ],
      nftSnapshot: DAppState[
        F,
        (AccountM, TokenDefinitionId, SnapshotState.SnapshotId),
        Set[TokenId],
      ],
      totalSupplySnapshot: DAppState[
        F,
        (TokenDefinitionId, SnapshotState.SnapshotId),
        BalanceAmount,
      ],
  )

  case class Voting[F[_]](
      proposal: DAppState[F, ProposalId, Proposal],
      votes: DAppState[F, (ProposalId, AccountM), (Utf8, BigNat)],
      counting: DAppState[F, ProposalId, Map[Utf8, BigNat]],
  )

  case class CreatorDao[F[_]](
      dao: DAppState[F, CreatorDaoId, CreatorDaoData],
      daoModerators: DAppState[F, (CreatorDaoId, AccountM), Unit],
      daoMembers: DAppState[F, (CreatorDaoId, AccountM), Unit],
  )

  def build[F[_]: Monad: NodeStore]: PlayNommState[F] =
    scribe.info(s"Building PlayNommState... ")

    val playNommState = DAppState.WithCommonPrefix("playnomm")

    new PlayNommState:
      val account: Account[F] = Account[F](
        name = playNommState.ofName("name"),
        key = playNommState.ofName("key"),
        externalChainAddresses = playNommState.ofName("pca"),
      )

      val group: Group[F] = Group[F](
        group = playNommState.ofName("group"),
        groupAccount = playNommState.ofName("group-account"),
      )

      val token: Token[F] = Token[F](
        definition = playNommState.ofName("token-def"),
        fungibleBalance = playNommState.ofName("fungible-balance"),
        nftBalance = playNommState.ofName("nft-balance"),
        nftState = playNommState.ofName("nft-state"),
        nftHistory = playNommState.ofName("nft-history"),
        rarityState = playNommState.ofName("rarity-state"),
        entrustFungibleBalance =
          playNommState.ofName("entrust-fungible-balance"),
        entrustNftBalance = playNommState.ofName("entrust-nft-balance"),
        snapshotState = playNommState.ofName("snapshot-state"),
        fungibleSnapshot = playNommState.ofName("fungible-snapshot"),
        nftSnapshot = playNommState.ofName("nft-snapshot"),
        totalSupplySnapshot = playNommState.ofName("total-supply-snapshot"),
      )

      val reward: Reward[F] = Reward[F](
        dao = playNommState.ofName("dao"),
        accountActivity = playNommState.ofName("account-activity"),
        tokenReceived = playNommState.ofName("token-received"),
        accountSnapshot = playNommState.ofName("account-snapshot"),
        tokenSnapshot = playNommState.ofName("token-snapshot"),
        ownershipSnapshot = playNommState.ofName("ownership-snapshot"),
        accountRewarded = playNommState.ofName("account-rewarded"),
        tokenRewarded = playNommState.ofName("token-rewarded"),
        ownershipRewarded = playNommState.ofName("ownership-rewarded"),
      )

      val voting: Voting[F] = Voting[F](
        proposal = playNommState.ofName("proposal"),
        votes = playNommState.ofName("votes"),
        counting = playNommState.ofName("counting"),
      )

      val creatorDao: CreatorDao[F] = CreatorDao[F](
        dao = playNommState.ofName("creator-dao"),
        daoModerators = playNommState.ofName("dao-moderators"),
        daoMembers = playNommState.ofName("dao-members"),
      )
