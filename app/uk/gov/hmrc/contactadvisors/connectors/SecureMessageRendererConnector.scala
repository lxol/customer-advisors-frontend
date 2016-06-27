/*
 * Copyright 2016 HM Revenue & Customs
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

import play.mvc.Http.Status
import uk.gov.hmrc.contactadvisors.WSHttp
import uk.gov.hmrc.contactadvisors.connectors.models.AdviceCreationBody
import uk.gov.hmrc.contactadvisors.domain._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost, Upstream4xxResponse}
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

abstract class SecureMessageRendererConnector extends AdviceRepository {

  def http: HttpPost
  def serviceUrl: String

  override def saveNew(advice: Advice, utr: String)
                      (implicit hc: HeaderCarrier): Future[StorageResult] =
    http.POST(url = s"$serviceUrl/advice", body = AdviceCreationBody.from(advice, utr)).
      map { _ => AdviceStored }.
      recover {
        case Upstream4xxResponse(_, Status.CONFLICT, _, _) => AdviceAlreadyExists
        case ex => UnexpectedError(ex.getMessage)
      }

}

object SecureMessageRendererConnector extends SecureMessageRendererConnector with ServicesConfig {
  override def http: HttpPost = WSHttp

  override def serviceUrl: String = baseUrl("secure-message-renderer")
}
