package org.leisuremeta.lmchain.core
package node
package service

import cats.Monad
import cats.data.{EitherT, StateT}
import cats.implicits._

import codec.byte.ByteEncoder.ops._
import crypto.Hash.ops._
import crypto.{MerkleTrie, Signature}
import crypto.MerkleTrie.MerkleTrieState
import crypto.Recover.ops._
import datatype.BigNat
import model._
import model.Account.{Named, Unnamed}
import model.AccountSignature.{NamedSignature, UnnamedSignature}
import model.Transaction.Name.{CreateName, DeleteName, UpdateName}
import model.Transaction.Token._
import repository.{StateRepository, TransactionRepository}
import repository.StateRepository._
import org.leisuremeta.lmchain.core.crypto.MerkleTrieNode

object StateService {

  case class MerkleState(
      namesState: MerkleTrieState[Account.Name, NameState],
      tokenState: MerkleTrieState[DefinitionId, TokenState],
      balanceState: MerkleTrieState[(Account, Transaction.Input.Tx), Unit],
  )

  object MerkleState {
    def from(header: Block.Header): MerkleState = MerkleState(
      namesState = buildMerkleTrieState(header.namesRoot),
      tokenState = buildMerkleTrieState(header.tokenRoot),
      balanceState = buildMerkleTrieState(header.balanceRoot),
    )
  }

  def buildMerkleTrieState[K, V](
      root: Option[MerkleTrieNode.MerkleRoot[K, V]]
  ): MerkleTrieState[K, V] =
    root.fold(MerkleTrieState.empty[K, V])(MerkleTrieState.fromRoot)

  def updateStateWithTx[F[_]: Monad](state: MerkleState, signedTx: Signed.Tx)(
      implicit
      namesStateRepo: StateRepository[F, Account.Name, NameState],
      tokenStateRepo: StateRepository[F, DefinitionId, TokenState],
      balanceStateRepo: StateRepository[
        F,
        (Account, Transaction.Input.Tx),
        Unit,
      ],
      txRepo: TransactionRepository[F],
  ): EitherT[F, String, MerkleState] = {
    scribe.debug(s"Updating state with tx: $signedTx")
    signedTx.value match {
      case tx: Transaction.Name =>
        updateStateWithNameTx(state, signedTx.sig, tx)
      case tx: Transaction.Token =>
        updateStateWithTokenTx(state, signedTx.sig, tx, signedTx.toHash)
    }
  }

  def updateStateWithNameTx[F[_]: Monad](
      ms: MerkleState,
      sig: AccountSignature,
      tx: Transaction.Name,
  )(implicit
      namesStateRepo: StateRepository[F, Account.Name, NameState]
  ): EitherT[F, String, MerkleState] = {

    val nameBits = tx.name.bytes.bits

    def verifySignatureAndRunS(ns: NameState, sig: AccountSignature)(
        program: StateT[
          EitherT[F, String, *],
          MerkleTrieState[Account.Name, NameState],
          Unit,
        ]
    ): EitherT[F, String, MerkleState] = for {
      _ <- EitherT.fromEither[F](
        verifySignature(tx, sig, Account.Named(tx.name), ns).left.flatMap {
          msg =>
            for {
              guardianAccount <- ns.guardian.toRight(msg)
              _               <- verifySignature(tx, sig, guardianAccount, ns)
            } yield ()
        }
      )
      ns1 <- program.runS(ms.namesState)
    } yield ms.copy(namesState = ns1)

    MerkleTrie
      .get[F, Account.Name, NameState](nameBits)
      .runA(ms.namesState)
      .flatMap {
        case None =>
          tx match {
            case cn: CreateName =>
              MerkleTrie
                .put[F, Account.Name, NameState](nameBits, cn.state)
                .runS(ms.namesState)
                .map { ns =>
                  ms.copy(namesState = ns)
                }
            case _ =>
              EitherT.leftT[F, MerkleState](s"Name ${tx.name} does not exists.")
          }
        case Some(ns) =>
          tx match {
            case _: CreateName =>
              EitherT.leftT[F, MerkleState](
                s"Name ${tx.name} exists already: $ns"
              )
            case _: DeleteName =>
              verifySignatureAndRunS(ns, sig) {
                MerkleTrie.removeByKey[F, Account.Name, NameState](nameBits)
              }
            case un: UpdateName =>
              verifySignatureAndRunS(ns, sig) {
                MerkleTrie.put[F, Account.Name, NameState](
                  nameBits,
                  un.state,
                )
              }
          }
      }
  }

