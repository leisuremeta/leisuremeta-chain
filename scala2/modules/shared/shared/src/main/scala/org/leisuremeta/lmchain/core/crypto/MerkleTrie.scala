package org.leisuremeta.lmchain.core
package crypto

import cats.Monad
import cats.data.{EitherT, Kleisli, StateT}
import cats.effect.Sync
import cats.implicits._

import eu.timepit.refined.api.Refined
import eu.timepit.refined.refineV
import monix.tail.Iterant
import scodec.bits.BitVector
import shapeless.nat._16
import shapeless.syntax.sized._

import codec.byte.{ByteDecoder, ByteEncoder, DecodeResult}
import util.refined.bitVector._
import ByteEncoder.ops._
import Hash.ops._

object MerkleTrie {

  type NodeStore[F[_], K, V] =
    Kleisli[EitherT[F, String, *], MerkleHash[K, V], Option[
      MerkleTrieNode[K, V]
    ]]

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  def get[F[_]: Monad, K, V: ByteDecoder](key: BitVector)(implicit
      ns: NodeStore[F, K, V]
  ): StateT[EitherT[F, String, *], MerkleTrieState[K, V], Option[V]] =
    StateT.inspectF { (state: MerkleTrieState[K, V]) =>
      if (state.root.isEmpty) EitherT.rightT[F, String](None)
      else
        for {
          node <- getNode[F, K, V](state)
          valueOption <- (node match {
            case MerkleTrieNode.Leaf(prefix, value) if key === prefix.value =>
              EitherT.fromEither[F](
                ByteDecoder[V].decode(value).left.map(_.msg).flatMap {
                  case DecodeResult(value, remainder) if remainder.isEmpty =>
                    Right(Some(value))
                  case result =>
                    Left(s"Non empty remainder after decoding: $result")
                }
              )
            case MerkleTrieNode.Branch(prefix, children)
                if (key startsWith prefix.value) && key.size >= prefix.value.size + 4 =>
              val (index, nextKey) = key.drop(prefix.value.size).splitAt(4)
              children.unsized(index.toInt(signed = false)) match {
                case Some(nextRoot) =>
                  get[F, K, V](nextKey) runA state.copy(root = Some(nextRoot))
                case None =>
                  EitherT.rightT[F, String](None)
              }
            case _ => EitherT.rightT[F, String](None)
          })
        } yield valueOption
    }

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  def put[F[_]: Monad, K, V: ByteEncoder](key: BitVector, value: V)(implicit
      ns: NodeStore[F, K, V]
  ): StateT[EitherT[F, String, *], MerkleTrieState[K, V], Unit] =
    StateT.modifyF((state: MerkleTrieState[K, V]) =>
      state.root match {
        case None =>
          val leaf = MerkleTrieNode
            .leaf[K, V](ensurePrefix(key), value.toBytes)
          val leafHash = leaf.toHash
          EitherT.rightT[F, String](
            state.copy(
              root = Some(leafHash),
              diff = state.diff.add(leafHash, leaf),
            )
          )
        case Some(root) =>
          getNode[F, K, V](state).flatMap { (node: MerkleTrieNode[K, V]) =>
            val prefix0: BitVector = node.prefix.value
            val commonPrefixNibbleSize: Int = (key ^ prefix0)
              .grouped(4L)
              .takeWhile(_ === BitVector.low(4L))
              .size
            val nextPrefixBitSize =
              ((key.size / 4L - 1L) min commonPrefixNibbleSize.toLong) * 4L
            val (commonPrefix, remainder1) = key splitAt nextPrefixBitSize
            val (index1, prefix1)          = remainder1 splitAt 4L
            val remainder0                 = prefix0 drop nextPrefixBitSize

            node match {
              case MerkleTrieNode.Leaf(_, value0) =>
                val (index00, prefix00) = remainder0 splitAt 4L
                val leaf0 =
                  MerkleTrieNode.leaf[K, V](ensurePrefix(prefix00), value0)
                val leaf0hash = leaf0.toHash
                val leaf1 = MerkleTrieNode
                  .leaf[K, V](ensurePrefix(prefix1), value.toBytes)
                val leaf1hash = leaf1.toHash
                val branch = MerkleTrieNode.branch(
                  ensurePrefix(commonPrefix),
                  Vector
                    .fill(16)(None)
                    .updated(index00.toInt(signed = false), Some(leaf0hash))
                    .updated(index1.toInt(signed = false), Some(leaf1hash))
                    .ensureSized[_16],
                )
                val branchHash = branch.toHash
                EitherT.rightT[F, String](
                  state.copy(
                    root = Some(branchHash),
                    diff = state.diff
                      .add(branchHash, branch)
                      .add(leaf0hash, leaf0)
                      .add(leaf1hash, leaf1)
                      .remove(root),
                  )
                )
              case MerkleTrieNode.Branch(_, children) if remainder0.isEmpty =>
                children.unsized(index1.toInt(signed = false)) match {
                  case None =>
                    val leaf1 = MerkleTrieNode
                      .leaf[K, V](ensurePrefix(prefix1), value.toBytes)
                    val leaf1hash = leaf1.toHash
                    val branch = MerkleTrieNode.branch(
                      ensurePrefix(commonPrefix),
                      children.unsized
                        .updated(index1.toInt(signed = false), Some(leaf1hash))
                        .ensureSized[_16],
                    )
                    val branchHash = branch.toHash
                    EitherT.rightT[F, String](
                      state.copy(
                        root = Some(branchHash),
                        diff = state.diff
                          .add(branchHash, branch)
                          .add(leaf1hash, leaf1)
                          .remove(root),
                      )
                    )
                  case Some(childHash) =>
                    put[F, K, V](prefix1, value) runS state.copy(root =
                      Some(childHash)
                    ) map { childState =>
                      val branch = MerkleTrieNode.branch(
                        node.prefix,
                        children.unsized
                          .updated(
                            index1.toInt(signed = false),
                            childState.root,
                          )
                          .ensureSized[_16],
                      )
                      val branchHash = branch.toHash
                      childState.copy(
                        root = Some(branchHash),
                        diff =
                          childState.diff.add(branchHash, branch).remove(root),
                      )
                    }
                }
              case MerkleTrieNode.Branch(_, children) =>
                val (index00, prefix00) = remainder0 splitAt 4L
                val branch00 =
                  MerkleTrieNode.branch(ensurePrefix(prefix00), children)
                val branch00hash = branch00.toHash
                val leaf1 = MerkleTrieNode
                  .leaf[K, V](ensurePrefix(prefix1), value.toBytes)
                val leaf1hash = leaf1.toHash
                val branch = MerkleTrieNode.branch(
                  ensurePrefix(commonPrefix),
                  Vector
                    .fill[Option[MerkleHash[K, V]]](16)(None)
                    .updated(index00.toInt(signed = false), Some(branch00hash))
                    .updated(index1.toInt(signed = false), Some(leaf1hash))
                    .ensureSized[_16],
                )
                val branchHash = branch.toHash
                EitherT.rightT[F, String](
                  state.copy(
                    root = Some(branchHash),
                    diff = state.diff
                      .add(branchHash, branch)
                      .add(branch00hash, branch00)
                      .add(leaf1hash, leaf1)
                      .remove(root),
                  )
                )
            }
          }
      }
    )

