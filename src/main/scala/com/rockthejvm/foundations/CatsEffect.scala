package com.rockthejvm.foundations

import cats.MonadError
import cats.effect.*

import java.io.{File, FileWriter, PrintWriter}
import scala.concurrent.duration.*
import scala.io.StdIn
import scala.util.Random

/**
 * describing computations as values
 */
// IO = data structure describing arbitrary computations (including side effects)
object CatsEffect extends /*IOApp*/ IOApp.Simple {
  val firstIO: IO[Int] = IO.pure(42)
  val delayedIO: IO[Int] = IO.apply {
    // complex code
    println("I'm just about to produce the meaning of life")
    42
  }

  def evaluateIO[A](io: IO[A]): Unit = {
    import cats.effect.unsafe.implicits.global // "platform". This includes a thread pool with an event loop and so on and so forth.
    val meaningOfLife = io.unsafeRunSync() // but this is not an ideal way. The ideal way is to extend IOApp
    println(s"the result of the effect is: $meaningOfLife")
  }

  // transformations
  // map + flatMap
  val improvedMeaningOfLife = firstIO.map(_ * 2)
  val printedMeaningOfLife = firstIO.flatMap(mol => IO(println(mol)))
  // for-comprehensions
  def smallProgram(): IO[Unit] = for {
    line1 <- IO(StdIn.readLine())
    line2 <- IO(StdIn.readLine())
    _ <- IO(println(line1 + line2))
  } yield ()

//  Below is the old style of standard Scala apps, which is deprecated in cats-effect.
//  def main(args: Array[String]): Unit = {
//    evaluateIO(delayedIO)
//  }

  // raise/"catch" errors in IO, also known as functional cats-effect try-catch methods
  val aFailure: IO[Int] = IO.raiseError(new RuntimeException("a proper failure"))
  val dealWithIt = aFailure.handleErrorWith { case _: RuntimeException =>
    IO.println("I'm still here, no worries")
  }

  // fibers = "lightweight threads. Descriptions of computations that can run in parallel"
  val delayedPrint = IO.sleep(1.second) *> IO(println(Random.nextInt(100)))
  val manyPrints = for {
    _ <-
      delayedPrint // These two delayedPrint will be performed sequentially. It means you will have to wait two seconds. Wait one second, println then wait another second and then print
    _ <- delayedPrint
  } yield ()

  val manyPrints_v2 = for {
    fib1 <-
      delayedPrint.start // These two delayedPrint will be performed in parallel now. It means you will have to wait one seconds. Wait a second and then two numbers will be printed roughly the same time.
    fib2 <- delayedPrint.start
    _ <- fib1.join
    _ <- fib2.join
  } yield ()

  // Fibers can also be cancelled. Here is an example demonstrating cancelling a fiber 500ms after it is spun.
  val cancelledFiber = for {
    fib <- delayedPrint.onCancel(IO(println("I'm cancelled!"))).start
    _ <- IO.sleep(500.millis) *> IO(println("cancelling fiber")) *> fib.cancel
    _ <-
      fib.join // fiber is very cheap. But when it contains some expensive logic like resource cleaning. It might be useful to wait and clean them up nicely and completely
  } yield ()

  // Mark IO as uncancellation
  val ignoredCancellation = for {
    fib <- IO.uncancelable(_ => delayedPrint.onCancel(IO(println("I'm cancelled!")))).start
    _ <- IO.sleep(500.millis) *> IO(println("cancelling fiber")) *> fib.cancel
    _ <- fib.join
  } yield ()

  // Managing resources
  val readingResource =
    Resource.make(IO(scala.io.Source.fromFile("src/main/scala/com/rockthejvm/foundations/CatsEffect.scala")))(source =>
      IO(println("closing source")) *> IO(source.close))
  val readingEffect = readingResource.use(source => IO(source.getLines().foreach(println)))

  // compose resources
  val copiedFileResource =
    Resource.make(IO(new PrintWriter(new FileWriter(new File("src/main/resources/dumpedFile.scala")))))(writer =>
      IO(println("closing duplicated file")) *> IO(writer.close()))
  val compositeResource = for {
    source <- readingResource
    destination <- copiedFileResource
  } yield (source, destination)

  val copyFileEffect = compositeResource.use {
    case (source, destination) => IO(source.getLines().foreach(destination.println))
  }


  // abstract kinds of computation

  // MonadCancel = cancelable computations
  trait MyMonadCancel[F[_], E] extends MonadError[F, E] {
    trait CancellationFlagResetter {
      def apply[A](fa: F[A]): F[A] // You can use this argument to mark certain pieces of your IO in `IO.uncancelable(_ => delayedPrint.onCancel(IO(println("I'm cancelled!")))).start` in `ignoredCancellation` to be uncancelable and some not to be cancelable.
    }

    def canceled: F[Unit] // This is the definition of a cancelled computation. If I changed some of these IOs (e.g. copiedFileResource etc...) and in the middle I do IO.canceled, the rest of the chain will not EXECUTED!

    def uncancelable[A](poll: CancellationFlagResetter => F[A]): F[A]
  }

  // monadCancel is not used explicited. Normally we use IO, which is enough
  val monadCancelIO: MonadCancel[IO, Throwable] = MonadCancel[IO]
  val uncancelableIO = monadCancelIO.uncancelable(_ => IO(42)) // same as IO.uncancelable(...)

  // CE apps have a "run" method returning an IO, which will internally be evaluated in a main function.
//  override def run: IO[Unit] = smallProgram()
//  override def run: IO[Unit] = manyPrints
//  override def run: IO[Unit] = cancelledFiber
//  override def run: IO[Unit] = ignoredCancellation // It will print "cancelling fiber" but the cancel operation is ignored. Number will be printed as well.
  override def run: IO[Unit] = copyFileEffect // It will print "cancelling fiber" but the cancel operation is ignored. Number will be printed as well.
}
