package com.rockthejvm.jobsboard.core

import cats.effect.*
import doobie.postgres.implicits.*
import doobie.implicits.*
import org.scalatest.freespec.AsyncFreeSpec
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.Logger

import com.rockthejvm.jobsboard.fixtures.*
import com.rockthejvm.jobsboard.logging.syntax.*
import java.util.UUID
import com.rockthejvm.jobsboard.domain.job.Job
import cats.Id
// In sbt console, either: test or testOnly com.rockthejvm.jobsboard.core.JobsSpec
class JobsSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers with DoobieSpec with JobFixture {
  override val initScript: String = "sql/jobs.sql"

  "Jobs 'algebra'" - {
    given logger: Logger[IO] = Slf4jLogger.getLogger[IO]
    // "should return no job if the given UUID does not exist" in {
    //   transactor.use { xa =>
    //     val program = for {
    //       jobs      <- LiveJobs[IO](xa)
    //       retrieved <- jobs.find(NotFoundJobUuid)
    //       _         <- Logger[IO].info("Trying to return job")
    //       _         <- IO(println(s"retrived = $retrieved"))
    //     } yield retrieved

    //     program.asserting(_ shouldBe None)
    //   }
    // }

    // "should return a job by id" in {
    //   transactor.use { xa =>
    //     val program = for {
    //       jobs      <- LiveJobs[IO](xa)
    //       retrieved <- jobs.find(AwesomeJobUuid)
    //     } yield retrieved

    //     program.asserting(_ shouldBe Some(AwesomeJob))
    //   }
    // }

    "should retrieve all jobs" in {
      transactor.use { xa =>
        val program = for {
          jobs        <- LiveJobs[IO](xa).logError(e => s"Parsing LiveJobs[IO](xa) failed: $e")
          countOfJobs <- sql"SELECT COUNT(*) FROM jobs".query[Int].unique.transact(xa)
          _           <- IO(println(s"countOfJobs = $countOfJobs"))
          ids         <- sql"SELECT id FROM jobs".query[UUID].to[List].transact(xa)
          _           <- IO(println(s"ids = $ids"))
          retrieved <-
            sql"SELECT * FROM jobs WHERE id = '843df718-ec6e-4d49-9289-f799c0f40064'"
              .query[Job]
              .option
              .transact(xa)
              .logError(e => s"Parsing 'SELECT * FROM jobs' failed: $e")
          _ <- IO(println(s"retrieved = $retrieved"))
        } yield countOfJobs

        program.asserting(_ shouldBe 1)
      }
    }

    // "should return count(1) in" {
    //   transactor.use { xa =>
    //     val program = for {
    //       jobs    <- LiveJobs[IO](xa)
    //       counted <- jobs.count()
    //       _       <- Logger[IO].info(s"counted = $counted")
    //     } yield counted

    //     program.asserting { _ shouldBe 1 }
    //   }
    // }

    // "should create a new job" in {
    //   transactor.use { xa =>
    //     val program = for {
    //       jobs     <- LiveJobs[IO](xa)
    //       jobId    <- jobs.create("daniel@rockthejvm.com", RockTheJvmNewJob)
    //       maybeJob <- jobs.find(jobId)
    //     } yield maybeJob

    //     program.asserting(_.map(_.jobInfo) shouldBe Some(RockTheJvmNewJob))
    //   }
    // }

    // "should return an updated job" in {
    //   transactor.use { xa =>
    //     val program = for {
    //       jobs            <- LiveJobs[IO](xa)
    //       maybeUpdatedJob <- jobs.update(AwesomeJobUuid, UpdatedAwesomeJob.jobInfo)
    //     } yield maybeUpdatedJob

    //     program.asserting(_.map(_.jobInfo) shouldBe Some(UpdatedAwesomeJob))
    //   }
    // }

    // "should return None when trying to update a job that does not exist" in {
    //   transactor.use { xa =>
    //     val program = for {
    //       jobs            <- LiveJobs[IO](xa)
    //       maybeUpdatedJob <- jobs.update(NotFoundJobUuid, UpdatedAwesomeJob.jobInfo)
    //     } yield maybeUpdatedJob

    //     program.asserting(_ shouldBe None)
    //   }
    // }

    // "should delete an existing job" in {
    //   transactor.use { xa =>
    //     val program = for {
    //       jobs                <- LiveJobs[IO](xa)
    //       numberOfDeletedJobs <- jobs.delete(AwesomeJobUuid)
    //       countOfJobs         <- sql"SELECT COUNT(*) FROM jobs WHERE id = $AwesomeJobUuid".query[Int].unique.transact(xa)
    //     } yield (numberOfDeletedJobs, countOfJobs)

    //     program.asserting { case (numberOfDeletedJobs, countOfJobs) =>
    //       numberOfDeletedJobs shouldBe 1
    //       countOfJobs shouldBe 0
    //     }
    //   }
    // }

    // "should return zero updated rows if the job ID to delete is not found" in {
    //   transactor.use { xa =>
    //     val program = for {
    //       jobs                <- LiveJobs[IO](xa)
    //       numberOfDeletedJobs <- jobs.delete(NotFoundJobUuid)
    //       countOfJobs         <- sql"SELECT COUNT(*) FROM jobs WHERE id = $AwesomeJobUuid".query[Int].unique.transact(xa)
    //     } yield numberOfDeletedJobs

    //     program.asserting(_ shouldBe 0)
    //   }
    // }
  }
}
