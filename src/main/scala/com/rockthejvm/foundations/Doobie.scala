package com.rockthejvm.foundations

import cats.effect.kernel.MonadCancelThrow
import cats.effect.{IO, IOApp}
import doobie.hikari.HikariTransactor
import doobie.util.transactor.Transactor
import doobie.util.ExecutionContexts
import doobie.implicits.*

/**
 * go to project path and run `docker-compose up`
 * docker ps
 * docker exec -it typelevel-project-db-1 psql -U docker
 * create database demo
 * \c demo
 * create table students(id serial not null, name character varying not null, primary key(id));
 * select * from students;
 * insert into students(id, name) values (1, 'daniel');
 * insert into students(id, name) values (2, 'master yoda');
 * select * from students;
 * insert into students(id, name) values (4, 'mike');
 * truncate students;
 * */
object Doobie extends IOApp.Simple {
  case class Student(id: Int, name: String)

  val xa: Transactor[IO] = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver", // JDBC connector
    "jdbc:postgresql:/demo", // jdbc:postgresql://localhost:5432/demo
    "docker",
    "docker"
  )

  def findAllStudentNames: IO[List[String]] = {
    val query = sql"select name from students".query[String] // result field type `name` is String
    val action = query.to[List]
    action.transact(xa)
  }

  def saveStudent(id: Int, name: String): IO[Int] = { // tell us effective number of rows inserted
    val query = sql"insert into students(id, name) values ($id, $name)"
    val action = query.update.run
    action.transact(xa)
  }

  // read as CC with fragments
  def findStudentsByInitial(letter: String): IO[List[Student]] = {
    val selectPart = fr"select id, name"
    val fromPart = fr"from students"
    val wherePart = fr"where left(name, 1) = $letter"

    val statement = selectPart ++ fromPart ++ wherePart
    val action = statement.query[Student].to[List]
    action.transact(xa)
  }

  // how to organize code?
  trait Students[F[_]] { // "repository"
    def findById(id: Int): F[Option[Student]]
    def findAll: F[List[Student]]
    def create(name: String): F[Int]
  }

  object Students {
    def make[F[_]: MonadCancelThrow](xa: Transactor[F]): Students[F] = new Students[F] {
      def findById(id: Int): F[Option[Student]] =
        sql"select id, name from students where id = $id".query[Student].option.transact(xa) // here I am not going to return a List of students, but an option of student, so I called the option method.

      def findAll: F[List[Student]] =
        sql"select id, name from students".query[Student].to[List].transact(xa)

      def create(name: String): F[Int] =
        sql"insert into students(name) values ($name)".update.withUniqueGeneratedKeys[Int]("id").transact(xa)
    }
  }

  val postgresResource = for {
    ec <- ExecutionContexts.fixedThreadPool[IO](16)
    xa <- HikariTransactor.newHikariTransactor[IO]("org.postgresql.Driver", // JDBC connector
      "jdbc:postgresql:/demo", // jdbc:postgresql://localhost:5432/demo
      "docker",
      "docker",
      ec)
  } yield xa

  val smallProgram = postgresResource.use { xa =>
    val studentsRepo = Students.make[IO](xa)
    for {
      id <- studentsRepo.create("Daniel")
      daniel <- studentsRepo.findById(id)
      _ <- IO.println(s"The first student of Rock the JVM is $daniel")
    } yield ()
  }

//  override def run: IO[Unit] = findAllStudentNames.map(println)
//  override def run: IO[Unit] = saveStudent(3, "alice").map(println)
//  override def run: IO[Unit] = findStudentsByInitial("m").map(println)
  override def run: IO[Unit] = smallProgram
}
