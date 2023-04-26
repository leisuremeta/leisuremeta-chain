package io.leisuremeta.chain.node.proxy.service

import cats.instances.queue
import cats.effect.kernel.Async
import cats.effect.std.Queue
import cats.effect.kernel.Ref
import java.time.Instant
import cats.implicits.*
import cats.syntax.*
import cats.syntax.functor._
import cats.effect.std.Queue
import cats.Monad

object PostTxQueue:
  def apply[F[_]: Async]: F[PostTxQueue[F]] =
    Queue.circularBuffer[F, String](5).map { 
      queue => new PostTxQueue[F](queue)
    }

class PostTxQueue[F[_]: Async](
  queue: Queue[F, String]
):
  def push(txJson: String): F[Unit] =
    queue.offer(txJson)

  def pollsAfter(lastTxOpt: Option[String]): F[List[String]] =
    lastTxOpt match
      case None => Async[F].pure(List.empty)
      case Some(lastTx) => 
        queue.tryTakeN(Some(5)).map {  
          _.dropWhile(_ != lastTx)
          .drop(1)
        }

  def peek(): F[Unit] =
    for {
      items <- queue.tryTakeN(None)
      _     <- items.traverse { item => Async[F].delay(println(item)) }
      _     <- items.traverse(queue.offer(_))
    } yield ()
    
    
  