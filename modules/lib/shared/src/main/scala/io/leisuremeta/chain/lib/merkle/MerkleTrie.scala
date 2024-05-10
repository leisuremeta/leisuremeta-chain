package io.leisuremeta.chain.lib
package merkle

import cats.Monad
import cats.data.{EitherT, Kleisli, OptionT, StateT}
import cats.syntax.either.given
import cats.syntax.eq.given
import cats.syntax.traverse.given

import io.github.iltotore.iron.*
import fs2.Stream
import scodec.bits.{BitVector, ByteVector}

import crypto.Hash.ops.*
import MerkleTrieNode.{Children, MerkleHash}

object MerkleTrie:

  type NodeStore[F[_]] =
    Kleisli[EitherT[F, String, *], MerkleHash, Option[MerkleTrieNode]]

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  def get[F[_]: Monad: NodeStore](
      key: Nibbles,
  ): StateT[EitherT[F, String, *], MerkleTrieState, Option[ByteVector]] =
    StateT.inspectF: (state: MerkleTrieState) =>
      val optionT = for
        node <- OptionT(getNode[F](state))
        stripped <- OptionT.fromOption[EitherT[F, String, *]]:
          key.stripPrefix(node.prefix)
        value <- stripped.unCons
          .fold:
            OptionT.fromOption[EitherT[F, String, *]](node.getValue)
          .apply: (head, remainder) =>
            for
              children <- OptionT.fromOption[EitherT[F, String, *]](
                node.getChildren,
              )
              nextRoot <- OptionT.fromOption[EitherT[F, String, *]]:
                children(head)
              value <- OptionT:
                get[F](remainder.assumeNibbles).runA(
                  state.copy(root = Some(nextRoot)),
                )
            yield value
      yield value

      optionT.value

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  def put[F[_]: Monad: NodeStore](
      key: Nibbles,
      value: ByteVector,
  ): StateT[EitherT[F, String, *], MerkleTrieState, Unit] =
    StateT.modifyF: (state: MerkleTrieState) =>
      getNodeAndStateRoot[F](state).flatMap:
        case None =>
          val leaf     = MerkleTrieNode.leaf(key, value)
          val leafHash = leaf.toHash
          EitherT.rightT[F, String]:
            state.copy(
              root = Some(leafHash),
              diff = state.diff.add(leafHash, leaf),
            )
        case Some((node, root)) =>
          val prefix0: Nibbles = node.prefix

          def putLeaf(value0: ByteVector): EitherT[F, String, MerkleTrieState] =
            val (commonPrefix, remainder0, remainder1) =
              getCommonPrefixNibbleAndRemainders(prefix0, key)
            (remainder0.unCons, remainder1.unCons) match
              case (None, None) =>
                if value0 === value then EitherT.rightT[F, String](state)
                else
                  val leaf1     = MerkleTrieNode.leaf(prefix0, value)
                  val leaf1Hash = leaf1.toHash
                  EitherT.rightT[F, String]:
                    state.copy(
                      root = Some(leaf1Hash),
                      diff = state.diff
                        .remove(root, node)
                        .add(leaf1Hash, leaf1),
                    )
              case (None, Some((index10, prefix10))) =>
                val leaf1 = MerkleTrieNode.leaf(prefix10.assumeNibbles, value)
                val leaf1Hash = leaf1.toHash
                val children: Children = Children.empty
                  .updateChild(index10, Some(leaf1Hash))
                val branch = MerkleTrieNode.branchWithData(
                  node.prefix,
                  children,
                  value0,
                )
                val branchHash = branch.toHash
                EitherT.rightT[F, String]:
                  state.copy(
                    root = Some(branchHash),
                    diff = state.diff
                      .remove(root, node)
                      .add(branchHash, branch)
                      .add(leaf1Hash, leaf1),
                  )
              case (Some((index00, prefix00)), None) =>
                val leaf0 = MerkleTrieNode.leaf(prefix00.assumeNibbles, value0)
                val leaf0Hash = leaf0.toHash
                val children: Children = Children.empty
                  .updateChild(index00, Some(leaf0Hash))
                val branch = MerkleTrieNode.branchWithData(
                  commonPrefix,
                  children,
                  value,
                )
                val branchHash = branch.toHash
                EitherT.rightT[F, String]:
                  state.copy(
                    root = Some(branchHash),
                    diff = state.diff
                      .remove(root, node)
                      .add(branchHash, branch)
                      .add(leaf0Hash, leaf0),
                  )
              case (Some((index00, prefix00)), Some((index10, prefix10))) =>
                val leaf0 = MerkleTrieNode.leaf(prefix00.assumeNibbles, value0)
                val leaf0Hash = leaf0.toHash
                val leaf1 = MerkleTrieNode.leaf(prefix10.assumeNibbles, value)
                val leaf1Hash = leaf1.toHash
                val children: Children = Children.empty
                  .updateChild(index00, Some(leaf0Hash))
                  .updateChild(index10, Some(leaf1Hash))
                val branch = MerkleTrieNode.branch(
                  commonPrefix,
                  children,
                )
                val branchHash = branch.toHash
                EitherT.rightT[F, String]:
                  state.copy(
                    root = Some(branchHash),
                    diff = state.diff
                      .remove(root, node)
                      .add(branchHash, branch)
                      .add(leaf0Hash, leaf0)
                      .add(leaf1Hash, leaf1),
                  )
          def putNode(
              children: MerkleTrieNode.Children,
          ): EitherT[F, String, MerkleTrieState] =
            val (commonPrefix, remainder0, remainder1) =
              getCommonPrefixNibbleAndRemainders(prefix0, key)
            (remainder0.unCons, remainder1.unCons) match
              case (None, None) =>
                node.getValue match
                  case Some(nodeValue) if nodeValue === value =>
                    EitherT.rightT[F, String](state)
                  case _ =>
                    val branch1     = node.setValue(value)
                    val branch1Hash = branch1.toHash
                    EitherT.rightT[F, String]:
                      state.copy(
                        root = Some(branch1Hash),
                        diff = state.diff
                          .remove(root, node)
                          .add(branch1Hash, branch1),
                      )
              case (None, Some((index10, prefix10))) =>
                children(index10) match
                  case None =>
                    val leaf1 =
                      MerkleTrieNode.leaf(prefix10.assumeNibbles, value)
                    val leaf1Hash = leaf1.toHash
                    val children1 =
                      children.updateChild(index10, Some(leaf1Hash))
                    val branch1     = node.setChildren(children1)
                    val branch1Hash = branch1.toHash
                    EitherT.rightT[F, String]:
                      state.copy(
                        root = Some(branch1Hash),
                        diff = state.diff
                          .remove(root, node)
                          .add(branch1Hash, branch1)
                          .add(leaf1Hash, leaf1),
                      )
                  case Some(childHash) =>
                    put[F](prefix10.assumeNibbles, value)
                      .runS(state.copy(root = Some(childHash)))
                      .map: childState =>
