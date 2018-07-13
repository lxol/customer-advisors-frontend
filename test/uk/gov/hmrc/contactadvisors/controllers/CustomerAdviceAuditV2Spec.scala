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
import play.api.i18n.{DefaultLangs, DefaultMessagesApi}
import play.api.test.FakeRequest
import play.api.{Configuration, Environment}
import uk.gov.hmrc.contactadvisors.FrontendAppConfig
import uk.gov.hmrc.contactadvisors.domain._
import uk.gov.hmrc.contactadvisors.service.SecureMessageService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.EventKeys
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class CustomerAdviceAuditV2Spec extends UnitSpec with ScalaFutures with OneAppPerSuite with IntegrationPatience with Eventually {

  "SecureMessageController" should {

    "audit the successful event" in new TestCaseV2 {
      when(secureMessageServiceMock.createMessageV2(any(), any())(any(), any()))
        .thenReturn(Future.successful(AdviceStored("1234")))

      controller.submitV2()(request).futureValue

      eventually {
        verify(auditConnectorMock).sendEvent(dataEventCaptor.capture())(any(), any())
      }

      val event = dataEventCaptor.getValue
      event.auditSource shouldBe "customer-advisors-frontend"
      event.auditType shouldBe "TxSucceeded"
      event.detail.get("messageId").get shouldBe "1234"
      event.tags.get(EventKeys.TransactionName).get shouldBe "Message Stored"
    }

    "audit the duplicate message event" in new TestCaseV2 {
      when(secureMessageServiceMock.createMessage(any(), any())(any(), any()))
        .thenReturn(Future.successful(AdviceAlreadyExists))

      controller.submitV2()(request).futureValue

      eventually {
        verify(auditConnectorMock).sendEvent(dataEventCaptor.capture())(any(), any())
      }

      val event = dataEventCaptor.getValue
      event.auditSource shouldBe "customer-advisors-frontend"
      event.auditType shouldBe "TxFailed"
      event.detail.get("reason").get shouldBe "Duplicate Message Found"
      event.tags.get(EventKeys.TransactionName).get shouldBe "Message Not Stored"
    }

    "audit the unknown tax id event" in new TestCaseV2 {
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

    "audit the user not paperless event" in new TestCaseV2 {
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

    "audit the unexpected error event" in new TestCaseV2 {
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

trait TestCaseV2 extends MockitoSugar {

  val secureMessageServiceMock = mock[SecureMessageService]
  val customerAdviceAuditMock = new CustomerAdviceAudit(auditConnectorMock)

  val env = Environment.simple()
  val configuration = Configuration.reference ++ Configuration.from(Map(
    "Test.google-analytics.token" -> "token",
    "Test.google-analytics.host" -> "host"))

  val auditConnectorMock = mock[AuditConnector]
  val customerAdviceAudit = new CustomerAdviceAudit(auditConnectorMock)
  val appConfig = new FrontendAppConfig(configuration, env)

  val messageApi = new DefaultMessagesApi(env, configuration, new DefaultLangs(configuration))
  val controller = new SecureMessageController(customerAdviceAudit, secureMessageServiceMock, messageApi)(appConfig) {
    val secureMessageService: SecureMessageService = secureMessageServiceMock

    def auditSource: String = "customer-advisors-frontend"
  }
  val dataEventCaptor = ArgumentCaptor.forClass(classOf[DataEvent])
  implicit val hc = HeaderCarrier
  when(auditConnectorMock.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))

  val request = FakeRequest("POST", "/customer-advisors-frontend/submit").withFormUrlEncodedBody(
      "content" -> "content",
      "subject" -> "subject",
      "recipientTaxidentifierName" -> "HMRC-OBTDS-ORG",
      "recipientTaxidentifierValue" -> "XZFH00000100024",
      "recipientEmail" -> "foo@bar.com",
      "recipientNameLine1" -> "Mr. John Smith",
      "messageType" -> "fhddsAlertMessage",
      "alertQueue" -> "PRIORITY"
  )
}
