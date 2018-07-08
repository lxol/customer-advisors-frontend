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

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{ JsObject, Json }
import play.api.{Configuration, Environment}
import play.mvc.Http.Status
import uk.gov.hmrc.contactadvisors.connectors.models.{SecureMessage, SecureMessageV2}
import uk.gov.hmrc.contactadvisors.domain._
import uk.gov.hmrc.http.{HeaderCarrier, Upstream4xxResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.Future
import play.api.Logger


@Singleton
class MessageConnector @Inject()(http: HttpClient,
                                 override val runModeConfiguration: Configuration,
                                 val environment: Environment) extends Status with ServicesConfig {

  lazy val serviceUrl: String = baseUrl("message")

  import scala.concurrent.ExecutionContext.Implicits.global

  override protected def mode = environment.mode

  def create(secureMessage: SecureMessage)
            (implicit hc: HeaderCarrier): Future[StorageResult] = {

    implicit val messageFormats = MessageResponse.formats

    val createMessageAPIurl: String = s"$serviceUrl/messages"


    Logger.info(s"******* POSTV1 createMessageAPIurl: ${createMessageAPIurl}}")
    Logger.info(s"******* POSTV1: ${secureMessage}}")
    http.POST[SecureMessage, MessageResponse](url = createMessageAPIurl, body = secureMessage).
      map {
        case MessageResponse(messageId) => AdviceStored(messageId)
      }.
      recover {
        case Upstream4xxResponse(conflictMessage, Status.CONFLICT, _, _) => AdviceAlreadyExists
        case ex => UnexpectedError(ex.getMessage)
      }
  }

  def createV2(secureMessage: SecureMessageV2)
            (implicit hc: HeaderCarrier): Future[StorageResult] = {

    implicit val messageFormats = MessageResponse.formats

    val createMessageAPIurl: String = s"$serviceUrl/messages"
    Logger.info(s"******* POSTV2 createMessageAPIurl: ${createMessageAPIurl}}")
    Logger.info(s"******* POSTV2: ${secureMessage}}")

    http.POST[SecureMessageV2, MessageResponse](url = createMessageAPIurl, body = secureMessage).
      map {
        case MessageResponse(messageId) => AdviceStored(messageId)
      }.
      recover {
        case Upstream4xxResponse(conflictMessage, Status.CONFLICT, _, _) => AdviceAlreadyExists
        case ex => UnexpectedError(ex.getMessage)
      }
  }

}

case class MessageResponse(id: String)

object MessageResponse {
  implicit val formats = Json.format[MessageResponse]
}

