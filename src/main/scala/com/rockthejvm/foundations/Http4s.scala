package com.rockthejvm.foundations

import cats.*
import cats.implicits.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import cats.effect.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.headers.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*
import org.typelevel.ci.CIString
import org.http4s.ember.server.EmberServerBuilder


import java.util.UUID

object Http4s extends IOApp.Simple {

  // simulate an HTTP server with "students" and "courses"
  type Student = String
  case class Instructor(firstName: String, lastName: String)
  case class Course(id: String, title: String, year: Int, students: List[Student], instructorName: String)

  object CourseRepository {
    // a "database"

    private val catsEffectCourse = Course(
      "780e82c8-26eb-4c2e-b83a-d9ca6287b192",
      "Rock the JVM Ultimate Scala course",
      2022,
      List("Daniel", "Master Yoda"),
      "Martin Odersky"
    )
    private val courses: Map[String, Course] = Map(catsEffectCourse.id -> catsEffectCourse)

    // API
    def findCoursesById(courseId: UUID): Option[Course] =
      courses.get(courseId.toString)

    def findCoursesByInstructor(name: String): List[Course] =
      courses.values.filter(_.instructorName == name).toList
  }
  // essential REST endpoints
  // GET 'localhost:8080/courses?instructor=Martin%20Odersky&year=2022'
  // GET 'localhost:8080/courses/780e82c8-26eb-4c2e-b83a-d9ca6287b192/students'

  object InstructorQueryParamMatcher extends QueryParamDecoderMatcher[String]("instructor") // must be match with `GET localhost:8080/...instructor=...
  object YearQueryParamMatcher extends OptionalValidatingQueryParamDecoderMatcher[Int]("year")

  def courseRoutes[F[_]: Monad]: HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl.*

    HttpRoutes.of[F] {
      // On terminal: type in `http GET 'localhost:8080/courses?instructor=Martin%20Odersky'`
//      case GET -> Root / "courses" :? InstructorQueryParamMatcher(instructor) => Ok()
      // On terminal: type in `http GET 'localhost:8080/courses?instructor=Martin%20Odersky&year=2001'`
      // On terminal: type in `http GET 'localhost:8080/courses?instructor=Martin%20Odersky&year=abc'`
      case GET -> Root / "courses" :? InstructorQueryParamMatcher(instructor) +& YearQueryParamMatcher(maybeYear) =>
        val courses = CourseRepository.findCoursesByInstructor(instructor)
        maybeYear match
          case Some(y) => y.fold(
            _ => BadRequest("Parameter 'year' is invalid"),
            year => Ok(courses.filter(_.year == year).asJson)
          )
          case None => Ok(courses.asJson)
      // On terminal: type in `http GET 'localhost:8080/courses/780e82c8-26eb-4c2e-b83a-d9ca6287b192/students'`
      // On terminal: type in `http GET 'localhost:8080/courses/780e82c8-26eb-4c2e-b83a-d9ca627b192/students'`
      case GET -> Root / "courses" / UUIDVar(courseId) / "students" =>
        CourseRepository.findCoursesById(courseId).map(_.students) match
//          case Some(students) => Ok(students.asJson)
          case Some(students) => Ok(students.asJson, Header.Raw(CIString("My-custom-header"), "rockthejvm"))
          case None => NotFound(s"No course with $courseId was found")
    }
  }

  def healthEndpoint[F[_]: Monad]: HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl.*
    HttpRoutes.of[F] {
      // On terminal: type in `http GET 'localhost:8080/health'`
      case GET -> Root / "health" => Ok("All going great!")
    }
  }

  def allRoutes[F[_]: Monad]: HttpRoutes[F] = courseRoutes[F] <+> healthEndpoint[F]

  private def routerWithPathPrefixes = Router(
    // On terminal: type in `http GET 'localhost:8080/api/courses/780e82c8-26eb-4c2e-b83a-d9ca6287b192/students'`
    "/api" -> courseRoutes[IO],
    // On terminal: type in `http GET 'localhost:8080/private/health`
    "/private" -> healthEndpoint[IO]
  ).orNotFound

//  override def run: IO[Unit] = IO.println(UUID.randomUUID())
  override def run: IO[Unit] = EmberServerBuilder
    .default[IO]
//    .withHttpApp(courseRoutes[IO].orNotFound)
//    .withHttpApp(allRoutes[IO].orNotFound)
    .withHttpApp(routerWithPathPrefixes)
    .build
    .use(_ => IO(println("Server ready!")) *> IO.never)
}
