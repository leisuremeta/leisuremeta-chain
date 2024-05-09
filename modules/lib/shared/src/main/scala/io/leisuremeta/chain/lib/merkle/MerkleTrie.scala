package io.leisuremeta.chain.lib
package merkle

import cats.Monad
import cats.data.{EitherT, Kleisli, StateT}
import cats.syntax.either.given
import cats.syntax.eq.given
import cats.syntax.traverse.given

import eu.timepit.refined.api.Refined
import eu.timepit.refined.refineV
import fs2.Stream
import scodec.bits.{BitVector, ByteVector}

import crypto.Hash.ops.*
import MerkleTrieNode.{Children, ChildrenCondition, MerkleHash}

object MerkleTrie:

  type NodeStore[F[_]] =
    Kleisli[EitherT[F, String, *], MerkleHash, Option[MerkleTrieNode]]

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  def get[F[_]: Monad: NodeStore](
      key: Nibbles,
  ): StateT[EitherT[F, String, *], MerkleTrieState, Option[ByteVector]] =
    val RightNone = EitherT.rightT[F, String](Option.empty[ByteVector])
    StateT.inspectF: (state: MerkleTrieState) =>
      state.root.fold(RightNone): _ =>
        getNode[F](state).flatMap: node =>
          key
            .stripPrefix(node.prefix)
            .fold(RightNone): stripped =>
              stripped.unCons.fold(EitherT.pure[F, String](node.getValue)):
                (head, remainder) =>
                  val nextRootOption = for
                    children <- node.getChildren
                    child    <- children.value(head.toInt(signed = false))
                  yield child
                  nextRootOption.fold(EitherT.rightT[F, String](None)):
                    nextRoot =>
                      get[F](remainder)
                        .runA(state.copy(root = Some(nextRoot)))

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  def put[F[_]: Monad: NodeStore](
      key: Nibbles,
      value: ByteVector,
  ): StateT[EitherT[F, String, *], MerkleTrieState, Unit] =
    StateT.modifyF: (state: MerkleTrieState) =>
      state.root match
        case None =>
          val leaf     = MerkleTrieNode.leaf(key, value)
          val leafHash = leaf.toHash
          EitherT.rightT[F, String]:
            state.copy(
              root = Some(leafHash),
              diff = state.diff.add(leafHash, leaf),
            )
        case Some(root) =>
          getNode[F](state).flatMap: (node: MerkleTrieNode) =>
            val prefix0: Nibbles = node.prefix

            node match
              case MerkleTrieNode.Leaf(_, value0) =>
                val (commonPrefix, remainder0, remainder1) =
                  getCommonPrefixNibbleAndRemainders(prefix0, key)
                (remainder0.unCons, remainder1.unCons) match
                  case (None, None) =>
                    if value0 === value then EitherT.rightT[F, String](state)
                    else
                      val leaf1 =
                        MerkleTrieNode.leaf(prefix0, value)
                      val leaf1Hash = leaf1.toHash
                      EitherT.rightT[F, String]:
                        state.copy(
                          root = Some(leaf1Hash),
                          diff = state.diff
                            .remove(root, node)
                            .add(leaf1Hash, leaf1),
                        )
                  case (None, Some((index10, prefix10))) =>
                    val leaf1 =
                      MerkleTrieNode.leaf(prefix10.assumeNibble, value)
                    val leaf1Hash = leaf1.toHash
                    val children: Children = Children.empty
                      .updated(index10.toInt(signed = false), Some(leaf1Hash))
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
                    val leaf0 =
                      MerkleTrieNode.leaf(prefix00.assumeNibble, value0)
                    val leaf0Hash = leaf0.toHash
                    val children: Children = Children.empty
                      .updated(index00.toInt(signed = false), Some(leaf0Hash))
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
                    val leaf0 =
                      MerkleTrieNode.leaf(prefix00.assumeNibble, value0)
                    val leaf0Hash = leaf0.toHash
                    val leaf1 =
                      MerkleTrieNode.leaf(prefix10.assumeNibble, value)
                    val leaf1Hash = leaf1.toHash
                    val children: Children = Children.empty
                      .updated(index00.toInt(signed = false), Some(leaf0Hash))
                      .updated(index10.toInt(signed = false), Some(leaf1Hash))
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
              case _ =>
                EitherT
                  .fromOption[F](
                    node.getChildren,
                    s"Node has no children: $node",
                  )
                  .flatMap: children =>
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
                        children.value(index10.toInt(signed = false)) match
                          case None =>
                            val leaf1 =
                              MerkleTrieNode.leaf(prefix10.assumeNibble, value)
                            val leaf1Hash = leaf1.toHash
                            EitherT
                              .fromEither:
                                refineV[ChildrenCondition]:
                                  children.value.updated(
                                    index10.toInt(signed = false),
                                    Some(leaf1Hash),
                                  )
                              .map: children1 =>
                                val branch1     = node.setChildren(children1)
                                val branch1Hash = branch1.toHash
                                state.copy(
                                  root = Some(branch1Hash),
                                  diff = state.diff
                                    .remove(root, node)
                                    .add(branch1Hash, branch1)
                                    .add(leaf1Hash, leaf1),
                                )
                          case Some(childHash) =>
                            put[F](prefix10.assumeNibble, value)
                              .runS(state.copy(root = Some(childHash)))
                              .map: childState =>