//                      println(s"======> Child state: $childState")
                        val children1 = children
                          .updateChild(index10, childState.root)
                        val branch1     = node.setChildren(children1)
                        val branch1Hash = branch1.toHash
                        childState.copy(
                          root = Some(branch1Hash),
                          diff = childState.diff
                            .remove(root, node)
                            .add(branch1Hash, branch1),
                        )
              case (Some((index00, prefix00)), None) =>
                val child0     = node.setPrefix(prefix00.assumeNibbles)
                val child0Hash = child0.toHash
                val children1 = MerkleTrieNode.Children.empty
                  .updateChild(index00, Some(child0Hash))
                val branch1 = MerkleTrieNode.branchWithData(
                  commonPrefix,
                  children1,
                  value,
                )
                val branch1Hash = branch1.toHash
                EitherT.rightT[F, String]:
                  state.copy(
                    root = Some(branch1Hash),
                    diff = state.diff
                      .remove(root, node)
                      .add(branch1Hash, branch1)
                      .add(child0Hash, child0),
                  )
              case (Some((index00, prefix00)), Some((index10, prefix10))) =>
                val child0     = node.setPrefix(prefix00.assumeNibbles)
                val child0Hash = child0.toHash
                val child1 = MerkleTrieNode.leaf(prefix10.assumeNibbles, value)
                val child1Hash = child1.toHash
                val children1 = Children.empty
                  .updateChild(index00, Some(child0Hash))
                  .updateChild(index10, Some(child1Hash))
                val branch1 = MerkleTrieNode.branch(
                  commonPrefix,
                  children1,
                )
                val branch1Hash = branch1.toHash
                EitherT.rightT[F, String]:
                  state.copy(
                    root = Some(branch1Hash),
                    diff = state.diff
                      .remove(root, node)
                      .add(branch1Hash, branch1)
                      .add(child0Hash, child0)
                      .add(child1Hash, child1),
                  )

          node match
            case MerkleTrieNode.Leaf(_, value0) =>
              putLeaf(value0)
            case MerkleTrieNode.Branch(_, children) =>
              putNode(children)
            case MerkleTrieNode.BranchWithData(_, children, _) =>
              putNode(children)

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  def remove[F[_]: Monad: NodeStore](
      key: Nibbles,
  ): StateT[EitherT[F, String, *], MerkleTrieState, Boolean] =
    StateT: (state: MerkleTrieState) =>
      val RightFalse = EitherT.pure[F, String]((state, false))
      getNodeAndStateRoot[F](state).flatMap:
        case None => RightFalse
        case Some((node, root)) =>
          node match
            case MerkleTrieNode.Leaf(prefix, _) if prefix === key =>
              val state1 =
                state.copy(root = None, diff = state.diff.remove(root, node))
              EitherT.rightT[F, String]((state1, true))
            case MerkleTrieNode.Leaf(_, _) => RightFalse

            case MerkleTrieNode.BranchWithData(prefix, children, value)
                if prefix === key =>
              val branch1: MerkleTrieNode =
                MerkleTrieNode.Branch(prefix, children)
              val branch1Hash = branch1.toHash

              val state1 = state.copy(
                root = Some(branch1Hash),
                diff = state.diff.remove(root, node).add(branch1Hash, branch1),
              )

              EitherT.rightT[F, String]((state1, true))
            case _ =>
              key
                .stripPrefix(node.prefix)
                .fold(RightFalse): stripped =>
                  stripped.unCons.fold(RightFalse): (index1, key1) =>
                    EitherT
                      .fromOption(node.getChildren, s"No children for $node")
                      .flatMap: children =>
                        children(index1).fold(RightFalse): childHash =>
                          remove[F](key1)
                            .run(state.copy(root = Some(childHash)))
                            .subflatMap:
                              case (_, false) =>
                                ((state, false)).asRight[String]
                              case (childState, true)
                                  if childState.root.isEmpty
                                    && children.count(_.nonEmpty) <= 1
                                    && node.getValue.isEmpty =>
                                val childState1 = childState.copy(
                                  root = None,
                                  diff = childState.diff.remove(root, node),
                                )
                                ((childState1, true)).asRight[String]
                              case (childState, true) =>
                                val children1 = children
                                  .updateChild(index1, childState.root)
                                val branch = node.getValue match
                                  case None =>
                                    MerkleTrieNode.branch(
                                      node.prefix,
                                      children1,
                                    )
                                  case Some(value) =>
                                    MerkleTrieNode.branchWithData(
                                      node.prefix,
                                      children1,
                                      value,
                                    )
                                val branchHash = branch.toHash
                                val childState1 = childState.copy(
                                  root = Some(branchHash),
                                  diff = childState.diff
                                    .remove(root, node)
                                    .add(branchHash, branch),
                                )
                                (childState1, true).asRight[String]

  type ByteStream[F[_]] = Stream[EitherT[F, String, *], (Nibbles, ByteVector)]

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  def streamFrom[F[_]: Monad: NodeStore](
      key: Nibbles,
  ): StateT[EitherT[F, String, *], MerkleTrieState, ByteStream[F]] =
    StateT.inspectF: (state: MerkleTrieState) =>
      scribe.debug(s"from: $key, $state")
      getNode[F](state).flatMap:
        case None => EitherT.rightT[F, String](Stream.empty)
        case Some(node) =>
          node match
            case MerkleTrieNode.Leaf(prefix, value) =>
              scribe.debug(s"Leaf: $key <= $prefix: ${key <= prefix}")
              if key <= prefix then EitherT.pure(Stream.emit((prefix, value)))
              else EitherT.rightT[F, String](Stream.empty)
            case node =>
              val prefix      = node.prefix
              val valueOption = node.getValue

              def runFrom(key: Nibbles)(
                  hashWithIndex: (Option[MerkleHash], Int),
              ): EitherT[F, String, ByteStream[F]] =
                streamFrom(key)
                  .runA(state.copy(root = hashWithIndex._1))
                  .map:
                    _.map: (key, a) =>
                      val key1 = prefix.value
                        ++ BitVector.fromInt(hashWithIndex._2, 4) ++ key.value
                      (key1.assumeNibbles, a)

              def flatten(enums: List[ByteStream[F]]): ByteStream[F] =
                enums.foldLeft[ByteStream[F]](Stream.empty)(_ ++ _)

              EitherT
                .fromOption(node.getChildren, s"No children for $node")
                .flatMap: children =>
                  if key <= prefix then
                    scribe.debug:
                      s"======>[Case #1] key: $key, prefix: ${prefix.value}"
                    val initialValue: ByteStream[F] = valueOption match
                      case None =>
                        Stream.empty
                      case Some(bytes) =>
                        Stream.eval(EitherT.pure((prefix, bytes)))
                    children.toList.zipWithIndex
                      .traverse(runFrom(BitVector.empty.assumeNibbles))
                      .map(flatten)
                      .map(initialValue ++ _)
                  else if prefix.value.nonEmpty &&
                    !key.value.startsWith(prefix.value)
                  then
                    scribe.debug:
                      s"======>[Case #2] prefix: ${prefix.value}, key: $key"
                    EitherT.rightT[F, String](Stream.empty)
                  else
                    val (index1, key1) =
                      key.value drop prefix.value.size splitAt 4L
                    scribe.debug:
                      s"======>[Case #3] index1: $index1, key1: $key1"
                    val targetChildren: List[(Option[MerkleHash], Int)] =
                      children.toList.zipWithIndex
                        .drop(index1.toInt(signed = false))
                    targetChildren match
                      case Nil => EitherT.rightT[F, String](Stream.empty)
                      case x :: xs =>
                        for
                          headList <- runFrom(key1.assumeNibbles)(x)
                          tailList <- xs.traverse:
                            runFrom(BitVector.empty.assumeNibbles)
                        yield headList ++ flatten(tailList)

  def getNodeAndStateRoot[F[_]: Monad](state: MerkleTrieState)(using
      ns: NodeStore[F],
  ): EitherT[F, String, Option[(MerkleTrieNode, MerkleHash)]] =
    state.root.fold(EitherT.rightT[F, String](None)): root =>
      state.diff
        .get(root)
        .fold(ns.run(root).map(_.map((_, root)))): node =>
          EitherT.rightT[F, String](Some((node, root)))

  def getNode[F[_]: Monad](state: MerkleTrieState)(using
      ns: NodeStore[F],
  ): EitherT[F, String, Option[MerkleTrieNode]] =
    state.root.fold(EitherT.rightT[F, String](None)): root =>
      state.diff
        .get(root)
        .fold(ns.run(root)): node =>
          EitherT.rightT[F, String](Some(node))

  def getCommonPrefixNibbleAndRemainders(
      nibbles0: Nibbles,
      nibbles1: Nibbles,
  ): (Nibbles, Nibbles, Nibbles) =
    val commonPrefixNibbleSize: Int = (nibbles0.value ^ nibbles1.value)
      .grouped(4L)
      .takeWhile(_ === BitVector.low(4L))
      .size
    val nextPrefixBitSize = commonPrefixNibbleSize.toLong * 4L
    val remainder0        = nibbles0.value drop nextPrefixBitSize
    val (commonPrefix, remainder1) =
      nibbles1.value splitAt nextPrefixBitSize
    (
      commonPrefix.assumeNibbles,
      remainder0.assumeNibbles,
      remainder1.assumeNibbles,
    )

end MerkleTrie
