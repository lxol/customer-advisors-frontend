/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.contactadvisors.connectors

import play.api.http.Status
import uk.gov.hmrc.contactadvisors.WSHttp
import uk.gov.hmrc.contactadvisors.domain.{CustomerIsNotPaperless, TaxIdNotFound, UnexpectedFailure, UnknownTaxId}
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

trait EntityResolverConnector {
  def http: HttpGet

  def serviceUrl: String

  def validPaperlessUserWith(utr: SaUtr)(implicit hc: HeaderCarrier): Future[Boolean] = {
    def unexpectedFailure(msg: String) = Future.failed(
      UnexpectedFailure(
        s"""Could not determine if user with utr $utr is paperless: $msg"""
      )
    )

    http.GET(s"$serviceUrl/portal/preferences/sa/$utr").
      flatMap { response =>
        response.status match {
          case Status.OK if (response.json \ "digital").as[Boolean] => Future.successful(true)
          case Status.OK => Future.failed(
            CustomerIsNotPaperless(s"Customer with tax id $utr has not opted in to paperless")
          )
          case code => unexpectedFailure(
            s"""Status code [$code] with response body [${response.body}]"""
          )
        }
      }.
      recoverWith {
        case nfe: NotFoundException => Future.failed(TaxIdNotFound(s"unknown tax id [$utr] provided"))
        case Upstream4xxResponse(msg, code, _, _) => unexpectedFailure(msg)
        case Upstream5xxResponse(msg, code, _) => unexpectedFailure(msg)
        case http: HttpException => unexpectedFailure(
          s"""[${http.responseCode}] ${http.message}"""
        )
      }
  }
}
object EntityResolverConnector extends EntityResolverConnector with ServicesConfig {
  lazy val http: HttpGet = WSHttp

  def serviceUrl: String = baseUrl("entity-resolver")
}