  def verifySignature(
      tx: Transaction,
      asig: AccountSignature,
      expectedAccount: Account,
      ns: NameState,
  ): Either[String, Unit] = (expectedAccount, asig) match {
    case (Account.Named(name1), NamedSignature(name2, sig)) =>
      for {
        _ <- Either.cond(
          name1 === name2,
          (),
          s"Different name: $name1 vs $name2",
        )
        address <- recoverSignature(tx, sig)
        weight <- ns.addressess
          .get(address)
          .toRight(s"Account name $name1 does not have address $address")
        _ <- Either.cond(
          weight.value >= ns.threshold.value,
          (),
          s"Weight $weight is less than the threshold ${ns.threshold} in account name $name1",
        )
      } yield ()
    case (Account.Named(name1), UnnamedSignature(_)) =>
      Left(s"Require named signature in $name1")
    case (Account.Unnamed(address1), NamedSignature(name2, _)) =>
      Left(
        s"Require unnamed account with address $address1, but received named signature with name $name2"
      )
    case (Account.Unnamed(address1), UnnamedSignature(sig)) =>
      for {
        address <- recoverSignature(tx, sig)
        _ <- Either.cond(
          address1 === address,
          (),
          s"Expected address $address1, but signed with address $address",
        )
      } yield ()
  }

  def recoverSignature(
      tx: Transaction,
      sig: Signature,
  ): Either[String, Address] = tx
    .recover(sig)
    .toRight(
      s"Cannot recover public key from signature: $sig and transaction: $tx"
    )
    .map(_.toHash)
    .map(Address.fromPublicKeyHash)

