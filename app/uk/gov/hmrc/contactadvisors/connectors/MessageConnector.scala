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

import play.api.libs.json.Json
import play.mvc.Http.Status
import uk.gov.hmrc.contactadvisors.WSHttp
import uk.gov.hmrc.contactadvisors.connectors.models.SecureMessage
import uk.gov.hmrc.contactadvisors.domain._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost, Upstream4xxResponse}

import scala.concurrent.Future
import scala.util.Try

trait MessageConnector {

  def http: HttpPost

  def serviceUrl: String

  import scala.concurrent.ExecutionContext.Implicits.global

  def create(secureMessage: SecureMessage)
            (implicit hc: HeaderCarrier): Future[StorageResult] = {

    val createMessageAPIurl: String = s"$serviceUrl/messages"

    http.POST(url = createMessageAPIurl, body = secureMessage).
      flatMap { response =>
        response.status match {
          case Status.CREATED => Future.fromTry(Try {
            (response.json \ "id").as[String]
          }).map(messageId => AdviceStored(messageId)).
            recoverWith { case x => Future.successful(UnexpectedError(s"Missing id in: '${response.body}'")) }
          case status => Future.successful(UnexpectedError(s"Unexpected status : $status from $createMessageAPIurl"))
        }
      }.
      recoverWith {
        case Upstream4xxResponse(conflictMessage, Status.CONFLICT, _, _) => Future.successful(AdviceAlreadyExists)
        case ex => Future.successful(UnexpectedError(ex.getMessage))
      }
  }
}

object MessageConnector extends MessageConnector with ServicesConfig {
  override def http: HttpPost = WSHttp
  override def serviceUrl: String = baseUrl("message")
}
