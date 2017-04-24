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

import org.joda.time.DateTime
import uk.gov.hmrc.contactadvisors.connectors.models._
import uk.gov.hmrc.contactadvisors.connectors.{MessageConnector, TaxpayerNameConnector}
import uk.gov.hmrc.contactadvisors.domain.{Advice, StorageResult}
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait SecureMessageService {
  def taxpayerNameConnector: TaxpayerNameConnector
  def messageConnector: MessageConnector

  def createMessageWithTaxpayerName(advice: Advice, saUtr: SaUtr)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[StorageResult] = {
    for {
      taxpayerName <- taxpayerNameConnector.taxpayerName(saUtr)
      storageResult <- messageConnector.create(createSecureMessageFrom(advice, taxpayerName, saUtr))
    } yield storageResult
  }

  def createSecureMessageFrom(advice: Advice, taxpayerName: Option[TaxpayerName], saUtr: SaUtr): SecureMessage = {
    val recipient = Recipient(saUtr, taxpayerName.getOrElse(TaxpayerName()))
    val externalReference = ExternalReference(UUID.randomUUID().toString, "customer-advisor")
    val messageType = "advisor-reply"
    val subject = advice.subject
    val content = advice.message
    val validFrom = DateTime.now().toLocalDate
    val details = Details(formId = "CA001", statutory = false, paperSent = false, batchId = None)
    SecureMessage(recipient, externalReference, messageType, subject, content, validFrom, details)
  }
}

object SecureMessageService extends SecureMessageService {

  override def taxpayerNameConnector: TaxpayerNameConnector = TaxpayerNameConnector
  override def messageConnector: MessageConnector = MessageConnector
}