  def updateStateWithTokenTx[F[_]: Monad](
      ms: MerkleState,
      sig: AccountSignature,
      tx: Transaction.Token,
      txHash: Signed.TxHash,
  )(implicit
      namesStateRepo: StateRepository[F, Account.Name, NameState],
      tokenStateRepo: StateRepository[F, DefinitionId, TokenState],
      balanceStateRepo: StateRepository[
        F,
        (Account, Transaction.Input.Tx),
        Unit,
      ],
      txRepo: TransactionRepository[F],
  ): EitherT[F, String, MerkleState] = tx match {
    case DefineToken(
          networkId,
          _,
          definitionId,
          name,
          symbol,
          divisionSize,
          data,
        ) =>
      for {
        tokenStateOption <- MerkleTrie
          .get[F, DefinitionId, TokenState](definitionId.bits)
          .runA(ms.tokenState)
        _ <- EitherT.cond[F](
          tokenStateOption.isEmpty,
          (),
          s"Token with def id $definitionId is already defined.",
        )
        _ <- EitherT.cond[F](
          divisionSize.value.isValidInt,
          (),
          s"Too big division size: $divisionSize",
        )
        account <- getAccountFromTxSig[F](tx, sig, ms.namesState)
        ts1 <- MerkleTrie
          .put[F, DefinitionId, TokenState](
            definitionId.bits,
            TokenState(
              networkId = networkId,
              definitionId = definitionId,
              name = name,
              symbol = symbol,
              divisionSize = divisionSize,
              data = data,
              admin = Some(account),
              totalAmount = BigNat.Zero,
              divisionAmount =
                Vector.fill(divisionSize.value.toInt)(BigNat.Zero),
            ),
          )
          .runS(ms.tokenState)
      } yield ms.copy(tokenState = ts1)
    case TransferAdmin(_, _, definitionId, output) =>
      for {
        tsOption <- MerkleTrie
          .get[F, DefinitionId, TokenState](definitionId.bits)
          .runA(ms.tokenState)
        ts <- EitherT
          .fromOption[F](tsOption, s"There is no token with id $definitionId")
        expectedAdminAccount <- EitherT
          .fromOption[F](ts.admin, s"There is no admin in token $definitionId")
        _ <- verifySignatureF[F](tx, sig, expectedAdminAccount, ms.namesState)
        ts1 <- MerkleTrie
          .put[F, DefinitionId, TokenState](
            definitionId.bits,
            ts.copy(admin = output),
          )
          .runS(ms.tokenState)
      } yield ms.copy(tokenState = ts1)
    case MintToken(_, _, definitionId, _, divisionIndex, outputs) =>
      scribe.debug(s"Minting token with definition id $definitionId: $outputs")
      for {
        tsOption <- MerkleTrie
          .get[F, DefinitionId, TokenState](definitionId.bits)
          .runA(ms.tokenState)
        ts <- EitherT
          .fromOption[F](tsOption, s"There is no token with id $definitionId")
        expectedAdminAccount <- EitherT
          .fromOption[F](ts.admin, s"There is no admin in token $definitionId")
        _ <- verifySignatureF[F](tx, sig, expectedAdminAccount, ms.namesState)
        outputList = outputs.toList
        bs1 <- outputList
          .traverse { case (account, _) =>
            MerkleTrie.put[F, (Account, Transaction.Input.Tx), Unit](
              (
                account,
                Transaction.Input.Tx(
                  txHash = txHash,
                  divisionIndex = divisionIndex,
                ),
              ).toBytes.bits,
              (),
            )
          }
          .runS(ms.balanceState)
        newTs = divisionIndex.fold {
          ts.copy(totalAmount =
            outputList.map(_._2).foldLeft(ts.totalAmount)(BigNat.add)
          )
        } { (index: BigNat) =>
          ts.copy(
            divisionAmount = ts.divisionAmount.updated(
              index.value.toInt,
              outputList
                .map(_._2)
                .foldLeft(
                  ts.divisionAmount(index.value.toInt)
                )(BigNat.add),
            )
          )
        }
        _ <- EitherT.pure[F, String] {
          scribe.debug(s"old token state: $ts")
          scribe.debug(s"new token state: $newTs")
        }
        ts1 <- {
          val program = for {
            _ <- MerkleTrie.removeByKey[F, DefinitionId, TokenState](
              definitionId.bits
            )
            _ <- MerkleTrie.put[F, DefinitionId, TokenState](
              definitionId.bits,
              newTs,
            )
          } yield ()
          program.runS(ms.tokenState)
        }
        _ <- EitherT.pure[F, String] {
          scribe.debug(s"ts1: $ts1")
        }
      } yield ms.copy(tokenState = ts1, balanceState = bs1)
    case TransferToken(
          _,
          _,
          definitionId,
          tokenId,
          divisionIndex,
          inputTxs,
          outputs,
        ) =>
      val inputTxHashList = inputTxs.toList
      val inputTxList: List[Transaction.Input.Tx] = inputTxHashList.map {
        inputTxHash =>
          Transaction.Input.Tx(
            txHash = inputTxHash,
            divisionIndex = divisionIndex,
          )
      }
      for {
        account <- getAccountFromTxSig[F](tx, sig, ms.namesState)
        existanceList <- inputTxList.traverse { inputTx =>
          scribe.debug(s"Checking account $account balance: $inputTx")
          MerkleTrie
            .get[F, (Account, Transaction.Input.Tx), Unit](
              (account, inputTx).toBytes.bits
            )
            .runA(ms.balanceState)
        }
        _ <- EitherT.cond[F](
          existanceList.forall(_.nonEmpty),
          (),
          s"Account $account does not have these txs: ${inputTxList.zip(existanceList).filter(_._2.nonEmpty).map(_._1)}",
        )
        transactionOptionList <- inputTxHashList.traverse(
          txRepo.get(_).leftMap(_.msg)
        )
        transactionList <- EitherT.fromEither[F](
          (inputTxList `zip` transactionOptionList).traverse {
            case (inputTx, Some(tx)) => Right((inputTx, tx.value))
            case (inputTx, None) =>
              Left(s"Transaction $inputTx does not exist.")
          }
        )
        definitionIdTokenIdAndAmountList <- EitherT.fromEither[F](
          transactionList.traverse { case (inputTx, tx) =>
            implicit val eq: cats.Eq[Option[datatype.BigNat]] =
              cats.Eq.fromUniversalEquals
            tx match {
              case tx0: MintToken =>
                Either.cond(
                  divisionIndex === tx0.divisionIndex,
                  (
                    inputTx,
                    tx0.definitionId,
                    tx0.tokenId,
                    tx0.outputs.get(account),
                  ),
                  s"Expected division index ${divisionIndex} but found ${tx0.divisionIndex} in transaction $inputTx",
                )
              case tx0: TransferToken =>
                Either.cond(
                  divisionIndex === tx0.divisionIndex,
                  (
                    inputTx,
                    tx0.definitionId,
                    tx0.tokenId,
                    tx0.outputs.get(account),
                  ),
                  s"Expected division index $divisionIndex but found ${tx0.divisionIndex} in transaction $inputTx",
                )
              case tx0: DivideToken =>
                Right(
                  (
                    inputTx,
                    tx0.definitionId,
                    tx0.tokenId,
                    Some(
                      divisionIndex.fold(tx0.remainder)(_ => tx0.divisionAmount)
                    ),
                  )
                )
              case tx0: CombineDivision =>
                Right {
                  (
                    inputTx,
                    tx0.definitionId,
                    tx0.tokenId,
                    Some(
                      divisionIndex.fold(tx0.amount)(i =>
                        tx0.divisionRemainder(i.value.toInt)
                      )
                    ),
                  )
                }
              case _ =>
                Left(
                  s"Transaction $inputTx is not an unused transaction output."
                )
            }
          }
        )
        checkDefIdAndTokenIdList = definitionIdTokenIdAndAmountList.map {
          case (txHash, defId0, tokenId0, _) =>
            (txHash, definitionId === defId0, tokenId === tokenId0)
        }
        _ <- EitherT.cond[F](
          checkDefIdAndTokenIdList.forall(_._2),
          (),
          s"Wrong definition id in txInput ${checkDefIdAndTokenIdList.filter(!_._2).map(_._1)}",
        )
        _ <- EitherT.cond[F](
          checkDefIdAndTokenIdList.forall(_._3),
          (),
          s"Wrong token id in txInput ${checkDefIdAndTokenIdList.filter(!_._3).map(_._1)}",
        )
        inputAmountSum = definitionIdTokenIdAndAmountList
          .flatMap(_._4.toList)
          .map(_.value)
          .sum
        _ <- EitherT.cond[F](
          inputAmountSum >= outputs.values.map(_.value).sum,
          (),
          s"Output amount is bigger than input amount: $inputAmountSum",
        )
        bs1 <- (for {
          _ <- inputTxList.traverse { inputTx =>
            MerkleTrie.removeByKey[F, (Account, Transaction.Input.Tx), Unit](
              (account, inputTx).toBytes.bits
            )
          }
          _ <- outputs.toList.traverse { case (account, amount @ _) =>
            MerkleTrie.put[F, (Account, Transaction.Input.Tx), Unit](
              (
                account,
                Transaction.Input.Tx(txHash, divisionIndex),
              ).toBytes.bits,
              (),
            )
          }
        } yield ()).runS(ms.balanceState)
        // TODO update TokenState when output amount is less than input amount
      } yield ms.copy(balanceState = bs1)
    case CombineDivision(
          _,
          _,
          definitionId,
          tokenId,
          inputTxs,
          amount,
          divisionRemainder,
        ) =>
      val inputTxList = inputTxs.toList
      val (txHashList, txDivisionIndexList) = inputTxList
        .map(inputTx => (inputTx.txHash, inputTx.divisionIndex))
        .unzip
      for {
        tokenStateOption <- MerkleTrie
          .get[F, DefinitionId, TokenState](definitionId.bits)
          .runA(ms.tokenState)
        tokenState <- EitherT.fromOption[F](
          tokenStateOption,
          s"Empty token state with id: $definitionId",
        )
        expectedDivisionSet = (BigInt(
          0
        ) until tokenState.divisionSize.value).toSet
        _ <- EitherT.cond[F](
          txDivisionIndexList
            .flatMap(_.toList.map(_.value))
            .toSet === expectedDivisionSet,
          (),
          s"input is not completed to combine: $inputTxs",
        )
        account <- getAccountFromTxSig[F](tx, sig, ms.namesState)
        existanceList <- txHashList.traverse { inputTx =>
          MerkleTrie
            .get[F, (Account, Transaction.Input.Tx), Unit](
              (account, inputTx).toBytes.bits
            )
            .runA(ms.balanceState)
        }
        _ <- EitherT.cond[F](
          existanceList.forall(_.nonEmpty),
          (),
          s"Account $account does not have these txs: ${txHashList.zip(existanceList).filter(_._2.nonEmpty).map(_._1)}",
        )
        transactionOptionList <- txHashList.traverse(
          txRepo.get(_).leftMap(_.msg)
        )
        transactionList <- EitherT.fromEither[F](
          (inputTxList `zip` transactionOptionList).traverse {
            case (inputTx, Some(tx)) => Right((inputTx, tx.value))
            case (inputTx, None) =>
              Left(s"Transaction ${inputTx.txHash} does not exist.")
          }
        )
        definitionIdTokenIdAndAmountList <- EitherT.fromEither[F](
          transactionList.traverse { case (inputTx, tx) =>
            implicit val eq: cats.Eq[Option[datatype.BigNat]] =
              cats.Eq.fromUniversalEquals
            tx match {
              case tx0: MintToken =>
                Either.cond(
                  inputTx.divisionIndex === tx0.divisionIndex,
                  (
                    inputTx.txHash,
                    tx0.definitionId,
                    tx0.tokenId,
                    inputTx.divisionIndex -> tx0.outputs.get(account),
                  ),
                  s"Expected division index ${inputTx.divisionIndex} but found ${tx0.divisionIndex} in transaction ${inputTx.txHash}",
                )
              case tx0: TransferToken =>
                Either.cond(
                  inputTx.divisionIndex === tx0.divisionIndex,
                  (
                    inputTx.txHash,
                    tx0.definitionId,
                    tx0.tokenId,
                    inputTx.divisionIndex -> tx0.outputs.get(account),
                  ),
                  s"Expected division index ${inputTx.divisionIndex} but found ${tx0.divisionIndex} in transaction ${inputTx.txHash}",
                )
              case tx0: DivideToken =>
                Right(
                  (
                    inputTx.txHash,
                    tx0.definitionId,
                    tx0.tokenId,
                    inputTx.divisionIndex -> Some(tx0.divisionAmount),
                  )
                )
              case tx0: CombineDivision =>
                Right(
                  (
                    inputTx.txHash,
                    tx0.definitionId,
                    tx0.tokenId,
                    inputTx.divisionIndex -> inputTx.divisionIndex.flatMap(i =>
                      tx0.divisionRemainder.get(i.value.toLong)
                    ),
                  )
                )
              case _ =>
                Left(
                  s"Transaction ${inputTx.txHash} is not an unused transaction output."
                )
            }
          }
        )
        checkDefIdAndTokenIdList = definitionIdTokenIdAndAmountList.map {
          case (txHash, defId0, tokenId0, amountOption @ _) =>
            (txHash, definitionId === defId0, tokenId === tokenId0)
        }
        _ <- EitherT.cond[F](
          checkDefIdAndTokenIdList.forall(_._2),
          (),
          s"Wrong definition id in txInput ${checkDefIdAndTokenIdList.filter(!_._2).map(_._1)}",
        )
        _ <- EitherT.cond[F](
          checkDefIdAndTokenIdList.forall(_._3),
          (),
          s"Wrong token id in txInput ${checkDefIdAndTokenIdList.filter(!_._3).map(_._1)}",
        )
        clearingList <- EitherT.fromEither[F] {
          definitionIdTokenIdAndAmountList.map { x => (x._1, x._4) }.traverse {
            case (txHash, (Some(divisionIndex), Some(inputAmount))) =>
              val outputAmount =
                divisionRemainder.apply(divisionIndex.value.toInt)
              val loss = inputAmount.value - (amount.value + outputAmount.value)
              Right((txHash, loss))
            case (txHash, (None, _)) => Left(s"Not a division input: $txHash")
            case (txHash, (Some(divisionIndex), None)) =>
              Left(s"Empty input: (division $divisionIndex) $txHash")
          }
        }
        newTotalAmount <- EitherT.fromEither[F](
          BigNat.fromBigInt(clearingList.map(_._2).sum).leftMap { _ =>
            s"input is less than output: ${clearingList.filter(_._2 < 0)}"
          }
        )
        bs <- (for {
          _ <- inputTxList.traverse { inputTx =>
            MerkleTrie.removeByKey[F, (Account, Transaction.Input.Tx), Unit](
              (account, inputTx).toBytes.bits
            )
          }
          _ <- inputTxList.traverse { inputTx =>
            MerkleTrie.put[F, (Account, Transaction.Input.Tx), Unit](
              (
                account,
                Transaction.Input.Tx(txHash, inputTx.divisionIndex),
              ).toBytes.bits,
              (),
            )
          }
          _ <- MerkleTrie.put[F, (Account, Transaction.Input.Tx), Unit](
            (
              account,
              Transaction.Input.Tx(
                txHash = txHash,
                divisionIndex = None,
              ),
            ).toBytes.bits,
            (),
          )
        } yield ()).runS(ms.balanceState)
        newTokenState = tokenState.copy(
          totalAmount = newTotalAmount,
          divisionAmount = divisionRemainder,
        )
        ts <- (for {
          _ <- MerkleTrie.removeByKey[F, DefinitionId, TokenState](
            definitionId.bits
          )
          _ <- MerkleTrie.put[F, DefinitionId, TokenState](
            definitionId.bits,
            newTokenState,
          )
        } yield ()).runS(ms.tokenState)
      } yield ms.copy(balanceState = bs, tokenState = ts)
    case DivideToken(
          networkId @ _,
          createdAt @ _,
          definitionId,
          tokenId @ _,
          inputTx,
          divisionAmount @ _,
          remainder @ _,
        ) =>
      implicit val eqBigNat: cats.Eq[BigNat] = cats.Eq.fromUniversalEquals
      for {
        account <- getAccountFromTxSig[F](tx, sig, ms.namesState)
        balanceOption <- MerkleTrie
          .get[F, (Account, Transaction.Input.Tx), Unit](
            (account, Transaction.Input.Tx(inputTx, None)).toBytes.bits
          )
          .runA(ms.balanceState)
        _ <- EitherT.fromOption[F](
          balanceOption,
          s"Account $account does not have utxo $inputTx",
        )
        transactionOption <- txRepo.get(inputTx).leftMap(_.msg)
        transaction <- EitherT.fromOption[F](
          transactionOption,
          s"Transaction $inputTx does not exist",
        )
        (divisionIndexOption, inputAmountOption) <- EitherT.fromEither[F](
          transaction.value match {
            case tx0: MintToken =>
              Right((tx0.divisionIndex, tx0.outputs.get(account)))
            case tx0: TransferToken =>
              Right((tx0.divisionIndex, tx0.outputs.get(account)))
            case tx0: CombineDivision => Right((None, Some(tx0.amount)))
            case tx0: DivideToken     => Right((None, Some(tx0.remainder)))
            case _ =>
              Left(s"Not a utxo balance input $inputTx: ${transaction.value}")
          }
        )
        _ <- EitherT.cond[F](
          divisionIndexOption.nonEmpty,
          (),
          s"Try to divide already divided token: $inputTx => ${transaction.value}",
        )
        inputAmount = inputAmountOption `getOrElse` BigNat.Zero
        clearingAmount =
          inputAmount.value - (divisionAmount.value + remainder.value)
        clearingAmountNat <- EitherT.fromEither[F](
          BigNat.fromBigInt(clearingAmount).leftMap { _ =>
            s"Input is smaller than output in $tx"
          }
        )
        tokenStateOption <- MerkleTrie
          .get[F, DefinitionId, TokenState](definitionId.bits)
          .runA(ms.tokenState)
        tokenState <- EitherT.fromOption[F](
          tokenStateOption,
          s"Empty token state with id: $definitionId",
        )
        bs <- (for {
          _ <- MerkleTrie.removeByKey[F, (Account, Transaction.Input.Tx), Unit](
            (account, Transaction.Input.Tx(inputTx, None)).toBytes.bits
          )
          _ <- (BigInt(0) until tokenState.divisionSize.value).toList.traverse {
            index =>
              val indexBigNat: BigNat = BigNat.unsafeFromBigInt(index)
              MerkleTrie.put[F, (Account, Transaction.Input.Tx), Unit](
                (
                  account,
                  Transaction.Input.Tx(txHash, Some(indexBigNat)),
                ).toBytes.bits,
                (),
              )
          }
          _ <- MerkleTrie.put[F, (Account, Transaction.Input.Tx), Unit](
            (account, Transaction.Input.Tx(txHash, None)).toBytes.bits,
            (),
          )
        } yield ()).runS(ms.balanceState)
        tsOption <-
          if (clearingAmountNat === BigNat.Zero) EitherT.pure[F, String](None)
          else
            (for {
              newTotalAmount <- EitherT.fromEither[F](
                BigNat.fromBigInt(tokenState.totalAmount.value - clearingAmount)
              )
              newDivisionAmount = tokenState.divisionAmount.map { amount =>
                BigNat.add(amount, divisionAmount)
              }
              newTokenState = tokenState.copy(
                totalAmount = newTotalAmount,
                divisionAmount = newDivisionAmount,
              )
              ts <- (for {
                _ <- MerkleTrie.removeByKey[F, DefinitionId, TokenState](
                  definitionId.bits
                )
                _ <- MerkleTrie.put[F, DefinitionId, TokenState](
                  definitionId.bits,
                  newTokenState,
                )
              } yield ()).runS(ms.tokenState)
            } yield Some(ts))
      } yield tsOption.fold(ms.copy(balanceState = bs))(ts =>
        ms.copy(balanceState = bs, tokenState = ts)
      )
  }

