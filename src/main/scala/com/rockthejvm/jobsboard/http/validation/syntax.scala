package com.rockthejvm.jobsboard.http.validation

import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import cats.*
import cats.data.*
import cats.data.Validated.*
import cats.implicits.*
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.implicits.*
import org.typelevel.log4cats.Logger

import validators.*

import com.rockthejvm.jobsboard.logging.syntax.*
import com.rockthejvm.jobsboard.http.responses.*

// HTTP Payload Validation, 用testOnly com.rockthejvm.jobsboard.http.routes.JobRoutesSpec来测试
object syntax {
  def validateEntity[A](entity: A)(using validator: Validator[A]): ValidationResult[A] =
    validator.validate(entity)

  trait HttpValidationDsl[F[_]: MonadThrow: Logger] extends Http4sDsl[F] {
    extension /* 这一块一定要删除，[F[_]: MonadThrow: Logger] 浪费一个小时才发现问题！！！ */ (req: Request[F])
      def validate[A: Validator](serverLogicIfValid: A => F[Response[F]])(using EntityDecoder[F, A]): F[Response[F]] =
        req
          .as[A]
          .logError(e => s"Parsing payload failed: $e")
          .map(validateEntity) // F[ValidationResult[A]]
          .flatMap {
            case Valid(entity) =>
              serverLogicIfValid(entity) // F[Response[F]]
            case Invalid(errors) =>
              BadRequest(FailureResponses(errors.toList.map(_.errorMessage).mkString(", ")))
          }
  }
}
