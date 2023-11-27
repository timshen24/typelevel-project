package com.rockthejvm.jobsboard.http.routes

import io.circe.generic.auto.*
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*
import org.http4s.circe.CirceEntityCodec.*
import cats.effect.*
import cats.implicits.*

import scala.collection.mutable
import java.util.UUID
import com.rockthejvm.jobsboard.domain.Job.*
import com.rockthejvm.jobsboard.http.responses.*

// 为什么在Jobs Endpoints implementation课里（16:00处）把最初的Mond改成了Concurrent？Circe的要求
class JobRoutes[F[_]: Concurrent] private extends Http4sDsl[F] {
  // "database"
  private val database = mutable.Map[UUID, Job]()

  // POST /jobs?offset=x&limit=y { filters } // TODO add query params and filters
  private val allJobsRoute: HttpRoutes[F] = HttpRoutes.of[F] { case POST -> Root =>
    Ok(database.values)
  }

  // GET /jobs/uuid
  private val findJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / UUIDVar(id) =>
    database.get(id) match {
      case Some(job) => Ok(job)
      case None      => NotFound(FailureResponses(s"Job $id not found."))
    }
  }

  // POST /jobs/create {jobInfo}
  private def createJob(jobInfo: JobInfo): F[Job] =
    Job(
      id = UUID.randomUUID(),
      date = System.currentTimeMillis(),
      ownerEmail = "TODO@rockthejvm.com",
      jobInfo = jobInfo,
      active = true
    ).pure[F]

  // Test case: http post localhost:4041/api/jobs/create company='Rock the JVM' title='Software Engineer' description='best job' externalUrl='rockthejvm.com' remote=false location='NYC'
  private val createJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root / "create" =>
    for {
      jobInfo <- req.as[JobInfo] // F[JobInfo]
      job     <- createJob(jobInfo)
      resp    <- Created(job.id)
    } yield resp
  }

  // PUT /jobs/uuid {jobInfo}
  private val updateJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case req @ PUT -> Root / UUIDVar(id) =>
    database.get(id) match
      case Some(job) =>
        for {
          jobInfo <- req.as[JobInfo]
          _       <- database.put(id, job.copy(jobInfo = jobInfo)).pure[F]
          resp    <- Ok()
        } yield resp
      case None => NotFound(FailureResponses(s"Cannot update job $id: not found"))
  }

  // DELETE /jobs/uuid
  private val deleteJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case req @ DELETE -> Root / UUIDVar(id) =>
    database.get(id) match
      case Some(_) =>
        for {
          _    <- database.remove(id).pure[F]
          resp <- Ok()
        } yield resp
      case None => NotFound(FailureResponses(s"Cannot delete job $id: not found"))
  }

  val routes = Router(
    "/jobs" -> (allJobsRoute <+> findJobRoute <+> createJobRoute <+> updateJobRoute <+> deleteJobRoute)
  )
}

object JobRoutes {
  def apply[F[_]: Concurrent] = new JobRoutes[F]
}
