package io.leisuremeta.chain.backend2
import cats.*
import cats.effect.*

// implicit class Loggable[A](io: IO[A]):
//   def log: IO[A] = io.flatMap { a =>
//     IO {
//       println(s"[log] ${a}")
//     }.map(_ => a)
//   }
//   def logM(msg: String = "DEBUG"): IO[A] = io.flatMap { a =>
//     IO {
//       println(s"[$msg] ${a}")
//     }.map(_ => a)
//   }

object Loggable:
  extension [A](io: IO[A])
    def log: IO[A] = io.flatMap { a =>
      IO {
        println(s"[log] ${a}")
      }.map(_ => a)
    }
  extension [A](io: IO[A])
    def logM(msg: String = "DEBUG"): IO[A] = io.flatMap { a =>
      IO {
        println(s"[$msg] ${a}")
      }.map(_ => a)
    }
