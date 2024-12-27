package io.leisuremeta.chain.lib
package merkle

import hedgehog.munit.HedgehogSuite
import hedgehog.*
import hedgehog.state.*

import scala.collection.immutable.SortedMap

import cats.Id
import cats.arrow.FunctionK
import cats.data.{EitherT, Kleisli}
import cats.effect.SyncIO
import cats.syntax.all.*

import fs2.Stream
import scodec.bits.ByteVector
import scodec.bits.hex

import codec.byte.{ByteDecoder, ByteEncoder}
import datatype.BigNat
import MerkleTrie.NodeStore
import MerkleTrieNode.{MerkleHash, MerkleRoot}

class MerkleTrieTest extends HedgehogSuite:

  given ByteEncoder[ByteVector] = (bytes: ByteVector) =>
    import ByteEncoder.ops.*
    BigNat.unsafeFromBigInt(bytes.size).toBytes ++ bytes

  given ByteDecoder[ByteVector] =
    ByteDecoder[BigNat].flatMap { size =>
      ByteDecoder.fromFixedSizeBytes(size.toBigInt.toLong)(identity)
    }

  case class State(
      current: SortedMap[ByteVector, ByteVector],
      hashLog: Map[SortedMap[ByteVector, ByteVector], Option[MerkleRoot]],
  )
  object State:
    def empty: State = State(SortedMap.empty, Map.empty)

  case class Get(key: Nibbles)
  case class Put(key: Nibbles, value: ByteVector)
  case class Remove(key: Nibbles)
  case class StreamFrom(key: Nibbles)
  case class ReverseStreamFrom(keyPrefix: Nibbles, keySuffix: Option[Nibbles])

  given emptyIdNodeStore: NodeStore[Id] = Kleisli: (_: MerkleHash) =>
    EitherT.rightT[Id, String](None)

  given emptyIoNodeStore: NodeStore[SyncIO] = Kleisli: (_: MerkleHash) =>
    EitherT.rightT[SyncIO, String](None)

  val initialState = MerkleTrieState.empty

  var merkleTrieState: MerkleTrieState = initialState

  val genByteVector = Gen.bytes(Range.linear(0, 64)).map(ByteVector.view)
  def commandGet: CommandIO[State] =
    new Command[State, Get, Option[ByteVector]]:

      override def gen(s: State): Option[Gen[Get]] = Some(
        (s.current.keys.toList match
          case Nil => genByteVector
          case h :: t =>
            Gen.frequency1(
              80 -> Gen.element(h, t),
              20 -> genByteVector,
            )
        ).map(bytes => Get(bytes.toNibbles)),
      )
      override def execute(
          env: Environment,
          i: Get,
      ): Either[String, Option[ByteVector]] =
        val program = MerkleTrie.get[Id](i.key)
        program.runA(merkleTrieState).value

      override def update(s: State, i: Get, o: Var[Option[ByteVector]]): State =
        s

      override def ensure(
          env: Environment,
          s0: State,
          s: State,
          i: Get,
          o: Option[ByteVector],
      ): Result = s.current.get(i.key.bytes) ==== o

  def commandPut: CommandIO[State] = new Command[State, Put, Unit]:

    override def gen(s: State): Option[Gen[Put]] =
      Some(for
        key   <- genByteVector
        value <- genByteVector
      yield Put(key.toNibbles, value))

    override def execute(env: Environment, i: Put): Either[String, Unit] =
      //    println(s"===> execute: $i")

      val program = MerkleTrie.put[Id](i.key, i.value)
      program.runS(merkleTrieState).value.map { (newState: MerkleTrieState) =>
        merkleTrieState = newState
      }

    override def update(s: State, i: Put, o: Var[Unit]): State =

//      println(s"===> Command Put (${i}) update: ${s.current}")
      val current1  = s.current + ((i.key.bytes -> i.value))
      val stateRoot = merkleTrieState.root
      val hashLog1  = s.hashLog + ((current1    -> stateRoot))

