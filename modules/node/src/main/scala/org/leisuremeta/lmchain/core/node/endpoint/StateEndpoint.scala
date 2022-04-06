package org.leisuremeta.lmchain.core
package node
package endpoint

import cats.effect.Async
import cats.implicits._
import io.finch._

import model.{Account, NameState, TokenState}
import model.Transaction.Input
import model.Transaction.Token.DefinitionId
import repository.{BlockRepository, StateRepository}
import service.StateReadService

object StateEndpoint {
  def GetName[F[_]: Async: BlockRepository: StateRepository.Name](implicit
      finch: EndpointModule[F]
  ): Endpoint[F, NameState] = {

    import finch._

    get("name" :: path[Account.Name].withToString("{name}")) {
      (name: Account.Name) =>
        StateReadService.getNameState[F](name).value.map {
          case Right(Some(nameState)) => Ok(nameState)
          case Right(None)            => NotFound(new Exception(s"Not found: $name"))
          case Left(errorMsg) =>
            scribe.debug(s"Get name $name error response: $errorMsg")
            InternalServerError(new Exception(errorMsg))
        }
    }
  }

  def GetToken[F[_]: Async: BlockRepository: StateRepository.Token](implicit
      finch: EndpointModule[F]
  ): Endpoint[F, TokenState] = {

    import finch._

    get("token" :: path[DefinitionId].withToString("{definitionId}")) {
      (definitionId: DefinitionId) =>
        StateReadService.getTokenState[F](definitionId).value.map {
          case Right(Some(tokenState)) => Ok(tokenState)
          case Right(None) =>
            NotFound(new Exception(s"Not found: $definitionId"))
          case Left(errorMsg) =>
            scribe.debug(s"Get token $TokenState error response: $errorMsg")
            InternalServerError(new Exception(errorMsg))
        }
    }
  }

  def GetBalance[F[_]: Async: BlockRepository: StateRepository.Balance](implicit
      finch: EndpointModule[F]
  ): Endpoint[F, List[Input.Tx]] = {

    import finch._

    get("balance" :: path[Account].withToString("{account}")) {
      (account: Account) =>
        StateReadService.getBalance[F](account).value.map {
          case Right(txs) => Ok(txs)
          case Left(errorMsg) =>
            scribe.debug(
              s"Get balance of account $account error response: $errorMsg"
            )
            InternalServerError(new Exception(errorMsg))
        }
    }
  }
}
