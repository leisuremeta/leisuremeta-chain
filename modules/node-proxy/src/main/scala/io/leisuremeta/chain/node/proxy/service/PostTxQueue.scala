package io.leisuremeta.chain.node.proxy.service

import cats.effect.kernel.Async
import cats.syntax.all.*
import cats.effect.std.Queue

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
        queue.tryTakeN(None).map {  
          _.dropWhile(_ != lastTx)
           .drop(1)
        }

  def peek(): F[Unit] =
    for {
      items <- queue.tryTakeN(None)
      _     <- items.traverse { item => Async[F].delay(println(item)) }
      _     <- items.traverse(queue.offer(_))
    } yield ()
    
    
  