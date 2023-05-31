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
import merkle.{MerkleTrie, MerkleTrieState}
import merkle.MerkleTrie.NodeStore

trait DAppState[F[_], K, V]:
  def get(k: K): StateT[EitherT[F, String, *], MerkleTrieState, Option[V]]
  def put(k: K, v: V): StateT[EitherT[F, String, *], MerkleTrieState, Unit]
  def remove(k: K): StateT[EitherT[F, String, *], MerkleTrieState, Boolean]
  def from(
      bytes: ByteVector,
  ): StateT[EitherT[F, String, *], MerkleTrieState, Stream[
    EitherT[F, String, *],
    (K, V),
  ]]

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
      scribe.info(
        s"Building DAppState from WithCommonPrefix $prefix of name $name",
      )
      DAppState.ofName[F, K, V](s"$prefix-$name")

  def ofName[F[_]: Monad: NodeStore, K: ByteCodec, V: ByteCodec](
      name: String,
  ): DAppState[F, K, V] =

    scribe.info(s"Initializing DAppState with name $name")

    type ETFS[A] = EitherT[F, String, A]

    @SuppressWarnings(Array("org.wartremover.warts.Throw"))
    val nameBytes: ByteVector = ByteVector.encodeUtf8(name) match
      case Left(e)      => throw e
      case Right(bytes) => bytes

    new DAppState[F, K, V]:
      def get(k: K): StateT[ETFS, MerkleTrieState, Option[V]] =
        for
          bytesOption <- MerkleTrie.get[F]((nameBytes ++ k.toBytes).bits)
          vOption <- StateT.liftF(EitherT.fromEither {
            bytesOption.traverse(_.to[V].leftMap(_.msg))
          })
        yield
//          scribe.info(s"state $name get($k) result: $vOption")
          vOption
      def put(k: K, v: V): StateT[ETFS, MerkleTrieState, Unit] =
        MerkleTrie
          .put[F]((nameBytes ++ k.toBytes).bits, v.toBytes)
//          .map { _ => scribe.info(s"state $name put($k, $v)") }
      def remove(k: K): StateT[ETFS, MerkleTrieState, Boolean] =
        MerkleTrie.remove[F]((nameBytes ++ k.toBytes).bits)
      def from(
          prefixBytes: ByteVector,
      ): StateT[ETFS, MerkleTrieState, Stream[ETFS, (K, V)]] =
        val prefixBits = (nameBytes ++ prefixBytes).bits
        MerkleTrie
          .from(prefixBits)
          .map { binaryStream =>
            binaryStream
              .takeWhile(_._1.startsWith(prefixBits))
              .evalMap { case (kBits, vBytes) =>
                EitherT
                  .fromEither {
                    for
                      k <- kBits.bytes.drop(nameBytes.size).to[K]
                      v <- vBytes.to[V]
                    yield (k, v)
                  }
                  .leftMap(_.msg)
              }
          }

