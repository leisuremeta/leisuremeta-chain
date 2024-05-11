package io.leisuremeta.chain.lib
package merkle

import cats.Monad
import cats.data.{EitherT, Kleisli, OptionT, StateT}
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
              children <- OptionT.fromOption[EitherT[F, String, *]]:
                node.getChildren
              nextRoot <- OptionT.fromOption[EitherT[F, String, *]]:
                children(head)
              value <- OptionT:
                get[F](remainder.assumeNibbles).runA:
                  state.copy(root = Some(nextRoot))
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
          val (commonPrefix, remainder0, remainder1) =
            getCommonPrefixNibbleAndRemainders(prefix0, key)

          def putLeaf(value0: ByteVector): EitherT[F, String, MerkleTrieState] =
            EitherT.rightT[F, String]:
              (remainder0.unCons, remainder1.unCons) match
                case (None, None) =>
                  if value0 === value then state
                  else
                    val leaf1     = MerkleTrieNode.leaf(prefix0, value)
                    val leaf1Hash = leaf1.toHash
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
            (remainder0.unCons, remainder1.unCons) match
              case (None, None) =>
                // key is equal to prefix0, so we need to update node value
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
                // key is starting with prefix0, but not equal to it
                children(index10) match
                  case None =>
                    // child is empty, so we need to create a new leaf
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
                    // child is not empty, so we need to update the child recursively
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
                // prefix is larger than key
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
    type ErrorOrF[A] = EitherT[F, String, A]
    StateT: (state: MerkleTrieState) =>
      val optionT = OptionT(getNodeAndStateRoot[F](state)).flatMap:
        case ((node, root)) =>
          node match
            case MerkleTrieNode.Leaf(prefix, _) =>
              OptionT.when(prefix === key):
                state.copy(root = None, diff = state.diff.remove(root, node))
            case MerkleTrieNode.BranchWithData(prefix, children, _)
                if prefix === key =>
              val branch1: MerkleTrieNode =
                MerkleTrieNode.Branch(prefix, children)
              val branch1Hash = branch1.toHash

              OptionT.pure[ErrorOrF]:
                state.copy(
                  root = Some(branch1Hash),
                  diff = state.diff.remove(root, node).add(branch1Hash, branch1),
                )
            case _ =>
              for
                stripped <- OptionT.fromOption[ErrorOrF]:
                  key.stripPrefix(node.prefix)
                (index1, key1) <- OptionT.fromOption[ErrorOrF]:
                  stripped.unCons
                children <- OptionT.fromOption[ErrorOrF]:
                  node.getChildren
                childHash <- OptionT.fromOption[ErrorOrF]:
                  children(index1)
                childStateAndResult <- OptionT.liftF:
                  remove[F](key1.assumeNibbles).run:
                    state.copy(root = Some(childHash))
                (childState, result) = childStateAndResult
                state1 <- OptionT.when[ErrorOrF, MerkleTrieState](result):
                  val needToRemoveSelf = childState.root.isEmpty
                    && children.count(_.nonEmpty) <= 1
                    && node.getValue.isEmpty
                  if needToRemoveSelf then
                    childState.copy(
                      root = None,
                      diff = childState.diff.remove(root, node),
                    )
                  else
                    val children1 =
                      children.updateChild(index1, childState.root)
                    val branch     = node.setChildren(children1)
                    val branchHash = branch.toHash
                    childState.copy(
                      root = Some(branchHash),
                      diff = childState.diff
                        .remove(root, node)
                        .add(branchHash, branch),
                    )
              yield state1
      optionT.value.map(_.fold((state, false))((_, true)))

  type ByteStream[F[_]] = Stream[EitherT[F, String, *], (Nibbles, ByteVector)]

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  def streamFrom[F[_]: Monad: NodeStore](
      key: Nibbles,
  ): StateT[EitherT[F, String, *], MerkleTrieState, ByteStream[F]] =
    type ErrorOrF[A] = EitherT[F, String, A]
    StateT.inspectF: (state: MerkleTrieState) =>
      scribe.debug(s"from: $key, $state")

      def branchStream(
          prefix: Nibbles,
          children: MerkleTrieNode.Children,
          value: Option[ByteVector],
      ): OptionT[ErrorOrF, ByteStream[F]] =

        def runFrom(key: Nibbles)(
            hashWithIndex: (Option[MerkleHash], Int),
        ): ErrorOrF[ByteStream[F]] =
          streamFrom(key)
            .runA(state.copy(root = hashWithIndex._1))
            .map: (byteStream: ByteStream[F]) =>
              byteStream.map: (key, a) =>
                val key1 = prefix.value
                  ++ BitVector.fromInt(hashWithIndex._2, 4) ++ key.value
                (key1.assumeNibbles, a)

        def flatten(enums: List[ByteStream[F]]): ByteStream[F] =
          enums.foldLeft[ByteStream[F]](Stream.empty)(_ ++ _)

        if key <= prefix then
          // key is less than or equal to prefix, so all of its value and children should be included
          val initialValue: ByteStream[F] = value.fold(Stream.empty): bytes =>
            Stream.eval(EitherT.pure((prefix, bytes)))
          OptionT.liftF:
            children.toList.zipWithIndex
              .traverse(runFrom(Nibbles.empty))
              .map(flatten)
              .map(initialValue ++ _)
        else
          for
            keyRemainder <- OptionT.fromOption[ErrorOrF]:
              // if key is not starting with prefix (fail to strip) there is no stream to return
              key.stripPrefix(prefix)
            (index1, key1) <- OptionT.fromOption[ErrorOrF]:
              keyRemainder.unCons // keyRemainder is not empty here (key > prefix)
            stream <- children.toList.zipWithIndex.drop(index1) match
              case Nil => OptionT.none[ErrorOrF, ByteStream[F]]
              case x :: xs =>
                OptionT.liftF:
                  for
                    headList <- runFrom(key1.assumeNibbles)(x)
                    tailList <- xs.traverse(runFrom(Nibbles.empty))
                  yield headList ++ flatten(tailList)
          yield stream

      OptionT(getNode[F](state))
        .flatMap:
          case MerkleTrieNode.Leaf(prefix, value) =>
            scribe.debug(s"Leaf: $key <= $prefix: ${key <= prefix}")
            OptionT.when(key <= prefix):
              Stream.emit((prefix, value))
          case MerkleTrieNode.Branch(prefix, children) =>
            branchStream(prefix, children, None)
          case MerkleTrieNode.BranchWithData(prefix, children, value) =>
            branchStream(prefix, children, Some(value))
        .value
        .map(_.getOrElse(Stream.empty))

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
