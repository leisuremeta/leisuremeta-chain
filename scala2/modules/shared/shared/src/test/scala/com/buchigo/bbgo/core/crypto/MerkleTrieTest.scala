package org.leisuremeta.lmchain.core
package crypto

import cats.Id
import cats.data.{EitherT, Kleisli}//, StateT}
import cats.effect.Sync

import MerkleTrie._
//import model._

class MerkleTrieTest extends munit.ScalaCheckSuite {

  implicit def emptyNodeStore[K, V]: NodeStore[Id, K, V] =
    Kleisli{ (_: MerkleHash[K, V]) => EitherT.pure(None) }

  implicit val idSync: Sync[Id] = new Sync[Id] {

    // Members declared in cats.Applicative
    def pure[A](x: A): Id[A] = x

    // Members declared in cats.ApplicativeError
    def handleErrorWith[A](fa: Id[A])(f: Throwable => Id[A]): Id[A] = fa
    def raiseError[A](e: Throwable): cats.Id[A] = throw new Exception(e)

    // Members declared in cats.effect.Bracket
    def bracketCase[A, B](acquire: Id[A])(use: A => Id[B])
      (release: (A, cats.effect.ExitCase[Throwable]) => Id[Unit]): Id[B] = use(acquire)

    // Members declared in cats.FlatMap
    def flatMap[A, B](fa: Id[A])(f: A => Id[B]): Id[B] = f(fa)
    def tailRecM[A, B](a: A)(f: A => Id[Either[A,B]]): Id[B] = f(a) match {
      case Left(a1) => tailRecM(a1)(f)
      case Right(b) => b
    }

    // Members declared in cats.effect.Sync
    def suspend[A](thunk: => Id[A]): Id[A] = thunk
  }

  val PRIVATE =
    "458f593649971c365618131155477c1db9f69b9d30fe13c6366ebeae889e9bdc"
  
  //def createAccount(n: Int): Acc = {
  //  val key = CryptoOps.fromPrivate(BigInt(PRIVATE, 16) + n)
  //  Acc(f"user$n%03d").addKeyPair(key)
  //}



  test("merkle trie") {
    //val emptyNode = MerkleTrieState.empty[Account.Name, NameState]
    assertEquals(true, true)
  }
}