  @SuppressWarnings(
    Array(
      "org.wartremover.warts.Recursion",
      "org.wartremover.warts.NonUnitStatements",
    )
  )
  def removeByKey[F[_]: Monad, K, V](key: BitVector)(implicit
      ns: NodeStore[F, K, V]
  ): StateT[EitherT[F, String, *], MerkleTrieState[K, V], Unit] =
    StateT.modifyF((state: MerkleTrieState[K, V]) =>
      state.root match {
        case None =>
          EitherT.leftT[F, MerkleTrieState[K, V]](
            s"Fail to remove element from empty merkle trie: $state"
          )
        case Some(root) =>
          getNode[F, K, V](state).flatMap {
            case MerkleTrieNode.Leaf(prefix, _) if prefix.value === key =>
              EitherT.rightT[F, String](
                state.copy(
                  root = None,
                  diff = state.diff.remove(root),
                )
              )
            case MerkleTrieNode.Leaf(_, _) =>
              EitherT.leftT[F, MerkleTrieState[K, V]](
                s"Fail to remove element: $key does not exist"
              )
            case MerkleTrieNode.Branch(prefix, children) =>
              val commonPrefixNibbleSize: Int = (key ^ prefix.value)
                .grouped(4L)
                .takeWhile(_ === BitVector.low(4L))
                .size
              val nextPrefixBitSize = commonPrefixNibbleSize.toLong * 4L
              val remainder1        = key drop nextPrefixBitSize

              if (remainder1.size < 4L)
                EitherT.leftT[F, MerkleTrieState[K, V]](
                  s"Fail to remove element: $key does not exist"
                )
              else {
                val (index1, key1) = remainder1 splitAt 4L
                children.unsized(index1.toInt(signed = false)) match {
                  case None =>
                    EitherT.leftT[F, MerkleTrieState[K, V]](
                      s"Fail to remove element: $key does not exist"
                    )
                  case Some(childHash) =>
                    removeByKey[F, K, V](key1) runS state.copy(root =
                      Some(childHash)
                    ) flatMap {
                      case childState
                          if childState.root.isEmpty && children.unsized
                            .count(_.nonEmpty) <= 1 =>
                        EitherT.rightT[F, String](
                          childState.copy(
                            root = None,
                            diff = childState.diff.remove(root),
                          )
                        )
                      case childState =>
                        val branch = MerkleTrieNode.branch[K, V](
                          prefix,
                          children.unsized
                            .updated(
                              index1.toInt(signed = false),
                              childState.root,
                            )
                            .ensureSized[_16],
                        )
                        val branchHash = branch.toHash

                        compact[F, K, V] runS state.copy(
                          root = Some(branchHash),
                          diff =
                            childState.diff.add(branchHash, branch).remove(root),
                        )
                    }
                }
              }
          }
      }
    )

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  def from[F[_]: Sync, K, V: ByteDecoder](key: BitVector)(implicit
      ns: NodeStore[F, K, V]
  ): StateT[EitherT[F, String, *], MerkleTrieState[K, V], Iterant[
    EitherT[F, String, *],
    (BitVector, V),
  ]] = {
    StateT.inspectF((state: MerkleTrieState[K, V]) =>
      state.root match {
        case None => EitherT.rightT[F, String](Iterant.empty)
        case Some(_) =>
          getNode[F, K, V](state).flatMap {
            case MerkleTrieNode.Leaf(prefix, value) =>
              if (key <= prefix.value)
                EitherT.fromEither[F](
                  ByteDecoder[V].decode(value).left.map(_.msg).flatMap {
                    case DecodeResult(v, remainder) if remainder.isEmpty =>
                      Right(Iterant.now((prefix.value, v)))
                    case result =>
                      Left(s"Decoding failure: nonEmpty remainder $result")
                  }
                )
              else EitherT.rightT[F, String](Iterant.empty)
            case MerkleTrieNode.Branch(prefix, children) =>
              def runFrom(key: BitVector)(
                  hashWithIndex: (Option[MerkleHash[K, V]], Int)
              ): EitherT[
                F,
                String,
                Iterant[EitherT[F, String, *], (BitVector, V)],
              ] = {
                from(key) runA state.copy(root = hashWithIndex._1) map (_.map {
                  case (key, a) =>
                    (
                      prefix.value ++ BitVector
                        .fromInt(hashWithIndex._2, 4) ++ key,
                      a,
                    )
                })
              }

              def flatten(
                  enums: List[Iterant[EitherT[F, String, *], (BitVector, V)]]
              ): Iterant[EitherT[F, String, *], (BitVector, V)] =
                enums.foldLeft(
                  Iterant.empty[EitherT[F, String, *], (BitVector, V)]
                )(_ ++ _)

              if (key <= prefix.value) {
                scribe.debug(
                  s"======>[Case #1] key: $key, prefix: ${prefix.value}"
                )
                children.unsized.toList.zipWithIndex traverse runFrom(
                  BitVector.empty
                ) map flatten
              } else if (
                prefix.value.nonEmpty && !key.startsWith(prefix.value)
              ) {
                scribe.debug(
                  s"======>[Casee #2] prefix: ${prefix.value}, key: $key"
                )
                EitherT.rightT[F, String](Iterant.empty)
              } else {
                val (index1, key1) = key drop prefix.value.size splitAt 4L
                scribe.debug(s"======>[Case #3] index1: $index1, key1: $key1")
                val targetChildren: List[(Option[MerkleHash[K, V]], Int)] =
                  children.unsized.toList.zipWithIndex
                    .drop(index1.toInt(signed = false))
                targetChildren match {
                  case Nil => EitherT.rightT[F, String](Iterant.empty)
                  case x :: xs =>
                    for {
                      headList <- runFrom(key1)(x)
                      tailList <- xs traverse runFrom(BitVector.empty)
                    } yield headList ++ flatten(tailList)
                }
              }
          }
      }
    )
  }