//      println(s"===> After Command Put(${i}) update: ${current1}")
      State(current1, hashLog1)

    override def ensure(
        env: Environment,
        s0: State,
        s: State,
        i: Put,
        o: Unit,
    ): Result = Result.all(
      List(
        //      s0.hashLog.get(s.current).fold(Result.success) {
        //        (rootOption: Option[MerkleRoot[K, V]]) =>
        //          if s.hashLog.get(s.current) != Some(rootOption) then
        //            println(s"===> current: ${s.current}")
        //          s.hashLog.get(s.current) ==== Some(rootOption)
        //      },
        merkleTrieState.root.fold(Result.success) { (root: MerkleRoot) =>
          val result = merkleTrieState.diff.get(root).nonEmpty
          if result == false then
            println(s"====> failed: $i with state ${s0.current}")
          Result.assert(result)
        },
        s.current.get(i.key.bytes) ==== Some(i.value),
      ),
    )

  def commandRemove: CommandIO[State] = new Command[State, Remove, Boolean]:

    override def gen(s: State): Option[Gen[Remove]] = Some(
      (s.current.keys.toList match
        case Nil => genByteVector
        case h :: t =>
          Gen.frequency1(
            80 -> Gen.element(h, t),
            20 -> genByteVector,
          )
      ).map(bytes => Remove(bytes.toNibbles)),
    )
    override def execute(env: Environment, i: Remove): Either[String, Boolean] =
      val program = MerkleTrie.remove[Id](i.key)
      program.run(merkleTrieState).value.map { case (state1, result) =>
        merkleTrieState = state1
        result
      }

    override def update(s: State, i: Remove, o: Var[Boolean]): State =
      val current1  = s.current - i.key.bytes
      val stateRoot = merkleTrieState.root
      val hashLog1  = s.hashLog + ((current1 -> stateRoot))
      State(current1, hashLog1)

    override def ensure(
        env: Environment,
        s0: State,
        s: State,
        i: Remove,
        o: Boolean,
    ): Result = Result.all(
      List(
        s0.current.contains(i.key.bytes) ==== o,
        s.current.get(i.key.bytes) ==== None,
      ),
    )

  type S = Stream[EitherT[Id, String, *], (Nibbles, ByteVector)]

  def commandStreamFrom: CommandIO[State] = new Command[State, StreamFrom, S]:

    override def gen(s: State): Option[Gen[StreamFrom]] = Some(
      (s.current.keys.toList match
        case Nil => genByteVector
        case h :: t =>
          Gen.frequency1(
            80 -> Gen.element(h, t),
            20 -> genByteVector,
          )
      ).map(bytes => StreamFrom(bytes.toNibbles)),
    )
    override def execute(env: Environment, i: StreamFrom): Either[String, S] =
      val program = MerkleTrie.streamFrom[Id](i.key)
      program.runA(merkleTrieState).value

    override def update(s: State, i: StreamFrom, o: Var[S]): State = s

    override def ensure(
        env: Environment,
        s0: State,
        s: State,
        i: StreamFrom,
        o: S,
    ): Result =
      val toId = new FunctionK[EitherT[Id, String, *], Id]:
        override def apply[A](fa: EitherT[Id, String, A]): Id[A] =
          fa.value.toOption.get

      val expected =
        s.current.iteratorFrom(i.key.bytes).take(10).toList.map { (k, v) =>
          (k.bits, v)
        }

      expected ==== o
        .take(10)
        .translate[EitherT[Id, String, *], Id](toId)
        .compile
        .toList

  def commandReverseStreamFrom: CommandIO[State] =
    new Command[State, ReverseStreamFrom, S]:
      override def gen(s: State): Option[Gen[ReverseStreamFrom]] =
        val keyGen = s.current.keys.toList match
          case Nil => genByteVector
          case h :: t =>
            Gen.frequency1(
              80 -> Gen.element(h, t),
              20 -> genByteVector,
            )
        Some:
          for
            bytes <- keyGen
            suffixSize <- Gen.frequency1(
              80 -> Gen.int(Range.linear(0, bytes.size.toInt)),
              20 -> Gen.constant(0),
            )
          yield
            val (prefix, suffix) = bytes.splitAt(bytes.size - suffixSize)
            val suffixOption: Option[Nibbles] = Option.when(suffixSize > 0)(suffix.toNibbles)
            ReverseStreamFrom(prefix.toNibbles, suffixOption)

      override def execute(
          env: Environment,
          i: ReverseStreamFrom,
      ): Either[String, S] =
        val program = MerkleTrie.reverseStreamFrom[Id](i.keyPrefix, i.keySuffix)
        program.runA(merkleTrieState).value

      override def update(s: State, i: ReverseStreamFrom, o: Var[S]): State = s

      override def ensure(
          env: Environment,
          s0: State,
          s: State,
          i: ReverseStreamFrom,
          o: S,
      ): Result =
        val toId = new FunctionK[EitherT[Id, String, *], Id]:
          override def apply[A](fa: EitherT[Id, String, A]): Id[A] =
            fa.value.toOption.get

        val withPrefix = s.current.filter(_._1.startsWith(i.keyPrefix.bytes))

        val expected = i.keySuffix
          .fold(withPrefix): suffix =>
            withPrefix.filter(_._1 <= i.keyPrefix.bytes ++ suffix.bytes) 
          .takeRight(10)
          .toList
          .reverse
          .map: (k, v) =>
            (k.bits, v)

        val result = o
          .take(10)
          .translate[EitherT[Id, String, *], Id](toId)
          .compile
          .toList

