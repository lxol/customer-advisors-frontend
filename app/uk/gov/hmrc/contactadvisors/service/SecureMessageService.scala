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

package uk.gov.hmrc.contactadvisors.service

import java.util.UUID

import javax.inject.{Inject, Singleton}
import org.apache.commons.codec.binary.Base64
import org.joda.time.DateTime
import play.api.libs.json.{ JsObject, Json }
import uk.gov.hmrc.contactadvisors.connectors.models._
import uk.gov.hmrc.contactadvisors.connectors.{EntityResolverConnector, MessageConnector, PaperlessPreference}
import uk.gov.hmrc.contactadvisors.domain._
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SecureMessageService @Inject()(messageConnector: MessageConnector, entityResolverConnector: EntityResolverConnector) {

  def generateExternalRefID = UUID.randomUUID().toString

  def createMessage(advice: Advice, saUtr: SaUtr)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[StorageResult] = {

    entityResolverConnector.validPaperlessUserWith(saUtr).flatMap {
      case Some(PaperlessPreference(true)) => for {
        storageResult <- messageConnector.create(secureMessageFrom(advice, saUtr))
      } yield storageResult
      case Some(PaperlessPreference(false)) => Future.successful(UserIsNotPaperless)
      case None => Future.successful(UnknownTaxId)
    }.recover {
      case UnexpectedFailure(reason) => UnexpectedError(s"Creation of the advice failed. Reason: $reason")
    }
  }

  def secureMessageFrom(advice: Advice, saUtr: SaUtr): SecureMessage = {
    val recipient = Recipient(saUtr)
    val externalReference = ExternalReference(generateExternalRefID, "customer-advisor")
    val messageType = "advisor-reply"
    val subject = advice.subject
    val content = new String(Base64.encodeBase64(advice.message.getBytes("UTF-8")))
    val validFrom = DateTime.now().toLocalDate
    val details = Details(formId = "CA001", statutory = false, paperSent = false, batchId = None)
    SecureMessage(recipient, externalReference, messageType, subject, content, validFrom, details)
  }

  def createMessageV2(advice: AdviceV2)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[StorageResult] = {
    messageConnector.createV2(secureMessageFromV2(advice))
  }
  

  def secureMessageFromV2(advice: AdviceV2): JsObject = {
    val taxpayerName = TaxpayerName(advice.recipientNameLine1)
    // val taxIdentifier = 
    // val recipient = RecipientV2("123456")

    // val externalReference = ExternalReference(generateExternalRefID, "customer-advisor")
    val externalReference = generateExternalRefID
    val messageType = "advisor-reply"
    val subject = advice.subject
    val content = new String(Base64.encodeBase64(advice.content.getBytes("UTF-8")))
    val validFrom = DateTime.now().toLocalDate
    val details = Details(formId = "CA001", statutory = false, paperSent = false, batchId = None)
    // SecureMessageV2(null, externalReference, messageType, subject, content, validFrom, details)

    val jsonString =
      s"""
         | {
         |   "externalRef":{
         |      "id":"${externalReference}",
         |      "source":"customer-advisor"
         |   },
         |   "recipient":{
         |      "taxIdentifier": {
         |         "name":"HMRC-ORG-OBTDS",
         |         "value":"${advice.recipientTaxidentifierValue}"
         |      },
         |      "name": {
         |         "line1": "${advice.recipientNameLine1}"
         |      },
         |      "email": "${advice.recipientEmail}"
         |   },
         |   "messageType":"$messageType",
         |   "validFrom": "${validFrom}",
         |   "subject":"$subject",
         |   "content":"$content",
         |   "details":{
         |      "formId":"CA001",
         |      "statutory":false,
         |      "paperSent": false
         |   }
         |}
    """.stripMargin

    Json.parse(jsonString).as[JsObject]
  }

}
