import scala.util.chaining.*

// custom function to multiply the result by a given factor
def multiply(factor: Int)(result: Int): Int = result * factor

// sequence of functions to be applied to the initial value
def plus1(i: Int)  = i + 1
def double(i: Int) = i * 2
def square(i: Int) = i * i

// initial value
val value = 1

// custom pipe function using reduce function
val customPipe = List(plus1 _, double _, square _)
  .reduce((f1, f2) => x => f1(x).pipe(f2))(value)
  .pipe(multiply(10))

// println(customPipe) // output: 400

// == todo : fix this code
//   def pipe[A, B, C](f: A => B, g: B => C): A => C =
//     (a: A) =>
//       for
//         b <- f(a)
//         c <- g(b)
//       yield c

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