//        if expected != result then
//          println(s"===> ReverseStreamFrom: (${i.keyPrefix.bytes}, ${i.keySuffix.map(_.bytes)})")
//          println(s"===> result: ${result}")
//          println(s"===> expected: $expected")

        expected ==== result

  test("put same key value twice expect not to change state") {
    withMunitAssertions { assertions =>
      val initialState = MerkleTrieState.empty
      val program =
        MerkleTrie.put[Id](ByteVector.empty.toNibbles, ByteVector.empty)

      val resultEitherT = for
        state1 <- program.runS(initialState)
        state2 <- program.runS(state1)
      yield assertions.assertEquals(state1, state2)

      resultEitherT.value
    }
  }

  test("put 10 -> put empty with empty -> put 10") {
    withMunitAssertions { assertions =>
      val initialState = MerkleTrieState.empty
      val put10 =
        MerkleTrie.put[Id](hex"10".toNibbles, ByteVector.empty)
      val putEmptyWithEmpty =
        MerkleTrie.put[Id](ByteVector.empty.toNibbles, ByteVector.empty)

//    val forPrint = for
//      state1 <- put10.runS(initialState)
//      state2 <- putEmptyWithEmpty.runS(state1)
//      state3 <- put10.runS(state2)
//    yield
//      Seq(state1, state2, state3).zipWithIndex.foreach{ (s, i) =>
//        println(s"====== State #${i + 1} ======")
//        println(s"root: ${s.root}")
//        s.diff.foreach{ (hash, node) => println(s" $hash: $node") }
//      }

      val initialProgram = for
        _ <- put10
        _ <- putEmptyWithEmpty
      yield ()

      val resultEitherT = for
        state1 <- initialProgram.runS(initialState)
        state2 <- put10.runS(state1)
      yield assertions.assertEquals(state1, state2)

      resultEitherT.value
    }
  }

  test(
    "put (10, empty) -> put (empty, empty) -> put (10, 00) -> put (10, empty)",
  ) {
    withMunitAssertions { assertions =>

      val initialState = MerkleTrieState.empty
      val put10withEmpty =
        MerkleTrie.put[Id](hex"10".toNibbles, ByteVector.empty)
      val putEmptyWithEmpty =
        MerkleTrie.put[Id](ByteVector.empty.toNibbles, ByteVector.empty)
      val put10with10 =
        MerkleTrie.put[Id](hex"10".toNibbles, hex"10")

//    val forPrint = for
//      state1 <- put10withEmpty.runS(initialState)
//      _ <- EitherT.pure[Id, String](println(s"===> state1: ${state1}"))
//      state2 <- putEmptyWithEmpty.runS(state1)
//      _ <- EitherT.pure[Id, String](println(s"===> state2: ${state2}"))
//      state3 <- put10with10.runS(state2)
//      _ <- EitherT.pure[Id, String](println(s"===> state3: ${state3}"))
//      state4 <- put10withEmpty.runS(state3)
//      _ <- EitherT.pure[Id, String](println(s"===> state4: ${state4}"))
//    yield
//      Seq(state1, state2, state3, state4).zipWithIndex.foreach{ (s, i) =>
//        println(s"====== State #${i + 1} ======")
//        println(s"root: ${s.root}")
//        s.diff.foreach{ (hash, node) => println(s" $hash: $node") }
//      }

      val program = for
        _ <- put10withEmpty
        _ <- putEmptyWithEmpty
        _ <- put10with10
        _ <- put10withEmpty
      yield ()

      program.runS(initialState).value match
        case Right(state) =>
          assertions.assert(state.diff.get(state.root.get).nonEmpty)
        case Left(error) =>
          assertions.fail(error)
    }
  }

  test("put (empty, empty) -> put (00, 00) -> get (empty)") {
    withMunitAssertions { assertions =>
      val initialState = MerkleTrieState.empty
      val putEmptyWithEmpty =
        MerkleTrie.put[Id](ByteVector.empty.toNibbles, ByteVector.empty)
      val put00_00 =
        MerkleTrie.put[Id](hex"00".toNibbles, hex"00")
      val getEmpty =
        MerkleTrie.get[Id](ByteVector.empty.toNibbles)

      val program = for
        _     <- putEmptyWithEmpty
        _     <- put00_00
        value <- getEmpty
      yield assertions.assertEquals(value, Some(ByteVector.empty))

//      for
//        state1 <- putEmptyWithEmpty.runS(initialState)
//        state2 <- put00_00.runS(state1)
//        state3 <- getEmpty.runS(state2)
//      yield
//        Seq(state1, state2, state3).zipWithIndex.foreach: (s, i) =>
//          println(s"====== State #${i + 1} ======")
//          println(s"root: ${s.root}")
//          s.diff.foreach{ (hash, node) => println(s" $hash: $node") }

      program.runA(initialState).value
    }
  }

  test("put 00 -> put 0000 -> put empty -> get empty") {
    withMunitAssertions { assertions =>
      val initialState = MerkleTrieState.empty
      val put00        = MerkleTrie.put[Id](hex"00".toNibbles, ByteVector.empty)
      val put0000 = MerkleTrie.put[Id](hex"0000".toNibbles, ByteVector.empty)
      val putEmpty =
        MerkleTrie.put[Id](ByteVector.empty.toNibbles, ByteVector.empty)
      val getEmpty = MerkleTrie.get[Id](ByteVector.empty.toNibbles)

      val program = for
        _     <- put00
        _     <- put0000
        _     <- putEmpty
        value <- getEmpty
      yield assertions.assertEquals(value, Some(ByteVector.empty))

//    val forPrint = for
//      state1 <- put00.runS(initialState)
//      state2 <- put0000.runS(state1)
//      state3 <- putEmpty.runS(state2)
//    yield
//      Seq(state1, state2, state3).zipWithIndex.foreach{ (s, i) =>
//        println(s"====== State #${i + 1} ======")
//        println(s"root: ${s.root}")
//        s.diff.foreach{ (hash, node) => println(s" $hash: $node") }
//      }

      program.runA(initialState).value
    }
  }

  test("put 0700 -> put 07 -> put 10 -> get empty") {
    withMunitAssertions { assertions =>
      val initialState = MerkleTrieState.empty
      val put0700  = MerkleTrie.put[Id](hex"0700".toNibbles, ByteVector.empty)
      val put07    = MerkleTrie.put[Id](hex"07".toNibbles, ByteVector.empty)
      val put10    = MerkleTrie.put[Id](hex"10".toNibbles, ByteVector.empty)
      val getEmpty = MerkleTrie.get[Id](ByteVector.empty.toNibbles)

      val program = for
        _     <- put0700
        _     <- put07
        _     <- put10
        value <- getEmpty
      yield assertions.assertEquals(value, None)

//    val forPrint = for
//      state1 <- put0700.runS(initialState)
//      state2 <- put07.runS(state1)
//      state3 <- put10.runS(state2)
//    yield
//      Seq(state1, state2, state3).zipWithIndex.foreach{ (s, i) =>
//        println(s"====== State #${i + 1} ======")
//        println(s"root: ${s.root}")
//        s.diff.foreach{ (hash, node) => println(s" $hash: $node") }
//      }

      program.runA(initialState).value
    }
  }

  test("put 00 -> put 01 -> get 00") {
    withMunitAssertions { assertions =>
      val initialState = MerkleTrieState.empty
      val put00        = MerkleTrie.put[Id](hex"00".toNibbles, ByteVector.empty)
      val put01        = MerkleTrie.put[Id](hex"01".toNibbles, ByteVector.empty)
      val get00        = MerkleTrie.get[Id](hex"00".toNibbles)

      val program = for
        _     <- put00
        _     <- put01
        value <- get00
      yield assertions.assertEquals(value, Some(ByteVector.empty))

//    val forPrint = for
//      state1 <- put00.runS(initialState)
//      state2 <- put01.runS(state1)
//    yield
//      Seq(state1, state2).zipWithIndex.foreach{ (s, i) =>
//        println(s"====== State #${i + 1} ======")
//        println(s"root: ${s.root}")
//        s.diff.foreach{ (hash, node) => println(s" $hash: $node") }
//      }

      program.runA(initialState).value
    }
  }

  test("put(00, empty) -> put(01, empty) -> put(00, 00) -> get 01") {
    withMunitAssertions { assertions =>
      val initialState = MerkleTrieState.empty
      val put00        = MerkleTrie.put[Id](hex"00".toNibbles, ByteVector.empty)
      val put01        = MerkleTrie.put[Id](hex"01".toNibbles, ByteVector.empty)
      val put00_00     = MerkleTrie.put[Id](hex"00".toNibbles, hex"00")
      val get01        = MerkleTrie.get[Id](hex"01".toNibbles)

      val program = for
        _     <- put00
        _     <- put01
        _     <- put00_00
        value <- get01
      yield value

//    val forPrint = for
//      state1 <- put00.runS(initialState)
//      state2 <- put01.runS(state1)
//      state3 <- put00_00.runS(state2)
//    yield
//      Seq(state1, state2, state3).zipWithIndex.foreach{ (s, i) =>
//        println(s"====== State #${i + 1} ======")
//        println(s"root: ${s.root}")
//        s.diff.foreach{ (hash, node) => println(s" $hash: $node") }
//      }

      val result = program.runA(initialState).value
      assertions.assertEquals(result, Right(Some(ByteVector.empty)))
    }
  }

  test("put 50 -> put 5000 -> remove 00") {
    withMunitAssertions { assertions =>
      val initialState = MerkleTrieState.empty

      def put(key: ByteVector) =
        MerkleTrie.put[Id](key.toNibbles, ByteVector.empty)
      def remove(key: ByteVector) = MerkleTrie.remove[Id](key.toNibbles)

      val program = for
        _      <- put(hex"50")
        _      <- put(hex"5000")
        result <- remove(hex"00")
      yield result

//    val forPrint = for
//      state1 <- put(hex"50").runS(initialState)
//      state2 <- put(hex"5000").runS(state1)
//      result <- remove(hex"00").run(state2)
//    yield
//      Seq(state1, state2, result._1).zipWithIndex.foreach{ (s, i) =>
//        println(s"====== State #${i + 1} ======")
//        println(s"root: ${s.root}")
//        s.diff.foreach{ (hash, node) => println(s" $hash: $node") }
//      }
//      println(s"result: ${result._2}")

      val result = program.runA(initialState).value
      assertions.assertEquals(result, Right(false))
    }
  }

  test("put d0 -> put d000 -> put empty -> put 000000 -> remove d000") {
    withMunitAssertions { assertions =>
      val initialState = MerkleTrieState.empty

      def put(key: ByteVector) =
        MerkleTrie.put[Id](key.toNibbles, ByteVector.empty)
      def remove(key: ByteVector) = MerkleTrie.remove[Id](key.toNibbles)

      val program = for
        _ <- put(hex"d0")
        _ <- put(hex"d000")
        _ <- put(hex"")
        _ <- put(hex"000000")
        _ <- remove(hex"d000")
      yield ()

//    val forPrint = for
//      state1 <- put(hex"d0").runS(initialState)
//      state2 <- put(hex"d000").runS(state1)
//      state3 <- put(hex"").runS(state2)
//      state4 <- put(hex"000000").runS(state3)
//      state5 <- remove(hex"d000").runS(state4)
//    yield
//      Seq(state1, state2, state3, state4).zipWithIndex.foreach{ (s, i) =>
//        println(s"====== State #${i + 1} ======")
//        println(s"root: ${s.root}")
//        s.diff.foreach{ (hash, node) => println(s" $hash: $node") }
//      }

      val result = program.runA(initialState).value
      assertions.assertEquals(result, Right(()))
    }
  }

  test("put 80 -> streamFrom 00"):
    withMunitAssertions: assertions =>

      def put(key: ByteVector) = MerkleTrie.put[SyncIO](key.toNibbles, ByteVector.empty)
      def streamFrom(key: ByteVector) = MerkleTrie.streamFrom[SyncIO](key.toNibbles)

      val program = for
        _ <- put(hex"80")
        value <- streamFrom(hex"00")
      yield value

      val resultIO = program
        .runA(initialState)
        .flatMap: stream =>
          stream.compile.toList
        .value

      val result = resultIO.unsafeRunSync()

      val expected: List[(Nibbles, ByteVector)] = List((hex"80".toNibbles, ByteVector.empty))

      assertions.assertEquals(result, expected.asRight[String])

  test("put 80 -> put empty -> streamFrom 00"):
    withMunitAssertions: assertions =>

      def put(key: ByteVector) = MerkleTrie.put[SyncIO](key.toNibbles, ByteVector.empty)
      def streamFrom(key: ByteVector) = MerkleTrie.streamFrom[SyncIO](key.toNibbles)

      val program = for
        _ <- put(hex"80")
        _ <- put(ByteVector.empty)
        value <- streamFrom(hex"01")
      yield value

