package com.rockthejvm.jobsboard.config

import pureconfig.ConfigSource
import cats.MonadThrow
import pureconfig.ConfigReader
import cats.implicits.*
import pureconfig.error.ConfigReaderException
import scala.reflect.ClassTag

// This one is for refactoring purposes. See 'Backend Scaffolding', 27:30mins why refactor offers better readability.
object syntax {
  extension (source: ConfigSource)
    def loadF[F[_], A: ClassTag /* What does this mean? */ ](using
        reader: ConfigReader[A],
        F: MonadThrow[F]
    ): F[A] =
      F.pure(source.load[A] /* Either[Errors, A] */ ).flatMap {
        case Left(errors)  => F.raiseError[A](ConfigReaderException(errors))
        case Right(config) => F.pure(config) // EmberConfig instance
      }
}
