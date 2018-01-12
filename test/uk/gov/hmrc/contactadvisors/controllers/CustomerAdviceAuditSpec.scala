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

package uk.gov.hmrc.contactadvisors.controllers

import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.test.FakeRequest
import uk.gov.hmrc.contactadvisors.domain._
import uk.gov.hmrc.contactadvisors.service.SecureMessageService
import uk.gov.hmrc.play.audit.EventKeys
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class CustomerAdviceAuditSpec extends UnitSpec with ScalaFutures with OneAppPerSuite with IntegrationPatience with Eventually {

  "SecureMessageController" should {

    "audit the successful event" in new TestCase {
      when(secureMessageServiceMock.createMessage(any(), any())(any(), any()))
        .thenReturn(Future.successful(AdviceStored("1234")))

      controller.submit("123456789")(request).futureValue

      eventually {
        verify(auditConnectorMock).sendEvent(dataEventCaptor.capture())(any(), any())
      }

      val event = dataEventCaptor.getValue
      event.auditSource shouldBe "customer-advisors-frontend"
      event.auditType shouldBe "TxSucceeded"
      event.detail.get("messageId").get shouldBe "1234"
      event.tags.get(EventKeys.TransactionName).get shouldBe "Message Stored"
    }

    "audit the duplicate message event" in new TestCase {
      when(secureMessageServiceMock.createMessage(any(), any())(any(), any()))
        .thenReturn(Future.successful(AdviceAlreadyExists))

      controller.submit("123456789")(request).futureValue

      eventually {
        verify(auditConnectorMock).sendEvent(dataEventCaptor.capture())(any(), any())
      }

      val event = dataEventCaptor.getValue
      event.auditSource shouldBe "customer-advisors-frontend"
      event.auditType shouldBe "TxFailed"
      event.detail.get("reason").get shouldBe "Duplicate Message Found"
      event.tags.get(EventKeys.TransactionName).get shouldBe "Message Not Stored"
    }

    "audit the unknown tax id event" in new TestCase {
      when(secureMessageServiceMock.createMessage(any(), any())(any(), any()))
        .thenReturn(Future.successful(UnknownTaxId))

      controller.submit("123456789")(request).futureValue

      eventually {
        verify(auditConnectorMock).sendEvent(dataEventCaptor.capture())(any(), any())
      }

      val event = dataEventCaptor.getValue
      event.auditSource shouldBe "customer-advisors-frontend"
      event.auditType shouldBe "TxFailed"
      event.detail.get("reason").get shouldBe "Unknown Tax Id"
      event.tags.get(EventKeys.TransactionName).get shouldBe "Message Not Stored"
    }

    "audit the user not paperless event" in new TestCase {
      when(secureMessageServiceMock.createMessage(any(), any())(any(), any()))
        .thenReturn(Future.successful(UserIsNotPaperless))

      controller.submit("123456789")(request).futureValue

      eventually {
        verify(auditConnectorMock).sendEvent(dataEventCaptor.capture())(any(), any())
      }

      val event = dataEventCaptor.getValue
      event.auditSource shouldBe "customer-advisors-frontend"
      event.auditType shouldBe "TxFailed"
      event.detail.get("reason").get shouldBe "User is not paperless"
      event.tags.get(EventKeys.TransactionName).get shouldBe "Message Not Stored"
    }

    "audit the unexpected error event" in new TestCase {
      when(secureMessageServiceMock.createMessage(any(), any())(any(), any()))
        .thenReturn(Future.successful(UnexpectedError("this is the reason")))

      controller.submit("123456789")(request).futureValue

      eventually {
        verify(auditConnectorMock).sendEvent(dataEventCaptor.capture())(any(), any())
      }

      val event = dataEventCaptor.getValue
      event.auditSource shouldBe "customer-advisors-frontend"
      event.auditType shouldBe "TxFailed"
      event.detail.get("reason").get shouldBe "Unexpected Error: this is the reason"
      event.tags.get(EventKeys.TransactionName).get shouldBe "Message Not Stored"
    }
  }
}

trait TestCase extends MockitoSugar {

  val secureMessageServiceMock = mock[SecureMessageService]
  val auditConnectorMock = mock[AuditConnector]

  val controller = new SecureMessageController {
    override val secureMessageService: SecureMessageService = secureMessageServiceMock
    override def auditSource: String = "customer-advisors-frontend"
    override def auditConnector: AuditConnector = auditConnectorMock
  }
  val dataEventCaptor = ArgumentCaptor.forClass(classOf[DataEvent])
  when(auditConnectorMock.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))

  implicit val hc = HeaderCarrier
  val request = FakeRequest("POST", "/inbox/123456789").withFormUrlEncodedBody(
    "subject" -> "New message subject",
    "message" -> "New message body"
  )
}
