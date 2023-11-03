package io.leisuremeta.chain
package bench

import org.openjdk.jmh.annotations._
import lib.merkle._
import lib.merkle.MerkleTrie.NodeStore
import lib.merkle.MerkleTrieNode.MerkleHash
import scodec.bits.ByteVector
import scodec.bits.hex
import cats.Id
import cats.data.EitherT
import cats.data.Kleisli

@State(Scope.Benchmark)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
class MerkleBench:
  val initialState = MerkleTrieState.empty
  val n = 100

  given emptyNodeStore: NodeStore[Id] =
    Kleisli { (_: MerkleHash) => EitherT.rightT[Id, String](None) }

  @Setup(Level.Iteration)
  def setup() =
    for
      i <- 1.to(n / 2)
      f = MerkleTrie.put[Id](ByteVector.fromInt(i).bits, ByteVector.fromInt(i))
      _ = f.runS(initialState)
    yield ()
    ()
  
  @Benchmark
  def runPutN() =
    for 
      i <- 1.to(n)
      f = MerkleTrie.put[Id](ByteVector.fromInt(i).bits, ByteVector.fromInt(i))
      _ = f.runS(initialState)
    yield () 

  @Benchmark
  def runGetN() =
    for 
      i <- 1.to(n)
      f = MerkleTrie.get[Id](ByteVector.fromInt(i).bits)
      _ = f.runS(initialState)
    yield ()

  @Benchmark
  def runFromN() =
    for 
      i <- 1.to(n)
      f = MerkleTrie.from[Id](ByteVector.fromInt(i).bits)
      _ = f.runS(initialState)
    yield ()

  @Benchmark
  def runRemoveN() =
    for 
      i <- 1.to(n)
      f = MerkleTrie.remove[Id](ByteVector.fromInt(i).bits)
      _ = f.runS(initialState)
    yield ()