  def getNode[F[_]: Monad, K, V](state: MerkleTrieState[K, V])(implicit
      ns: NodeStore[F, K, V]
  ): EitherT[F, String, MerkleTrieNode[K, V]] = for {
    root <- EitherT.fromOption[F](
      state.root,
      s"Cannot get node from empty merkle trie: $state",
    )
    nodeOption <- EitherT.fromEither[F](
      Either.cond(
        !(state.diff.removal contains root),
        state.diff.addition.get(root),
        s"Merkle trie node is removed: $state",
      )
    )
    node <- nodeOption.fold[EitherT[F, String, MerkleTrieNode[K, V]]] {
      ns.run(root).subflatMap[String, MerkleTrieNode[K, V]] {
        _.toRight(s"Merkle trie node $root is not found: $state")
      }
    }(EitherT.rightT[F, String](_))
  } yield {
    scribe.debug(s"Accessing node ${state.root} -> $node")
    node
  }

  type PrefixBits = BitVector Refined MerkleTrieNode.PrefixCondition

  @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
  def ensurePrefix(bits: BitVector): PrefixBits = {
    refineV[MerkleTrieNode.PrefixCondition](bits).toOption.get
  }

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  def compact[F[_]: Monad, K, V](implicit
      ns: NodeStore[F, K, V]
  ): StateT[EitherT[F, String, *], MerkleTrieState[K, V], Unit] = {
    StateT.modifyF((state: MerkleTrieState[K, V]) =>
      state.root match {
        case None => EitherT.rightT[F, String](state)
        case Some(root) =>
          getNode[F, K, V](state).flatMap {
            case MerkleTrieNode.Branch(prefix, children) =>
              children.unsized.zipWithIndex.filter(_._1.nonEmpty) match {
                case Vector() =>
                  EitherT.rightT[F, String](
                    state.copy(
                      root = None,
                      diff = state.diff.remove(root),
                    )
                  )
                case Vector((Some(childHash), index)) =>
                  def getPrefix(local: PrefixBits): PrefixBits = ensurePrefix(
                    prefix.value ++ BitVector.fromInt(index, 4) ++ local.value
                  )
                  compact[F, K, V] runS state.copy(root =
                    Some(childHash)
                  ) flatMap { childState =>
                    getNode[F, K, V](childState).map {
                      case MerkleTrieNode.Leaf(prefix0, value) =>
                        val nextLeaf =
                          MerkleTrieNode.leaf[K, V](getPrefix(prefix0), value)
                        val nextLeafHash = nextLeaf.toHash
                        state.copy(
                          root = Some(nextLeafHash),
                          diff = childState.diff
                            .add(nextLeafHash, nextLeaf)
                            .remove(root),
                        )
                      case MerkleTrieNode.Branch(prefix0, children) =>
                        val nextBranch =
                          MerkleTrieNode.branch(getPrefix(prefix0), children)
                        val nextBranchHash = nextBranch.toHash
                        state.copy(
                          root = Some(nextBranchHash),
                          diff = childState.diff
                            .add(nextBranchHash, nextBranch)
                            .remove(root),
                        )
                    }
                  }
                case _ =>
                  EitherT.rightT[F, String](state)
              }
            case _ => EitherT.rightT[F, String](state)
          }
      }
    )
  }

