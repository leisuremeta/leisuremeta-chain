package io.leisuremeta.chain
package node
package proxy

import cats.effect.std.Dispatcher
import cats.effect.Async
import cats.effect.kernel.Resource
import com.linecorp.armeria.server.Server

import sttp.tapir.server.interceptor.log.DefaultServerLog
import sttp.tapir.server.armeria.cats.{
  ArmeriaCatsServerInterpreter,
  ArmeriaCatsServerOptions,
}
import sttp.tapir.server.ServerEndpoint
import sttp.capabilities.fs2.Fs2Streams

import io.leisuremeta.chain.node.proxy.{NodeProxyApi as Api}
import service.InternalApiService

import api.model.creator_dao.CreatorDaoId
import api.model.account.EthAddress
import api.model.*
import api.model.token.*

final case class NodeProxyApp[F[_]: Async](
    apiService: InternalApiService[F],
):

  def getBlockServerEndpoint = Api.getBlockEndpoint.serverLogicSuccess:
    (blockHash: String) =>
      apiService.getBlock(blockHash)

  def getAccountServerEndpoint: ServerEndpoint[Fs2Streams[F], F] =
    Api.getAccountEndpoint.serverLogicSuccess: (a: Account) =>
      apiService.getAccount(a)

  def getEthServerEndpoint = Api.getEthEndpoint.serverLogicSuccess:
    (ethAddress: EthAddress) =>
      apiService.getEthAccount(ethAddress)

  def getGroupServerEndpoint = Api.getGroupEndpoint.serverLogicSuccess:
    (g: GroupId) =>
      apiService.getGroupInfo(g)

  def getBlockListServerEndpoint = Api.getBlockListEndpoint.serverLogicSuccess:
    (fromOption, limitOption) =>
      apiService.getBlockList(fromOption, limitOption)

  def getStatusServerEndpoint = Api.getStatusEndpoint.serverLogicSuccess: _ =>
    apiService.getStatus

  def getTokenDefServerEndpoint = Api.getTokenDefinitionEndpoint.serverLogicSuccess:
    (tokenDefinitionId: TokenDefinitionId) =>
      apiService.getTokenDef(tokenDefinitionId)

  def getBalanceServerEndpoint = Api.getBalanceEndpoint.serverLogicSuccess:
    (account, movable) =>
      apiService.getBalance(account, movable)

  def getNftBalanceServerEndpoint = Api.getNftBalanceEndpoint.serverLogicSuccess:
    (account, movable) =>
      apiService.getNftBalance(account, movable)

  def getTokenServerEndpoint = Api.getTokenEndpoint.serverLogicSuccess:
    (tokenId: TokenId) =>
      apiService.getToken(tokenId)

  def getTokenHistoryServerEndpoint = Api.getTokenHistoryEndpoint.serverLogicSuccess:
    (txHash: String) =>
      apiService.getTokenHistory(txHash)

  def getOwnersServerEndpoint = Api.getOwnersEndpoint.serverLogicSuccess:
    (tokenDefinitionId: TokenDefinitionId) =>
      apiService.getOwners(tokenDefinitionId)

  def getAccountActivityServerEndpoint =
    Api.getAccountActivityEndpoint.serverLogicSuccess: (account: Account) =>
      apiService.getAccountActivity(account)

  def getTokenActivityServerEndpoint =
    Api.getTokenActivityEndpoint.serverLogicSuccess: (tokenId: TokenId) =>
      apiService.getTokenActivity(tokenId)

  def getAccountSnapshotServerEndpoint =
    Api.getAccountSnapshotEndpoint.serverLogicSuccess: (account: Account) =>
      apiService.getAccountSnapshot(account)

  def getTokenSnapshotServerEndpoint =
    Api.getTokenSnapshotEndpoint.serverLogicSuccess: (tokenId: TokenId) =>
      apiService.getTokenSnapshot(tokenId)


  def getOwnershipSnapshotServerEndpoint =
    Api.getOwnershipSnapshotEndpoint.serverLogicSuccess: (tokenId: TokenId) =>
      apiService.getOwnershipSnapshot(tokenId)

  def getOwnershipSnapshotMapServerEndpoint =
    Api.getOwnershipSnapshotMapEndpoint.serverLogicSuccess:
      (from: Option[TokenId], limit: Option[Int]) =>
        apiService.getOwnershipSnapshotMap(from, limit)

  def getOwnershipRewardedServerEndpoint =
    Api.getOwnershipRewardedEndpoint.serverLogicSuccess: (tokenId: TokenId) =>
      apiService.getOwnershipRewarded(tokenId)

  def getDaoInfoServerEndpoint =
    Api.getDaoInfoEndpoint.serverLogicSuccess: (groupId: GroupId) =>
      apiService.getDaoInfo(groupId)

  def getTxServerEndpoint = Api.getTxEndpoint.serverLogicSuccess: (txHash: String) =>
    apiService.getTx(txHash)

  def getTxSetServerEndpoint =
    Api.getTxSetEndpoint.serverLogicSuccess: (block: String) =>
      apiService.getTxSet(block)

  def postTxHashServerEndpoint =
    Api.postTxHashEndpoint.serverLogicSuccess: (txs: String) =>
      scribe.info(s"received postTxHash request: $txs")
      apiService.postTxHash(txs)

  def postTxServerEndpoint =
    Api.postTxEndpoint.serverLogicSuccess: (txs: String) =>
      scribe.info(s"received postTx request: $txs")
      apiService.postTx(txs)

  def getSnapshotStateServerEndpoint =
    Api.getSnapshotStateEndpoint.serverLogicSuccess:
      (tokenDefinitionId: TokenDefinitionId) =>
        apiService.getSnapshotState(tokenDefinitionId)

  def getFungibleSnapshotBalanceServerEndpoint =
    Api.getFungibleSnapshotBalanceEndpoint.serverLogicSuccess:
      (
          account: Account,
          tokenDefinitionId: TokenDefinitionId,
          snapshotId: String,
      ) =>
        apiService
          .getFungibleSnapshotBalance(account, tokenDefinitionId, snapshotId)
          

  def getNftSnapshotBalanceServerEndpoint =
    Api.getNftSnapshotBalanceEndpoint.serverLogicSuccess:
      (
          account: Account,
          tokenDefinitionId: TokenDefinitionId,
          snapshotId: String,
      ) =>
        apiService
          .getNftSnapshotBalance(account, tokenDefinitionId, snapshotId)
          

  def getVoteProposalServerEndpoint = Api.getVoteProposalEndpoint.serverLogicSuccess:
    (proposalId: String) =>
      apiService.getVoteProposal(proposalId)

  def getAccountVotesServerEndpoint = Api.getAccountVotesEndpoint.serverLogicSuccess:
    (proposalId: String, account: Account) =>
      apiService.getAccountVotes(proposalId, account)

  def getVoteCountServerEndpoint = Api.getVoteCountEndpoint.serverLogicSuccess:
    (proposalId: String) =>
      apiService.getVoteCount(proposalId)

  def getCreatorDaoInfoServerEndpoint =
    Api.getCreatorDaoInfoEndpoint.serverLogicSuccess: (creatorDaoId: CreatorDaoId) =>
      apiService.getCreatorDaoInfo(creatorDaoId)

  def getCreatorDaoMemberServerEndpoint =
    Api.getCreatorDaoMemberEndpoint.serverLogicSuccess:
      (creatorDaoId: CreatorDaoId, from: Option[Account], limit: Option[Int]) =>
        apiService.getCreatorDaoMember(creatorDaoId, from, limit)

  def proxyNodeEndpoints = List(
    getAccountServerEndpoint,
    getEthServerEndpoint,
    getBlockListServerEndpoint,
    getBlockServerEndpoint,
    getGroupServerEndpoint,
    getStatusServerEndpoint,
    getTxServerEndpoint,
    getTokenDefServerEndpoint,
    getBalanceServerEndpoint,
    getNftBalanceServerEndpoint,
    getTokenServerEndpoint,
    getTokenHistoryServerEndpoint,
    getOwnersServerEndpoint,
    getTxSetServerEndpoint,
    getAccountActivityServerEndpoint,
    getTokenActivityServerEndpoint,
    getAccountSnapshotServerEndpoint,
    getTokenSnapshotServerEndpoint,
    getOwnershipSnapshotServerEndpoint,
    getOwnershipSnapshotMapServerEndpoint,
//    getRewardServerEndpoint,
    getOwnershipRewardedServerEndpoint,
    getDaoInfoServerEndpoint,
    postTxServerEndpoint,
    postTxHashServerEndpoint,
    getSnapshotStateServerEndpoint,
    getFungibleSnapshotBalanceServerEndpoint,
    getNftSnapshotBalanceServerEndpoint,
    getVoteProposalServerEndpoint,
    getAccountVotesServerEndpoint,
    getVoteCountServerEndpoint,
    getCreatorDaoInfoServerEndpoint,
    getCreatorDaoMemberServerEndpoint,
  )

  def getServer[IO](
      dispatcher: Dispatcher[F],
  ): F[Server] = Async[F].fromCompletableFuture:
    def log[F[_]: Async](
        level: scribe.Level,
    )(msg: String, exOpt: Option[Throwable])(using
        mdc: scribe.mdc.MDC,
    ): F[Unit] = Async[F].delay:
      exOpt match
        case None     => scribe.log(level, mdc, msg)
        case Some(ex) => scribe.log(level, mdc, msg, ex)
    val serverLog = DefaultServerLog(
      doLogWhenReceived = log(scribe.Level.Info)(_, None),
      doLogWhenHandled = log(scribe.Level.Info),
      doLogAllDecodeFailures = log(scribe.Level.Info),
      doLogExceptions =
        (msg: String, ex: Throwable) => Async[F].delay(scribe.warn(msg, ex)),
      noLog = Async[F].pure(()),
    )
    val serverOptions = ArmeriaCatsServerOptions
      .customiseInterceptors[F](dispatcher)
      .serverLog(serverLog)
      .options
    val tapirService = ArmeriaCatsServerInterpreter[F](serverOptions)
      .toService(proxyNodeEndpoints)
    val server = Server.builder
      .maxRequestLength(128 * 1024 * 1024)
      .requestTimeout(java.time.Duration.ofMinutes(10))
      .http(8080)
      .service(tapirService)
      .build
    Async[F].delay:
      server.start().thenApply(_ => server)

  def resource: F[Resource[F, Server]] = Async[F].delay:
    for
      dispatcher <- Dispatcher.parallel[F]
      server     <- Resource.fromAutoCloseable(getServer(dispatcher))
    yield server
