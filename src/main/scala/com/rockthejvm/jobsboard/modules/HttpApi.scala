package com.rockthejvm.jobsboard.modules

import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*
import cats.effect.*
import cats.implicits.*
import org.typelevel.log4cats.Logger

import com.rockthejvm.jobsboard.http.routes.*
import com.rockthejvm.jobsboard.modules.Core.*

// Huge refactor in lesson "A Full Jobs CRUD App"
class HttpApi[F[_]: Concurrent: Logger] private (val core: Core[F]) {
  private val healthRoutes = HealthRoutes[F].routes
  private val jobRoutes    = JobRoutes[F](core.jobs).routes

  val endpoints = Router(
    "/api" -> (healthRoutes <+> jobRoutes)
  )
}

object HttpApi {
  def apply[F[_]: Concurrent: Logger /* 每一处都要完全对齐，不然稍不留意就会出错！ */ ](core: Core[F]): Resource[F, HttpApi[F]] =
    Resource.pure(new HttpApi[F](core))
}
