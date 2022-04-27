package org.leisuremeta.lmchain.core
package model

import java.time.Instant

import eu.timepit.refined.numeric.NonNegative
import eu.timepit.refined.refineV
import org.scalacheck.{Arbitrary, Gen}
import scodec.bits.{BitVector, ByteVector}
import shapeless.nat._16
import shapeless.syntax.sized._

import crypto._
import datatype._
import Transaction.Token.{DefinitionId, TokenId}
import util.refined.bitVector._

trait ModelArbitrary {

  def bigNat(bigint: BigInt): BigNat = refineV[NonNegative](bigint).toOption.get

  implicit val arbitraryBigNat: Arbitrary[BigNat] = Arbitrary(for {
    bigint <- Arbitrary.arbitrary[BigInt]
  } yield bigNat(bigint.abs))

  implicit val arbitraryUInt256BigInt: Arbitrary[UInt256BigInt] = Arbitrary(
    for {
      bytes <- Gen.containerOfN[Array, Byte](32, Arbitrary.arbitrary[Byte])
    } yield UInt256Refine.from(BigInt(1, bytes)).toOption.get
  )

  implicit val arbitraryUInt256Bytes: Arbitrary[UInt256Bytes] = Arbitrary(for {
    bytes <- Gen.containerOfN[Array, Byte](32, Arbitrary.arbitrary[Byte])
  } yield UInt256Refine.from(ByteVector.view(bytes)).toOption.get)

  implicit val arbitraryUtf8: Arbitrary[Utf8] = Arbitrary(for {
    str <- Gen.asciiPrintableStr
  } yield Utf8.unsafeFrom(str))

  implicit def arbitraryList[A](implicit aa: Arbitrary[A]): Arbitrary[List[A]] =
    Arbitrary(Gen.sized { size =>
      Gen.containerOfN[List, A](size, aa.arbitrary)
    })

  implicit val arbitraryNetworkId: Arbitrary[NetworkId] = Arbitrary(for {
    bignat <- arbitraryBigNat.arbitrary
  } yield NetworkId(bignat))

  implicit val arbitraryPublicKey: Arbitrary[PublicKey] = Arbitrary(for {
    x <- arbitraryUInt256BigInt.arbitrary
    y <- arbitraryUInt256BigInt.arbitrary
  } yield PublicKey(x, y))

  implicit val arbitraryKeyPair: Arbitrary[KeyPair] = Arbitrary(for {
    privateKey <- arbitraryUInt256BigInt.arbitrary
    publicKey  <- arbitraryPublicKey.arbitrary
  } yield KeyPair(privateKey, publicKey))

  implicit val arbitrarySignature: Arbitrary[Signature] = Arbitrary(for {
    v <- Gen.choose(27, 34)
    r <- arbitraryUInt256BigInt.arbitrary
    s <- arbitraryUInt256BigInt.arbitrary
  } yield Signature(refineV[Signature.HeaderRange](v).toOption.get, r, s))

  @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
  implicit def arbitraryMerkleTrieNode[K, V]: Arbitrary[MerkleTrieNode[K, V]] =
    Arbitrary(
      for {
        isLeaf     <- Gen.oneOf(true, false)
        nibbleSize <- Gen.choose(0, 127)
        prefixBytes <- Gen.listOfN(
          (nibbleSize + 1) / 2,
          Arbitrary.arbitrary[Byte],
        )
        prefix = refineV[MerkleTrieNode.PrefixCondition] {
          BitVector.view(prefixBytes.toArray).take(nibbleSize * 4L)
        }.toOption.get
        node <-
          if (isLeaf) {
            arbitraryList[Byte].arbitrary.map { byteList =>
              MerkleTrieNode
                .Leaf[K, V](prefix, ByteVector.view(byteList.toArray))
            }
          } else {
            Gen
              .containerOfN[Vector, Option[MerkleTrieNode.MerkleHash[K, V]]](
                16,
                Gen.option(arbitraryHashValue.arbitrary),
              )
              .map { unsizedChildren =>
                MerkleTrieNode
                  .branch[K, V](prefix, unsizedChildren.sized(_16).get)
              }
          }
      } yield node
    )

  implicit def arbitraryHashValue[A]: Arbitrary[Hash.Value[A]] = Arbitrary(
    arbitraryUInt256Bytes.arbitrary.map[Hash.Value[A]](
      shapeless.tag[A][UInt256Bytes](_)
    )
  )

  implicit def arbitraryMerkleRoot[K, V]
      : Arbitrary[MerkleTrieNode.MerkleRoot[K, V]] =
    arbitraryHashValue[MerkleTrieNode[K, V]]