//      val forPrint = for
//        (state1, _)     <- put(hex"80").run(initialState)
//        (state2, _)     <- put(ByteVector.empty).run(state1)
//        (state3, value) <- streamFrom(hex"01").run(state2)
//        resultList      <- value.compile.toList
//      yield
//        Seq(state1, state2, state3).zipWithIndex.foreach: (s, i) =>
//          println(s"====== State #${i + 1} ======")
//          println(s"root: ${s.root}")
//          s.diff.foreach { (hash, node) => println(s" $hash: $node") }
//        println(s"========")
//        println(s"result: ${resultList}")
//
//        value
//
//      val result = forPrint
//        .flatMap(_.compile.toList)
//        .value
//        .unsafeRunSync()

      val resultIO = program
        .runA(initialState)
        .flatMap: stream =>
          stream.compile.toList
        .value

      val result = resultIO.unsafeRunSync()

      val expected: List[(Nibbles, ByteVector)] = List((hex"80".toNibbles, ByteVector.empty))

      assertions.assertEquals(result, expected.asRight[String])

  test("put empty -> reverseStreamFrom (00, None)"):
    withMunitAssertions: assertions =>

      def put(key: ByteVector) = MerkleTrie.put[SyncIO](key.toNibbles, ByteVector.empty)
      def reverseStreamFrom(keyPrefix: ByteVector, keySuffix: Option[Nibbles]) =
        MerkleTrie.reverseStreamFrom[SyncIO](keyPrefix.toNibbles, keySuffix)

      val program = for
        _     <- put(ByteVector.empty)
        value <- reverseStreamFrom(hex"00", None)
      yield value

