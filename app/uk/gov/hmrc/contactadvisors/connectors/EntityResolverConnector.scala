/*
 * Copyright 2019 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}
import play.api.http.Status
import play.api.libs.json.Json
import play.api.{Configuration, Environment}
import uk.gov.hmrc.contactadvisors.domain.UnexpectedFailure
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.{HeaderCarrier, HttpException, Upstream4xxResponse, Upstream5xxResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class EntityResolverConnector @Inject()(http: HttpClient,
                                        runModeConfiguration: Configuration,
                                        servicesConfig: ServicesConfig,
                                        environment: Environment) extends Status {

  lazy val serviceUrl: String = servicesConfig.baseUrl("entity-resolver")

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

case class PaperlessPreference(digital: Boolean)

object PaperlessPreference {
  implicit val formats = Json.format[PaperlessPreference]
}