  implicit class BitVectorCompare(val bits: BitVector) extends AnyVal {
    def <=(that: BitVector): Boolean = {
      val thisBytes = bits.bytes
      val thatBytes = that.bytes
      val minSize   = thisBytes.size min thatBytes.size

      (0L until minSize)
        .find { i =>
          thisBytes.get(i) =!= thatBytes.get(i)
        }
        .fold(bits.size <= that.size) { i =>
          thisBytes.get(i) <= thatBytes.get(i)
        }
    }
  }

  type MerkleHash[K, V] = MerkleTrieNode.MerkleHash[K, V]
  type MerkleRoot[K, V] = MerkleTrieNode.MerkleRoot[K, V]

  final case class MerkleTrieState[K, V](
      root: Option[MerkleRoot[K, V]],
      base: Option[MerkleRoot[K, V]],
      diff: MerkleTrieStateDiff[K, V],
  ) {
    def rebase(that: MerkleTrieState[K, V]): Either[String, MerkleTrieState[K, V]] = {
      if (that.base != base) Left(s"Different base")
      else if ((that.diff.removal -- diff.removal).nonEmpty) {
        Left("Try to add already removed node")
      } else {
        val addtion = diff.addition -- that.diff.addition.keySet
        val removal = diff.removal -- that.diff.removal
        Right(this.copy(
          base = that.root,
          diff = MerkleTrieStateDiff(addtion, removal),
        ))
      }
    }
  }
  final case class MerkleTrieStateDiff[K, V](
      addition: Map[MerkleHash[K, V], MerkleTrieNode[K, V]],
      removal: Set[MerkleHash[K, V]],
  ) {
    def add(
        hash: MerkleHash[K, V],
        node: MerkleTrieNode[K, V],
    ): MerkleTrieStateDiff[K, V] =
      this.copy(addition = addition.updated(hash, node))
    def remove(hash: MerkleHash[K, V]): MerkleTrieStateDiff[K, V] =
      if (addition contains hash) this.copy(addition = addition - hash)
      else this.copy(removal = removal + hash)
  }
  object MerkleTrieState {
    def empty[K, V]: MerkleTrieState[K, V] = MerkleTrieState(
      None,
      None,
      MerkleTrieStateDiff[K, V](Map.empty, Set.empty),
    )
    def fromRoot[K, V](root: MerkleRoot[K, V]): MerkleTrieState[K, V] =
      MerkleTrieState[K, V](
        root = Some(root),
        base = Some(root),
        diff = MerkleTrieStateDiff[K, V](Map.empty, Set.empty),
      )
  }
}