  implicit val arbitraryTxHash: Arbitrary[Signed.TxHash] =
    arbitraryHashValue[Signed.Tx]

  implicit val arbitraryTxInputTx: Arbitrary[Transaction.Input.Tx] = Arbitrary(
    for {
      txHash        <- arbitraryTxHash.arbitrary
      divisionIndex <- Gen.option(arbitraryBigNat.arbitrary)
    } yield Transaction.Input.Tx(txHash, divisionIndex)
  )

  implicit val arbitraryName: Arbitrary[Account.Name] = Arbitrary(for {
    size     <- Gen.choose(6, 12)
    charList <- Gen.containerOfN[List, Char](size, Arbitrary.arbChar.arbitrary)
    string = charList.mkString
    bytes  = string.getBytes("UTF-8")
  } yield new Account.Name(ByteVector.view(bytes)))

  implicit val arbitraryAddress: Arbitrary[Address] = Arbitrary(for {
    bytes <- Gen.containerOfN[Array, Byte](20, Arbitrary.arbitrary[Byte])
  } yield Address(ByteVector.view(bytes)).toOption.get)

  implicit val arbitraryAccount: Arbitrary[Account] = Arbitrary(
    Gen.frequency(
      (1, arbitraryName.arbitrary.map(Account.Named(_))),
      (9, arbitraryAddress.arbitrary.map(Account.Unnamed(_))),
    )
  )

  implicit val arbitraryAccountNamedSig
      : Arbitrary[AccountSignature.NamedSignature] = Arbitrary(for {
    name <- arbitraryName.arbitrary
    sig  <- arbitrarySignature.arbitrary
  } yield AccountSignature.NamedSignature(name, sig))

  implicit val arbitraryAccountUnnamedSig
      : Arbitrary[AccountSignature.UnnamedSignature] = Arbitrary(
    arbitrarySignature.arbitrary.map(AccountSignature.UnnamedSignature(_))
  )

  implicit val arbitraryAccountSignature: Arbitrary[AccountSignature] =
    Arbitrary(
      Gen.frequency(
        (1, Arbitrary.arbitrary[AccountSignature.NamedSignature]),
        (9, Arbitrary.arbitrary[AccountSignature.UnnamedSignature]),
      )
    )

  implicit def arbitrarySigned[A: Arbitrary]: Arbitrary[Signed[A]] = Arbitrary(
    for {
      sig <- arbitraryAccountSignature.arbitrary
      a   <- Arbitrary.arbitrary[A]
    } yield Signed(sig, a)
  )

  implicit val arbitraryNameState: Arbitrary[NameState] = Arbitrary(
    for {
      addressessSize <- Gen.choose(1, 3)
      addressessSeq <- Gen.containerOfN[Seq, (Address, BigNat)](
        addressessSize,
        Gen.zip(
          arbitraryAddress.arbitrary,
          Gen.choose(1, 3).map(BigInt(_)).map(bigNat),
        ),
      )
      thresholdInt <- Gen.choose(
        1,
        addressessSeq.unzip._2.map(_.value).sum.toInt,
      )
      guardian <- Gen.option(arbitraryAccount.arbitrary)
    } yield NameState(
      addressessSeq.toMap,
      bigNat(BigInt(thresholdInt)),
      guardian,
    )
  )

  implicit val arbitraryTokenState: Arbitrary[TokenState] = Arbitrary(
    for {
      networkId    <- arbitraryNetworkId.arbitrary
      definitionId <- arbitraryUInt256Bytes.arbitrary
      name         <- arbitraryUtf8.arbitrary
      symbol       <- arbitraryUtf8.arbitrary
      divisionSize <- Gen.choose(1, 100)
      dataSize     <- Gen.choose(1, 10)
      bytes <- Gen
        .containerOfN[Array, Byte](dataSize, Arbitrary.arbitrary[Byte])
      data = ByteVector.view(bytes)
      admin       <- Gen.option(arbitraryAccount.arbitrary)
      totalAmount <- arbitraryBigNat.arbitrary
      divisionAmount <- Gen.containerOfN[Vector, BigNat](
        divisionSize,
        arbitraryBigNat.arbitrary,
      )
    } yield TokenState(
      networkId,
      DefinitionId(definitionId),
      name,
      symbol,
      bigNat(divisionSize),
      data,
      admin,
      totalAmount,
      divisionAmount,
    )
  )

