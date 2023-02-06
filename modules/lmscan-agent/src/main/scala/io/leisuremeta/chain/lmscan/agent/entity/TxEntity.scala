package io.leisuremeta.chain.lmscan.agent.entity



import java.time.Instant
import io.leisuremeta.chain.api.model.Transaction.AccountTx.*
import io.leisuremeta.chain.api.model.Transaction.TokenTx.*
import io.leisuremeta.chain.api.model.Block
import io.leisuremeta.chain.api.model.Transaction.GroupTx.*
import io.leisuremeta.chain.api.model.Transaction.RewardTx.*



final case class TxEntity(
  hash: String,
  txType: String, // col_name : type
  tokenType: String,
  fromAddr: Option[String],
  toAddr: Option[Seq[String]],
  blockHash: String,
  blockNumber: Long,
  eventTime: Long,
  createdAt: Long,
  inputHashs: Option[Seq[String]],
  outputVals: Option[Seq[String]],
  json: String,
) 

object TxEntity:
  def from(txHash: String, nft: DefineToken, block: Block, blockHash: String, txJson: String): TxEntity =
    TxEntity(
      hash = txHash,
      txType = "Token",
      tokenType = nft.definitionId.utf8.value,
      fromAddr = None,
      toAddr = None,
      blockHash = blockHash,
      blockNumber = block.header.number.toBigInt.longValue,
      eventTime = nft.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond,
      inputHashs = None,
      outputVals = None,
      json = txJson,
    )

  def from(txHash: String, nft: MintNFT, block: Block, blockHash: String, txJson: String): TxEntity =
    TxEntity(
      txHash,
      "Token",
      "NFT",
      None,
      Some(Seq(nft.output.utf8.value)),
      blockHash,
      block.header.number.toBigInt.longValue,
      nft.createdAt.getEpochSecond(),
      Instant.now().getEpochSecond(),
      None,
      Some(Seq(nft.output.utf8.value+"/"+nft.tokenId.utf8.value)),
      txJson,
  )

  def from(txHash: String, nft: TransferNFT, block: Block, blockHash: String, txJson: String): TxEntity =
    TxEntity(
      txHash,
      "Token",
      "NFT",
      None,
      Some(Seq(nft.output.utf8.value)),
      blockHash,
      block.header.number.toBigInt.longValue,
      nft.createdAt.getEpochSecond(),
      Instant.now().getEpochSecond,
      Some(Seq(nft.input.toUInt256Bytes.toHex)),
      Some(Seq(nft.output.utf8.value+"/"+nft.tokenId.utf8.value)),
      txJson,
    )

  def from(txHash: String, nft: BurnNFT, tokenId: String, block: Block, blockHash: String, txJson: String): TxEntity =
    TxEntity(
      txHash,
      "Token",
      "NFT",
      None,
      None,
      blockHash,
      block.header.number.toBigInt.longValue,
      nft.createdAt.getEpochSecond(),
      Instant.now().getEpochSecond,
      Some(Seq(nft.input.toUInt256Bytes.toHex)),
      Some(Seq("/"+tokenId)),
      txJson,
    )

  def from(txHash: String, nft: EntrustNFT, block: Block, blockHash: String, txJson: String): TxEntity =
    TxEntity(
      hash = txHash,
      txType = "Token",
      tokenType = "NFT",
      fromAddr = None,
      toAddr = Some(Seq(nft.to.utf8.value)),
      blockHash = blockHash,
      blockNumber = block.header.number.toBigInt.longValue,
      eventTime = nft.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond,
      inputHashs = Some(Seq(nft.input.toUInt256Bytes.toHex)),
      outputVals = Some(Seq(nft.to.utf8.value+"/"+nft.tokenId.utf8.value)),
      json = txJson,
    )

  def from(txHash: String, nft: DisposeEntrustedNFT, block: Block, blockHash: String, txJson: String): TxEntity =
    val toAccount = nft.output match 
        case Some(value) => value.utf8.value
        case None =>  ""

    val toAccountOpt = nft.output match 
        case Some(value) => Some(Seq(value.utf8.value))
        case None =>  None
    TxEntity(
      hash = txHash,
      txType = "Token",
      tokenType = "NFT",
      fromAddr = None,
      toAddr = toAccountOpt,
      blockHash = blockHash,
      blockNumber = block.header.number.toBigInt.longValue,
      eventTime = nft.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond,
      inputHashs = Some(Seq(nft.input.toUInt256Bytes.toHex)),
      outputVals = Some(Seq(toAccount+"/"+nft.tokenId.utf8.value)),
      json = txJson,
    )
  
  
  def from(txHash: String, tx: CreateAccount, block: Block, blockHash: String, txJson: String): TxEntity =
    val toAccount = tx.account.utf8.value
    TxEntity(
      hash = txHash,
      txType = "Account",
      tokenType = "LM",
      fromAddr = None,
      toAddr = None,
      blockHash = blockHash,
      blockNumber = block.header.number.toBigInt.longValue,
      eventTime = tx.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond,
      inputHashs = None,
      outputVals = Some(Seq(toAccount+"/"+0)),
      json = txJson,
    )

  def from(txHash: String, tx: UpdateAccount, block: Block, blockHash: String, txJson: String): TxEntity =
    val toAccount = tx.account.utf8.value
    TxEntity(
      hash = txHash,
      txType = "Account",
      tokenType = "LM",
      fromAddr = None,
      toAddr = None,
      blockHash = blockHash,
      blockNumber = block.header.number.toBigInt.longValue,
      eventTime = tx.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond,
      inputHashs = None,
      outputVals = Some(Seq(toAccount+"/"+0)),
      json = txJson,
    )

  def from(txHash: String, tx: AddPublicKeySummaries, block: Block, blockHash: String, txJson: String): TxEntity =
    val toAccount = tx.account.utf8.value
    TxEntity(
      hash = txHash,
      txType = "Account",
      tokenType = "LM",
      fromAddr = None,
      toAddr = None,
      blockHash = blockHash,
      blockNumber = block.header.number.toBigInt.longValue,
      eventTime = tx.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond,
      inputHashs = None,
      outputVals = Some(Seq(toAccount+"/"+0)),
      json = txJson,
    )

  def from(txHash: String, tx: CreateGroup, block: Block, blockHash: String, txJson: String): TxEntity =

    TxEntity(
      hash = txHash,
      txType = "Group",
      tokenType = "LM",
      fromAddr = None,
      toAddr = None,
      blockHash = blockHash,
      blockNumber = block.header.number.toBigInt.longValue,
      eventTime = tx.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond,
      inputHashs = None,
      outputVals = None,
      json = txJson,
    )

  def from(txHash: String, tx: AddAccounts, block: Block, blockHash: String, txJson: String): TxEntity =
    TxEntity(
      hash = txHash,
      txType = "Group",
      tokenType = "LM",
      fromAddr = None,
      toAddr = None,
      blockHash = blockHash,
      blockNumber = block.header.number.toBigInt.longValue,
      eventTime = tx.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond,
      inputHashs = None,
      outputVals = None,
      json = txJson,
    )
  
  def from(txHash: String, tx: RegisterDao, block: Block, blockHash: String, txJson: String): TxEntity =
    TxEntity(
      hash = txHash,
      txType = "Reward",
      tokenType = "LM",
      fromAddr = None,
      toAddr = None,
      blockHash = blockHash,
      blockNumber = block.header.number.toBigInt.longValue,
      eventTime = tx.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond,
      inputHashs = None,
      outputVals = None,
      json = txJson,
    )
   

  
  def from(txHash: String, tx: UpdateDao, block: Block, blockHash: String, txJson: String): TxEntity =
    TxEntity(
      hash = txHash,
      txType = "Reward",
      tokenType = "LM",
      fromAddr = None,
      toAddr = None,
      blockHash = blockHash,
      blockNumber = block.header.number.toBigInt.longValue,
      eventTime = tx.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond,
      inputHashs = None,
      outputVals = None,
      json = txJson,
    )

  def from(txHash: String, tx: RecordActivity, block: Block, blockHash: String, txJson: String): TxEntity =
    TxEntity(
      hash = txHash,
      txType = "Reward",
      tokenType = "LM",
      fromAddr = None,
      toAddr = None,
      blockHash = blockHash,
      blockNumber = block.header.number.toBigInt.longValue,
      eventTime = tx.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond,
      inputHashs = None,
      outputVals = None,
      json = txJson,
    )

  def from(txHash: String, tx: MintFungibleToken, block: Block, blockHash: String, txJson: String): TxEntity =
    TxEntity(
      hash = txHash,
      txType = "Token",
      tokenType = "LM",
      fromAddr = None,
      toAddr = None,
      blockHash = blockHash,
      blockNumber = block.header.number.toBigInt.longValue,
      eventTime = tx.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond,
      inputHashs = None,
      outputVals = None,
      json = txJson,
    )

  def from(txHash: String, tx: BurnFungibleToken, block: Block, blockHash: String, txJson: String, account: String): TxEntity =
    TxEntity(
      hash = txHash,
      txType = "Token",
      tokenType = "LM",
      fromAddr = None,
      toAddr = Some(Seq(account)),
      blockHash = blockHash,
      blockNumber = block.header.number.toBigInt.longValue,
      eventTime = tx.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond,
      inputHashs = Some(tx.inputs.map(_.toUInt256Bytes.toBytes.toHex).toSeq),
      outputVals = Some(Seq(account + "/" + tx.amount.toBigInt.toString())),
      json = txJson,
    )

  def from(txHash: String, tx: TransferFungibleToken, block: Block, blockHash: String, txJson: String, fromAccount: String): TxEntity =
    val toAccounts = tx.outputs.keySet.map(_.utf8.value).toSeq
    val outputVals: Seq[String] = tx.outputs.toList.map { case (account, amount) =>
      account.utf8.value + "/" + amount.toBigInt.toString()
    }
    TxEntity(
      hash = txHash,
      txType = "Token",
      tokenType = "LM",
      fromAddr = Some(fromAccount),
      toAddr = Some(toAccounts),
      blockHash = blockHash,
      blockNumber = block.header.number.toBigInt.longValue,
      eventTime = tx.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond,
      inputHashs = Some(tx.inputs.map(_.toUInt256Bytes.toBytes.toHex).toSeq),
      outputVals = Some(outputVals),
      json = txJson,
    )

  def from(txHash: String, tx: EntrustFungibleToken, block: Block, blockHash: String, txJson: String, fromAccount: String): TxEntity =
    TxEntity(
      hash = txHash,
      txType = "Token",
      tokenType = "LM",
      fromAddr = Some(fromAccount),
      toAddr = Some(Seq(tx.to.utf8.value)),
      blockHash = blockHash,
      blockNumber = block.header.number.toBigInt.longValue,
      eventTime = tx.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond,
      inputHashs = Some(tx.inputs.map(_.toUInt256Bytes.toBytes.toHex).toSeq),
      outputVals = Some(Seq(tx.to.utf8.value + "/" + tx.amount.toBigInt.toString())),
      json = txJson,
    )

  def from(txHash: String, tx: DisposeEntrustedFungibleToken, block: Block, blockHash: String, txJson: String, fromAccount: String): TxEntity =
    val toAccounts = tx.outputs.keySet.map(_.utf8.value).toSeq
    val outputVals: Seq[String] = tx.outputs.toList.map { case (account, amount) =>
      account.utf8.value + "/" + amount.toBigInt.toString()
    }
    TxEntity(
      hash = txHash,
      txType = "Token",
      tokenType = "LM",
      fromAddr = Some(fromAccount),
      toAddr = Some(toAccounts),
      blockHash = blockHash,
      blockNumber = block.header.number.toBigInt.longValue,
      eventTime = tx.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond,
      inputHashs = Some(tx.inputs.map(_.toUInt256Bytes.toBytes.toHex).toSeq),
      outputVals = Some(outputVals),
      json = txJson,
    )

  

  def from(txHash: String, tx: OfferReward, block: Block, blockHash: String, txJson: String, fromAccount: String): TxEntity =

    val toAccounts = tx.outputs.keySet.map(_.utf8.value).toSeq
    val outputVals: Seq[String] = tx.outputs.toList.map { case (account, amount) =>
      account.utf8.value + "/" + amount.toBigInt.toString()
    }
    TxEntity(
      hash = txHash,
      txType = "Token",
      tokenType = "LM",
      fromAddr = Some(fromAccount),
      toAddr = Some(toAccounts),
      blockHash = blockHash,
      blockNumber = block.header.number.toBigInt.longValue,
      eventTime = tx.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond,
      inputHashs = Some(tx.inputs.map(_.toUInt256Bytes.toBytes.toHex).toSeq),
      outputVals = Some(outputVals),
      json = txJson,
    )

  
  def from(txHash: String, tx: ExecuteReward, rewardRes: ExecuteRewardResult, block: Block, blockHash: String, txJson: String, fromAccount: String): TxEntity =

    val toAccounts = rewardRes.outputs.keySet.map(_.utf8.value).toSeq
    val outputVals: Seq[String] = rewardRes.outputs.toList.map { case (account, amount) =>
      account.utf8.value + "/" + amount.toBigInt.toString()
    }
    TxEntity(
      hash = txHash,
      txType = "Token",
      tokenType = "LM",
      fromAddr = Some(fromAccount),
      toAddr = Some(toAccounts),
      blockHash = blockHash,
      blockNumber = block.header.number.toBigInt.longValue,
      eventTime = tx.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond,
      inputHashs = None,
      outputVals = Some(outputVals),
      json = txJson,
    )