//      val forPrint = for
//        (state1, _)     <- put(ByteVector.empty).run(initialState)
//        (state2, value) <- reverseStreamFrom(hex"00", None).run(state1)
//        resultList      <- value.compile.toList
//      yield
//        Seq(state1, state2).zipWithIndex.foreach: (s, i) =>
//          println(s"====== State #${i + 1} ======")
//          println(s"root: ${s.root}")
//          s.diff.foreach { (hash, node) => println(s" $hash: $node") }
//        println(s"========")
//        println(s"result: ${resultList}")
//
//        value
//
//      forPrint
//        .flatMap(_.compile.toList)
//        .value
//        .unsafeRunSync()

      val resultIO = program
        .runA(initialState)
        .flatMap: stream =>
          stream.compile.toList
        .value

      resultIO.unsafeRunSync()
      val result = resultIO.unsafeRunSync()
      val expected: List[(Nibbles, ByteVector)] = List.empty

      assertions.assertEquals(result, expected.asRight[String])

  test("put 00 -> reverseStreamFrom (empty, None)"):
    withMunitAssertions: assertions =>

      def put(key: ByteVector) = MerkleTrie.put[SyncIO](key.toNibbles, ByteVector.empty)
      def reverseStreamFrom(keyPrefix: ByteVector, keySuffix: Option[Nibbles]) =
        MerkleTrie.reverseStreamFrom[SyncIO](keyPrefix.toNibbles, keySuffix)

      val program = for
        _     <- put(hex"00")
        value <- reverseStreamFrom(ByteVector.empty, None)
      yield value

