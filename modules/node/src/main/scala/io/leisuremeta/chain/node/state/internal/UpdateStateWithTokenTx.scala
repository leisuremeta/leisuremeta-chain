package io.leisuremeta.chain
package node
package state
package internal

import java.time.Instant

import cats.data.{EitherT, StateT}
import cats.effect.Concurrent
import cats.syntax.either.given
import cats.syntax.eq.given
import cats.syntax.foldable.given
import cats.syntax.traverse.given

import fs2.Stream
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
import lib.merkle.{GenericMerkleTrie, GenericMerkleTrieState}
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
            tokenDefinitionOption <- GenericMerkleTrie
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
            tokenDefinitionState <- GenericMerkleTrie
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
            tokenDefinitionOption <- GenericMerkleTrie
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
            groupAccountOption <- GenericMerkleTrie
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
            accountPubKeyOption <- GenericMerkleTrie
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
              GenericMerkleTrie
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
                _ <- GenericMerkleTrie.remove[F, TokenDefinitionId, TokenDefinition](
                  mf.definitionId.toBytes.bits,
                )
                _ <- GenericMerkleTrie.put[F, TokenDefinitionId, TokenDefinition](
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
            tokenDefinitionOption <- GenericMerkleTrie
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
            groupAccountOption <- GenericMerkleTrie
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
            accountPubKeyOption <- GenericMerkleTrie
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
            nftBalanceState <- GenericMerkleTrie
              .put[
                F,
                (Account, TokenId, Hash.Value[TransactionWithResult]),
                Unit,
              ](balanceItem.toBytes.bits, ())
              .runS(ms.token.nftBalanceState)
            weightOption = for
              nftInfo <- tokenDefinition.nftInfo
              weight  <- nftInfo.rarity.get(mn.rarity)
            yield weight
            nftStateItem = NftState(
              tokenId = mn.tokenId,
              tokenDefinitionId = mn.tokenDefinitionId,
              rarity = mn.rarity,
              // Set default weight as 2
              weight = weightOption.getOrElse(BigNat.unsafeFromLong(2)),
              currentOwner = mn.output,
            )
            nftState <- GenericMerkleTrie
              .put[F, TokenId, NftState](mn.tokenId.toBytes.bits, nftStateItem)
              .runS(ms.token.nftState)
            rarityItem = (mn.tokenDefinitionId, mn.rarity, mn.tokenId)
            rarityState <- GenericMerkleTrie
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
            GenericMerkleTrieState[FungibleBalance, Unit],
            Unit,
          ] = for
            _ <- tf.inputs.toList.traverse { (txHash) =>
              GenericMerkleTrie.remove[F, FungibleBalance, Unit](
                (sig.account, tf.tokenDefinitionId, txHash).toBytes.bits,
              )
            }
            _ <- tf.outputs.toList.traverse { (account, amount) =>
              GenericMerkleTrie.put[F, FungibleBalance, Unit](
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
            accountPubKeyOption <- GenericMerkleTrie
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
        case tn: Transaction.TokenTx.TransferNFT =>
          val txWithResult = TransactionWithResult(Signed(sig, tx), None)
          type NftBalance =
            (Account, TokenId, Hash.Value[TransactionWithResult])

          val nftBalanceProgram: StateT[
            EitherT[F, String, *],
            GenericMerkleTrieState[NftBalance, Unit],
            Unit,
          ] = for
            _ <- GenericMerkleTrie.remove[F, NftBalance, Unit] {
              (sig.account, tn.tokenId, tn.input).toBytes.bits
            }
            _ <- GenericMerkleTrie.put[F, NftBalance, Unit](
              (
                tn.output,
                tn.tokenId,
                txWithResult.toHash,
              ).toBytes.bits,
              (),
            )
          yield ()

          val nftStateProgram: StateT[
            EitherT[F, String, *],
            GenericMerkleTrieState[TokenId, NftState],
            Unit,
          ] = for
            nftStateOption <- GenericMerkleTrie.get[F, TokenId, NftState]((tn.tokenId).toBytes.bits)
            nftState <- StateT.liftF{
              EitherT.fromOption(nftStateOption, s"No nft state in token ${tn.tokenId}")
            }
            _ <- GenericMerkleTrie.remove[F, TokenId, NftState]((tn.tokenId).toBytes.bits)
            _ <- GenericMerkleTrie.put[F, TokenId, NftState](
              tn.tokenId.toBytes.bits,
              nftState.copy(currentOwner = tn.output),
            )
          yield ()

          for
            pubKeySummary <- EitherT.fromEither[F](
              recoverSignature(tn, sig.sig),
            )
            accountPubKeyOption <- GenericMerkleTrie
              .get[F, (Account, PublicKeySummary), PublicKeySummary.Info](
                (sig.account, pubKeySummary).toBytes.bits,
              )
              .runA(ms.account.keyState)
            _ <- EitherT.fromOption[F](
              accountPubKeyOption,
              s"Account ${sig.account} does not have public key summary $pubKeySummary",
            )
//            _ <- EitherT.pure{
//              scribe.info(s"Old NftBalanceState: ${ms.token.nftBalanceState}")
//            }
            nftBalanceState <- nftBalanceProgram.runS(
              ms.token.nftBalanceState,
            )
//            _ <- EitherT.pure{
//              scribe.info(s"New NftBalanceState: ${nftBalanceState}")
//            }
            nftState <- nftStateProgram.runS(ms.token.nftState)
          yield (
            ms.copy(token =
              ms.token.copy(
                nftBalanceState = nftBalanceState,
                nftState = nftState,
              ),
            ),
            txWithResult,
          )
        case bf: Transaction.TokenTx.BurnFungibleToken =>
          type FungibleBalance =
            (Account, TokenDefinitionId, Hash.Value[TransactionWithResult])

          def burnFungibleTokenProgram(
              txWithResult: TransactionWithResult,
          ): StateT[
            EitherT[F, String, *],
            GenericMerkleTrieState[FungibleBalance, Unit],
            Unit,
          ] =
            val txHash = txWithResult.toHash
            for
              _ <- bf.inputs.toList.traverse { (txHash) =>
                GenericMerkleTrie.remove[F, FungibleBalance, Unit](
                  (sig.account, bf.definitionId, txHash).toBytes.bits,
                )
              }
              _ <- GenericMerkleTrie.put[F, FungibleBalance, Unit](
                (
                  sig.account,
                  bf.definitionId,
                  txHash,
                ).toBytes.bits,
                (),
              )
            yield ()

          for
            pubKeySummary <- EitherT.fromEither[F](
              recoverSignature(bf, sig.sig),
            )
            accountPubKeyOption <- GenericMerkleTrie
              .get[F, (Account, PublicKeySummary), PublicKeySummary.Info](
                (sig.account, pubKeySummary).toBytes.bits,
              )
              .runA(ms.account.keyState)
            _ <- EitherT.fromOption[F](
              accountPubKeyOption,
              s"Account ${sig.account} does not have public key summary $pubKeySummary",
            )
            inputList = bf.inputs.toList
            inputAmountList <- inputList.traverse { (txHash) =>
              for
                txOption <- TransactionRepository[F]
                  .get(txHash.toResultHashValue)
                  .leftMap(_.msg)
                tx <- EitherT
                  .fromOption[F](txOption, s"input $txHash is not exist")
                amount <- EitherT.fromEither[F](fungibleAmount(tx, sig.account))
              yield amount
            }
            remainder <- EitherT.fromEither[F] {
              BigNat.fromBigInt(
                inputAmountList.map(_.toBigInt).sum - bf.amount.toBigInt,
              )
            }
            txWithResult = TransactionWithResult(
              Signed(sig, tx),
              Some(Transaction.TokenTx.BurnFungibleTokenResult(remainder)),
            )
            fungibleBalanceState <- burnFungibleTokenProgram(txWithResult).runS(
              ms.token.fungibleBalanceState,
            )
            tokenDefinitionOption <- GenericMerkleTrie
              .get[F, TokenDefinitionId, TokenDefinition](
                bf.definitionId.toBytes.bits,
              )
              .runA(ms.token.tokenDefinitionState)
            tokenDefinition <- EitherT.fromOption(
              tokenDefinitionOption,
              s"No token definition of ${bf.definitionId}",
            )
            totalAmount <- EitherT.fromEither[F] {
              BigNat.fromBigInt(
                tokenDefinition.totalAmount.toBigInt - bf.amount.toBigInt,
              )
            }
            tokenDefinitionState <- GenericMerkleTrie
              .put[F, TokenDefinitionId, TokenDefinition](
                bf.definitionId.toBytes.bits,
                tokenDefinition.copy(totalAmount = totalAmount),
              )
              .runS(ms.token.tokenDefinitionState)
          yield (
            ms.copy(token =
              ms.token.copy(
                fungibleBalanceState = fungibleBalanceState,
                tokenDefinitionState = tokenDefinitionState,
              ),
            ),
            txWithResult,
          )
        case bn: Transaction.TokenTx.BurnNFT =>
          val txWithResult = TransactionWithResult(Signed(sig, tx), None)
          type NftBalance =
            (Account, TokenId, Hash.Value[TransactionWithResult])

          for
            pubKeySummary <- EitherT.fromEither[F](
              recoverSignature(bn, sig.sig),
            )
            accountPubKeyOption <- GenericMerkleTrie
              .get[F, (Account, PublicKeySummary), PublicKeySummary.Info](
                (sig.account, pubKeySummary).toBytes.bits,
              )
              .runA(ms.account.keyState)
            _ <- EitherT.fromOption[F](
              accountPubKeyOption,
              s"Account ${sig.account} does not have public key summary $pubKeySummary",
            )
//            _ <- EitherT.pure{
//              scribe.info(s"Old NftBalanceState: ${ms.token.nftBalanceState}")
//            }
            inputTxOption <- TransactionRepository[F]
              .get(bn.input.toResultHashValue)
              .leftMap(_.msg)
            inputTx <- EitherT.fromOption(
              inputTxOption,
              s"input ${bn.input} does not exist",
            )
            tokenId <- {
              inputTx.signedTx.value match
                case nftBalance: Transaction.NftBalance =>
                  EitherT.pure(nftBalance.tokenId)
                case _ =>
                  EitherT.leftT(s"input ${bn.input} is not an NFT balance")
            }
            nftBalanceState <- GenericMerkleTrie
              .remove[F, NftBalance, Unit] {
                (sig.account, tokenId, bn.input).toBytes.bits
              }
              .runS(ms.token.nftBalanceState)
//            _ <- EitherT.pure{
//              scribe.info(s"New NftBalanceState: ${nftBalanceState}")
//            }
            nftState <- GenericMerkleTrie
              .remove[F, TokenId, NftState](tokenId.toBytes.bits)
              .runS(ms.token.nftState)
            rarityStream <- GenericMerkleTrie
              .from[F, (TokenDefinitionId, Rarity, TokenId), Unit](
                bn.definitionId.toBytes.bits,
              )
              .runA(ms.token.rarityState)
            rarityList <- rarityStream
              .flatMap { case element =>
                Stream.eval {
                  EitherT.fromEither {
                    ByteDecoder[(TokenDefinitionId, Rarity, TokenId)]
                      .decode(element._1.bytes)
                      .leftMap(_.msg)
                      .map { case DecodeResult((defId, rarity, tokenId), _) =>
                        (rarity, tokenId)
                      }
                  }
                }
              }
              .filter { _._2 === tokenId }
              .compile
              .toList
            rarity <- EitherT.fromOption(
              rarityList.headOption,
              s"No rarity item of tokenId ${tokenId} in rarity state",
            )
            rarityState <- GenericMerkleTrie
              .remove[F, (TokenDefinitionId, Rarity, TokenId), Unit](
                (bn.definitionId, rarity, tokenId).toBytes.bits,
              )
              .runS(ms.token.rarityState)
          yield (
            ms.copy(token =
              ms.token.copy(
                nftBalanceState = nftBalanceState,
                nftState = nftState,
                rarityState = rarityState,
              ),
            ),
            txWithResult,
          )

        case ef: Transaction.TokenTx.EntrustFungibleToken =>
          type FungibleBalance =
            (Account, TokenDefinitionId, Hash.Value[TransactionWithResult])

          type EntrustFungibleBalance =
            (
                Account,
                Account,
                TokenDefinitionId,
                Hash.Value[TransactionWithResult],
            )
          for
            pubKeySummary <- EitherT.fromEither[F](
              recoverSignature(ef, sig.sig),
            )
            accountPubKeyOption <- GenericMerkleTrie
              .get[F, (Account, PublicKeySummary), PublicKeySummary.Info](
                (sig.account, pubKeySummary).toBytes.bits,
              )
              .runA(ms.account.keyState)
            _ <- EitherT.fromOption[F](
              accountPubKeyOption,
              s"Account ${sig.account} does not have public key summary $pubKeySummary",
            )
            inputAmounts <- ef.inputs.toList.traverse {
              (txHash: Signed.TxHash) =>
                TransactionRepository[F]
                  .get(txHash.toResultHashValue)
                  .leftMap(_.msg)
                  .flatMap {
                    case Some(txWithResult) =>
                      txWithResult.signedTx.value match
                        case fb: Transaction.FungibleBalance =>
                          fb match
                            case mf: Transaction.TokenTx.MintFungibleToken =>
                              EitherT.pure(
                                mf.outputs
                                  .get(sig.account)
                                  .getOrElse(BigNat.Zero),
                              )
                            case bf: Transaction.TokenTx.BurnFungibleToken =>
                              txWithResult.result match
                                case Some(
                                      Transaction.TokenTx
                                        .BurnFungibleTokenResult(outputAmount),
                                    ) =>
                                  EitherT.pure(outputAmount)
                                case other =>
                                  EitherT.leftT[F, BigNat](
                                    s"burn fungible token result of $txHash has wrong result: $other",
                                  )
                            case tf: Transaction.TokenTx.TransferFungibleToken =>
                              EitherT.pure(
                                tf.outputs
                                  .get(sig.account)
                                  .getOrElse(BigNat.Zero),
                              )

                            case ef: Transaction.TokenTx.EntrustFungibleToken =>
                              EitherT.pure(
                                txWithResult.result.fold(BigNat.Zero) {
                                  case Transaction.TokenTx
                                        .EntrustFungibleTokenResult(
                                          remainder,
                                        ) =>
                                    remainder
                                  case _ => BigNat.Zero
                                },
                              )
                            case df: Transaction.TokenTx.DisposeEntrustedFungibleToken =>
                              EitherT.pure(
                                df.outputs
                                  .get(sig.account)
                                  .getOrElse(BigNat.Zero),
                              )
                            case xr: Transaction.RewardTx.ExecuteReward =>
                              EitherT.pure {
                                txWithResult.result match
                                  case Some(
                                        Transaction.RewardTx
                                          .ExecuteRewardResult(outputs),
                                      ) =>
                                    outputs
                                      .get(sig.account)
                                      .getOrElse(BigNat.Zero)
                                  case _ => BigNat.Zero
                              }
                        case _ =>
                          EitherT.leftT[F, BigNat](
                            s"input tx $txHash is not a fungible balance",
                          )
                    case None =>
                      EitherT.leftT[F, BigNat](
                        s"input tx $txHash does not exist",
                      )
                  }
            }
            remainder <- EitherT.fromEither[F] {
              val inputSum = inputAmounts.map(_.toBigInt).sum
              BigNat
                .fromBigInt(inputSum - ef.amount.toBigInt)
                .leftMap(_ =>
                  s"input sum $inputSum is less than output amount ${ef.amount}",
                )
            }
            result = Transaction.TokenTx.EntrustFungibleTokenResult(remainder)
            txWithResult = TransactionWithResult(Signed(sig, tx), Some(result))
            fungibleBalanceState0 <- ef.inputs.toList
              .traverse { (txHash) =>
                GenericMerkleTrie.remove[F, FungibleBalance, Unit](
                  (sig.account, ef.definitionId, txHash).toBytes.bits,
                )
              }
              .runS(ms.token.fungibleBalanceState)
            fungibleBalanceState <- GenericMerkleTrie
              .put[F, FungibleBalance, Unit](
                (
                  sig.account,
                  ef.definitionId,
                  txWithResult.toHash,
                ).toBytes.bits,
                (),
              )
              .runS(fungibleBalanceState0)
            entrustFungibleBalanceState <- GenericMerkleTrie
              .put[F, EntrustFungibleBalance, Unit](
                (
                  sig.account,
                  ef.to,
                  ef.definitionId,
                  txWithResult.toHash,
                ).toBytes.bits,
                (),
              )
              .runS(ms.token.entrustFungibleBalanceState)
          yield (
            ms.copy(token =
              ms.token.copy(
                fungibleBalanceState = fungibleBalanceState,
                entrustFungibleBalanceState = entrustFungibleBalanceState,
              ),
            ),
            txWithResult,
          )
        case en: Transaction.TokenTx.EntrustNFT =>
          val txWithResult = TransactionWithResult(Signed(sig, tx), None)
          type NftBalance =
            (Account, TokenId, Hash.Value[TransactionWithResult])
          type EntrustNftBalance =
            (Account, Account, TokenId, Hash.Value[TransactionWithResult])

          for
            pubKeySummary <- EitherT.fromEither[F](
              recoverSignature(en, sig.sig),
            )
            accountPubKeyOption <- GenericMerkleTrie
              .get[F, (Account, PublicKeySummary), PublicKeySummary.Info](
                (sig.account, pubKeySummary).toBytes.bits,
              )
              .runA(ms.account.keyState)
            _ <- EitherT.fromOption[F](
              accountPubKeyOption,
              s"Account ${sig.account} does not have public key summary $pubKeySummary",
            )
            nftBalanceState <- GenericMerkleTrie
              .remove[F, NftBalance, Unit] {
                (sig.account, en.tokenId, en.input).toBytes.bits
              }
              .runS(ms.token.nftBalanceState)
            entrustNftBalanceState <- GenericMerkleTrie
              .put[F, EntrustNftBalance, Unit](
                (
                  sig.account,
                  en.to,
                  en.tokenId,
                  txWithResult.toHash,
                ).toBytes.bits,
                (),
              )
              .runS(ms.token.entrustNftBalanceState)
          yield (
            ms.copy(token =
              ms.token.copy(
                nftBalanceState = nftBalanceState,
                entrustNftBalanceState = entrustNftBalanceState,
              ),
            ),
            txWithResult,
          )
        case de: Transaction.TokenTx.DisposeEntrustedFungibleToken =>
          val txWithResult = TransactionWithResult(Signed(sig, tx), None)

          type FungibleBalance =
            (Account, TokenDefinitionId, Hash.Value[TransactionWithResult])

          type EntrustFungibleBalance =
            (
                Account,
                Account,
                TokenDefinitionId,
                Hash.Value[TransactionWithResult],
            )
          for
            pubKeySummary <- EitherT.fromEither[F](
              recoverSignature(de, sig.sig),
            )
            accountPubKeyOption <- GenericMerkleTrie
              .get[F, (Account, PublicKeySummary), PublicKeySummary.Info](
                (sig.account, pubKeySummary).toBytes.bits,
              )
              .runA(ms.account.keyState)
            _ <- EitherT.fromOption[F](
              accountPubKeyOption,
              s"Account ${sig.account} does not have public key summary $pubKeySummary",
            )
            inputs <- de.inputs.toList.traverse { (txHash) =>
              TransactionRepository[F]
                .get(txHash.toResultHashValue)
                .leftMap(_.msg)
                .flatMap {
                  case None =>
                    EitherT.leftT(s"Entrust Input $txHash does not exist.")
                  case Some(txWithResult) =>
                    txWithResult.signedTx.value match
                      case ef: Transaction.TokenTx.EntrustFungibleToken =>
                        EitherT.pure(
                          (
                            (txWithResult.signedTx.sig.account, txHash),
                            ef.amount.toBigInt,
                          ),
                        )
                      case otherTx =>
                        EitherT.leftT(
                          s"input tx $txHash is not EntrustFunbleToken: $otherTx",
                        )
                }
            }
            inputBalance = inputs.unzip._2.sum
            outputSum    = de.outputs.values.map(_.toBigInt).sum
            _ <- EitherT.cond(
              inputBalance >= outputSum,
              (),
              s"Input balance $inputBalance is less than output sum $outputSum",
            )
            entrustFungibleBalanceState <- inputs.unzip._1
              .traverse { case (account, txHash) =>
                GenericMerkleTrie.remove[F, EntrustFungibleBalance, Unit](
                  (account, sig.account, de.definitionId, txHash).toBytes.bits,
                )
              }
              .runS(ms.token.entrustFungibleBalanceState)
            fungibleBalanceState <- de.outputs.toList
              .traverse { case (account, amount) =>
                GenericMerkleTrie.put[F, FungibleBalance, Unit](
                  (account, de.definitionId, txWithResult.toHash).toBytes.bits,
                  (),
                )
              }
              .runS(ms.token.fungibleBalanceState)
          yield (
            ms.copy(token =
              ms.token.copy(
                fungibleBalanceState = fungibleBalanceState,
                entrustFungibleBalanceState = entrustFungibleBalanceState,
              ),
            ),
            txWithResult,
          )
        case dn: Transaction.TokenTx.DisposeEntrustedNFT =>
          type NftBalance =
            (Account, TokenId, Hash.Value[TransactionWithResult])
          type EntrustNftBalance =
            (Account, Account, TokenId, Hash.Value[TransactionWithResult])

          for
            pubKeySummary <- EitherT.fromEither[F](
              recoverSignature(dn, sig.sig),
            )
            accountPubKeyOption <- GenericMerkleTrie
              .get[F, (Account, PublicKeySummary), PublicKeySummary.Info](
                (sig.account, pubKeySummary).toBytes.bits,
              )
              .runA(ms.account.keyState)
            _ <- EitherT.fromOption[F](
              accountPubKeyOption,
              s"Account ${sig.account} does not have public key summary $pubKeySummary",
            )
            inputTxOption <- TransactionRepository[F]
              .get(dn.input.toResultHashValue)
              .leftMap(_.msg)
            inputTx <- inputTxOption match
              case None => EitherT.leftT(s"Input tx ${dn.input} does not exist")
              case Some(txWithResult) =>
                txWithResult.signedTx.value match
                  case eb: Transaction.TokenTx.EntrustNFT =>
                    if eb.to === sig.account then
                      EitherT.pure((txWithResult.signedTx.sig.account, eb))
                    else
                      EitherT.leftT(
                        s"Input $eb is not entrusted to ${sig.account}",
                      )
                  case other =>
                    EitherT.leftT(s"input tx $other is not a EntrustNft tx")
            inputAccount  = inputTx._1
            outputAccount = dn.output.getOrElse(inputAccount)
            entrustNftBalanceState <- GenericMerkleTrie
              .remove[F, EntrustNftBalance, Unit] {
                (inputAccount, sig.account, dn.tokenId, dn.input).toBytes.bits
              }
              .runS(ms.token.entrustNftBalanceState)
            txWithResult = TransactionWithResult(Signed(sig, tx), None)
            nftBalanceState <- GenericMerkleTrie
              .put[F, NftBalance, Unit](
                (
                  outputAccount,
                  dn.tokenId,
                  txWithResult.toHash,
                ).toBytes.bits,
                (),
              )
              .runS(ms.token.nftBalanceState)
          yield (
            ms.copy(token =
              ms.token.copy(
                nftBalanceState = nftBalanceState,
                entrustNftBalanceState = entrustNftBalanceState,
              ),
            ),
            txWithResult,
          )

  def fungibleAmount(
      tx: TransactionWithResult,
      account: Account,
  ): Either[String, BigNat] =
    tx.signedTx.value match
      case fb: Transaction.FungibleBalance =>
        fb match
          case mf: Transaction.TokenTx.MintFungibleToken =>
            Either.fromOption(
              mf.outputs.get(account),
              s"Account $account does not have output of tx $mf",
            )
          case bf: Transaction.TokenTx.BurnFungibleToken =>
            tx.result match
              case Some(
                    Transaction.TokenTx.BurnFungibleTokenResult(outputAmount),
                  ) =>
                Either.cond(
                  account === tx.signedTx.sig.account,
                  outputAmount,
                  s"Account $account is not owner of tx $tx",
                )
              case _ => Either.left(s"Invalid transaction result: $tx")
          case tf: Transaction.TokenTx.TransferFungibleToken =>
            Either.fromOption(
              tf.outputs.get(account),
              s"Account $account does not have output of tx $tf",
            )
          case ef: Transaction.TokenTx.EntrustFungibleToken =>
            tx.result match
              case Some(
                    Transaction.TokenTx.EntrustFungibleTokenResult(remainder),
                  ) =>
                Either.cond(
                  account === tx.signedTx.sig.account,
                  remainder,
                  s"Account $account is not owner of tx $tx",
                )
              case _ => Either.left(s"Invalid transaction result: $tx")
          case de: Transaction.TokenTx.DisposeEntrustedFungibleToken =>
            Either.fromOption(
              de.outputs.get(account),
              s"Account $account does not have output of tx $de",
            )
          case er: Transaction.RewardTx.ExecuteReward =>
            tx.result match
              case Some(
                    Transaction.RewardTx.ExecuteRewardResult(outputs),
                  ) =>
                Either.fromOption(
                  outputs.get(account),
                  s"Account $account does not have output of tx $tx",
                )
              case _ => Either.left(s"Invalid transaction result: $tx")
      case _ => Either.left(s"Transaction $tx is not a fungible balance")
