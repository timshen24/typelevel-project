package com.rockthejvm.jobsboard.http.routes

import io.circe.generic.auto.*
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*
import org.http4s.circe.CirceEntityCodec.*
import cats.effect.*
import cats.implicits.*
import org.typelevel.log4cats.Logger

import scala.collection.mutable
import java.util.UUID
import com.rockthejvm.jobsboard.core.*
import com.rockthejvm.jobsboard.domain.Job.*
import com.rockthejvm.jobsboard.http.responses.*
import com.rockthejvm.jobsboard.logging.syntax.*

// 为什么在Jobs Endpoints implementation课里（16:00处）把最初的Mond改成了Concurrent？Circe的要求
class JobRoutes[F[_]: Concurrent: Logger] private (jobs: Jobs[F]) extends Http4sDsl[F] {
  // "database" removed in Lesson "A Full jobs CRUD App"
  // private val database = mutable.Map[UUID, Job]()

  // POST /jobs?offset=x&limit=y { filters } // TODO add query params and filters
  private val allJobsRoute: HttpRoutes[F] = HttpRoutes.of[F] { case POST -> Root =>
    for {
      jobsList <- jobs.all()
      resp     <- Ok(jobsList)
    } yield resp
  // Ok(database.values)
  }

  // GET /jobs/uuid
  private val findJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / UUIDVar(id) =>
    // database.get(id) match {
    jobs.find(id).flatMap {
      case Some(job) => Ok(job)
      case None      => NotFound(FailureResponses(s"Job $id not found."))
    }
  }

  // POST /jobs/create {jobInfo}
  // private def createJob(jobInfo: JobInfo): F[Job] =
  //   Job(
  //     id = UUID.randomUUID(),
  //     date = System.currentTimeMillis(),
  //     ownerEmail = "TODO@rockthejvm.com",
  //     jobInfo = jobInfo,
  //     active = true
  //   ).pure[F]

  // Test case: http post localhost:4041/api/jobs/create company='Rock the JVM' title='Software Engineer' description='best job' externalUrl='rockthejvm.com' remote=false location='NYC'
  private val createJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root / "create" =>
    for {
      // _ <- Logger[F].info("Trying to add job")
      // http post localhost:4041/api/jobs/create < src/main/resources/payloads/jobinfo.json
      jobInfo <- req.as[JobInfo].logError(e => s"Parsing payload failed: $e") // F[JobInfo]
      // _       <- Logger[F].info(s"Parsed job info: $jobInfo")
      // job <- createJob(jobInfo) // commented on Lesson "A Full Jobs CRUD App"
      jobId <- jobs.create("TODO@rockthejvm.com", jobInfo)
      // _       <- Logger[F].info(s"Created job: $job")
      // _    <- database.put(job.id, job).pure[F] // FP里面一个没有Type parameter的语句，在for里面用pure方法强行加上F[...] commented on Lesson "A Full Jobs CRUD App"
      resp <- Created(jobId)
    } yield resp
  }

  // PUT /jobs/uuid {jobInfo}
  private val updateJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case req @ PUT -> Root / UUIDVar(id) =>
    // database.get(id) match
    //   case Some(job) =>
    //     for {
    //       jobInfo <- req.as[JobInfo]
    //       _       <- database.put(id, job.copy(jobInfo = jobInfo)).pure[F]
    //       resp    <- Ok()
    //     } yield resp
    //   case None => NotFound(FailureResponses(s"Cannot update job $id: not found"))
    for {
      jobInfo     <- req.as[JobInfo]
      maybeNewJob <- jobs.update(id, jobInfo)
      resp <- maybeNewJob match {
        case Some(job) => Ok()
        case None      => NotFound(FailureResponses(s"Cannot update job $id: not found"))
      }
    } yield resp
  }

  // DELETE /jobs/uuid
  private val deleteJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case req @ DELETE -> Root / UUIDVar(id) =>
    // database.get(id) match
    jobs.find(id).flatMap {
      case Some(job) =>
        for {
          // _    <- database.remove(id).pure[F]
          _    <- jobs.delete(id)
          resp <- Ok()
        } yield resp
      case None => NotFound(FailureResponses(s"Cannot delete job $id: not found"))
    }
  }

  val routes = Router(
    "/jobs" -> (allJobsRoute <+> findJobRoute <+> createJobRoute <+> updateJobRoute <+> deleteJobRoute)
  )
}

object JobRoutes {
  def apply[F[_]: Concurrent: Logger](jobs: Jobs[F]) = new JobRoutes[F](jobs)
}
