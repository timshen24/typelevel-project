package com.rockthejvm.jobsboard.http.routes

import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*
import cats.*

class HealthRoutes[F[_]: Monad] private extends Http4sDsl[F] {
  val healthRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    // On terminal: type in `http GET 'localhost:8080/health'`
    case GET -> Root => Ok("All going great!")
  }

  val routes = Router(
    "/health" -> healthRoute
  )
}

object HealthRoutes {
  def apply[F[_]: Monad] = new HealthRoutes[F]
}
