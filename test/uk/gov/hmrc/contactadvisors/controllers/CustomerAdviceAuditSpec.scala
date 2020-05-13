/*
 * Copyright 2020 HM Revenue & Customs
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
import org.scalatest.concurrent.{ Eventually, IntegrationPatience, ScalaFutures }
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.MessagesApi
import play.api.mvc.MessagesControllerComponents
import play.api.test.FakeRequest
import play.api.{ Configuration, Environment }
import uk.gov.hmrc.contactadvisors.FrontendAppConfig
import uk.gov.hmrc.contactadvisors.domain._
import uk.gov.hmrc.contactadvisors.service.SecureMessageService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.EventKeys
import uk.gov.hmrc.play.audit.http.connector.{ AuditConnector, AuditResult }
import uk.gov.hmrc.play.audit.model.DataEvent

import scala.concurrent.Future

class CustomerAdviceAuditSpec extends PlaySpec with ScalaFutures with GuiceOneAppPerSuite with IntegrationPatience with Eventually {

  "SecureMessageController" should {

    "audit the successful event" in new TestCase {
      when(secureMessageServiceMock.createMessage(any(), any())(any(), any()))
        .thenReturn(Future.successful(AdviceStored("1234")))

      controller.submit("123456789")(request).futureValue

      eventually {
        verify(auditConnectorMock).sendEvent(dataEventCaptor.capture())(any(), any())
      }

      val event = dataEventCaptor.getValue
      event.auditSource must be("customer-advisors-frontend")
      event.auditType must be("TxSucceeded")
      event.detail.get("messageId").get must be("1234")
      event.tags.get(EventKeys.TransactionName).get must be("Message Stored")
    }

    "audit the duplicate message event" in new TestCase {
      when(secureMessageServiceMock.createMessage(any(), any())(any(), any()))
        .thenReturn(Future.successful(AdviceAlreadyExists))

      controller.submit("123456789")(request).futureValue

      eventually {
        verify(auditConnectorMock).sendEvent(dataEventCaptor.capture())(any(), any())
      }

      val event = dataEventCaptor.getValue
      event.auditSource must be("customer-advisors-frontend")
      event.auditType must be("TxFailed")
      event.detail.get("reason").get must be("Duplicate Message Found")
      event.tags.get(EventKeys.TransactionName).get must be("Message Not Stored")
    }

    "audit the unknown tax id event" in new TestCase {
      when(secureMessageServiceMock.createMessage(any(), any())(any(), any()))
        .thenReturn(Future.successful(UnknownTaxId))

      controller.submit("123456789")(request).futureValue

      eventually {
        verify(auditConnectorMock).sendEvent(dataEventCaptor.capture())(any(), any())
      }

      val event = dataEventCaptor.getValue
      event.auditSource must be("customer-advisors-frontend")
      event.auditType must be("TxFailed")
      event.detail.get("reason").get must be("Unknown Tax Id")
      event.tags.get(EventKeys.TransactionName).get must be("Message Not Stored")
    }

    "audit the user not paperless event" in new TestCase {
      when(secureMessageServiceMock.createMessage(any(), any())(any(), any()))
        .thenReturn(Future.successful(UserIsNotPaperless))

      controller.submit("123456789")(request).futureValue

      eventually {
        verify(auditConnectorMock).sendEvent(dataEventCaptor.capture())(any(), any())
      }

      val event = dataEventCaptor.getValue
      event.auditSource must be("customer-advisors-frontend")
      event.auditType must be("TxFailed")
      event.detail.get("reason").get must be("User is not paperless")
      event.tags.get(EventKeys.TransactionName).get must be("Message Not Stored")
    }

    "audit the unexpected error event" in new TestCase {
      when(secureMessageServiceMock.createMessage(any(), any())(any(), any()))
        .thenReturn(Future.successful(UnexpectedError("this is the reason")))

      controller.submit("123456789")(request).futureValue

      eventually {
        verify(auditConnectorMock).sendEvent(dataEventCaptor.capture())(any(), any())
      }

      val event = dataEventCaptor.getValue
      event.auditSource must be("customer-advisors-frontend")
      event.auditType must be("TxFailed")
      event.detail.get("reason").get must be("Unexpected Error: this is the reason")
      event.tags.get(EventKeys.TransactionName).get must be("Message Not Stored")
    }
  }

  trait TestCase extends MockitoSugar {

    val secureMessageServiceMock = mock[SecureMessageService]
    val customerAdviceAuditMock = new CustomerAdviceAudit(auditConnectorMock)

    val env = Environment.simple()
    val configuration = Configuration.reference ++ Configuration.from(Map("Test.google-analytics.token" -> "token", "Test.google-analytics.host" -> "host"))

    val auditConnectorMock = mock[AuditConnector]
    val customerAdviceAudit = new CustomerAdviceAudit(auditConnectorMock)
    val appConfig = app.injector.instanceOf[FrontendAppConfig]

    val controllerComponents = app.injector.instanceOf[MessagesControllerComponents]

    val messageApi = app.injector.instanceOf[MessagesApi]

    val controller = new SecureMessageController(controllerComponents, customerAdviceAudit, secureMessageServiceMock, messageApi)(appConfig) {
      val secureMessageService: SecureMessageService = secureMessageServiceMock

      def auditSource: String = "customer-advisors-frontend"
    }
    val dataEventCaptor = ArgumentCaptor.forClass(classOf[DataEvent])
    implicit val hc = HeaderCarrier
    when(auditConnectorMock.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))

    val request = FakeRequest("POST", "/inbox/123456789").withFormUrlEncodedBody(
      "subject" -> "New message subject",
      "message" -> "New message body"
    )
  }
}
