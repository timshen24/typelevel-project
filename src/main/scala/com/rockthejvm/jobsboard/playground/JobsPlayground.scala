package com.rockthejvm.jobsboard.playground

import cats.effect.*
import doobie.*
import doobie.implicits.*
import doobie.util.*
import doobie.hikari.HikariTransactor
import com.rockthejvm.jobsboard.core.*
import com.rockthejvm.jobsboard.domain.Job.JobInfo
import scala.io.StdIn

/**
  * Begins with lesson "Running Jobs'Algebra'"
  * 在runner的terminal里sbt "runMain com.rockthejvm.jobsboard.playground.JobsPlayground"
  * 遇到问题，org.postgresql.util.PSQLException: ERROR: column "tags" of relation "jobs" does not exist
  * 过了十分钟找到错误原因：relation就是表的意思，jobs表里tags列名错误写成了'tages'。
  * 重新修改init.sql文件，然后stop container, 删除container，删除image后重新执行docker-compose up
  * 
  * 又遇到问题：org.postgresql.util.PSQLException: ERROR: syntax error at or near "WHERE"
  * 原因在于，update方法中，最后一个update字段other = ${jobInfo.other}后面不能有逗号
  */
object JobsPlayground extends IOApp.Simple {

  val postgresResource: Resource[IO, HikariTransactor[IO]] = for {
    ec <- ExecutionContexts.fixedThreadPool(32)
    xa <- HikariTransactor.newHikariTransactor[IO](
      "org.postgresql.Driver",
      "jdbc:postgresql:board",
      "docker",
      "docker",
      ec
    )
  } yield xa

  val jobInfo = JobInfo.minimal(
    company = "Rock the JVM",
    title = "Software Engineer",
    description = "Best job ever",
    externalUrl = "rockthejvm.com",
    remote = true,
    location = "Anywhere"
  )

  override def run: IO[Unit] = postgresResource.use { xa =>
    for {
      jobs      <- LiveJobs[IO](xa)
      _         <- IO(println("Ready. Next ...")) *> IO(StdIn.readLine)
      id        <- jobs.create("daniel@rockthejvm.com", jobInfo)
      _         <- IO(println("Next...")) *> IO(StdIn.readLine)
      list      <- jobs.all()
      _         <- IO(println(s"All jobs: $list. Next...")) *> IO(StdIn.readLine)
      _         <- jobs.update(id, jobInfo.copy(title = "Software rockstar"))
      newJob    <- jobs.find(id)
      _         <- IO(println(s"New job: $newJob. Next...")) *> IO(StdIn.readLine)
      _         <- jobs.delete(id)
      listAfter <- jobs.all()
      _         <- IO(println(s"Deleted job. List now $listAfter. Next...")) *> IO(StdIn.readLine)
    } yield ()
  }
  // "Running Jobs 'Algebra'课程结束后的问题，怎么把这段Prosgres的操作和父目录里Application里的http的操作整合到一起呢？"
}