//      val forPrint = for
//        (state1, _)     <- put(hex"00").run(initialState)
//        (state2, value) <- reverseStreamFrom(ByteVector.empty, None).run(state1)
//        resultList      <- value.compile.toList
//      yield
//        Seq(state1, state2).zipWithIndex.foreach: (s, i) =>
//          println(s"====== State #${i + 1} ======")
//          println(s"root: ${s.root}")
//          s.diff.foreach { (hash, node) => println(s" $hash: $node") }
//        println(s"========")
//        println(s"result: ${resultList}")
//
//        value
//
//      forPrint
//        .flatMap(_.compile.toList)
//        .value
//        .unsafeRunSync()

      val resultIO = program
        .runA(initialState)
        .flatMap: stream =>
          stream.compile.toList
        .value

      resultIO.unsafeRunSync()
      val result = resultIO.unsafeRunSync()

      val expected: List[(Nibbles, ByteVector)] = List((hex"00".toNibbles, ByteVector.empty))

      assertions.assertEquals(result, expected.asRight[String])


  test("put empty -> put 00 -> reverseStreamFrom (00, None)"):
    withMunitAssertions: assertions =>

      def put(key: ByteVector) = MerkleTrie.put[SyncIO](key.toNibbles, ByteVector.empty)
      def reverseStreamFrom(keyPrefix: ByteVector, keySuffix: Option[Nibbles]) =
        MerkleTrie.reverseStreamFrom[SyncIO](keyPrefix.toNibbles, keySuffix)

      val program = for
        _     <- put(ByteVector.empty)
        _     <- put(hex"00")
        value <- reverseStreamFrom(hex"00", None)
      yield value

