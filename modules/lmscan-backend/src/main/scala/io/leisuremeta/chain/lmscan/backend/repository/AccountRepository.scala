package io.leisuremeta.chain.lmscan.backend.repository

import io.leisuremeta.chain.lmscan.backend.repository.CommonQuery
import io.leisuremeta.chain.lmscan.backend.entity.Account
import cats.effect.kernel.Async
import cats.data.EitherT
import io.getquill.*
import io.leisuremeta.chain.lmscan.backend.entity.Balance
import io.getquill.autoQuote

object AccountRepository extends CommonQuery:
  import ctx.{*, given}

  // def get[F[_]: Async](
  //     address: String,
  // ): EitherT[F, String, Option[Account]] =
  //   inline def detailQuery =
  //     quote { (addr: String) =>
  //       query[Account].filter(a => a.address == addr).take(1)
  //     }
  //   optionQuery(detailQuery(lift(address)))

  def get[F[_]: Async](
      addr: String,
  ): EitherT[F, String, Option[Account]] =
    inline def detailQuery =
      quote { (addr: String) =>
        query[Account]
          .join(query[Balance])
          .on((a, b) => a.address == b.address)
          .filter(_._1.address == addr)
          .take(1)
          .map { case (a, b) =>
            Account(
              address = a.address,
              balance = b.free,
              amount = a.amount,
              createdAt = a.createdAt,
            )
          }
      }
    optionQuery(detailQuery(lift(addr)))