//                                println(s"======> Child state: $childState")
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
                      case (Some((index00, prefix00)), None) =>
                        val child0     = node.setPrefix(prefix00.assumeNibble)
                        val child0Hash = child0.toHash
                        val children1 = MerkleTrieNode.Children.empty.updated(
                          index00.toInt(signed = false),
                          Some(child0Hash),
                        )
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
                      case (
                            Some((index00, prefix00)),
                            Some((index10, prefix10)),
                          ) =>
                        val child0     = node.setPrefix(prefix00.assumeNibble)
                        val child0Hash = child0.toHash
                        val child1 =
                          MerkleTrieNode.leaf(prefix10.assumeNibble, value)
                        val child1Hash = child1.toHash
                        val children1 = Children.empty
                          .updated(
                            index00.toInt(signed = false),
                            Some(child0Hash),
                          )
                          .updated(
                            index10.toInt(signed = false),
                            Some(child1Hash),
                          )
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

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  def remove[F[_]: Monad: NodeStore](
      key: Nibbles,
  ): StateT[EitherT[F, String, *], MerkleTrieState, Boolean] =
    StateT: (state: MerkleTrieState) =>
      val RightFalse = EitherT.pure[F, String]((state, false))
      state.root.fold(RightFalse): root =>
        getNode[F](state).flatMap: node =>
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
                        children
                          .value(index1.toInt(signed = false))
                          .fold(RightFalse): childHash =>
                            remove[F](key1)
                              .run(state.copy(root = Some(childHash)))
                              .subflatMap:
                                case (_, false) =>
                                  ((state, false)).asRight[String]
                                case (childState, true)
                                    if childState.root.isEmpty
                                      && children.value.count(_.nonEmpty) <= 1
                                      && node.getValue.isEmpty =>
                                  val childState1 = childState.copy(
                                    root = None,
                                    diff = childState.diff.remove(root, node),
                                  )
                                  ((childState1, true)).asRight[String]
                                case (childState, true) =>
                                  val refined = refineV[ChildrenCondition]:
                                    children.value.updated(
                                      index1.toInt(signed = false),
                                      childState.root,
                                    )
                                  refined.map: children1 =>
                                    val branch = node.getValue match
                                      case None =>
                                        MerkleTrieNode.branch(node.prefix, children1)
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
                                    (childState1, true)
  
  type ByteStream[F[_]] = Stream[EitherT[F, String, *], (Nibbles, ByteVector)]

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  def from[F[_]: Monad: NodeStore](
      key: Nibbles,
  ): StateT[EitherT[F, String, *], MerkleTrieState, ByteStream[F]] =
    StateT.inspectF: (state: MerkleTrieState) =>
      scribe.debug(s"from: $key, $state")
      state.root match
        case None => EitherT.rightT[F, String](Stream.empty)
        case Some(_) =>
          getNode[F](state).flatMap:
            case MerkleTrieNode.Leaf(prefix, value) =>
              scribe.debug(s"Leaf: $key <= $prefix: ${key <= prefix}")
              if key <= prefix then
                EitherT.pure(Stream.emit((prefix, value)))
              else EitherT.rightT[F, String](Stream.empty)
            case node =>
              val prefix      = node.prefix
              val valueOption = node.getValue

              def runFrom(key: Nibbles)(
                  hashWithIndex: (Option[MerkleHash], Int),
              ): EitherT[F, String, ByteStream[F]] =
                from(key)
                  .runA(state.copy(root = hashWithIndex._1))
                  .map:
                    _.map: (key, a) =>
                      val key1 = prefix.value
                        ++ BitVector.fromInt(hashWithIndex._2, 4) ++ key.value
                      (key1.assumeNibble, a)

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
                    children.value.toList.zipWithIndex
                      .traverse(runFrom(BitVector.empty.assumeNibble))
                      .map(flatten)
                      .map(initialValue ++ _)
                  else if prefix.value.nonEmpty && !key.value.startsWith(prefix.value)
                  then
                    scribe.debug:
                      s"======>[Case #2] prefix: ${prefix.value}, key: $key"
                    EitherT.rightT[F, String](Stream.empty)
                  else
                    val (index1, key1) = key.value drop prefix.value.size splitAt 4L
                    scribe.debug:
                      s"======>[Case #3] index1: $index1, key1: $key1"
                    val targetChildren: List[(Option[MerkleHash], Int)] =
                      children.value.toList.zipWithIndex
                        .drop(index1.toInt(signed = false))
                    targetChildren match
                      case Nil => EitherT.rightT[F, String](Stream.empty)
                      case x :: xs =>
                        for
                          headList <- runFrom(key1.assumeNibble)(x)
                          tailList <- xs.traverse(runFrom(BitVector.empty.assumeNibble))
                        yield headList ++ flatten(tailList)
  
  def getNode[F[_]: Monad](
      state: MerkleTrieState,
  )(using ns: NodeStore[F]): EitherT[F, String, MerkleTrieNode] = for
    root <- EitherT.fromOption[F](
      state.root,
      s"Cannot get node from empty merkle trie: $state",
    )
    node <- state.diff
      .get(root)
      .fold:
        ns.run(root)
          .subflatMap:
            _.toRight(s"Merkle trie node $root is not found: $state")
      .apply(EitherT.pure(_))
  yield
    scribe.debug(s"Accessing node ${state.root} -> $node")
    node

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
      commonPrefix.assumeNibble,
      remainder0.assumeNibble,
      remainder1.assumeNibble,
    )

end MerkleTrie
