package com.rockthejvm.jobsboard.modules

import cats.effect.*
import cats.implicits.*

import com.rockthejvm.jobsboard.core.*
import doobie.util.transactor.Transactor

final class Core[F[_]] private (val jobs: Jobs[F])

// postgres -> jobs -> core -> httpApi -> app
object Core {
  def apply[F[_]: Async](xa: Transactor[F]): Resource[F, Core[F]] =
    Resource
      .eval(LiveJobs[F](xa)) // eval takes an effect that returns a value and wraps that in a resource.
      .map(jobs => new Core(jobs))
}
