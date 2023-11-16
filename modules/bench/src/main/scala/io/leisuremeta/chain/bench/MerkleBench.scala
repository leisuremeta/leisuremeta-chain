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
import io.leisuremeta.chain.lib.datatype.Utf8.bytes
import io.leisuremeta.chain.lib.datatype.Utf8

@State(Scope.Benchmark)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
class MerkleBench:
  var initialState = MerkleTrieState.empty
  val n = 50
  val m = 50 
  def keyMaker(i: Int) = Utf8.unsafeFrom(s"Bench-Test-$i").bytes.bits

  given emptyNodeStore: NodeStore[Id] =
    Kleisli { (_: MerkleHash) => EitherT.rightT[Id, String](None) }

  def put(i: Int, s: MerkleTrieState, x: Int = 1, vf: Int => Int = n => n): MerkleTrieState =
    if i < 1 then return s
    val f = MerkleTrie.put[Id](keyMaker(i), ByteVector.fromInt(vf(i)))
    put(i - 1, f.runS(s).getOrElse(s))

  @Setup(Level.Iteration)
  def setup() =
    initialState = put(m, MerkleTrieState.empty)
    ()
  
  @Benchmark
  def runPutN() =
    put(n + m, initialState, m)
    ()

  @Benchmark
  def runUpdateN() =
    put(n, initialState, 1, i => -i)
    ()

  @Benchmark
  def runGetN() =
    for 
      i <- 1.to(n)
      f = MerkleTrie.get[Id](keyMaker(i))
      _ = f.runS(initialState)
    yield ()

  @Benchmark
  def runFromN() =
    for 
      i <- 1.to(n)
      f = MerkleTrie.from[Id](keyMaker(i))
      _ = f.runS(initialState)
    yield ()

  @Benchmark
  def runRemoveN() =
    for 
      i <- 1.to(n)
      f = MerkleTrie.remove[Id](keyMaker(i))
      _ = f.runS(initialState)
    yield ()
