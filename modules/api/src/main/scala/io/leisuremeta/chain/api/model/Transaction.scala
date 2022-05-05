package io.leisuremeta.chain
package api.model

import java.time.Instant

import lib.datatype.{BigNat, UInt256Bytes}

sealed trait Transaction:
  def networkId: NetworkId
  def createdAt: Instant

object Transaction:
  sealed trait AccountTx extends Transaction
  object AccountTx:
    final case class CreateAccount(
        networkId: NetworkId,
        createdAt: Instant,
        account: Account,
        guardian: Option[Account],
    ) extends AccountTx

    final case class AddPublicKeySummaries(
        networkId: NetworkId,
        createdAt: Instant,
        account: Account,
        summaries: Map[PublicKeySummary, String],
    ) extends AccountTx

    final case class RemovePublicKeySummaries(
        networkId: NetworkId,
        createdAt: Instant,
        account: Account,
        summaries: Set[PublicKeySummary],
    ) extends AccountTx

    final case class RemoveAccount(
        networkId: NetworkId,
        createdAt: Instant,
        account: Account,
    ) extends AccountTx
  end AccountTx

  sealed trait GroupTx extends Transaction
  object GroupTx:
    final case class CreateGroup(
        networkId: NetworkId,
        createdAt: Instant,
        groupId: GroupId,
        name: String,
        coordinator: Account,
    ) extends GroupTx

    final case class DisbandGroup(
        networkId: NetworkId,
        createdAt: Instant,
        groupId: GroupId,
    ) extends GroupTx

    final case class AddAccounts(
        networkId: NetworkId,
        createdAt: Instant,
        groupId: GroupId,
        accounts: Set[Account],
    ) extends GroupTx

    final case class RemoveAccounts(
        networkId: NetworkId,
        createdAt: Instant,
        groupId: GroupId,
        accounts: Set[Account],
    ) extends GroupTx
    final case class ReplaceCoordinator(
        networkId: NetworkId,
        createdAt: Instant,
        groupId: GroupId,
        newCoordinator: Account,
    ) extends GroupTx
  end GroupTx

  sealed trait TokenTx extends Transaction
  object TokenTx:
    final case class DefineToken(
        networkId: NetworkId,
        createdAt: Instant,
        definitionId: TokenDefinitionId,
        name: String,
        symbol: Option[String],
        minterGroup: Option[GroupId],
        nftInfo: Option[NftInfo],
    ) extends TokenTx

    final case class NftInfo(
        creater: Account,
        rarity: Map[Rarity, BigNat],
        dataUrl: String,
        contentHash: UInt256Bytes,
    )

    final case class MintFungibleToken(
        networkId: NetworkId,
        createdAt: Instant,
        definitionId: TokenDefinitionId,
        outputs: Map[Account, BigNat],
    ) extends TokenTx

    final case class MintNft(
        networkId: NetworkId,
        createdAt: Instant,
        definitionId: TokenDefinitionId,
        tokenId: TokenDefinitionId,
        rarity: Rarity,
        dataUrl: String,
        contentHash: UInt256Bytes,
        output: Account,
    ) extends TokenTx


    final case class TransferFungibleToken(
        networkId: NetworkId,
        createdAt: Instant,
        definitionId: TokenDefinitionId,
//        inputs: Set[]
//        outputs: Map[Account, BigNat],
    ) extends TokenTx

    final case class TransferNft(
        networkId: NetworkId,
        createdAt: Instant,
    ) extends TokenTx

    final case class SuggestDeal(
        networkId: NetworkId,
        createdAt: Instant,
    ) extends TokenTx

    final case class AcceptDeal(
        networkId: NetworkId,
        createdAt: Instant,
    ) extends TokenTx

    final case class CancelSuggestion(
        networkId: NetworkId,
        createdAt: Instant,
    ) extends TokenTx
  end TokenTx

  sealed trait RandomOfferingTx extends Transaction
  object RandomOfferingTx:
    final case class Notice(
        networkId: NetworkId,
        createdAt: Instant,
    ) extends RandomOfferingTx

    final case class InitialTokenOffering(
        networkId: NetworkId,
        createdAt: Instant,
    ) extends RandomOfferingTx

    final case class ClaimToken(
        networkId: NetworkId,
        createdAt: Instant,
    ) extends RandomOfferingTx
  end RandomOfferingTx

  sealed trait AgendaTx extends Transaction
  object AgendaTx:
    final case class Suggest(
        networkId: NetworkId,
        createdAt: Instant,
    ) extends AgendaTx

    final case class Vote(
        networkId: NetworkId,
        createdAt: Instant,
    ) extends AgendaTx

    final case class Finalize(
        networkId: NetworkId,
        createdAt: Instant,
    ) extends AgendaTx
  end AgendaTx