  implicit val arbitraryTxCreateName: Arbitrary[Transaction.Name.CreateName] =
    Arbitrary(for {
      networkId <- arbitraryNetworkId.arbitrary
      createdAt <- arbInstant.arbitrary
      name      <- arbitraryName.arbitrary
      state     <- arbitraryNameState.arbitrary
    } yield Transaction.Name.CreateName(networkId, createdAt, name, state))

  implicit val arbitraryTxUpdateName: Arbitrary[Transaction.Name.UpdateName] =
    Arbitrary(for {
      networkId <- arbitraryNetworkId.arbitrary
      createdAt <- arbInstant.arbitrary
      name      <- arbitraryName.arbitrary
      state     <- arbitraryNameState.arbitrary
    } yield Transaction.Name.UpdateName(networkId, createdAt, name, state))

  implicit val arbitraryTxDeleteName: Arbitrary[Transaction.Name.DeleteName] =
    Arbitrary(for {
      networkId <- arbitraryNetworkId.arbitrary
      createdAt <- arbInstant.arbitrary
      name      <- arbitraryName.arbitrary
    } yield Transaction.Name.DeleteName(networkId, createdAt, name))

  implicit val arbitraryTxDefineToken
      : Arbitrary[Transaction.Token.DefineToken] = Arbitrary(
    for {
      networkId    <- arbitraryNetworkId.arbitrary
      createdAt    <- arbInstant.arbitrary
      definitionId <- arbitraryUInt256Bytes.arbitrary
      name         <- arbitraryUtf8.arbitrary
      symbol       <- arbitraryUtf8.arbitrary
      divisionSize <- arbitraryBigNat.arbitrary
      dataSize     <- Gen.choose(1, 10)
      bytes <- Gen
        .containerOfN[Array, Byte](dataSize, Arbitrary.arbitrary[Byte])
      data = ByteVector.view(bytes)
    } yield Transaction.Token.DefineToken(
      networkId,
      createdAt,
      DefinitionId(definitionId),
      name,
      symbol,
      divisionSize,
      data,
    )
  )

  implicit val arbitraryTxTransferAdmin
      : Arbitrary[Transaction.Token.TransferAdmin] = Arbitrary(
    for {
      networkId    <- arbitraryNetworkId.arbitrary
      createdAt    <- arbInstant.arbitrary
      definitionId <- arbitraryUInt256Bytes.arbitrary
      output       <- Gen.option(arbitraryAccount.arbitrary)
    } yield Transaction.Token.TransferAdmin(
      networkId,
      createdAt,
      DefinitionId(definitionId),
      output,
    )
  )

  implicit val arbitraryOutputEntry: Arbitrary[(Account, BigNat)] = Arbitrary(
    for {
      account <- arbitraryAccount.arbitrary
      amount  <- arbitraryBigNat.arbitrary
    } yield (account, amount)
  )

  implicit val arbitraryTxMintToken: Arbitrary[Transaction.Token.MintToken] =
    Arbitrary(
      for {
        networkId    <- arbitraryNetworkId.arbitrary
        createdAt    <- arbInstant.arbitrary
        definitionId <- arbitraryUInt256Bytes.arbitrary
        tokenId      <- arbitraryUInt256Bytes.arbitrary
        divisionIndex <- Gen.option(
          Gen.choose(1, 10).map(BigInt(_)).map(bigNat)
        )
        outputSize <- Gen.choose(1, 5)
        outputs    <- Gen.listOfN(outputSize, arbitraryOutputEntry.arbitrary)
      } yield Transaction.Token
        .MintToken(
          networkId,
          createdAt,
          DefinitionId(definitionId),
          TokenId(tokenId),
          divisionIndex,
          outputs.toMap,
        )
    )

  implicit val arbitraryTxTransferToken
      : Arbitrary[Transaction.Token.TransferToken] =
    Arbitrary(
      for {
        networkId    <- arbitraryNetworkId.arbitrary
        createdAt    <- arbInstant.arbitrary
        definitionId <- arbitraryUInt256Bytes.arbitrary
        tokenId      <- arbitraryUInt256Bytes.arbitrary
        divisionIndex <- Gen.option(
          Gen.choose(1, 10).map(BigInt(_)).map(bigNat)
        )
        inputSize <- Gen.choose(1, 5)
        inputs <- Gen.containerOfN[Set, Signed.TxHash](
          inputSize,
          arbitraryTxHash.arbitrary,
        )
        outputSize <- Gen.choose(1, 5)
        outputs    <- Gen.listOfN(outputSize, arbitraryOutputEntry.arbitrary)
      } yield Transaction.Token.TransferToken(
        networkId,
        createdAt,
        DefinitionId(definitionId),
        TokenId(tokenId),
        divisionIndex,
        inputs,
        outputs.toMap,
      )
    )

