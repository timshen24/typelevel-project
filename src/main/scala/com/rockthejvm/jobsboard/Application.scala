package com.rockthejvm.jobsboard

import cats.*
import cats.implicits.*
import cats.effect.*
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*
import cats.effect.*
import org.http4s.ember.server.EmberServerBuilder
import pureconfig.ConfigSource
import pureconfig.error.ConfigReaderException

import com.rockthejvm.jobsboard.http.routes.*
import com.rockthejvm.jobsboard.config.*
import com.rockthejvm.jobsboard.config.syntax.loadF

/** 1 - add a plain health endpoint to our app 
  * 2 - add minimal configuration 
  * 3 - basic http server layout 
  * 4 - Start a terminal window;sbt;~compile to keep compiling latest changes 
  * 5 - Start another terminal window; sbt "runMain com.rockthejvm.jobsboard.Application" 
  * 6 - Start another terminal window; `http get localhost:8080/health` 
  * 7 - After adding application.conf and refactor, go to terminal and enter `http get localhost:4041/health`
  */

object Application extends IOApp.Simple {

  override def run: IO[Unit] = ConfigSource.default.loadF[IO, EmberConfig].flatMap { config =>
    EmberServerBuilder
      .default[IO]
      .withHost(config.host) // String, need Host
      .withPort(config.port) // String, need Port
      // .withHttpApp(HealthRoutes[IO].routes.orNotFound) // In lesson 'Backend Scaffolding', on terminal: type in `http GET 'localhost:8080/health'`
      .withHttpApp(
        HttpApi[IO].endpoints.orNotFound
      ) // In lesson 'Jobs Endpoints', on terminal: type in `http get localhost:4041/api/health` and `http get localhost:4041/api/jobs/780e82c8-26eb-4c2e-b83a-d9ca6287b192`
      .build
      .use(_ => IO(println("Server ready!")) *> IO.never)
  }
}
