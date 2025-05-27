package io.leisuremeta.chain.lmscan.agent.apps

import cats.effect.*
import io.leisuremeta.chain.lmscan.agent.service.RemoteStoreApp
import io.leisuremeta.chain.lmscan.agent.service.RequestServiceApp
import io.circe.Decoder
import io.circe.generic.semiauto.*
import cats.implicits.*
import scala.concurrent.duration.*
import io.leisuremeta.chain.lmscan.agent.ESConfig
import io.leisuremeta.chain.lmscan.agent.MarketConfig
import cats.data.EitherT

trait SummaryStoreApp[F[_]]:
  def run: F[Unit]

object SummaryStoreApp:
  case class TokenBalance(status: Int, message: String, result: BigDecimal)
  case class LmPrice(status: MarketStatus, data: TokenMap)
  case class MarketStatus(error_code: Int, error_message: Option[String])
  case class TokenMap(`20315`: MarketData)
  case class MarketData(
      id: Int,
      name: String,
      symbol: String,
      last_updated: String,
      quote: Currency,
      circulating_supply: BigDecimal,
  )
  case class Currency(USD: USDCurrency)
  case class USDCurrency(
      price: BigDecimal,
      last_updated: String,
      market_cap: BigDecimal,
  )
  given Decoder[LmPrice]      = deriveDecoder[LmPrice]
  given Decoder[TokenBalance] = deriveDecoder[TokenBalance]

  def build[F[_]: Async](market: MarketConfig, es: ESConfig)(
      remote: RemoteStoreApp[F],
      client: RequestServiceApp[F],
  ): SummaryStoreApp[F] = new SummaryStoreApp[F]:
    def run =
      val loop = for
        _ <- EitherT.liftF:
          Async[F].delay:
            scribe.info("start summary loop")
        balance <- getTotalBalance
        lmprice <- getLmPriceAndSupply
        data   = lmprice.data.`20315`
        supply = data.circulating_supply
        cap    = data.quote.USD.market_cap
        price  = data.quote.USD.price
        _ <- EitherT(
          remote.summary
            .updateSummary(
              balance,
              cap,
              supply,
              price,
            ),
        ).leftMap(_.getMessage)
        _ <- EitherT(
          remote.summary.updateValidatorInfo,
        ).leftMap(_.getMessage)
      yield ()

      for
        _ <- Async[F].sleep(10.minutes)
        _ <- loop.value
        r <- run
      yield r

    def getLmPriceAndSupply =
      client
        .getResultFromKeyApi[LmPrice](
          s"https://pro-api.coinmarketcap.com/v2/cryptocurrency/quotes/latest?id=${market.token}",
          Map("X-CMC_PRO_API_KEY" -> market.key),
        )

    def getTotalBalance =
      def url(addr: String) =
        s"https://api.etherscan.io/v2/api?chainid=1&module=account&action=tokenbalance&contractaddress=${es.lm}&address=${addr}&tag=latest&apikey=${es.key}"
      es.addrs
        .map(addr => client.getResult[TokenBalance](url(addr)))
        .parSequence
        .map(
          _.map(_.result)
            .fold(BigDecimal(0))((acc, x) => acc + x),
        )