  def verifySignatureF[F[_]: Monad](
      tx: Transaction,
      asig: AccountSignature,
      expectedAccount: Account,
      namesRoot: MerkleTrieState[Account.Name, NameState],
  )(implicit
      namesStateRepo: StateRepository[F, Account.Name, NameState]
  ): EitherT[F, String, Unit] = asig match {
    case NamedSignature(name, sig) =>
      for {
        _ <- EitherT.fromEither[F](expectedAccount match {
          case Unnamed(address) =>
            Left(s"Expected unnamed $address, but received $asig")
          case Named(name1) =>
            Either.cond(
              name === name1,
              (),
              s"Expected name $name1, but received name $name",
            )
        })
        nameStateOption <- MerkleTrie
          .get[F, Account.Name, NameState](name.bytes.bits)
          .runA(namesRoot)
        nameState <- EitherT
          .fromOption[F](nameStateOption, s"No name with $name is registered.")
        address <- EitherT.fromEither[F](recoverSignature(tx, sig))
        weight <- EitherT.fromOption[F](
          nameState.addressess.get(address),
          s"Account $name does not have address $address",
        )
        _ <- EitherT.cond[F](
          weight.value >= nameState.threshold.value,
          (),
          s"Weight $weight is less than the threshold ${nameState.threshold} in account name $name",
        )
      } yield ()
    case UnnamedSignature(sig) =>
      EitherT.fromEither[F](recoverSignature(tx, sig).flatMap { address =>
        expectedAccount match {
          case Named(name) =>
            Left(s"expected named account $name, but received $address")
          case Unnamed(expectedAddress) =>
            Either.cond(
              expectedAddress === address,
              (),
              s"Different address: expected $expectedAddress, but received $address",
            )
        }
      })
  }

