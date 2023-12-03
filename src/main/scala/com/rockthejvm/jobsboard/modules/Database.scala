package com.rockthejvm.jobsboard.modules

import cats.effect.*
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts

import com.rockthejvm.jobsboard.config.*

object Database {
  def makePostgresResource[F[_]: Async](config: PostgresConfig): Resource[F, HikariTransactor[F]] =
    for {
      ec <- ExecutionContexts.fixedThreadPool(config.nThreads)
      xa <- HikariTransactor.newHikariTransactor[F](
        "org.postgresql.Driver",
        config.url, // TODO: move to config
        config.user,
        config.pass,
        ec
      )
    } yield xa
}
