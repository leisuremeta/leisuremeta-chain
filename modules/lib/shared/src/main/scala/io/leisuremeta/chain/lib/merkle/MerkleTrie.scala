package io.leisuremeta.chain.lib
package merkle

import cats.Monad
import cats.data.{EitherT, Kleisli, OptionT, StateT}
import cats.syntax.eq.given

import fs2.Stream
import io.github.iltotore.iron.*
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
                  val leaf0 =
                    MerkleTrieNode.leaf(prefix00.assumeNibbles, value0)
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
                  val leaf0 =
                    MerkleTrieNode.leaf(prefix00.assumeNibbles, value0)
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

//  @SuppressWarnings(Array("org.wartremover.warts.Var"))
//  var count = 0L

  @SuppressWarnings(Array("org.wartremover.warts.Overloading"))
  def streamFrom[F[_]: Monad: NodeStore](
      key: Nibbles,
  ): StateT[EitherT[F, String, *], MerkleTrieState, Stream[
    EitherT[F, String, *],
    (Nibbles, ByteVector),
  ]] =
    streamFrom[F](key, key)

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  def streamFrom[F[_]: Monad: NodeStore](
      key: Nibbles,
      originalKey: Nibbles,
  ): StateT[EitherT[F, String, *], MerkleTrieState, Stream[
    EitherT[F, String, *],
    (Nibbles, ByteVector),
  ]] =
    type ErrorOrF[A] = EitherT[F, String, A]
//    scribe.info(s"""#${count}\t streamFrom: "${key.value.toHex}", originalKey: "${originalKey.value.toHex}"""")
//    scribe.info(s"""#${count}\t originalKey UTF8: "${originalKey.value.bytes.decodeUtf8}"""")
//    count += 1L
    StateT.inspectF: (state: MerkleTrieState) =>
      scribe.debug(s"from: $key, $state")

      def branchStream(
          prefix: Nibbles,
          children: MerkleTrieNode.Children,
          value: Option[ByteVector],
      ): OptionT[ErrorOrF, Stream[ErrorOrF, (Nibbles, ByteVector)]] =
        def runFrom(key: Nibbles)(
            hashWithIndex: (Option[MerkleHash], Int),
        ): Stream[EitherT[F, String, *], (Nibbles, ByteVector)] =
          Stream
            .eval:
              streamFrom(key, originalKey)
                .runA(state.copy(root = hashWithIndex._1))
            .flatten
            .map: (key, a) =>
              val indexNibble =
                BitVector.fromInt(hashWithIndex._2, 4).assumeNibbles
              val key1 = Nibbles.combine(prefix, indexNibble, key)
              (key1, a)

//        scribe.info(s"Branch: $key <= $prefix: ${key <= prefix}")
        if key <= prefix then
          // key is less than or equal to prefix, so all of its value and children should be included
          val initialValue: Stream[ErrorOrF, (Nibbles, ByteVector)] =
            value.fold(Stream.empty): bytes =>
              Stream.eval(EitherT.pure((prefix, bytes)))
          val tailStream = Stream
            .emits:
              children.toList.zipWithIndex.filter(_._1.nonEmpty)
            .flatMap(runFrom(Nibbles.empty))

          OptionT.liftF:
            EitherT.rightT[F, String]:
              initialValue ++ tailStream
        else
          for
            keyRemainder <- OptionT.fromOption[ErrorOrF]:
              // if key is not starting with prefix (fail to strip) there is no stream to return
              key.stripPrefix(prefix)
            (index1, key1) <- OptionT.fromOption[ErrorOrF]:
              keyRemainder.unCons // keyRemainder is not empty here (key > prefix)
            targetChildrenWithIndex = children.toList.zipWithIndex.drop(index1)
            stream <- OptionT.liftF:
              EitherT.rightT[F, String]:
                targetChildrenWithIndex match
                  case Nil => Stream.empty
                  case x :: xs =>
                    val head =
                      Stream.emit(x).flatMap(runFrom(key1.assumeNibbles))
                    val tail = Stream
                      .emits(xs.filter(_._1.nonEmpty))
                      .flatMap(runFrom(Nibbles.empty))
                    head ++ tail
          yield stream

      OptionT(getNode[F](state))
        .flatMap:
          case MerkleTrieNode.Leaf(prefix, value) =>
//            scribe.info(s"Leaf: $key <= $prefix: ${key <= prefix}")
//            scribe.info(s"#$count\tLeaf: ${prefix.value.toHex}")
            OptionT.when(key <= prefix):
              Stream.emit((prefix, value))
          case MerkleTrieNode.Branch(prefix, children) =>
//            scribe.info(s"#$count\tBranch: ${prefix.value.toHex}")
//            scribe.info(s"Children Size: ${children.flatten.size}")
            branchStream(prefix, children, None)
          case MerkleTrieNode.BranchWithData(prefix, children, value) =>
//            scribe.info(s"#$count\tBranch: ${prefix.value.toHex}")
//            scribe.info(s"Children Size: ${children.flatten.size}")
            branchStream(prefix, children, Some(value))
        .value
        .map:
          _.getOrElse:
//            scribe.info(s"#${count}\tNo node found for key: ${key.value.toHex}")
            Stream.empty

  /** @param keyPrefix:
    *   the key prefix to get the stream from. This prefix must be included.
    * @param keySuffix:
    *   optional key suffix. If this suffix is provided, the stream's key
    *   iterates over values less than keyPrefix + keySuffix.
    * @return
    *   the stream of values starting with the given key prefix.
    */
  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  def reverseStreamFrom[F[_]: Monad: NodeStore](
      keyPrefix: Nibbles,
      keySuffix: Option[Nibbles],
  ): StateT[EitherT[F, String, *], MerkleTrieState, Stream[
    EitherT[F, String, *],
    (Nibbles, ByteVector),
  ]] =
    StateT.inspectF: (state: MerkleTrieState) =>
      scribe.debug(s"from: ($keyPrefix, $keySuffix): $state")
      def reverseBranchStream(
          prefix: Nibbles,
          children: MerkleTrieNode.Children,
          value: Option[ByteVector],
      ): OptionT[EitherT[F, String, *], Stream[
        EitherT[F, String, *],
        (Nibbles, ByteVector),
      ]] =

        def reverseRunFrom(keyPrefix: Nibbles, keySuffix: Option[Nibbles])(
            hashWithIndex: (Option[MerkleHash], Int),
        ): Stream[EitherT[F, String, *], (Nibbles, ByteVector)] =
//          scribe.info(s"reverseRunFrom: $key, $hashWithIndex")
          Stream
            .eval:
              reverseStreamFrom(keyPrefix, keySuffix)
                .runA(state.copy(root = hashWithIndex._1))
            .flatten
            .map: (key, a) =>
              val indexNibble =
                BitVector.fromInt(hashWithIndex._2, 4).assumeNibbles
              val key1 = Nibbles.combine(prefix, indexNibble, key)
              (key1, a)

        def reverseRunAllOptionT =
          val lastStream =
            value.fold(Stream.empty): bytes =>
              Stream.eval:
                EitherT.rightT[F, String]:
                  (prefix, bytes)

          val initStream = Stream
            .emits(children.toList.zipWithIndex.reverse)
            .flatMap(reverseRunFrom(Nibbles.empty, None))

          OptionT.liftF:
            EitherT.rightT[F, String]:
              initStream ++ lastStream

        def streamFromKeySuffix(
            keySuffix: Nibbles,
        ): OptionT[EitherT[F, String, *], Stream[
          EitherT[F, String, *],
          (Nibbles, ByteVector),
        ]] = keySuffix.unCons match
          case None => reverseRunAllOptionT
          case Some((index1, key1)) =>
            val targetChildren = children.toList.zipWithIndex
              .take(index1 + 1)
              .reverse
            targetChildren match
              case Nil =>
                OptionT.none
              case x :: xs =>
                OptionT.liftF:
                  EitherT.rightT[F, String]:
                    val headStream = Stream
                      .emit(x)
                      .flatMap:
                        reverseRunFrom(Nibbles.empty, Some(key1.assumeNibbles))
                    val tailStream = Stream
                      .emits(xs.filter(_._1.nonEmpty))
                      .flatMap:
                        reverseRunFrom(Nibbles.empty, None)
                    val lastStream = value.fold(Stream.empty): bytes =>
                      Stream.emit((prefix, bytes))

                    headStream ++ tailStream ++ lastStream

        keyPrefix.stripPrefix(prefix) match

          // keyPrefix is not starting with prefix
          case None =>
            prefix.stripPrefix(keyPrefix) match

              // prefix is not starting with keyPrefix so we don't need to include it
              case None => OptionT.none

              // prefix is starting with keyPrefix so we need to include it
              case Some(prefixRemainder) =>
                keySuffix.fold(reverseRunAllOptionT): keySuffix1 =>
                  if prefixRemainder <= keySuffix1 then reverseRunAllOptionT
                  else
                    // Here, keySuffix1 < prefixRemainder
                    keySuffix1.stripPrefix(prefixRemainder) match
                      case None => OptionT.none
                      case Some(keySuffix2) =>
                        streamFromKeySuffix(keySuffix2.assumeNibbles)
          case Some(keyRemainder) =>
            keyRemainder.unCons match
              case None =>
                keySuffix.fold(reverseRunAllOptionT)(streamFromKeySuffix)
              case Some((index1, key1)) =>
                children.toList.zipWithIndex.take(index1 + 1).reverse match
                  case Nil => OptionT.none
                  case x :: xs =>
                    OptionT.liftF:
                      EitherT.rightT[F, String]:
                        Stream
                          .emit(x)
                          .flatMap:
                            reverseRunFrom(key1.assumeNibbles, keySuffix)

      OptionT(getNode[F](state))
        .flatMap:
          case MerkleTrieNode.Leaf(prefix, value) =>
            prefix.stripPrefix(keyPrefix) match
              case None => OptionT.none
              case Some(prefixRemainder) =>
                keySuffix match
                  case None =>
                    OptionT.pure:
                      Stream.emit((prefix, value))
                  case Some(suffix) =>
                    OptionT.when(prefixRemainder <= suffix):
                      Stream.emit((prefix, value))
          case MerkleTrieNode.Branch(prefix, children) =>
            reverseBranchStream(prefix, children, None)
          case MerkleTrieNode.BranchWithData(prefix, children, value) =>
            reverseBranchStream(prefix, children, Some(value))
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
