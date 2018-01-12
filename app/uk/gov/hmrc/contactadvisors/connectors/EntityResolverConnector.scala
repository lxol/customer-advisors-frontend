/*
 * Copyright 2018 HM Revenue & Customs
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

import play.api.libs.json.Json
import uk.gov.hmrc.contactadvisors.WSHttp
import uk.gov.hmrc.contactadvisors.domain.UnexpectedFailure
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.http.{ HeaderCarrier, HttpException, HttpGet, Upstream4xxResponse, Upstream5xxResponse }
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

trait EntityResolverConnector {
  def http: HttpGet

  def serviceUrl: String

  def validPaperlessUserWith(utr: SaUtr)(implicit hc: HeaderCarrier): Future[Option[PaperlessPreference]] = {

    implicit val preferenceFormats = PaperlessPreference.formats

    def unexpectedFailure(msg: String) = Future.failed(
      UnexpectedFailure(
        s"""Could not determine if user with utr $utr is paperless: $msg"""
      )
    )

    http.GET[Option[PaperlessPreference]](s"$serviceUrl/portal/preferences/sa/$utr").
      recoverWith {
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

case class PaperlessPreference(digital: Boolean)
object PaperlessPreference {
  implicit val formats = Json.format[PaperlessPreference]
}