//      val forPrint = for
//        (state1, _)     <- put(ByteVector.empty).run(initialState)
//        (state2, _)     <- put(hex"00").run(state1)
//        (state3, value) <- reverseStreamFrom(hex"00", None).run(state2)
//        resultList      <- value.compile.toList
//      yield
//        Seq(state1, state2, state3).zipWithIndex.foreach: (s, i) =>
//          println(s"====== State #${i + 1} ======")
//          println(s"root: ${s.root}")
//          s.diff.foreach { (hash, node) => println(s" $hash: $node") }
//        println(s"========")
//        println(s"result: ${resultList}")
//        value
//
//      forPrint
//        .flatMap(_.compile.toList)
//        .value
//        .unsafeRunSync()

      val resultIO = program
        .runA(initialState)
        .flatMap: stream =>
          stream.compile.toList
        .value

      resultIO.unsafeRunSync()
      val result = resultIO.unsafeRunSync()

      val expected: List[(Nibbles, ByteVector)] = List(
        (hex"00".toNibbles, ByteVector.empty),
      )

      assertions.assertEquals(result, expected.asRight[String])

  test("put 00 -> put 0000 -> reverseStreamFrom (empty, None)"):
    withMunitAssertions: assertions =>

      def put(key: ByteVector) = MerkleTrie.put[SyncIO](key.toNibbles, ByteVector.empty)
      def reverseStreamFrom(keyPrefix: ByteVector, keySuffix: Option[Nibbles]) =
        MerkleTrie.reverseStreamFrom[SyncIO](keyPrefix.toNibbles, keySuffix)

      val program = for
        _     <- put(hex"00")
        _     <- put(hex"0000")
        value <- reverseStreamFrom(ByteVector.empty, None)
      yield value

      val resultIO = program
        .runA(initialState)
        .flatMap: stream =>
          stream.compile.toList
        .value

      resultIO.unsafeRunSync()
      val result = resultIO.unsafeRunSync()

      val expected: List[(Nibbles, ByteVector)] = List(
        (hex"0000".toNibbles, ByteVector.empty),
        (hex"00".toNibbles, ByteVector.empty),
      )

      assertions.assertEquals(result, expected.asRight[String])


  test("put 0000 -> put 10 -> reverseStreamFrom (10, None)"):
    withMunitAssertions: assertions =>

      def put(key: ByteVector) = MerkleTrie.put[SyncIO](key.toNibbles, ByteVector.empty)
      def reverseStreamFrom(keyPrefix: ByteVector, keySuffix: Option[Nibbles]) =
        MerkleTrie.reverseStreamFrom[SyncIO](keyPrefix.toNibbles, keySuffix)

      val program = for
        _     <- put(hex"0000")
        _     <- put(hex"10")
        value <- reverseStreamFrom(hex"10", None)
      yield value

      val resultIO = program
        .runA(initialState)
        .flatMap: stream =>
          stream.compile.toList
        .value

      resultIO.unsafeRunSync()
      val result = resultIO.unsafeRunSync()

      val expected: List[(Nibbles, ByteVector)] = List(
        (hex"10".toNibbles, ByteVector.empty),
      )

      assertions.assertEquals(result, expected.asRight[String])

  property("test merkle trie"):
    sequential(
      range = Range.linear(1, 100),
      initial = State.empty,
      commands = List(
        commandGet,
        commandPut,
        commandRemove,
        commandStreamFrom,
        commandReverseStreamFrom,
      ),
      cleanup = () => merkleTrieState = MerkleTrieState.empty,
    )