  def getAccountFromTxSig[F[_]: Monad](
      tx: Transaction,
      asig: AccountSignature,
      namesRoot: MerkleTrieState[Account.Name, NameState],
  )(implicit
      namesStateRepo: StateRepository[F, Account.Name, NameState]
  ): EitherT[F, String, Account] = asig match {
    case NamedSignature(name, sig) =>
      for {
        nameStateOption <- MerkleTrie
          .get[F, Account.Name, NameState](name.bytes.bits)
          .runA(namesRoot)
        nameState <- EitherT
          .fromOption[F](nameStateOption, s"Name is not registered: $name")
        publicKey <- EitherT
          .fromOption[F](tx.recover(sig), s"Bad signature $asig from tx: $tx")
        address = Address.fromPublicKeyHash(publicKey.toHash)
        threshold <- EitherT.fromOption[F](
          nameState.addressess.get(address),
          s"Name $name does not have address $address",
        )
        _ <- EitherT.cond[F](
          threshold.value >= nameState.threshold.value,
          (),
          s"Address $address is less than thresgold value ${nameState.threshold}",
        )
      } yield Account.Named(name)
    case UnnamedSignature(sig) =>
      for {
        publicKey <- EitherT
          .fromOption[F](tx.recover(sig), s"Bad signature $asig from tx: $tx")
        address = Address.fromPublicKeyHash(publicKey.toHash)
      } yield Account.Unnamed(address)
  }

}
