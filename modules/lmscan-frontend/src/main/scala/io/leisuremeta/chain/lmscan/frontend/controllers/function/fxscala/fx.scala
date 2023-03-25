package io.leisuremeta.chain.lmscan.frontend

import io.leisuremeta.chain.lmscan.frontend.Products.*
import scala.util.chaining.*

case class ProductCase(name: String, price: Int)

object Products:
  val todayProducts = List(
    ProductCase(name = "반팔티", price = 123),
    ProductCase(name = "긴팔티", price = 15000),
    ProductCase(name = "크롭티", price = 300),
    ProductCase(name = "팬티", price = 500),
    ProductCase(name = "싼티", price = 800),
  )

  val inName      = (a: ProductCase) => a.name
  val inPrice     = (a: ProductCase) => a.price
  val filterUp500 = (a: ProductCase) => a.price > 500
  val filterName  = (a: ProductCase) => a.name == "팬티"

object Fx:
  def map[A, B](f: (A) => B, xs: Seq[A]): Seq[B] =
    for x <- xs
    yield f(x)

  def filter[A](f: (A) => Boolean, xs: Seq[A]): Seq[A] =
    for x <- xs if (f(x))
    yield x

  def reduce2[A](f: (A, A) => A, xs: Seq[A]): A =
    val acc = xs(0)
    for
      x <- xs
      result = f(x, acc)
    yield result
    acc

  def reduce3[A, B](f: (A, B) => B, acc: B, xs: Seq[A]): B =
    for
      x <- xs
      result = f(x, acc)
    yield result
    acc

  def reduce_recursive[A](f: (A, A) => A, acc: A, iter: List[A]): A =
    iter match
      case Nil     => acc
      case x :: xs => reduce_recursive(f, f(acc, x), xs)

  // js
  // const go = (...args) => reduce((a,f) => f(a),args)
  // const pipe = (...fs) => a => go(a,...fs)

  // def go[A](xs: Seq[A]) = reduce2[A, A => A]((a: A, f: A => A) => f(a), xs)
  // def pipe(df, *functions): A
  //   res = df
  //   for f in functions:
  //       res = f(res)
  //   return res

  def pipe[A](df: A, functions: Seq[A => A]): A =
    functions.foldLeft(df) { (res, f) => f(res) }

  val pipe_int = pipe[Int](1, Seq(a => a + 1))

  val pipe_products = pipe[Seq[ProductCase]](
    Products.todayProducts,
    Seq(products => filter(filterUp500, products)),
  )
  val reduce_product = reduce2(
    (a: Int, b: Int) => a + b,
    map(inPrice, filter(filterUp500, Products.todayProducts)),
  )

  val pipe_ex_int = 1
    .pipe((a) => a + 1)
    .pipe(a => a * 2)
    .tap(res => println(s"DEBUG: x = $res"))
    .pipe(a => a * 2)

  def view = pipe_ex_int

  // pipe(
  //   Products.todayProducts,
  //   Seq((a) => filter(filterUp500, a)),
  // )

  // val x = 1
  //   .pipe((a) => a + 1)
  //   .pipe(a => a * 2)
  //   .tap(res => println(s"DEBUG: x = $res"))
  //   .pipe(a => a * 2)
  // x
  // go(0, (a: Int) => a + 1)
