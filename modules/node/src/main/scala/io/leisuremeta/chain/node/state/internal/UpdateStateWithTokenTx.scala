package io.leisuremeta.chain
package node
package state
package internal

import java.time.Instant

import cats.data.{EitherT, StateT}
import cats.effect.Concurrent
import cats.syntax.eq.given
import cats.syntax.foldable.given
import cats.syntax.traverse.given

import scodec.bits.BitVector

import GossipDomain.MerkleState
import UpdateState.*
import api.model.{
  Account,
  AccountSignature,
  GroupData,
  GroupId,
  PublicKeySummary,
  Signed,
  Transaction,
  TransactionWithResult,
}
import api.model.token.*
import api.model.TransactionWithResult.ops.*
import lib.merkle.{MerkleTrie, MerkleTrieState}
import lib.codec.byte.{ByteDecoder, DecodeResult}
import lib.codec.byte.ByteEncoder.ops.*
import lib.crypto.Hash
import lib.crypto.Hash.ops.*
import lib.datatype.{BigNat, Utf8}
import repository.{StateRepository, TransactionRepository}
import repository.StateRepository.given
import io.leisuremeta.chain.node.service.StateReadService

trait UpdateStateWithTokenTx:

  given updateStateWithTokenTx[F[_]
    : Concurrent: StateRepository.TokenState: StateRepository.GroupState: StateRepository.AccountState: TransactionRepository]
      : UpdateState[F, Transaction.TokenTx] =
    (ms: MerkleState, sig: AccountSignature, tx: Transaction.TokenTx) =>
      tx match
        case dt: Transaction.TokenTx.DefineToken =>
          for
            tokenDefinitionOption <- MerkleTrie
              .get[F, TokenDefinitionId, TokenDefinition](
                dt.definitionId.toBytes.bits,
              )
              .runA(ms.token.tokenDefinitionState)
            _ <- EitherT.cond(
              tokenDefinitionOption.isEmpty,
              (),
              s"Token definition ${dt.definitionId} already exists",
            )
            tokenDefinition = TokenDefinition(
              id = dt.definitionId,
              name = dt.name,
              symbol = dt.symbol,
              adminGroup = dt.minterGroup,
              totalAmount = BigNat.Zero,
              nftInfo = dt.nftInfo,
            )
            tokenDefinitionState <- MerkleTrie
              .put[F, TokenDefinitionId, TokenDefinition](
                dt.definitionId.toBytes.bits,
                tokenDefinition,
              )
              .runS(ms.token.tokenDefinitionState)
          yield
            scribe.info(
              s"===> new Token Definition state: $tokenDefinitionState",
            )
            (
              ms.copy(token =
                ms.token.copy(tokenDefinitionState = tokenDefinitionState),
              ),
              TransactionWithResult(Signed(sig, tx), None),
            )
        case mf: Transaction.TokenTx.MintFungibleToken =>
          for
            tokenDefinitionOption <- MerkleTrie
              .get[F, TokenDefinitionId, TokenDefinition](
                mf.definitionId.toBytes.bits,
              )
              .runA(ms.token.tokenDefinitionState)
            tokenDefinition <- EitherT.fromOption[F](
              tokenDefinitionOption,
              s"Token definition ${mf.definitionId} does not exist",
            )
            adminGroupId <- EitherT.fromOption[F](
              tokenDefinition.adminGroup,
              s"Admin group does not exist in token $tokenDefinition",
            )
            groupAccountOption <- MerkleTrie
              .get[F, (GroupId, Account), Unit](
                (adminGroupId, sig.account).toBytes.bits,
              )
              .runA(ms.group.groupAccountState)
            _ <- EitherT.cond(
              groupAccountOption.nonEmpty,
              (),
              s"Account ${sig.account} is not a member of admin group $adminGroupId",
            )
            pubKeySummary <- EitherT.fromEither[F](
              recoverSignature(mf, sig.sig),
            )
            accountPubKeyOption <- MerkleTrie
              .get[F, (Account, PublicKeySummary), PublicKeySummary.Info](
                (sig.account, pubKeySummary).toBytes.bits,
              )
              .runA(ms.account.keyState)
            _ <- EitherT.cond(
              accountPubKeyOption.nonEmpty,
              (),
              s"Account ${sig.account} does not have public key summary $pubKeySummary",
            )
            txWithResult = TransactionWithResult(Signed(sig, tx), None)
            items = mf.outputs.map { case (account, _) =>
              (account, mf.definitionId, txWithResult.toHash)
            }
            fungibleBalanceState <- items.toList.foldLeftM(
              ms.token.fungibleBalanceState,
            ) { case (state, item) =>
              MerkleTrie
                .put[
                  F,
                  (
                      Account,
                      TokenDefinitionId,
                      Hash.Value[TransactionWithResult],
                  ),
                  Unit,
                ](item.toBytes.bits, ())
                .runS(state)
            }
            totalAmount = mf.outputs.map(_._2).foldLeft(BigNat.Zero)(BigNat.add)
            tokenDefinition1 = tokenDefinition.copy(
              totalAmount = BigNat.add(tokenDefinition.totalAmount, totalAmount),
            )
            tokenDefinitionState1 <- {
              for
                _ <- MerkleTrie.remove[F, TokenDefinitionId, TokenDefinition](
                  mf.definitionId.toBytes.bits,
                )
                _ <- MerkleTrie.put[F, TokenDefinitionId, TokenDefinition](
                  mf.definitionId.toBytes.bits,
                  tokenDefinition1,
                )
              yield ()
            }.runS(ms.token.tokenDefinitionState)
          yield (
            ms.copy(token =
              ms.token.copy(
                fungibleBalanceState = fungibleBalanceState,
                tokenDefinitionState = tokenDefinitionState1,
              ),
            ),
            txWithResult,
          )
        case mn: Transaction.TokenTx.MintNFT =>
          for
            tokenDefinitionOption <- MerkleTrie
              .get[F, TokenDefinitionId, TokenDefinition](
                mn.tokenDefinitionId.toBytes.bits,
              )
              .runA(ms.token.tokenDefinitionState)
            tokenDefinition <- EitherT.fromOption[F](
              tokenDefinitionOption,
              s"Token definition ${mn.tokenDefinitionId} does not exist",
            )
            adminGroupId <- EitherT.fromOption[F](
              tokenDefinition.adminGroup,
              s"Admin group does not exist in token $tokenDefinition",
            )
            groupAccountOption <- MerkleTrie
              .get[F, (GroupId, Account), Unit](
                (adminGroupId, sig.account).toBytes.bits,
              )
              .runA(ms.group.groupAccountState)
            _ <- EitherT.cond(
              groupAccountOption.nonEmpty,
              (),
              s"Account ${sig.account} is not a member of admin group $adminGroupId",
            )
            pubKeySummary <- EitherT.fromEither[F](
              recoverSignature(mn, sig.sig),
            )
            accountPubKeyOption <- MerkleTrie
              .get[F, (Account, PublicKeySummary), PublicKeySummary.Info](
                (sig.account, pubKeySummary).toBytes.bits,
              )
              .runA(ms.account.keyState)
            _ <- EitherT.cond(
              accountPubKeyOption.nonEmpty,
              (),
              s"Account ${sig.account} does not have public key summary $pubKeySummary",
            )
            txWithResult = TransactionWithResult(Signed(sig, mn), None)
            balanceItem  = (mn.output, mn.tokenId, txWithResult.toHash)
            nftBalanceState <- MerkleTrie
              .put[
                F,
                (Account, TokenId, Hash.Value[TransactionWithResult]),
                Unit,
              ](balanceItem.toBytes.bits, ())
              .runS(ms.token.nftBalanceState)
            nftStateItem = NftState(mn.tokenId, mn.tokenDefinitionId, mn.output)
            nftState <- MerkleTrie
              .put[F, TokenId, NftState](mn.tokenId.toBytes.bits, nftStateItem)
              .runS(ms.token.nftState)
            rarityItem = (mn.tokenDefinitionId, mn.rarity, mn.tokenId)
            rarityState <- MerkleTrie
              .put[F, (TokenDefinitionId, Rarity, TokenId), Unit](
                rarityItem.toBytes.bits,
                (),
              )
              .runS(ms.token.rarityState)
            tokenState = ms.token.copy(
              nftBalanceState = nftBalanceState,
              nftState = nftState,
              rarityState = rarityState,
            )
          yield (ms.copy(token = tokenState), txWithResult)
        case tf: Transaction.TokenTx.TransferFungibleToken =>
          val txWithResult = TransactionWithResult(Signed(sig, tx), None)

          type FungibleBalance =
            (Account, TokenDefinitionId, Hash.Value[TransactionWithResult])

          val transferFungibleTokenProgram: StateT[
            EitherT[F, String, *],
            MerkleTrieState[FungibleBalance, Unit],
            Unit,
          ] = for
            _ <- tf.inputs.toList.traverse { (txHash) =>
              MerkleTrie.remove[F, FungibleBalance, Unit](
                (sig.account, tf.tokenDefinitionId, txHash).toBytes.bits,
              )
            }
            _ <- tf.outputs.toList.traverse { (account, amount) =>
              MerkleTrie.put[F, FungibleBalance, Unit](
                (
                  account,
                  tf.tokenDefinitionId,
                  txWithResult.toHash,
                ).toBytes.bits,
                (),
              )
            }
          yield ()

          for
            pubKeySummary <- EitherT.fromEither[F](
              recoverSignature(tf, sig.sig),
            )
            accountPubKeyOption <- MerkleTrie
              .get[F, (Account, PublicKeySummary), PublicKeySummary.Info](
                (sig.account, pubKeySummary).toBytes.bits,
              )
              .runA(ms.account.keyState)
            _ <- EitherT.fromOption[F](
              accountPubKeyOption,
              s"Account ${sig.account} does not have public key summary $pubKeySummary",
            )
            fungibleBalanceState <- transferFungibleTokenProgram.runS(
              ms.token.fungibleBalanceState,
            )
          yield (
            ms.copy(token =
              ms.token.copy(fungibleBalanceState = fungibleBalanceState),
            ),
            txWithResult,
          )
        case sf: Transaction.TokenTx.SuggestFungibleTokenDeal =>
          val txWithResult = TransactionWithResult(Signed(sig, tx), None)

          for
            pubKeySummary <- EitherT.fromEither[F](
              recoverSignature(sf, sig.sig),
            )
            accountPubKeyOption <- MerkleTrie
              .get[F, (Account, PublicKeySummary), PublicKeySummary.Info](
                (sig.account, pubKeySummary).toBytes.bits,
              )
              .runA(ms.account.keyState)
            _ <- EitherT.fromOption[F](
              accountPubKeyOption,
              s"Account ${sig.account} does not have public key summary $pubKeySummary",
            )
            fungibleBalanceState <- sf.inputs.toList
              .traverse { (txHash) =>
                MerkleTrie.remove[
                  F,
                  (
                      Account,
                      TokenDefinitionId,
                      Hash.Value[TransactionWithResult],
                  ),
                  Unit,
                ](
                  (sig.account, sf.inputDefinitionId, txHash).toBytes.bits,
                )
              }
              .runS(ms.token.fungibleBalanceState)
            lockState <- sf.inputs.toList
              .traverse { (txHash) =>
                MerkleTrie
                  .put[F, (Account, Hash.Value[TransactionWithResult]), Unit](
                    (sig.account, txHash).toBytes.bits,
                    (),
                  )
              }
              .runS(ms.token.lockState)
            deadlineState <- sf.inputs.toList
              .traverse { (txHash) =>
                MerkleTrie
                  .put[F, (Instant, Hash.Value[TransactionWithResult]), Unit](
                    (sf.dealDeadline, txHash).toBytes.bits,
                    (),
                  )
              }
              .runS(ms.token.deadlineState)
            suggestionState <- sf.originalSuggestion match
              case Some(originalSuggestion) => MerkleTrie
                .put[F, (Hash.Value[TransactionWithResult], Hash.Value[TransactionWithResult]), Unit](
                  (originalSuggestion, txWithResult.toHash).toBytes.bits,
                  (),
                ).runS(ms.token.suggestionState)
              case None => EitherT.pure(ms.token.suggestionState)
          yield (
            ms.copy(token =
              ms.token.copy(
                fungibleBalanceState = fungibleBalanceState,
                lockState = lockState,
                deadlineState = deadlineState,
                suggestionState = suggestionState,
              ),
            ),
            txWithResult,
          )

        case ad: Transaction.TokenTx.AcceptDeal =>
          def getTx(
              txHash: Signed.TxHash,
          ): EitherT[F, String, TransactionWithResult] =
            for
              txWithResultOption <- TransactionRepository[F]
                .get(ad.suggestion.toResultHashValue)
                .leftMap(_.msg)
              txWithResult <- EitherT.fromOption[F](
                txWithResultOption,
                s"Suggestion transaction ${ad.suggestion} not found",
              )
            yield txWithResult

          for
            pubKeySummary <- EitherT.fromEither[F](
              recoverSignature(ad, sig.sig),
            )
            accountPubKeyOption <- MerkleTrie
              .get[F, (Account, PublicKeySummary), PublicKeySummary.Info](
                (sig.account, pubKeySummary).toBytes.bits,
              )
              .runA(ms.account.keyState)
            _ <- EitherT.fromOption[F](
              accountPubKeyOption,
              s"Account ${sig.account} does not have public key summary $pubKeySummary",
            )
            suggestionTx <- getTx(ad.suggestion)

            suggestionAccount = suggestionTx.signedTx.sig.account

            dealSuggestion <- suggestionTx.signedTx.value match
              case s: Transaction.DealSuggestion =>
                EitherT.pure(s)
              case _ =>
                EitherT.leftT(
                  s"Transaction ${ad.suggestion} is not a suggestion",
                )
            result <- dealSuggestion match
              case sf: Transaction.TokenTx.SuggestFungibleTokenDeal =>
                val inputList = ad.inputs.toList
                for
                  inputTxs <- inputList.traverse(getTx)
                  inputAmounts <- inputTxs.traverse{ txWithResult =>
                    txWithResult.signedTx.value match
                      case f: Transaction.FungibleBalance =>
                        f match
                          case mf: Transaction.TokenTx.MintFungibleToken =>
                            EitherT.pure(mf.outputs.get(sig.account).getOrElse(BigNat.Zero))
                          case tf: Transaction.TokenTx.TransferFungibleToken =>
                            EitherT.pure(tf.outputs.get(sig.account).getOrElse(BigNat.Zero))
                          case ad: Transaction.TokenTx.AcceptDeal =>
                            txWithResult.result match
                              case Some(Transaction.TokenTx.AcceptDealResult(outputs)) =>
                                EitherT.pure(outputs
                                  .get(sig.account)
                                  .flatMap(_.get(sf.requirement.definitionId))
                                  .fold(BigNat.Zero) {
                                    case TokenDetail.FungibleDetail(amount) => amount
                                    case TokenDetail.NftDetail(_)           => BigNat.Zero
                                  }
                                )
                              case _ =>
                                EitherT.leftT(
                                  s"AcceptDeal result is not found for $ad",
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
                                    .flatMap(_.get(sf.inputDefinitionId))
                                    .getOrElse(BigNat.Zero)
                                }
                              case _ =>
                                EitherT.leftT(
                                  s"InitialTokenOffering result is not found for $it",
                                )
                      case tx => EitherT.leftT(
                        s"Transaction ${tx.toHash} is not a fungible balance",
                      )
                  }
                  inputSum = inputAmounts.map(_.toBigInt).sum
                  outputAmount <- EitherT.fromEither[F](
                    BigNat.fromBigInt(inputSum - sf.requirement.amount.toBigInt)
                  ).leftMap(_ => s"Input amount $inputSum is not enough for $sf")
                  receivedTxHashes <- sf.inputs.toList.traverse(getTx)
                  receivedAmounts <- receivedTxHashes.traverse{ txWithResult =>
                    txWithResult.signedTx.value match
                      case f: Transaction.FungibleBalance =>
                        f match
                          case mf: Transaction.TokenTx.MintFungibleToken =>
                            EitherT.pure(mf.outputs.get(txWithResult.signedTx.sig.account).getOrElse(BigNat.Zero))
                          case tf: Transaction.TokenTx.TransferFungibleToken =>
                            EitherT.pure(tf.outputs.get(txWithResult.signedTx.sig.account).getOrElse(BigNat.Zero))
                          case ad: Transaction.TokenTx.AcceptDeal =>
                            txWithResult.result match
                              case Some(Transaction.TokenTx.AcceptDealResult(outputs)) =>
                                EitherT.pure(outputs
                                  .get(txWithResult.signedTx.sig.account)
                                  .flatMap(_.get(sf.inputDefinitionId))
                                  .fold(BigNat.Zero) {
                                    case TokenDetail.FungibleDetail(amount) => amount
                                    case TokenDetail.NftDetail(_)           => BigNat.Zero
                                  }
                                )
                              case _ =>
                                EitherT.leftT(
                                  s"AcceptDeal result is not found: $txWithResult",
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
                                    .flatMap(_.get(sf.inputDefinitionId))
                                    .getOrElse(BigNat.Zero)
                                }
                              case _ =>
                                EitherT.leftT(
                                  s"InitialTokenOffering result is not found for $it",
                                )
                      case tx => EitherT.leftT(
                        s"Transaction ${tx.toHash} is not a fungible balance",
                      )
                  }
                  receivedSum = receivedAmounts.foldLeft(BigNat.Zero)(BigNat.add)

                  result = TransactionWithResult(
                    Signed(sig, tx),
                    result = Some(Transaction.TokenTx.AcceptDealResult(Map(
                      sig.account -> Map(
                        sf.requirement.definitionId -> TokenDetail.FungibleDetail(
                          outputAmount,
                        ),
                        sf.inputDefinitionId -> TokenDetail.FungibleDetail(
                          receivedSum,
                        ),
                      ),
                      suggestionAccount -> Map(
                        sf.requirement.definitionId -> TokenDetail.FungibleDetail(
                          sf.requirement.amount,
                        ),
                      ),
                    )))
                  )

                  fungibleBalanceState <- {
                    for
                      _ <- inputList.traverse{ inputTx =>
                        MerkleTrie.remove[F, (Account, TokenDefinitionId, Hash.Value[TransactionWithResult]), Unit](
                          (sig.account, sf.inputDefinitionId, inputTx).toBytes.bits,
                        )
                      }
                      _ <- MerkleTrie.put[F, (Account, TokenDefinitionId, Hash.Value[TransactionWithResult]), Unit](
                        (sig.account, sf.inputDefinitionId, result.toHash).toBytes.bits,
                        (),
                      )
                      _ <- MerkleTrie.put[F, (Account, TokenDefinitionId, Hash.Value[TransactionWithResult]), Unit](
                        (suggestionAccount, sf.requirement.definitionId, result.toHash).toBytes.bits,
                        (),
                      )
                    yield ()
                  }.runS(ms.token.fungibleBalanceState)

                  lockState <- MerkleTrie.remove[F, (Account, Hash.Value[TransactionWithResult]), Unit](
                    (suggestionAccount, ad.suggestion).toBytes.bits,
                  ).runS(ms.token.lockState)

                  suggestionState <- sf.originalSuggestion match
                    case None => EitherT.pure(ms.token.suggestionState)
                    case Some(originalSuggestionTxHash) => {
                      for
                        pairStream <- MerkleTrie.from[F, (Hash.Value[TransactionWithResult], Hash.Value[TransactionWithResult]), Unit](originalSuggestionTxHash.toBytes.bits)
                        pairList <- StateT.liftF(pairStream.takeWhile(_._1.startsWith(suggestionAccount.toBytes.bits)).compile.toList)
                        _ <- pairList.traverse{ case (k, _) =>
                          MerkleTrie.remove[F, (Hash.Value[TransactionWithResult], Hash.Value[TransactionWithResult]), Unit](k)
                        }
                      yield ()
                    }.runS(ms.token.suggestionState)
                  
                yield (ms.copy(token = ms.token.copy(
                  fungibleBalanceState = fungibleBalanceState,
                  lockState = lockState,
                  suggestionState = suggestionState,
                )), result)
          yield result
