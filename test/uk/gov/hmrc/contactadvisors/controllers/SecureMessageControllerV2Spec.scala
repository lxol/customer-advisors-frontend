/*
 * Copyright 2019 HM Revenue & Customs
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

import org.jsoup.Jsoup
import org.scalatest.Inside
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.contactadvisors.FrontendAppConfig
import uk.gov.hmrc.contactadvisors.dependencies.MessageStubV2
import uk.gov.hmrc.contactadvisors.service.SecureMessageService
import uk.gov.hmrc.utils.{SecureMessageCreatorV2, WithWiremock}

import scala.collection.JavaConverters._
import scala.concurrent.Future

class SecureMessageControllerV2Spec
  extends PlaySpec
    with GuiceOneAppPerSuite
    with ScalaFutures
    with IntegrationPatience
    with WithWiremock
    with MessageStubV2 {

  implicit lazy override val app: Application = new GuiceApplicationBuilder()
    .configure(
      "Test.microservice.services.message.port" -> "10100",
      "Test.microservice.services.entity-resolver.port" -> "10100"
    )
    .build()

  val getRequest = FakeRequest("GET", "/")
  val postRequest = FakeRequest("POST", "/")
  val subject = "This is a response to your HMRC request"
  val content = "A message success to the customer."
  val recipientTaxidentifierName = "HMRC-OBTDS-ORG"
  val recipientTaxidentifierValue = "XZFH00000100024"
  val recipientEmail = "foo@bar.com"
  val recipientNameLine1 = "Mr. John Smith"
  val messageType = "fhddsAlertMessage"
  val adviceBody = "<p>This is the content of the secure message</p>"
  val messagesApi = app.injector.instanceOf[MessagesApi]
  val appConfig = app.injector.instanceOf[FrontendAppConfig]
  val controllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val messageApi = app.injector.instanceOf[MessagesApi]
  val customerAdviceAudit = app.injector.instanceOf[CustomerAdviceAudit]
  val secureMessageService = app.injector.instanceOf[SecureMessageService]

  val controller = new SecureMessageController(controllerComponents, customerAdviceAudit, secureMessageService, messageApi)(appConfig) {
    def auditSource: String = "customer-advisors-frontend"
  }

  "GET /customer-advisors-frontend/inbox" should {
    "return 200" in {
      val result = controller.inboxV2()(getRequest)
      status(result) must be(Status.OK)
    }

    "return HTML" in {
      val result = controller.inboxV2()(getRequest)
      contentType(result) must be(Some("text/html"))
      charset(result) must be(Some("utf-8"))
    }

    "show main banner" in {
      val result = controller.inboxV2()(getRequest)
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByTag("header").attr("id") must be("global-header")
    }

    "have the expected elements on the form" in {
      val result = controller.inboxV2()(getRequest)
      status(result) must be(200)

      val document = Jsoup.parse(contentAsString(result))
      val form = document.select("form#form-submit-customer-advice").get(0)
      val formElements = form.getAllElements.asScala

      val adviceSubject = formElements.find(_.id() == "subject")
      withClue("advice subject field") {
        Inside.inside(adviceSubject) {
          case Some(element) =>
            element.tagName() must be("input")
        }
      }

      val adviceMessage = formElements.find(_.id() == "content")
      withClue("advice message field") {
        adviceMessage.map(_.tagName()) must be(Some("textarea"))
      }

      val submitAdvice = formElements.find(_.id() == "submit-advice")
      withClue("submit advice button") {
        submitAdvice.map(_.tagName()) must be(Some("button"))
        submitAdvice.map(_.text()) must be(Some("Send"))
      }


      val recipientTaxIdentifierName = formElements.find(_.id() == "recipient_taxidentifier_name")
      withClue("advice recipient taxidentifier name field") {
        Inside.inside(adviceSubject) {
          case Some(element) =>
            element.tagName() must be("input")
        }
      }

      val recipientTaxIdentifierValue = formElements.find(_.id() == "recipient_taxidentifier_value")
      withClue("advice recipient taxidentifier value field") {
        Inside.inside(adviceSubject) {
          case Some(element) =>
            element.tagName() must be("input")
        }
      }

      val recipientEmail = formElements.find(_.id() == "recipient_email")
      withClue("advice recipient email field") {
        Inside.inside(adviceSubject) {
          case Some(element) =>
            element.tagName() must be("input")
        }
      }

      val recipientNameLine1 = formElements.find(_.id() == "recipient_name_line1")
      withClue("advice recipient name line1 field") {
        Inside.inside(adviceSubject) {
          case Some(element) =>
            element.tagName() must be("input")
        }
      }

      val recipientMessageType = formElements.find(_.id() == "messageType")
      withClue("advice messageType field") {
        Inside.inside(adviceSubject) {
          case Some(element) =>
            element.tagName() must be("input")
        }
      }
    }
  }

  "POST /customer-advisors-frontend/submit" should {
    "indicate a bad request when any of the form elements are empty" in {
      val emptySubject = controller.submitV2()(
        FakeRequest().withFormUrlEncodedBody(
          "content" -> content,
          "recipientTaxidentifierName" -> recipientTaxidentifierName,
          "recipientTaxidentifierValue" -> recipientTaxidentifierValue,
          "recipientEmail" -> recipientEmail,
          "recipientNameLine1" -> recipientNameLine1,
          "messageType" -> messageType
        )
      )
      Jsoup.parse(contentAsString(emptySubject)).getElementsByClass("error-notification").asScala must have size 1
      status(emptySubject) must be(BAD_REQUEST)

      val emptyMessage = controller.submitV2()(
        FakeRequest().withFormUrlEncodedBody(
          "subject" -> subject,
          "recipientTaxidentifierName" -> recipientTaxidentifierName,
          "recipientTaxidentifierValue" -> recipientTaxidentifierValue,
          "recipientEmail" -> recipientEmail,
          "recipientNameLine1" -> recipientNameLine1,
          "messageType" -> messageType
        )
      )
      Jsoup.parse(contentAsString(emptyMessage)).getElementsByClass("error-notification").asScala must have size 1
      status(emptyMessage) must be(BAD_REQUEST)

      val emptyFormFields = controller.submitV2()(FakeRequest())
      Jsoup.parse(contentAsString(emptyFormFields)).getElementsByClass("error-notification").asScala must have size 7
      status(emptyFormFields) must be(BAD_REQUEST)
    }

    "redirect to the success page when the form submission is successful" in {
      val advice = SecureMessageCreatorV2.adviceWithUncleanContent

      givenMessageRespondsWith(advice, successfulResponse)

      val xssMessage = controller.submitV2()(
        FakeRequest().withFormUrlEncodedBody(
          "content" -> advice.content,
          "subject" -> advice.subject,
          "recipientTaxidentifierName" -> advice.recipientTaxidentifierName,
          "recipientTaxidentifierValue" -> advice.recipientTaxidentifierValue,
          "recipientEmail" -> advice.recipientEmail,
          "recipientNameLine1" -> advice.recipientNameLine1,
          "messageType" -> advice.messageType
        )
      )

      xssMessage returnsRedirectTo s"/customer-advisors-frontend/inbox/success"
    }
    "Leave script tags in the message and subject" in {
      val advice = SecureMessageCreatorV2.adviceWithUncleanContent

      givenMessageRespondsWith(advice, successfulResponse)

      val xssMessage = controller.submitV2()(
        FakeRequest().withFormUrlEncodedBody(
          "content" -> advice.content,
          "subject" -> advice.subject,
          "recipientTaxidentifierName" -> advice.recipientTaxidentifierName,
          "recipientTaxidentifierValue" -> advice.recipientTaxidentifierValue,
          "recipientEmail" -> advice.recipientEmail,
          "recipientNameLine1" -> advice.recipientNameLine1,
          "messageType" -> advice.messageType
        )
      )

      xssMessage returnsRedirectTo s"/customer-advisors-frontend/inbox/success"
    }

    "redirect and indicate a duplicate message submission" in {
      val advice = SecureMessageCreatorV2.adviceWithCleanContent
      givenMessageRespondsWith(advice, duplicatedMessage)

      val xssMessage = controller.submitV2()(
        FakeRequest().withFormUrlEncodedBody(
          "content" -> advice.content,
          "subject" -> advice.subject,
          "recipientTaxidentifierName" -> advice.recipientTaxidentifierName,
          "recipientTaxidentifierValue" -> advice.recipientTaxidentifierValue,
          "recipientEmail" -> advice.recipientEmail,
          "recipientNameLine1" -> advice.recipientNameLine1,
          "messageType" -> advice.messageType
        )
      )

      xssMessage returnsRedirectTo s"/customer-advisors-frontend/inbox/duplicate"
    }

    "redirect and indicate an unexpected error has occurred when processing the submission" in {
      val advice = SecureMessageCreatorV2.adviceWithCleanContent
      givenMessageRespondsWith(advice, (1, "asdfasdf"))

      val xssMessage = controller.submitV2()(
        FakeRequest().withFormUrlEncodedBody(
          "content" -> advice.content,
          "subject" -> advice.subject,
          "recipientTaxidentifierName" -> advice.recipientTaxidentifierName,
          "recipientTaxidentifierValue" -> advice.recipientTaxidentifierValue,
          "recipientEmail" -> advice.recipientEmail,
          "recipientNameLine1" -> advice.recipientNameLine1,
          "messageType" -> advice.messageType
        )
      )

      xssMessage returnsRedirectTo s"/customer-advisors-frontend/inbox/unexpected"
    }


  }
  "submission result page" should {
    "contain correct message for success" in {
      val result = controller.successV2()(getRequest)

      status(result) must be(200)
      contentType(result) must be(Some("text/html"))
      charset(result) must be(Some("utf-8"))
      val document = Jsoup.parse(contentAsString(result))

      document.getElementsByTag("header").attr("id") must be("global-header")

      withClue("result page title") {
        document.title() must be("Advice creation successful")
      }
      withClue("result page h2") {
        document.select("h2").text().trim must include(s"Success")
      }
    }
    "contain correct message for duplicate" in {
      val result = controller.duplicateV2()(getRequest)
      status(result) must be(200)
      contentType(result) must be(Some("text/html"))
      charset(result) must be(Some("utf-8"))
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByTag("header").attr("id") must be("global-header")

      withClue("result page title") {
        document.title() must be("Advice already exists")
      }
    }

    "contain correct message for unexpected error" in {
      val result = controller.unexpectedV2()(getRequest)
      status(result) must be(200)
      contentType(result) must be(Some("text/html"))
      charset(result) must be(Some("utf-8"))
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByTag("header").attr("id") must be("global-header")

      withClue("result page title") {
        document.title() must be("Unexpected error")
      }
      withClue("result page h2") {
        document.select("h2").text().trim must include(s"Failed")
      }
    }
  }

  def submissionOfCompletedForm() = controller.submitV2()(
    FakeRequest().withFormUrlEncodedBody(
      "content" -> "content",
      "subject" -> "subject",
      "recipientTaxidentifierName" -> "name",
      "recipientTaxidentifierValue" -> "value",
      "recipientEmail" -> "foo@bar.com",
      "recipientNameLine1" -> "Mr. John Smith",
      "messageType" -> "fhddsAlertMessage",
      "alertQueue" -> "PRIORITY"
    )
  )

  implicit class ReturnsRedirectTo(result: Future[Result]) {
    def returnsRedirectTo(url: String) = {
      status(result) must be(303)
      redirectLocation(result) match {
        case Some(redirect) => redirect must (startWith(s"/secure-message$url"))
        case _ => fail("redirect location should always be present")
      }
    }
  }
  override val dependenciesPort: Int = 10100
}
