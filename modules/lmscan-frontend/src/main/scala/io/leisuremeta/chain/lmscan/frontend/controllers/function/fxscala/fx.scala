package io.leisuremeta.chain.lmscan.frontend

import io.leisuremeta.chain.lmscan.frontend.Products.*
import scala.util.chaining.*

case class ProductCase(name: String, price: Int)

object Products:
  val todayProducts = List(
    ProductCase(name = "반팔티", price = 123),
    ProductCase(name = "긴팔티", price = 15000),
    ProductCase(name = "긴팔티", price = 15000),
    ProductCase(name = "크롭티", price = 300),
  )

  val inName      = (a: ProductCase) => a.name
  val inPrice     = (a: ProductCase) => a.price
  val filterUp500 = (a: ProductCase) => a.price > 500
  val filterName  = (a: ProductCase) => a.name == "긴팔티"

object Fx:
  def map[A, B](f: (A) => B, xs: Seq[A]): Seq[B] =
    for x <- xs
    yield f(x)
  def cmap[A, B](f: (A) => B)(xs: Seq[A]): Seq[B] =
    for x <- xs
    yield f(x)

  def filter[A](f: (A) => Boolean, xs: Seq[A]): Seq[A] =
    for x <- xs if (f(x))
    yield x

  def cfilter[A](f: (A) => Boolean)(xs: Seq[A]): Seq[A] =
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

  def pipe[A](df: A, functions: Seq[A => A]): A =
    functions.foldLeft(df) { (res, f) => f(res) }

  def pipe2[A](df: A, functions: (A => A)*): A =
    functions.foldLeft(df) { (res, f) => f(res) }

  // def pipe[B](f: A => B): B = f(self)
  // def pipe3[A, B](df: A, functions: (A => B)*) =
  //   functions.foldLeft(df) { (res, f) => f(res) }

  val pipe_int = pipe[Int](1, Seq(a => a + 1))

  val pipe_products = pipe[Seq[ProductCase]](
    Products.todayProducts,
    Seq(cfilter(filterUp500)),
  )

  val pipe_products2 = pipe2[Seq[ProductCase]](
    Products.todayProducts,
    cfilter(filterUp500),
    // cmap(inPrice),
  )

  val pipe_products3 = pipe2[Seq[ProductCase]](
    Products.todayProducts,
    cfilter(filterUp500),
    // cmap(inPrice),
  )

  val pipe_products4 = Products.todayProducts
    .pipe(cfilter(filterUp500))
    .pipe(d => d.map(d => d.price))
    .pipe(d => d.reduce((a, b) => a + b))

  val reduce_product = reduce2(
    (a: Int, b: Int) => a + b,
    map(inPrice, cfilter(filterUp500)(Products.todayProducts)),
  )

  val pipe_ex_int = 1
    .pipe((a) => a + 1)
    .pipe(a => a * 2)
    .tap(res => println(s"DEBUG: x = $res"))
    .pipe(a => a * 2)

  def view = pipe_products4