  implicit val arbitraryTxCombineDivision
      : Arbitrary[Transaction.Token.CombineDivision] = Arbitrary(
    for {
      networkId    <- arbitraryNetworkId.arbitrary
      createdAt    <- arbInstant.arbitrary
      definitionId <- arbitraryUInt256Bytes.arbitrary
      tokenId      <- arbitraryUInt256Bytes.arbitrary
      inputSize    <- Gen.choose(1, 10)
      inputs <- Gen.containerOfN[Set, Transaction.Input.Tx](
        inputSize,
        arbitraryTxInputTx.arbitrary,
      )
      amount <- arbitraryBigNat.arbitrary
      divisionRemainder <- Gen.containerOfN[Vector, BigNat](
        inputSize,
        arbitraryBigNat.arbitrary,
      )
    } yield Transaction.Token
      .CombineDivision(
        networkId,
        createdAt,
        DefinitionId(definitionId),
        TokenId(tokenId),
        inputs,
        amount,
        divisionRemainder,
      )
  )

  implicit val arbitraryTxDivideToken
      : Arbitrary[Transaction.Token.DivideToken] = Arbitrary(
    for {
      networkId      <- arbitraryNetworkId.arbitrary
      createdAt      <- arbInstant.arbitrary
      definitionId   <- arbitraryUInt256Bytes.arbitrary
      tokenId        <- arbitraryUInt256Bytes.arbitrary
      input          <- arbitraryTxHash.arbitrary
      divisionAmount <- arbitraryBigNat.arbitrary
      remainder      <- arbitraryBigNat.arbitrary
    } yield Transaction.Token
      .DivideToken(
        networkId,
        createdAt,
        DefinitionId(definitionId),
        TokenId(tokenId),
        input,
        divisionAmount,
        remainder,
      )
  )

  implicit val arbitraryTransaction: Arbitrary[Transaction] = Arbitrary(
    Gen.frequency(
      (1, arbitraryTxCreateName.arbitrary),
      (1, arbitraryTxUpdateName.arbitrary),
      (1, arbitraryTxDeleteName.arbitrary),
      (1, arbitraryTxDefineToken.arbitrary),
      (1, arbitraryTxTransferAdmin.arbitrary),
      (1, arbitraryTxMintToken.arbitrary),
      (1, arbitraryTxTransferToken.arbitrary),
      (1, arbitraryTxCombineDivision.arbitrary),
      (1, arbitraryTxDivideToken.arbitrary),
    )
  )

  implicit val arbInstant: Arbitrary[Instant] = Arbitrary(for {
    millis <- Gen.chooseNum(
      Instant.MIN.getEpochSecond,
      Instant.MAX.getEpochSecond,
    )
  } yield Instant.ofEpochMilli(millis))

  implicit val arbitraryBlock: Arbitrary[Block] = Arbitrary(for {
    number     <- arbitraryBigNat.arbitrary
    parentHash <- arbitraryHashValue[Block].arbitrary
    namesRoot <- Gen.option(
      arbitraryMerkleRoot[Account.Name, NameState].arbitrary
    )
    tokenRoot <- Gen.option(
      arbitraryMerkleRoot[DefinitionId, TokenState].arbitrary
    )
    balanceRoot <- Gen.option(
      arbitraryMerkleRoot[(Account, Transaction.Input.Tx), Unit].arbitrary
    )
    transactionsRoot <- Gen.option(
      arbitraryMerkleRoot[Signed.TxHash, Unit].arbitrary
    )
    timestamp <- arbInstant.arbitrary
    header = Block.Header(
      number,
      parentHash,
      namesRoot,
      tokenRoot,
      balanceRoot,
      transactionsRoot,
      timestamp,
    )
    transactionHashSize <- Gen.choose(1, 2)
    transactionHashes <- Gen.containerOfN[Set, Signed.TxHash](
      transactionHashSize,
      arbitraryTxHash.arbitrary,
    )
    voteSize <- Gen.choose(1, 2)
    votes <- Gen
      .containerOfN[Set, Signature](voteSize, arbitrarySignature.arbitrary)
  } yield Block(header, transactionHashes, votes))

  implicit val arbitraryNodeStatus: Arbitrary[NodeStatus] = Arbitrary(for {
    networkId   <- arbitraryNetworkId.arbitrary
    genesisHash <- arbitraryHashValue[Block].arbitrary
    bestHash    <- arbitraryHashValue[Block].arbitrary
    number      <- arbitraryBigNat.arbitrary
  } yield NodeStatus(networkId, genesisHash, bestHash, number))
}
