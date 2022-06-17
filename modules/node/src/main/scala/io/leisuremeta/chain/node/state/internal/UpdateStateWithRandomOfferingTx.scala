package io.leisuremeta.chain
package node
package state
package internal

import cats.data.EitherT
import cats.effect.Concurrent
import cats.syntax.traverse.given

import GossipDomain.MerkleState
import UpdateState.*
import api.model.{
  Account,
  AccountSignature,
  GroupId,
  PublicKeySummary,
  Signed,
  Transaction,
  TransactionWithResult,
}
import api.model.TransactionWithResult.ops.*
import api.model.token.{TokenDefinition, TokenDefinitionId, TokenId}
import lib.codec.byte.ByteEncoder.ops.*
import lib.crypto.Hash
import lib.crypto.Hash.ops.*
import lib.merkle.MerkleTrie
import repository.{StateRepository, TransactionRepository}
import repository.StateRepository.given

trait UpdateStateWithRandomOfferingTx:

  given updateStateWithRandomOfferingTx[F[_]
    : Concurrent: StateRepository.AccountState: StateRepository.TokenState: StateRepository.GroupState: StateRepository.RandomOfferingState: TransactionRepository]
      : UpdateState[F, Transaction.RandomOfferingTx] =
    (
        ms: MerkleState,
        sig: AccountSignature,
        tx: Transaction.RandomOfferingTx,
    ) =>
      tx match
        case nt: Transaction.RandomOfferingTx.NoticeTokenOffering =>
          for
            tokenDefinitionOption <- MerkleTrie
              .get[F, TokenDefinitionId, TokenDefinition](
                nt.tokenDefinitionId.toBytes.bits,
              )
              .runA(ms.token.tokenDefinitionState)
            tokenDefinition <- EitherT.fromOption[F](
              tokenDefinitionOption,
              s"Token definition not found: ${nt.tokenDefinitionId}",
            )
            adminGroup <- EitherT.fromOption[F](
              tokenDefinition.adminGroup,
              s"Token definition admin group not found: ${nt.tokenDefinitionId}",
            )
            isMemberOfAdminGroup <- MerkleTrie
              .get[F, (GroupId, Account), Unit](
                (adminGroup, sig.account).toBytes.bits,
              )
              .runA(ms.group.groupAccountState)
              .map(_.isDefined)
            _ <- EitherT.cond[F](
              isMemberOfAdminGroup,
              (),
              s"Account ${sig.account} is not a member of admin group ${adminGroup}",
            )
            publicKeySummary <- EitherT.fromEither[F](
              recoverSignature(nt, sig.sig),
            )
            isValidSig <- MerkleTrie
              .get[F, (Account, PublicKeySummary), PublicKeySummary.Info](
                (sig.account, publicKeySummary).toBytes.bits,
              )
              .runA(ms.account.keyState)
              .map(_.isDefined)
            _ <- EitherT.cond[F](
              isValidSig,
              (),
              s"Account ${sig.account} signature does not have public key summary ${publicKeySummary}",
            )
            inputTxHashList = nt.inputs.toList.map(_.toResultHashValue)
            inputTxOptions <- inputTxHashList.traverse { (txHash) =>
              TransactionRepository[F]
                .get(txHash)
                .leftMap(_.msg)
            }
            inputTokenIdAndTxHashes <- (inputTxHashList zip inputTxOptions).traverse {
              case (txHash, None) =>
                EitherT.leftT(s"Transaction not found: ${txHash}")
              case (txHash, Some(txResult)) =>
                txResult.signedTx.value match
                  case nftBalanceTx: Transaction.NftBalance =>
                    nftBalanceTx match
                      case mn: Transaction.TokenTx.MintNFT =>
                        EitherT.cond(
                          mn.tokenDefinitionId == nt.tokenDefinitionId,
                          (mn.tokenId, txHash),
                          s"Transaction ${txHash} is not an NFT balance for token definition ${nt.tokenDefinitionId}",
                        )
                  case _ =>
                    EitherT.leftT(s"Transaction is not NftBalance: ${txHash}")
            }
            result = TransactionWithResult(Signed(sig, tx), None)
            nftBalanceState <- {
              for
                _ <- inputTokenIdAndTxHashes.traverse{ (tokenId, txHash) =>
                  MerkleTrie.remove[F, (Account, TokenId, Hash.Value[TransactionWithResult]), Unit](
                    (sig.account, tokenId, txHash).toBytes.bits,
                  )
                }
                _ <- inputTokenIdAndTxHashes.traverse{ (tokenId, _) =>
                  MerkleTrie.put[F, (Account, TokenId, Hash.Value[TransactionWithResult]), Unit](
                    (nt.offeringAccount, tokenId, result.toHash).toBytes.bits,
                    (),
                  )
                }
              yield ()
            }.runS(ms.token.nftBalanceState)

            randomOfferingState <- MerkleTrie
              .put[F, TokenDefinitionId, Hash.Value[TransactionWithResult]](
                nt.tokenDefinitionId.toBytes.bits,
                result.toHash,
              )
              .runS(ms.offering.offeringState)
          yield (
            ms.copy(
              token = ms.token.copy(nftBalanceState = nftBalanceState),
              offering = ms.offering.copy(offeringState = randomOfferingState),
            ),
            result,
          )
