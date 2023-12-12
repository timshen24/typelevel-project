package com.rockthejvm.jobsboard.http.validation

import cats.*
import cats.data.*
import cats.data.Validated.*
import cats.implicits.*
import scala.util.Try
import scala.util.Failure
import scala.util.Success
import java.net.URL

import com.rockthejvm.jobsboard.domain.job.*

// HTTP Payload Validation, 用testOnly com.rockthejvm.jobsboard.http.routes.JobRoutesSpec来测试
object validators {
  sealed trait ValidationFailure(val errorMessage: String)
  case class EmptyField(fieldName: String) extends ValidationFailure(s"'$fieldName' is empty")
  case class InvalidUrl(fieldName: String) extends ValidationFailure(s"'$fieldName' is not a valid URL")

  type ValidationResult[A] = ValidatedNel[ValidationFailure, A]

  // Type class的实际运用
  trait Validator[A] {
    def validate(value: A): ValidationResult[A]
  }

  def validateRequired[A](field: A, fieldName: String)(required: A => Boolean): ValidationResult[A] =
    if (required(field)) field.validNel
    else EmptyField(fieldName).invalidNel

  def validateUrl(field: String, fieldName: String): ValidationResult[String] =
    Try(URL(field).toURI()) match // throws some exception
      case Success(_)         => field.validNel
      case Failure(exception) => InvalidUrl(fieldName).invalidNel

  // 这个Validator[JobInfo]就是给“def validate[A: Validator]”用的
  given jobInfoValidator: Validator[JobInfo] = (jobInfo: JobInfo) => {
    val JobInfo(
      company,
      title,
      description,
      externalUrl,
      remote,
      location,
      salaryLo,
      salaryHi,
      currency,
      country,
      tags,
      image,
      seniority,
      other
    ) = jobInfo

    val validCompany     = validateRequired(company, "company")(_.nonEmpty)
    val validTitle       = validateRequired(title, "title")(_.nonEmpty)
    val validDescription = validateRequired(description, "description")(_.nonEmpty)
    val validExternalUrl = validateUrl(externalUrl, "externalUrl")
    val validLocation    = validateRequired(company, "location")(_.nonEmpty)

    (
      validCompany,       // company
      validTitle,         // title
      validDescription,   // description
      validExternalUrl,   // externalUrl
      remote.validNel,    // remote
      validLocation,      // location
      salaryLo.validNel,  // salaryLo
      salaryHi.validNel,  // salaryHi
      currency.validNel,  // currency
      country.validNel,   // country
      tags.validNel,      // tags
      image.validNel,     // image
      seniority.validNel, // seniority
      other.validNel      // other
    ).mapN(JobInfo.apply) // ValidatedNel[ValidationFailure, JobInfo]
  }
}
