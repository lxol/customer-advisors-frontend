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


      withClue("event transactionName") {
        event.tags.get(EventKeys.TransactionName) should not {be (None)}
        event.tags.get(EventKeys.TransactionName).get shouldBe "Message Created"
      }

      withClue("event messageId") {
        event.detail.get("messageId") should not {be (None)}
        event.detail.get("messageId").get shouldBe "1234"
      }

      withClue("event fhdds Ref") {
        event.detail.get("fhdds Ref") should not {be (None)}
        event.detail.get("fhdds Ref").get should include regex ("XZFH00000100024")
      }

      withClue("event secureMessageId") {
        event.detail.get("secureMessageId") should not {be (None)}
        event.detail.get("secureMessageId").get shouldBe "1234"
      }

      withClue("event eventId") {
        event.eventId should include regex ("^[0-9a-fA-F-]+$")
      }

    }

    "audit the duplicate message event" in new TestCaseV2 {
      when(secureMessageServiceMock.createMessageV2(any(), any())(any(), any()))
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


    "audit the unexpected error event" in new TestCaseV2 {
      when(secureMessageServiceMock.createMessageV2(any(), any())(any(), any()))
        .thenReturn(Future.successful(UnexpectedError("this is the reason")))

      controller.submitV2()(request).futureValue

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

// Message Created
// {
//    "auditSource":"customer-advisors-frontend",
//    "auditType":"TxSucceeded",
//    "detail":{
//       "messageId":"Event_ID",
//       "fhdds Ref":"recipient.taxIdentifier.name",
//       "secureMessageId":"Event_ID",
//    },
//    "eventId":"EXTERNAL_REF",
//    "generatedAt":"2018-07-13T09:17:04.077Z"
//    "tags":{
//       "transactionName":"Message Created"
//    }
// }
// Message Duplicated
// {
//    "auditSource":"customer-advisors-frontend",
//    "auditType":"TxSucceeded",
//    "detail":{
//       "messageId":"Event_ID",
//       "fhdds Ref":"recipient.taxIdentifier.name",
//       "secureMessageId":"EVENT_ID"
//    },
//    "eventId":"EXTERNAL_REF",
//    "generatedAt":"2018-07-13T09:17:04.077Z"
//    "tags":{
//       "transactionName":"Message Duplicate Request"
//    }
// }
// Message not Created - Unexpected problem
// {
//    "auditSource":"customer-advisors-frontend",
//    "auditType":"TxFailed",
//    "detail":{
//       "messageId":"",
//       "fhdds Ref":"recipient.taxIdentifier.name",
//       "secureMessageId":"",
//       "reason":"errorMessage"
//    },
//    "eventId":"EXTERNAL_REF",
//    "generatedAt":"2018-07-13T09:17:04.077Z"
//    "tags":{
//       "transactionName":"Message Not Created"
//    }
// }

// looking at the following ... some slight tweaks (I think!)
