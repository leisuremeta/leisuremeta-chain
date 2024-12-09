package io.leisuremeta.chain.lib
package application

import cats.Monad
import cats.data.{EitherT, StateT}
import cats.syntax.either.catsSyntaxEither
import cats.syntax.traverse.toTraverseOps

import fs2.Stream
import scodec.bits.ByteVector

import codec.byte.ByteCodec
import codec.byte.ByteDecoder.ops.*
import codec.byte.ByteEncoder.ops.*
import merkle.*
import merkle.MerkleTrie.NodeStore
import io.leisuremeta.chain.lib.merkle.toNibbles

trait DAppState[F[_], K, V]:
  type ErrorOrF[A] = EitherT[F, String, A]

  def get(k: K): StateT[ErrorOrF, MerkleTrieState, Option[V]]
  def put(k: K, v: V): StateT[ErrorOrF, MerkleTrieState, Unit]
  def remove(k: K): StateT[ErrorOrF, MerkleTrieState, Boolean]
  def streamWithPrefix(
      prefixBytes: ByteVector,
  ): StateT[ErrorOrF, MerkleTrieState, Stream[ErrorOrF, (K, V)]]
  def streamFrom(
      keyBytes: ByteVector,
  ): StateT[ErrorOrF, MerkleTrieState, Stream[ErrorOrF, (K, V)]]
  def reverseStreamFrom(
      keyPrefix: ByteVector,
      keySuffix: Option[ByteVector],
  ): StateT[ErrorOrF, MerkleTrieState, Stream[ErrorOrF, (K, V)]]

object DAppState:

  case class WithCommonPrefix(prefix: String):

    /** @param name
      *   must be alpha-numeric
      * @return
      *   DAppState[F, K, V]
      */
    def ofName[F[_]: Monad: NodeStore, K: ByteCodec, V: ByteCodec](
        name: String,
    ): DAppState[F, K, V] =
      scribe.info:
        s"Building DAppState from WithCommonPrefix $prefix of name $name"
      DAppState.ofName[F, K, V](s"$prefix-$name")

  def ofName[F[_]: Monad: NodeStore, K: ByteCodec, V: ByteCodec](
      name: String,
  ): DAppState[F, K, V] =

    scribe.info(s"Initializing DAppState with name $name")

    @SuppressWarnings(Array("org.wartremover.warts.Throw"))
    val nameBytes: ByteVector = ByteVector.encodeUtf8(name) match
      case Left(e)      => throw e
      case Right(bytes) => bytes

    new DAppState[F, K, V]:
      def get(k: K): StateT[ErrorOrF, MerkleTrieState, Option[V]] =
        for
          bytesOption <- MerkleTrie.get[F]((nameBytes ++ k.toBytes).toNibbles)
          vOption <- StateT.liftF:
            EitherT.fromEither:
              bytesOption.traverse(_.to[V].leftMap(_.msg))
        yield
//          scribe.info(s"state $name get($k) result: $vOption")
          vOption
      def put(k: K, v: V): StateT[ErrorOrF, MerkleTrieState, Unit] =
        MerkleTrie
          .put[F]((nameBytes ++ k.toBytes).toNibbles, v.toBytes)
//          .map { _ => scribe.info(s"state $name put($k, $v)") }
      def remove(k: K): StateT[ErrorOrF, MerkleTrieState, Boolean] =
        MerkleTrie.remove[F]((nameBytes ++ k.toBytes).toNibbles)
      def streamFrom(
          keyBytes: ByteVector,
      ): StateT[ErrorOrF, MerkleTrieState, Stream[ErrorOrF, (K, V)]] =
        MerkleTrie
          .streamFrom(keyBytes.toNibbles)
          .map: binaryStream =>
            binaryStream
              .takeWhile(_._1.value.startsWith(nameBytes.bits))
              .evalMap: (kNibbles, vBytes) =>
                EitherT
                  .fromEither:
                    for
                      k <- kNibbles.bytes.drop(nameBytes.size).to[K]
                      v <- vBytes.to[V]
                    yield (k, v)
                  .leftMap(_.msg)
      def streamWithPrefix(
          prefixBytes: ByteVector,
      ): StateT[ErrorOrF, MerkleTrieState, Stream[ErrorOrF, (K, V)]] =
        val prefixNibbles = (nameBytes ++ prefixBytes).toNibbles
        MerkleTrie
          .streamFrom(prefixNibbles)
          .map: binaryStream =>
            binaryStream
              .takeWhile(_._1.value.startsWith(prefixNibbles.value))
              .evalMap: (kNibbles, vBytes) =>
                EitherT
                  .fromEither:
                    for
                      k <- kNibbles.bytes.drop(nameBytes.size).to[K]
                      v <- vBytes.to[V]
                    yield (k, v)
                  .leftMap(_.msg)

      def reverseStreamFrom(
          keyPrefix: ByteVector,
          keySuffix: Option[ByteVector],
      ): StateT[ErrorOrF, MerkleTrieState, Stream[ErrorOrF, (K, V)]] =
        val prefixNibbles = (nameBytes ++ keyPrefix).toNibbles
        MerkleTrie
          .reverseStreamFrom(prefixNibbles, keySuffix.map(_.toNibbles))
          .map: binaryStream =>
            binaryStream
              .evalMap: (kNibbles, vBytes) =>
                EitherT
                  .fromEither:
                    for
                      k <- kNibbles.bytes.drop(nameBytes.size).to[K]
                      v <- vBytes.to[V]
                    yield (k, v)
                  .leftMap(_.msg)
