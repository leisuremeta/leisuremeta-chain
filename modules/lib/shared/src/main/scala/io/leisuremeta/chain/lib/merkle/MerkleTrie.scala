package io.leisuremeta.chain.lib
package merkle

import cats.Monad
import cats.data.{EitherT, Kleisli, StateT}
import cats.syntax.eq.given
import cats.syntax.flatMap.given
import cats.syntax.functor.given
import cats.syntax.traverse.given

import eu.timepit.refined.api.Refined
import eu.timepit.refined.refineV
import fs2.Stream
import scodec.bits.{BitVector, ByteVector}

import codec.byte.{ByteDecoder, ByteEncoder, DecodeResult}
import codec.byte.ByteEncoder.ops.*
import crypto.Hash.ops.*
import MerkleTrieNode.{Children, ChildrenCondition, MerkleHash}

import util.refined.bitVector.given

object MerkleTrie:

  type NodeStore[F[_], K, V] =
    Kleisli[EitherT[F, String, *], MerkleHash[K, V], Option[
      MerkleTrieNode[K, V],
    ]]

  @SuppressWarnings(
    Array(
      "org.wartremover.warts.Any",
      "org.wartremover.warts.Nothing",
      "org.wartremover.warts.Recursion",
    ),
  )
  def get[F[_]: Monad, K, V: ByteDecoder](key: BitVector)(implicit
      ns: NodeStore[F, K, V],
  ): StateT[EitherT[F, String, *], MerkleTrieState[K, V], Option[V]] =
    StateT.inspectF { (state: MerkleTrieState[K, V]) =>
      if state.root.isEmpty then EitherT.rightT[F, String](None)
      else
        for
          node <- getNode[F, K, V](state)
          valueOption <-
            if node.prefix.value === key then
              node.getValue match
                case None => EitherT.pure[F, String](Option.empty[V])
                case Some(bytes) =>
                  EitherT.fromEither[F] {
                    ByteDecoder[V].decode(bytes).left.map(_.msg).flatMap {
                      case DecodeResult(value, remainder)
                          if remainder.isEmpty =>
                        Right(Some(value))
                      case result =>
                        Left(s"Non empty remainder after decoding: $result")
                    }
                  }
            else if (key startsWith node.prefix.value) && key.size >= node.prefix.value.size + 4
            then
              val (index, nextKey) = key.drop(node.prefix.value.size).splitAt(4)
              node.getChildren.flatMap(
                _.value(index.toInt(signed = false)),
              ) match
                case Some(nextRoot) =>
                  get[F, K, V](nextKey) runA state.copy(root = Some(nextRoot))
                case None =>
                  EitherT.rightT[F, String](None)
            else EitherT.rightT[F, String](None)
        yield valueOption
    }

  @SuppressWarnings(
    Array(
      "org.wartremover.warts.Equals",
      "org.wartremover.warts.OptionPartial",
      "org.wartremover.warts.Nothing",
      "org.wartremover.warts.Recursion",
    ),
  )
  def put[F[_]: Monad, K, V: ByteEncoder](key: BitVector, value: V)(implicit
      ns: NodeStore[F, K, V],
  ): StateT[EitherT[F, String, *], MerkleTrieState[K, V], Unit] =
    StateT.modifyF((state: MerkleTrieState[K, V]) =>
      state.root match
        case None =>
          val leaf = MerkleTrieNode
            .leaf[K, V](ensurePrefix(key), value.toBytes)
          val leafHash = leaf.toHash
          EitherT.rightT[F, String](
            state.copy(
              root = Some(leafHash),
              diff = state.diff.add(leafHash, leaf),
            ),
          )
        case Some(root) =>
          getNode[F, K, V](state).flatMap { (node: MerkleTrieNode[K, V]) =>

            val prefix0: BitVector = node.prefix.value

            node match
              case MerkleTrieNode.Leaf(_, value0) =>
                val (commonPrefix, remainder0, remainder1) =
                  getCommonPrefixNibbleAndRemainders(prefix0, key)
                (remainder0.nonEmpty, remainder1.nonEmpty) match
                  case (false, false) =>
                    if value0 === value.toBytes then
                      EitherT.rightT[F, String](state)
                    else
                      val leaf1 = MerkleTrieNode
                        .leaf[K, V](ensurePrefix(prefix0), value.toBytes)
                      val leaf1Hash = leaf1.toHash
                      EitherT.rightT[F, String](
                        state.copy(
                          root = Some(leaf1Hash),
                          diff = state.diff
                            .remove(root, node)
                            .add(leaf1Hash, leaf1),
                        ),
                      )
                  case (false, true) =>
                    val (index10, prefix10) = remainder1.splitAt(4)
                    val leaf1 = MerkleTrieNode
                      .leaf[K, V](ensurePrefix(prefix10), value.toBytes)
                    val leaf1Hash = leaf1.toHash
                    val children: Children[K, V] = Children
                      .empty[K, V]
                      .updated(index10.toInt(signed = false), Some(leaf1Hash))
                    val branch = MerkleTrieNode.branchWithData(
                      node.prefix,
                      children,
                      value0,
                    )
                    val branchHash = branch.toHash
                    EitherT.rightT[F, String](
                      state.copy(
                        root = Some(branchHash),
                        diff = state.diff
                          .remove(root, node)
                          .add(branchHash, branch)
                          .add(leaf1Hash, leaf1),
                      ),
                    )
                  case (true, false) =>
                    val (index00, prefix00) = remainder0.splitAt(4)
                    val leaf0 = MerkleTrieNode
                      .leaf[K, V](ensurePrefix(prefix00), value0)
                    val leaf0Hash = leaf0.toHash
                    val children: Children[K, V] = Children
                      .empty[K, V]
                      .updated(index00.toInt(signed = false), Some(leaf0Hash))
                    val branch = MerkleTrieNode.branchWithData(
                      ensurePrefix(commonPrefix),
                      children,
                      value.toBytes,
                    )
                    val branchHash = branch.toHash
                    EitherT.rightT[F, String](
                      state.copy(
                        root = Some(branchHash),
                        diff = state.diff
                          .remove(root, node)
                          .add(branchHash, branch)
                          .add(leaf0Hash, leaf0),
                      ),
                    )
                  case (true, true) =>
                    val (index00, prefix00) = remainder0.splitAt(4)
                    val leaf0 = MerkleTrieNode
                      .leaf[K, V](ensurePrefix(prefix00), value0)
                    val leaf0Hash           = leaf0.toHash
                    val (index10, prefix10) = remainder1.splitAt(4)
                    val leaf1 = MerkleTrieNode
                      .leaf[K, V](ensurePrefix(prefix10), value.toBytes)
                    val leaf1Hash = leaf1.toHash
                    val children: Children[K, V] = Children
                      .empty[K, V]
                      .updated(index00.toInt(signed = false), Some(leaf0Hash))
                      .updated(index10.toInt(signed = false), Some(leaf1Hash))
                    val branch = MerkleTrieNode.branch(
                      ensurePrefix(commonPrefix),
                      children,
                    )
                    val branchHash = branch.toHash
                    EitherT.rightT[F, String](
                      state.copy(
                        root = Some(branchHash),
                        diff = state.diff
                          .remove(root, node)
                          .add(branchHash, branch)
                          .add(leaf0Hash, leaf0)
                          .add(leaf1Hash, leaf1),
                      ),
                    )
              case _ =>
                val children     = node.getChildren.get
                val valueOption0 = node.getValue
                val (commonPrefix, remainder0, remainder1) =
                  getCommonPrefixNibbleAndRemainders(prefix0, key)
                (remainder0.nonEmpty, remainder1.nonEmpty) match
                  case (false, false) =>
                    if valueOption0 == Some(value.toBytes) then
                      EitherT.rightT[F, String](state)
                    else
                      val branch1     = node.setValue(value.toBytes)
                      val branch1Hash = branch1.toHash
                      EitherT.rightT[F, String](
                        state.copy(
                          root = Some(branch1Hash),
                          diff = state.diff
                            .remove(root, node)
                            .add(branch1Hash, branch1),
                        ),
                      )
                  case (false, true) =>
                    val (index10, prefix10) = remainder1.splitAt(4)
                    children.value(index10.toInt(signed = false)) match
                      case None =>
                        val leaf1 = MerkleTrieNode
                          .leaf[K, V](ensurePrefix(prefix10), value.toBytes)
                        val leaf1Hash = leaf1.toHash
                        val children1 = refineV[ChildrenCondition] {
                          children.value.updated(
                            index10.toInt(signed = false),
                            Some(leaf1Hash),
                          )
                        }.toOption.get
                        val branch1     = node.setChildren(children1)
                        val branch1Hash = branch1.toHash
                        EitherT.rightT[F, String](
                          state.copy(
                            root = Some(branch1Hash),
                            diff = state.diff
                              .remove(root, node)
                              .add(branch1Hash, branch1)
                              .add(leaf1Hash, leaf1),
                          ),
                        )
                      case Some(childHash) =>
                        put[F, K, V](prefix10, value)
                          .runS(state.copy(root = Some(childHash)))
                          .map { childState =>
//                            println(s"======> Child state: $childState")
                            val children1 = children.updated(
                              index10.toInt(signed = false),
                              childState.root,
                            )
                            val branch1     = node.setChildren(children1)
                            val branch1Hash = branch1.toHash
                            childState.copy(
                              root = Some(branch1Hash),
                              diff = childState.diff
                                .remove(root, node)
                                .add(branch1Hash, branch1),
                            )
                          }
                  case (true, false) =>
                    val (index00, prefix00) = remainder0.splitAt(4)
                    val child0     = node.setPrefix(ensurePrefix(prefix00))
                    val child0Hash = child0.toHash
                    val children1 = MerkleTrieNode.Children.empty.updated(
                      index00.toInt(signed = false),
                      Some(child0Hash),
                    )
                    val branch1 = MerkleTrieNode.branchWithData(
                      ensurePrefix(commonPrefix),
                      children1,
                      value.toBytes,
                    )
                    val branch1Hash = branch1.toHash
                    EitherT.rightT[F, String](
                      state.copy(
                        root = Some(branch1Hash),
                        diff = state.diff
                          .remove(root, node)
                          .add(branch1Hash, branch1)
                          .add(child0Hash, child0),
                      ),
                    )
                  case (true, true) =>
                    val (index00, prefix00) = remainder0.splitAt(4)
                    val (index10, prefix10) = remainder1.splitAt(4)
                    val child0     = node.setPrefix(ensurePrefix(prefix00))
                    val child0Hash = child0.toHash
                    val child1 = MerkleTrieNode
                      .leaf[K, V](ensurePrefix(prefix10), value.toBytes)
                    val child1Hash = child1.toHash
                    val children1 = Children
                      .empty[K, V]
                      .updated(index00.toInt(signed = false), Some(child0Hash))
                      .updated(index10.toInt(signed = false), Some(child1Hash))
                    val branch1 = MerkleTrieNode.branch(
                      ensurePrefix(commonPrefix),
                      children1,
                    )
                    val branch1Hash = branch1.toHash
                    EitherT.rightT[F, String](
                      state.copy(
                        root = Some(branch1Hash),
                        diff = state.diff
                          .remove(root, node)
                          .add(branch1Hash, branch1)
                          .add(child0Hash, child0)
                          .add(child1Hash, child1),
                      ),
                    )

          },
    )

  @SuppressWarnings(
    Array(
      "org.wartremover.warts.NonUnitStatements",
      "org.wartremover.warts.OptionPartial",
      "org.wartremover.warts.Recursion",
    ),
  )
  def remove[F[_]: Monad, K, V](key: BitVector)(implicit
      ns: NodeStore[F, K, V],
  ): StateT[EitherT[F, String, *], MerkleTrieState[K, V], Boolean] =
    StateT((state: MerkleTrieState[K, V]) =>
      state.root match
        case None =>
          EitherT.pure[F, String]((state, false))
        case Some(root) =>
          getNode[F, K, V](state).flatMap { node =>
            node match
              case MerkleTrieNode.Leaf(prefix, _) if prefix.value === key =>
                EitherT.rightT[F, String](
                  (
                    state.copy(
                      root = None,
                      diff = state.diff.remove(root, node),
                    ),
                    true,
                  ),
                )
              case MerkleTrieNode.Leaf(_, _) =>
                EitherT.pure[F, String]((state, false))
              case MerkleTrieNode.BranchWithData(prefix, children, value)
                  if prefix.value === key =>
                val branch1: MerkleTrieNode[K, V] =
                  MerkleTrieNode.Branch(prefix, children)
                val branch1Hash = branch1.toHash

                EitherT.rightT[F, String](
                  (
                    state.copy(
                      root = Some(branch1Hash),
                      diff =
                        state.diff.remove(root, node).add(branch1Hash, branch1),
                    ),
                    true,
                  ),
                )
              case _ if !key.startsWith(node.prefix.value) =>
                EitherT.pure[F, String]((state, false))
              case _ =>
                val prefix      = node.prefix
                val children    = node.getChildren.get
                val valueOption = node.getValue

                val remainder1 =
                  getCommonPrefixNibbleAndRemainders(prefix.value, key)._3

                if remainder1.size < 4L then
                  EitherT.pure[F, String]((state, false))
                else
                  val (index1, key1) = remainder1 splitAt 4L
                  children.value(index1.toInt(signed = false)) match
                    case None =>
                      EitherT.pure[F, String]((state, false))
                    case Some(childHash) =>
                      remove[F, K, V](key1) run state.copy(root =
                        Some(childHash),
                      ) flatMap {
                        case (_, false) =>
                          EitherT.pure[F, String]((state, false))
                        case (childState, true)
                            if childState.root.isEmpty
                              && children.value.count(_.nonEmpty) <= 1
                              && node.getValue.isEmpty =>
                          EitherT.rightT[F, String](
                            (
                              childState.copy(
                                root = None,
                                diff = childState.diff.remove(root, node),
                              ),
                              true,
                            ),
                          )
                        case (childState, true) =>
                          val children1 = refineV[ChildrenCondition](
                            children.value.updated(
                              index1.toInt(signed = false),
                              childState.root,
                            ),
                          ).toOption.get
                          val branch = valueOption.fold(
                            MerkleTrieNode.branch(prefix, children1),
                          )(MerkleTrieNode.branchWithData(prefix, children1, _))
                          val branchHash = branch.toHash

                          EitherT.rightT[F, String](
                            (
                              childState.copy(
                                root = Some(branchHash),
                                diff = childState.diff
                                  .remove(root, node)
                                  .add(branchHash, branch),
                              ),
                              true,
                            ),
                          )
                      }
          },
    )

  @SuppressWarnings(
    Array(
      "org.wartremover.warts.Nothing",
      "org.wartremover.warts.OptionPartial",
      "org.wartremover.warts.Recursion",
    ),
  )
  def from[F[_]: Monad, K, V: ByteDecoder](key: BitVector)(implicit
      ns: NodeStore[F, K, V],
  ): StateT[EitherT[F, String, *], MerkleTrieState[K, V], Stream[
    EitherT[F, String, *],
    (BitVector, V),
  ]] =
    StateT.inspectF((state: MerkleTrieState[K, V]) =>
      state.root match
        case None => EitherT.rightT[F, String](Stream.empty)
        case Some(_) =>
          getNode[F, K, V](state).flatMap {
            case MerkleTrieNode.Leaf(prefix, value) =>
              if key <= prefix.value then
                EitherT.fromEither[F](
                  ByteDecoder[V].decode(value).left.map(_.msg).flatMap {
                    case DecodeResult(v, remainder) if remainder.isEmpty =>
                      Right(Stream.emit((prefix.value, v)))
                    case result =>
                      Left(s"Decoding failure: nonEmpty remainder $result")
                  },
                )
              else EitherT.rightT[F, String](Stream.empty)
            case node =>
              val prefix      = node.prefix
              val children    = node.getChildren.get
              val valueOption = node.getValue

              def runFrom(key: BitVector)(
                  hashWithIndex: (Option[MerkleHash[K, V]], Int),
              ): EitherT[
                F,
                String,
                Stream[EitherT[F, String, *], (BitVector, V)],
              ] =
                from(key) runA state.copy(root = hashWithIndex._1) map (_.map {
                  case (key, a) =>
                    (
                      prefix.value ++ BitVector
                        .fromInt(hashWithIndex._2, 4) ++ key,
                      a,
                    )
                })

              def flatten(
                  enums: List[Stream[EitherT[F, String, *], (BitVector, V)]],
              ): Stream[EitherT[F, String, *], (BitVector, V)] =
                enums.foldLeft[Stream[EitherT[F, String, *], (BitVector, V)]](
                  Stream.empty,
                )(_ ++ _)

              if key <= prefix.value then
                scribe.debug(
                  s"======>[Case #1] key: $key, prefix: ${prefix.value}",
                )
                val initialValue
                    : Stream[EitherT[F, String, *], (BitVector, V)] =
                  valueOption match
                    case None =>
                      Stream.empty[EitherT[F, String, *]]
                    case Some(bytes) =>
                      Stream.eval(EitherT.fromEither {
                        ByteDecoder[V].decode(bytes).left.map(_.msg).flatMap {
                          case DecodeResult(v, remainder)
                              if remainder.isEmpty =>
                            Right((prefix.value, v))
                          case result =>
                            Left(
                              s"Decoding failure: nonEmpty remainder $result",
                            )
                        }
                      })
                children.value.toList.zipWithIndex
                  .traverse(runFrom(BitVector.empty))
                  .map(flatten)
                  .map(initialValue ++ _)
              else if prefix.value.nonEmpty && !key.startsWith(prefix.value)
              then
                scribe.debug(
                  s"======>[Case #2] prefix: ${prefix.value}, key: $key",
                )
                EitherT.rightT[F, String](Stream.empty)
              else
                val (index1, key1) = key drop prefix.value.size splitAt 4L
                scribe.debug(s"======>[Case #3] index1: $index1, key1: $key1")
                val targetChildren: List[(Option[MerkleHash[K, V]], Int)] =
                  children.value.toList.zipWithIndex
                    .drop(index1.toInt(signed = false))
                targetChildren match
                  case Nil => EitherT.rightT[F, String](Stream.empty)
                  case x :: xs =>
                    for
                      headList <- runFrom(key1)(x)
                      tailList <- xs traverse runFrom(BitVector.empty)
                    yield headList ++ flatten(tailList)
          },
    )

  def getNode[F[_]: Monad, K, V](state: MerkleTrieState[K, V])(implicit
      ns: NodeStore[F, K, V],
  ): EitherT[F, String, MerkleTrieNode[K, V]] = for
    root <- EitherT.fromOption[F](
      state.root,
      s"Cannot get node from empty merkle trie: $state",
    )
    nodeOption = state.diff.get(root)
    node <- nodeOption.fold[EitherT[F, String, MerkleTrieNode[K, V]]] {
      ns.run(root).subflatMap[String, MerkleTrieNode[K, V]] {
        _.toRight(s"Merkle trie node $root is not found: $state")
      }
    }(EitherT.rightT[F, String](_))
  yield
    scribe.debug(s"Accessing node ${state.root} -> $node")
    node

  private def getCommonPrefixNibbleAndRemainders(
      bits0: BitVector,
      bits1: BitVector,
  ): (BitVector, BitVector, BitVector) =
    val commonPrefixNibbleSize: Int = (bits0 ^ bits1)
      .grouped(4L)
      .takeWhile(_ === BitVector.low(4L))
      .size
    val nextPrefixBitSize          = commonPrefixNibbleSize.toLong * 4L
    val remainder0                 = bits0 drop nextPrefixBitSize
    val (commonPrefix, remainder1) = bits1 splitAt nextPrefixBitSize
    (commonPrefix, remainder0, remainder1)

  type PrefixBits = BitVector Refined MerkleTrieNode.PrefixCondition

  @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
  def ensurePrefix(bits: BitVector): PrefixBits =
    refineV[MerkleTrieNode.PrefixCondition](bits).toOption.get

  extension (bits: BitVector)
    def <=(that: BitVector): Boolean =
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

end MerkleTrie
