package io.leisuremeta.chain
package node
package state
package internal

import cats.data.{EitherT, StateT}
import cats.effect.Concurrent
import cats.syntax.either.given
import cats.syntax.eq.given
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
import api.model.token.{TokenDefinition, TokenDefinitionId, TokenDetail, TokenId}
import lib.codec.byte.ByteDecoder
import lib.codec.byte.ByteEncoder.ops.*
import lib.crypto.Hash
import lib.crypto.Hash.ops.*
import lib.datatype.BigNat
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
            inputTokenIdAndTxHashes <- (inputTxHashList zip inputTxOptions)
              .traverse {
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
                        case cs: Transaction.TokenTx.CancelSuggestion =>
                          txResult.result match
                            case Some(Transaction.TokenTx.CancelSuggestionResult(cancelDefId, detail)) =>
                              detail match
                                case TokenDetail.NftDetail(tokenId) =>
                                  EitherT.cond(
                                    cancelDefId == nt.tokenDefinitionId,
                                    (tokenId, txHash),
                                    s"Transaction ${txHash} is not an NFT balance for token definition ${nt.tokenDefinitionId}",
                                  )
                                case _ =>
                                  EitherT.leftT(
                                    s"Transaction ${txHash} is not an NFT balance for token definition ${nt.tokenDefinitionId}",
                                  )
                            case _ => EitherT.leftT(s"Transaction ${txHash} is not an NFT balance for token definition ${nt.tokenDefinitionId}")
                    case _ =>
                      EitherT.leftT(s"Transaction is not NftBalance: ${txHash}")
              }
            result = TransactionWithResult(Signed(sig, tx), None)
            nftBalanceState <- {
              for
                _ <- inputTokenIdAndTxHashes.traverse { (tokenId, txHash) =>
                  MerkleTrie.remove[
                    F,
                    (Account, TokenId, Hash.Value[TransactionWithResult]),
                    Unit,
                  ](
                    (sig.account, tokenId, txHash).toBytes.bits,
                  )
                }
                _ <- inputTokenIdAndTxHashes.traverse { (tokenId, _) =>
                  MerkleTrie.put[
                    F,
                    (Account, TokenId, Hash.Value[TransactionWithResult]),
                    Unit,
                  ](
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
        case jt: Transaction.RandomOfferingTx.JoinTokenOffering =>
          for
            txOption <- TransactionRepository[F]
              .get(jt.noticeTxHash.toResultHashValue)
              .leftMap(_.msg)
            tx <- EitherT.fromOption[F](
              txOption,
              s"Notice transaction not found: ${jt.noticeTxHash}",
            )
            noticeTx <- tx.signedTx.value match
              case nt: Transaction.RandomOfferingTx.NoticeTokenOffering => EitherT.pure(nt)
              case _ =>
                EitherT.right(Concurrent[F].raiseError {
                  new Exception(s"Notice transaction is not a NoticeTokenOffering: ${jt.noticeTxHash}")
                })
            requirement <- EitherT.fromOption(
              noticeTx.requirement,
              s"Notice transaction does not have requirement: ${jt.noticeTxHash}",
            )
            _ <- EitherT.cond(
              jt.inputTokenDefinitionId === requirement._1,
              (),
              s"Notice transaction requirement token definition ${jt.noticeTxHash} is not ${jt.inputTokenDefinitionId}",
            )
            inputAmountList <- jt.inputs.toList.traverse{ (txHash) =>
              for
                txOption <- TransactionRepository[F]
                  .get(txHash.toResultHashValue)
                  .leftMap(_.msg)
                txWithResult <- EitherT.fromOption[F](
                  txOption,
                  s"Input transaction not found: ${txHash}",
                )
                amount <- txWithResult.signedTx.value match
                    case fungibleBalanceTx: Transaction.FungibleBalance =>
                      fungibleBalanceTx match
                        case mf: Transaction.TokenTx.MintFungibleToken =>
                          EitherT.pure(mf.outputs.get(txWithResult.signedTx.sig.account).getOrElse(BigNat.Zero))
                        case tf: Transaction.TokenTx.TransferFungibleToken =>
                          EitherT.pure(tf.outputs.get(txWithResult.signedTx.sig.account).getOrElse(BigNat.Zero))
                        case ad: Transaction.TokenTx.AcceptDeal =>
                          txWithResult.result match
                            case Some(Transaction.TokenTx.AcceptDealResult(outputs)) =>
                              EitherT.pure(outputs
                                .get(txWithResult.signedTx.sig.account)
                                .flatMap(_.get(jt.inputTokenDefinitionId))
                                .fold(BigNat.Zero) {
                                  case TokenDetail.FungibleDetail(amount) => amount
                                  case TokenDetail.NftDetail(_)           => BigNat.Zero
                                }
                              )
                            case _ =>
                              EitherT.leftT(
                                s"AcceptDeal result is not found: $txWithResult",
                              )
                        case cs: Transaction.TokenTx.CancelSuggestion =>
                          txWithResult.result match
                            case Some(Transaction.TokenTx.CancelSuggestionResult(cancelDefId, detail)) =>
                              detail match
                                case TokenDetail.FungibleDetail(amount) =>
                                  EitherT.pure(amount)
                                case TokenDetail.NftDetail(_) =>
                                  EitherT.leftT(
                                    s"CancelSuggestion result is not found: $txWithResult",
                                  )
                            case _ =>
                              EitherT.leftT(
                                s"CancelSuggestion result is not found: $txWithResult",
                              )
                        case jt: Transaction.RandomOfferingTx.JoinTokenOffering =>
                          txWithResult.result match
                            case Some(Transaction.RandomOfferingTx.JoinTokenOfferingResult(output))
                              if txWithResult.signedTx.sig.account === sig.account =>
                              EitherT.pure(output)
                            case _ =>
                              EitherT.leftT(
                                s"JoinTokenOffering result is not found for $jt",
                              )
                        case it: Transaction.RandomOfferingTx.InitialTokenOffering =>
                          txWithResult.result match
                            case Some(Transaction.RandomOfferingTx.InitialTokenOfferingResult(outputs)) =>
                              EitherT.pure{
                                outputs
                                  .get(txWithResult.signedTx.sig.account)
                                  .flatMap(_.get(jt.inputTokenDefinitionId))
                                  .getOrElse(BigNat.Zero)
                              }
                            case _ =>
                              EitherT.leftT(
                                s"InitialTokenOffering result is not found for $txHash",
                              )

                    case _ =>
                      EitherT.leftT(s"Transaction is not FungibleBalance: ${txHash}")
              yield amount
            }
            inputTotalAmount = inputAmountList.foldLeft(BigNat.Zero)(BigNat.add)
            outputAmount <- EitherT.fromEither[F](
              BigNat.fromBigInt(inputTotalAmount.toBigInt - noticeTx.requirement.fold(BigInt(0))(_._2.toBigInt))
            ).leftMap{ msg =>
              s"Insufficient input total amount: $inputTotalAmount vs ${requirement._2}: $msg"
            }
            result = TransactionWithResult(
              Signed(sig, jt),
              Some(Transaction.RandomOfferingTx.JoinTokenOfferingResult(outputAmount)),
            )
            fungibleBalanceState <- {
              for
                _ <- jt.inputs.toList.traverse{ (txHash) =>
                  MerkleTrie.remove[F, (Account, TokenDefinitionId, Hash.Value[TransactionWithResult]), Unit]{
                    (sig.account, jt.inputTokenDefinitionId, txHash.toResultHashValue).toBytes.bits
                  }
                }
                _ <- MerkleTrie.put[F, (Account, TokenDefinitionId, Hash.Value[TransactionWithResult]), Unit](
                  (sig.account, jt.inputTokenDefinitionId, result.toHash).toBytes.bits,
                  (),
                )
              yield ()
            }.runS(ms.token.fungibleBalanceState)
            lockState <- MerkleTrie.put[F, (Account, Hash.Value[TransactionWithResult]), Unit](
              (sig.account, result.toHash).toBytes.bits,
              (),
            ).runS(ms.token.lockState)
            suggestionState <- MerkleTrie.put[F, (Hash.Value[TransactionWithResult], Hash.Value[TransactionWithResult]), Unit](
              (jt.noticeTxHash.toResultHashValue, result.toHash).toBytes.bits,
              (),
            ).runS(ms.token.suggestionState)
          yield (
            ms.copy(
              token = ms.token.copy(
                fungibleBalanceState = fungibleBalanceState,
                lockState = lockState,
                suggestionState = suggestionState,
              ),
            ),
            result,
          )
        case it: Transaction.RandomOfferingTx.InitialTokenOffering =>
          for
            txOption <- TransactionRepository[F]
              .get(it.noticeTxHash.toResultHashValue)
              .leftMap(_.msg)
            tx <- EitherT.fromOption[F](
              txOption,
              s"Notice transaction not found: ${it.noticeTxHash}",
            )
            noticeTx <- tx.signedTx.value match
              case nt: Transaction.RandomOfferingTx.NoticeTokenOffering => EitherT.pure(nt)
              case _ =>
                EitherT.right(Concurrent[F].raiseError {
                  new Exception(s"Notice transaction is not a NoticeTokenOffering: ${it.noticeTxHash}")
                })
            lockedTxHashStream <- MerkleTrie.from[F, (Hash.Value[TransactionWithResult], Hash.Value[TransactionWithResult]), Unit](
              it.noticeTxHash.toBytes.bits,
            ).runA(ms.token.suggestionState)
            lockedTxHashBitVectorList <- lockedTxHashStream
              .takeWhile(_._1.startsWith(it.noticeTxHash.toBytes.bits))
              .map(_._1.drop(it.noticeTxHash.toBytes.bits.size))
              .compile
              .toList
            lockedTxHashList <- lockedTxHashBitVectorList.traverse{ (txHashBitVector) =>
              EitherT.fromEither[F]{
                ByteDecoder[Hash.Value[TransactionWithResult]]
                  .decode(txHashBitVector.bytes)
                  .leftMap(_.msg)
                  .map(_.value)
              }
            }
            lockedTxList <- lockedTxHashList.traverse{ (txHash) =>
              TransactionRepository[F]
                .get(txHash)
                .map(txOption => txOption.map{ tx => (txHash, tx)})
                .leftMap(_.msg)
            }
            joinTxWithAccountList <- lockedTxList.flatten.traverse{ (txHash, tx) =>
              tx.signedTx.value match
                case jt: Transaction.RandomOfferingTx.JoinTokenOffering => EitherT.pure((txHash, jt, tx.signedTx.sig.account))
                case _ =>
                  EitherT.right(Concurrent[F].raiseError {
                    new Exception(s"Locked transaction is not a JoinTokenOffering: ${tx.signedTx.sig}")
                  })
            }
            totalOutputs: Map[Account, Map[TokenDefinitionId, BigNat]] = {
              it.outputs.map{ (account, amount) => (account, noticeTx.tokenDefinitionId, amount) }
                ++ joinTxWithAccountList.map{ case (_, jt, account) => (account, jt.inputTokenDefinitionId, jt.amount) }
            }.groupMapReduce(_._1)((_, defId, amount) => Seq((defId, amount)))(_ ++ _).view.mapValues{
              _.groupMapReduce(_._1)(_._2)(BigNat.add)
            }.toMap
            result = TransactionWithResult(
              Signed(sig, it),
              Some(Transaction.RandomOfferingTx.InitialTokenOfferingResult(totalOutputs)),
            )
            fungibleBalanceState <- totalOutputs
              .toList
              .flatMap{ (account, map) => map.toList.map((defId, _) => (account, defId)) }
              .traverse{ (account, tokenDefId) =>
                MerkleTrie.put[F, (Account, TokenDefinitionId, Hash.Value[TransactionWithResult]), Unit](
                  (account, tokenDefId, result.toHash).toBytes.bits,
                  (),
                )
              }.runS(ms.token.fungibleBalanceState)
            lockState <- joinTxWithAccountList.traverse{ case (txHash, _, account) =>
              MerkleTrie.remove[F, (Account, Hash.Value[TransactionWithResult]), Unit](
                (account, txHash).toBytes.bits,
              )
            }.runS(ms.token.lockState)
            suggestionState <- lockedTxHashBitVectorList.traverse{ (txHashBitVector) =>
              MerkleTrie.remove[F, (Hash.Value[TransactionWithResult], Hash.Value[TransactionWithResult]), Unit](
                it.noticeTxHash.toBytes.bits ++ txHashBitVector
              )
            }.runS(ms.token.suggestionState)
          yield (
            ms.copy(
              token = ms.token.copy(
                fungibleBalanceState = fungibleBalanceState,
                lockState = lockState,
                suggestionState = suggestionState,
              ),
            ),
            result,
          )

