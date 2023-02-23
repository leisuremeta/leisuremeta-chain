package io.leisuremeta.chain.lmscan.agent.service

import io.leisuremeta.chain.lmscan.agent.entity.{BlockEntity, TxEntity}

case class BatchSaver(
  store: Map[BlockEntity, Seq[TxEntity]]
)

object BatchSaver:

  def apply: BatchSaver = new BatchSaver(scala.collection.mutable.Map.empty)

  def put(block: BlockEntity) =
    store.put(block)

  def add(block: BlockEntity, tx: TxEntity) =
    store.get(block) match 
      case Some(vals) => store.add(vals ++ tx)
      case None => store.get()
