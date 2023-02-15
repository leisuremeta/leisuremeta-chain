package io.leisuremeta.chain
package lmscan.agent
package entity

import java.time.Instant
import io.leisuremeta.chain.api.model.Transaction.AccountTx.*
import io.leisuremeta.chain.api.model.Transaction.TokenTx.*
import io.leisuremeta.chain.api.model.Transaction.GroupTx.*
import io.leisuremeta.chain.api.model.Transaction.RewardTx.*
import model.Block0
import cats.effect.IO
import cats.effect.ExitCode
import cats.effect.IOApp


final case class TxEntity(
  hash: String,
  txType: String, // col_name : type
  tokenType: String,
  fromAddr: String,
  toAddr: Seq[String],
  blockHash: String,
  blockNumber: Long,
  eventTime: Long,
  createdAt: Long,
  inputHashs: Option[Seq[String]],
  outputVals: Option[Seq[String]],
  json: String,
) 

object TxEntity:
  def from(txHash: String, nft: DefineToken, block: Block0, blockHash: String, txJson: String, fromAccount: String): TxEntity =
    TxEntity(
      hash = txHash,
      txType = "Token",
      tokenType = nft.definitionId.utf8.value,
      fromAddr = fromAccount,
      toAddr = Seq(),
      blockHash = blockHash,
      blockNumber = block.header.number.toBigInt.longValue,
      eventTime = nft.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond,
      inputHashs = None,
      outputVals = None,
      json = txJson,
    )

  def from(txHash: String, nft: MintNFT, block: Block0, blockHash: String, txJson: String, fromAccount: String): TxEntity =
    TxEntity(
      txHash,
      "Token",
      "NFT",
      fromAccount,
      Seq(nft.output.utf8.value),
      blockHash,
      block.header.number.toBigInt.longValue,
      nft.createdAt.getEpochSecond(),
      Instant.now().getEpochSecond(),
      None,
      Some(Seq(nft.output.utf8.value+"/"+nft.tokenId.utf8.value)),
      txJson,
  )

  def from(txHash: String, nft: TransferNFT, block: Block0, blockHash: String, txJson: String, fromAccount: String): TxEntity =
    TxEntity(
      txHash,
      "Token",
      "NFT",
      fromAccount,
      Seq(nft.output.utf8.value),
      blockHash,
      block.header.number.toBigInt.longValue,
      nft.createdAt.getEpochSecond(),
      Instant.now().getEpochSecond,
      Some(Seq(nft.input.toUInt256Bytes.toHex)),
      Some(Seq(nft.output.utf8.value+"/"+nft.tokenId.utf8.value)),
      txJson,
    )

  def from(txHash: String, nft: BurnNFT, tokenId: String, block: Block0, blockHash: String, txJson: String, fromAccount: String): TxEntity =
    TxEntity(
      txHash,
      "Token",
      "NFT",
      fromAccount,
      Seq(),
      blockHash,
      block.header.number.toBigInt.longValue,
      nft.createdAt.getEpochSecond(),
      Instant.now().getEpochSecond,
      Some(Seq(nft.input.toUInt256Bytes.toHex)),
      Some(Seq("/"+tokenId)),
      txJson,
    )

  def from(txHash: String, nft: EntrustNFT, block: Block0, blockHash: String, txJson: String, fromAccount: String): TxEntity =
    TxEntity(
      hash = txHash,
      txType = "Token",
      tokenType = "NFT",
      fromAddr = fromAccount,
      toAddr = Seq(nft.to.utf8.value),
      blockHash = blockHash,
      blockNumber = block.header.number.toBigInt.longValue,
      eventTime = nft.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond,
      inputHashs = Some(Seq(nft.input.toUInt256Bytes.toHex)),
      outputVals = Some(Seq(nft.to.utf8.value+"/"+nft.tokenId.utf8.value)),
      json = txJson,
    )

  def from(txHash: String, nft: DisposeEntrustedNFT, block: Block0, blockHash: String, txJson: String, fromAccount: String): TxEntity =
    val toAccount = nft.output match 
        case Some(value) => value.utf8.value
        case None =>  ""

    val toAccountSeq = nft.output match 
        case Some(value) => Seq(value.utf8.value)
        case None =>  Seq()
    TxEntity(
      hash = txHash,
      txType = "Token",
      tokenType = "NFT",
      fromAddr = fromAccount,
      toAddr = toAccountSeq,
      blockHash = blockHash,
      blockNumber = block.header.number.toBigInt.longValue,
      eventTime = nft.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond,
      inputHashs = Some(Seq(nft.input.toUInt256Bytes.toHex)),
      outputVals = Some(Seq(toAccount+"/"+nft.tokenId.utf8.value)),
      json = txJson,
    )
  
  
  def from(txHash: String, tx: CreateAccount, block: Block0, blockHash: String, txJson: String, fromAccount: String): TxEntity =
    val toAccount = tx.account.utf8.value
    TxEntity(
      hash = txHash,
      txType = "Account",
      tokenType = "LM",
      fromAddr = fromAccount,
      toAddr = Seq(),
      blockHash = blockHash,
      blockNumber = block.header.number.toBigInt.longValue,
      eventTime = tx.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond,
      inputHashs = None,
      outputVals = Some(Seq(toAccount+"/"+0)),
      json = txJson,
    )

  def from(txHash: String, tx: UpdateAccount, block: Block0, blockHash: String, txJson: String, fromAccount: String): TxEntity =
    val toAccount = tx.account.utf8.value
    TxEntity(
      hash = txHash,
      txType = "Account",
      tokenType = "LM",
      fromAddr = fromAccount,
      toAddr = Seq(),
      blockHash = blockHash,
      blockNumber = block.header.number.toBigInt.longValue,
      eventTime = tx.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond,
      inputHashs = None,
      outputVals = None,
      json = txJson,
    )

  def from(txHash: String, tx: AddPublicKeySummaries, block: Block0, blockHash: String, txJson: String, fromAccount: String): TxEntity =
    val toAccount = tx.account.utf8.value
    TxEntity(
      hash = txHash,
      txType = "Account",
      tokenType = "LM",
      fromAddr = fromAccount,
      toAddr = Seq(),
      blockHash = blockHash,
      blockNumber = block.header.number.toBigInt.longValue,
      eventTime = tx.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond,
      inputHashs = None,
      outputVals = None,
      json = txJson,
    )

  def from(txHash: String, tx: CreateGroup, block: Block0, blockHash: String, txJson: String, fromAccount: String): TxEntity =

    TxEntity(
      hash = txHash,
      txType = "Group",
      tokenType = "LM",
      fromAddr = fromAccount,
      toAddr = Seq(),
      blockHash = blockHash,
      blockNumber = block.header.number.toBigInt.longValue,
      eventTime = tx.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond,
      inputHashs = None,
      outputVals = None,
      json = txJson,
    )

  def from(txHash: String, tx: AddAccounts, block: Block0, blockHash: String, txJson: String, fromAccount: String): TxEntity =
    TxEntity(
      hash = txHash,
      txType = "Group",
      tokenType = "LM",
      fromAddr = fromAccount,
      toAddr = Seq(),
      blockHash = blockHash,
      blockNumber = block.header.number.toBigInt.longValue,
      eventTime = tx.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond,
      inputHashs = None,
      outputVals = None,
      json = txJson,
    )
  
  def from(txHash: String, tx: RegisterDao, block: Block0, blockHash: String, txJson: String, fromAccount: String): TxEntity =
    TxEntity(
      hash = txHash,
      txType = "Reward",
      tokenType = "LM",
      fromAddr = fromAccount,
      toAddr = Seq(),
      blockHash = blockHash,
      blockNumber = block.header.number.toBigInt.longValue,
      eventTime = tx.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond,
      inputHashs = None,
      outputVals = None,
      json = txJson,
    )
   

  
  def from(txHash: String, tx: UpdateDao, block: Block0, blockHash: String, txJson: String, fromAccount: String): TxEntity =
    TxEntity(
      hash = txHash,
      txType = "Reward",
      tokenType = "LM",
      fromAddr = fromAccount,
      toAddr = Seq(),
      blockHash = blockHash,
      blockNumber = block.header.number.toBigInt.longValue,
      eventTime = tx.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond,
      inputHashs = None,
      outputVals = None,
      json = txJson,
    )

  def from(txHash: String, tx: RecordActivity, block: Block0, blockHash: String, txJson: String, fromAccount: String): TxEntity =
    TxEntity(
      hash = txHash,
      txType = "Reward",
      tokenType = "LM",
      fromAddr = fromAccount,
      toAddr = Seq(),
      blockHash = blockHash,
      blockNumber = block.header.number.toBigInt.longValue,
      eventTime = tx.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond,
      inputHashs = None,
      outputVals = None,
      json = txJson,
    )

  def from(txHash: String, tx: MintFungibleToken, block: Block0, blockHash: String, txJson: String, fromAccount: String): TxEntity =
    TxEntity(
      hash = txHash,
      txType = "Token",
      tokenType = "LM",
      fromAddr = fromAccount,
      toAddr = Seq(),
      blockHash = blockHash,
      blockNumber = block.header.number.toBigInt.longValue,
      eventTime = tx.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond,
      inputHashs = None,
      outputVals = None,
      json = txJson,
    )

  def from(txHash: String, tx: BurnFungibleToken, block: Block0, blockHash: String, txJson: String, fromAccount: String): TxEntity =
    TxEntity(
      hash = txHash,
      txType = "Token",
      tokenType = "LM",
      fromAddr = fromAccount,
      toAddr = Seq(fromAccount),
      blockHash = blockHash,
      blockNumber = block.header.number.toBigInt.longValue,
      eventTime = tx.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond,
      inputHashs = Some(tx.inputs.map(_.toUInt256Bytes.toBytes.toHex).toSeq),
      outputVals = Some(Seq(fromAccount + "/" + tx.amount.toBigInt.toString())),
      json = txJson,
    )

  def from(txHash: String, tx: TransferFungibleToken, block: Block0, blockHash: String, txJson: String, fromAccount: String): TxEntity =
    val toAccounts = tx.outputs.keySet.map(_.utf8.value).toSeq
    val outputVals: Seq[String] = tx.outputs.toList.map { case (account, amount) =>
      account.utf8.value + "/" + amount.toBigInt.toString()
    }
    TxEntity(
      hash = txHash,
      txType = "Token",
      tokenType = "LM",
      fromAddr = fromAccount,
      toAddr = toAccounts,
      blockHash = blockHash,
      blockNumber = block.header.number.toBigInt.longValue,
      eventTime = tx.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond,
      inputHashs = Some(tx.inputs.map(_.toUInt256Bytes.toBytes.toHex).toSeq),
      outputVals = Some(outputVals),
      json = txJson,
    )

  def from(txHash: String, tx: EntrustFungibleToken, block: Block0, blockHash: String, txJson: String, fromAccount: String): TxEntity =
    TxEntity(
      hash = txHash,
      txType = "Token",
      tokenType = "LM",
      fromAddr = fromAccount,
      toAddr = Seq(tx.to.utf8.value),
      blockHash = blockHash,
      blockNumber = block.header.number.toBigInt.longValue,
      eventTime = tx.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond,
      inputHashs = Some(tx.inputs.map(_.toUInt256Bytes.toBytes.toHex).toSeq),
      outputVals = Some(Seq(tx.to.utf8.value + "/" + tx.amount.toBigInt.toString())),
      json = txJson,
    )

  def from(txHash: String, tx: DisposeEntrustedFungibleToken, block: Block0, blockHash: String, txJson: String, fromAccount: String): TxEntity =
    val toAccounts = tx.outputs.keySet.map(_.utf8.value).toSeq
    val outputVals: Seq[String] = tx.outputs.toList.map { case (account, amount) =>
      account.utf8.value + "/" + amount.toBigInt.toString()
    }
    TxEntity(
      hash = txHash,
      txType = "Token",
      tokenType = "LM",
      fromAddr = fromAccount,
      toAddr = toAccounts,
      blockHash = blockHash,
      blockNumber = block.header.number.toBigInt.longValue,
      eventTime = tx.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond,
      inputHashs = Some(tx.inputs.map(_.toUInt256Bytes.toBytes.toHex).toSeq),
      outputVals = Some(outputVals),
      json = txJson,
    )

  

  def from(txHash: String, tx: OfferReward, block: Block0, blockHash: String, txJson: String, fromAccount: String): TxEntity =

    val toAccounts = tx.outputs.keySet.map(_.utf8.value).toSeq
    val outputVals: Seq[String] = tx.outputs.toList.map { case (account, amount) =>
      account.utf8.value + "/" + amount.toBigInt.toString()
    }
    TxEntity(
      hash = txHash,
      txType = "Token",
      tokenType = "LM",
      fromAddr = fromAccount,
      toAddr = toAccounts,
      blockHash = blockHash,
      blockNumber = block.header.number.toBigInt.longValue,
      eventTime = tx.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond,
      inputHashs = Some(tx.inputs.map(_.toUInt256Bytes.toBytes.toHex).toSeq),
      outputVals = Some(outputVals),
      json = txJson,
    )

  
  def from(txHash: String, tx: ExecuteReward, rewardRes: ExecuteRewardResult, block: Block0, blockHash: String, txJson: String, fromAccount: String): TxEntity =

    val toAccounts = rewardRes.outputs.keySet.map(_.utf8.value).toSeq
    val outputVals: Seq[String] = rewardRes.outputs.toList.map { case (account, amount) =>
      account.utf8.value + "/" + amount.toBigInt.toString()
    }
    TxEntity(
      hash = txHash,
      txType = "Token",
      tokenType = "LM",
      fromAddr = fromAccount,
      toAddr = toAccounts,
      blockHash = blockHash,
      blockNumber = block.header.number.toBigInt.longValue,
      eventTime = tx.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond,
      inputHashs = None,
      outputVals = Some(outputVals),
      json = txJson,
    )

  
  // override def run(args: List[String]): IO[ExitCode] = IO.blocking{
  //   import io.circe.generic.auto.*
  //   import io.circe.syntax.*
  //   import lib.crypto.Hash.ops.*

  //   val txHash0 = "cb030fab7185edca39d80921f37c0d7c2154f43a50acb9742cf7c569b7363a9e"
  //   val block0 = 	"""{"header":{"number":1090366,"parentHash":"410bf4665e5868d4cc5334014028821607eb3f110c8e317ee9ebc46bd7ee1384","stateRoot":{"account":{"namesRoot":"2bc663aaef6b35d57f2da83491e9789c9d9610a00e8f0e8df9fc29998a207236","keyRoot":"bad83767f0eb34cc6ddc83a08c9c3f6a784579d26d264bd194c6418f891c4afc","ethRoot":"0c8ebc2f3e7b529af624d0e5a19e1671f974c50d343cecfa77f4e8a65c7f0bf3"},"group":{"groupRoot":"70234eb0fbd923afcaa4dad81ba6d73242db95b4c5e52e6840b6a74a82c23343","groupAccountRoot":"47cc51b7c1438cfdc0d40b2600d606fc2fdbfa89a176ee5f68445abb7b458647"},"token":{"tokenDefinitionRoot":"275759c7af233e1cbae01f356fbd513ae3bde2958390a1983ff45744027b9b8c","fungibleBalanceRoot":"37d27ead0950853f31820a65d1fdb9dc503d2a78438b5073d9d137f44c74958d","nftBalanceRoot":"ee0e4b1947dc638b3b33fb301b97afe24d33357680cb93308bfb3e735124d085","nftRoot":"d710612115e23ce1372a0e353412955c5dc534802c4ee47f257979d1e89b1e74","rarityRoot":"df9d4ad62155f1ea976dd8ce8c81bfb2e447f980979d00774e26c54d0c018ab3","entrustFungibleBalanceRoot":null,"entrustNftBalanceRoot":"a5c4f421324dc4017d11feb1e263eb32e17cdb5af473a82ce0bc89e439bb8cd0"},"reward":{"dao":"d96623f3fe6abf66fda0845c33e1472afd09f5e87271820007e6167fba1947fb","userActivity":"bd6a98b0c98f55e1ba0908376f3899b0e4d8404772807e698b32e61ac2779542","tokenReceived":"d6cd49f91b3df54921a33d98078133875a59c28342a525812891c5d8a700b45b"}},"transactionsRoot":"21621ca105f8eecda6c5c4f68cdebb7dfe96df700b3632fbf779cb199f6aa5c5","timestamp":"2023-02-14T02:24:58.531Z"},"transactionHashes":["25014a6ebdad7bc47f6b701216467f3cb612edf444b899fbaccb364a66b5b536"],"votes":[{"v":28,"r":"d2e0025c1c794843509fef1a4d1786375d1a3d3b5bf24a4f5057ca58628f059d","s":"79afe2497087507ea8d069cbd3412d2c072b8b9b63e4c35de0149435585bdb24"}]}"""
  //   val txHash1 = "6f1365f7956c4b49c8a3a5b9234c0503334dae8c531713a22757b119fcb2888c"
  //   val block1 = """{"header":{"number":1090367,"parentHash":"fc5bc39d86832685e4982023ab7475efd91af788a86dba3a6896645a2417f660","stateRoot":{"account":{"namesRoot":"2bc663aaef6b35d57f2da83491e9789c9d9610a00e8f0e8df9fc29998a207236","keyRoot":"bad83767f0eb34cc6ddc83a08c9c3f6a784579d26d264bd194c6418f891c4afc","ethRoot":"0c8ebc2f3e7b529af624d0e5a19e1671f974c50d343cecfa77f4e8a65c7f0bf3"},"group":{"groupRoot":"70234eb0fbd923afcaa4dad81ba6d73242db95b4c5e52e6840b6a74a82c23343","groupAccountRoot":"47cc51b7c1438cfdc0d40b2600d606fc2fdbfa89a176ee5f68445abb7b458647"},"token":{"tokenDefinitionRoot":"275759c7af233e1cbae01f356fbd513ae3bde2958390a1983ff45744027b9b8c","fungibleBalanceRoot":"37d27ead0950853f31820a65d1fdb9dc503d2a78438b5073d9d137f44c74958d","nftBalanceRoot":"ee0e4b1947dc638b3b33fb301b97afe24d33357680cb93308bfb3e735124d085","nftRoot":"d710612115e23ce1372a0e353412955c5dc534802c4ee47f257979d1e89b1e74","rarityRoot":"df9d4ad62155f1ea976dd8ce8c81bfb2e447f980979d00774e26c54d0c018ab3","entrustFungibleBalanceRoot":null,"entrustNftBalanceRoot":"a5c4f421324dc4017d11feb1e263eb32e17cdb5af473a82ce0bc89e439bb8cd0"},"reward":{"dao":"d96623f3fe6abf66fda0845c33e1472afd09f5e87271820007e6167fba1947fb","userActivity":"bd6a98b0c98f55e1ba0908376f3899b0e4d8404772807e698b32e61ac2779542","tokenReceived":"d6cd49f91b3df54921a33d98078133875a59c28342a525812891c5d8a700b45b"}},"transactionsRoot":"cddb82f8947c94c842d58cfd3528ff8cc6a6dfa8d387a76dc29f7c00dc7f7e35","timestamp":"2023-02-14T02:25:00.618Z"},"transactionHashes":["17800e8db1352d926dda43f7442a60a439b88a42f3ae1b05309151d632d11311"],"votes":[{"v":28,"r":"75406de471e923894cb7d12bacfa0b6f27b5066b3f58441e9a38d0f66a147e64","s":"5e518060f72bddb81ebd7a4b109b34f97466047620b7a260e247bbd7e82f94ef"}]}"""
  //   val txHash2 = "380c5c2bf79eec810010b857f00436d2c7e31b9ab9fb978fb8d9904188d96bcf"
  //   val block2 = """{"header":{"number":1090368,"parentHash":"c9af0224a07e6cf69569eb878a2b5eaefddc866014d90f942e7210ec20d14ac3","stateRoot":{"account":{"namesRoot":"2bc663aaef6b35d57f2da83491e9789c9d9610a00e8f0e8df9fc29998a207236","keyRoot":"bad83767f0eb34cc6ddc83a08c9c3f6a784579d26d264bd194c6418f891c4afc","ethRoot":"0c8ebc2f3e7b529af624d0e5a19e1671f974c50d343cecfa77f4e8a65c7f0bf3"},"group":{"groupRoot":"70234eb0fbd923afcaa4dad81ba6d73242db95b4c5e52e6840b6a74a82c23343","groupAccountRoot":"47cc51b7c1438cfdc0d40b2600d606fc2fdbfa89a176ee5f68445abb7b458647"},"token":{"tokenDefinitionRoot":"275759c7af233e1cbae01f356fbd513ae3bde2958390a1983ff45744027b9b8c","fungibleBalanceRoot":"37d27ead0950853f31820a65d1fdb9dc503d2a78438b5073d9d137f44c74958d","nftBalanceRoot":"ee0e4b1947dc638b3b33fb301b97afe24d33357680cb93308bfb3e735124d085","nftRoot":"d710612115e23ce1372a0e353412955c5dc534802c4ee47f257979d1e89b1e74","rarityRoot":"df9d4ad62155f1ea976dd8ce8c81bfb2e447f980979d00774e26c54d0c018ab3","entrustFungibleBalanceRoot":null,"entrustNftBalanceRoot":"a5c4f421324dc4017d11feb1e263eb32e17cdb5af473a82ce0bc89e439bb8cd0"},"reward":{"dao":"d96623f3fe6abf66fda0845c33e1472afd09f5e87271820007e6167fba1947fb","userActivity":"bd6a98b0c98f55e1ba0908376f3899b0e4d8404772807e698b32e61ac2779542","tokenReceived":"d6cd49f91b3df54921a33d98078133875a59c28342a525812891c5d8a700b45b"}},"transactionsRoot":"256ce26388a2c9fa09a7ef61e372b6a568a3f4b82c80ce1e4d22850723942a42","timestamp":"2023-02-14T02:25:04.693Z"},"transactionHashes":["5a0ca838b92d66f33af5b478e3b33ded05814dc8f80983cc9c3811da703a3b1d"],"votes":[{"v":28,"r":"05ed144dae66aff9c8253770bacd2f7cacd89447624b8eda8d6eb8f222a86b4c","s":"46629329a934d584797921330770dbc0c664ec0f86957a5b8796af3233cb3d80"}]}"""

  //   import io.circe.parser.decode

  //   val Right(b0) = decode[Block0](block0): @unchecked
  //   val Right(b1) = decode[Block0](block1): @unchecked


  //   println(b1.asJson.spaces2)
  //   println(b1.toHash)

  //   println(b0.toHash)

    
  //   ExitCode.Success
  // }
