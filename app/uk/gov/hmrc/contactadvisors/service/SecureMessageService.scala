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

package uk.gov.hmrc.contactadvisors.service

import java.util.UUID

import org.apache.commons.codec.binary.Base64
import org.joda.time.DateTime
import uk.gov.hmrc.contactadvisors.connectors.models._
import uk.gov.hmrc.contactadvisors.connectors.{EntityResolverConnector, MessageConnector, PaperlessPreference, TaxpayerNameConnector}
import uk.gov.hmrc.contactadvisors.domain._
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait SecureMessageService {
  def taxpayerNameConnector: TaxpayerNameConnector

  def messageConnector: MessageConnector

  def entityResolverConnector: EntityResolverConnector

  def generateExternalRefID = UUID.randomUUID().toString

  def createMessageWithTaxpayerName(advice: Advice, saUtr: SaUtr)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[StorageResult] = {

    entityResolverConnector.validPaperlessUserWith(saUtr).flatMap {
      isPaperless =>
        isPaperless match {
          case Some(PaperlessPreference(true)) => for {
            taxpayerName <- taxpayerNameConnector.taxpayerName(saUtr)
            storageResult <- messageConnector.create(secureMessageFrom(advice, taxpayerName, saUtr))
          } yield storageResult
          case Some(PaperlessPreference(false)) => Future.successful(UserIsNotPaperless)
          case None => Future.successful(UnknownTaxId)
        }
    }.recover {
      case UnexpectedFailure(reason) => UnexpectedError(s"Creation of the advice failed. Reason: $reason")
    }
  }

  def secureMessageFrom(advice: Advice, taxpayerName: Option[TaxpayerName], saUtr: SaUtr): SecureMessage = {
    val recipient = Recipient(saUtr, taxpayerName.getOrElse(TaxpayerName()))
    val externalReference = ExternalReference(generateExternalRefID, "customer-advisor")
    val messageType = "advisor-reply"
    val subject = advice.subject
    val content = new String(Base64.encodeBase64(advice.message.getBytes("UTF-8")))
    val validFrom = DateTime.now().toLocalDate
    val details = Details(formId = "CA001", statutory = false, paperSent = false, batchId = None)
    SecureMessage(recipient, externalReference, messageType, subject, content, validFrom, details)
  }
}

object SecureMessageService extends SecureMessageService {

  override def taxpayerNameConnector: TaxpayerNameConnector = TaxpayerNameConnector

  override def messageConnector: MessageConnector = MessageConnector

  override def entityResolverConnector: EntityResolverConnector = EntityResolverConnector
}
