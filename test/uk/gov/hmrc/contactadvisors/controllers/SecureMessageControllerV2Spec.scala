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

import org.jsoup.Jsoup
import org.scalatest.Inside
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.OneServerPerSuite
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.contactadvisors.FrontendAppConfig
import uk.gov.hmrc.contactadvisors.dependencies.MessageStubV2
import uk.gov.hmrc.contactadvisors.service.SecureMessageService
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.utils.{SecureMessageCreatorV2, WithWiremock}

import scala.collection.JavaConverters._
import scala.concurrent.Future

class SecureMessageControllerV2Spec
  extends UnitSpec
    with OneServerPerSuite
    with ScalaFutures
    with IntegrationPatience
    with WithWiremock
    with MessageStubV2 {

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

  val controller = new SecureMessageController(app.injector.instanceOf(classOf[CustomerAdviceAudit]), app.injector.instanceOf(classOf[SecureMessageService]), app.injector.instanceOf(classOf[MessagesApi]))(app.injector.instanceOf(classOf[FrontendAppConfig])) {
    def auditSource: String = "customer-advisors-frontend"
  }

  "GET /customer-advisors-frontend/inbox" should {
    "return 200" in {
      val result = controller.inboxV2()(getRequest)
      status(result) shouldBe Status.OK
    }

    "return HTML" in {
      val result = controller.inboxV2()(getRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
    }

    "show main banner" in {
      val result = controller.inboxV2()(getRequest)
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByTag("header").attr("id") shouldBe "global-header"
    }

    "have the expected elements on the form" in {
      val result = controller.inboxV2()(getRequest)
      status(result) shouldBe 200

      val document = Jsoup.parse(contentAsString(result))
      val form = document.select("form#form-submit-customer-advice").get(0)
      val formElements = form.getAllElements.asScala

      val adviceSubject = formElements.find(_.id() == "subject")
      withClue("advice subject field") {
        Inside.inside(adviceSubject) {
          case Some(element) =>
            element.tagName() shouldBe "input"
        }
      }

      val adviceMessage = formElements.find(_.id() == "content")
      withClue("advice message field") {
        adviceMessage.map(_.tagName()) shouldBe Some("textarea")
      }

      val submitAdvice = formElements.find(_.id() == "submit-advice")
      withClue("submit advice button") {
        submitAdvice.map(_.tagName()) shouldBe Some("button")
        submitAdvice.map(_.text()) shouldBe Some("Send")
      }


      val recipientTaxIdentifierName = formElements.find(_.id() == "recipient_taxidentifier_name")
      withClue("advice recipient taxidentifier name field") {
        Inside.inside(adviceSubject) {
          case Some(element) =>
            element.tagName() shouldBe "input"
        }
      }

      val recipientTaxIdentifierValue = formElements.find(_.id() == "recipient_taxidentifier_value")
      withClue("advice recipient taxidentifier value field") {
        Inside.inside(adviceSubject) {
          case Some(element) =>
            element.tagName() shouldBe "input"
        }
      }

      val recipientEmail = formElements.find(_.id() == "recipient_email")
      withClue("advice recipient email field") {
        Inside.inside(adviceSubject) {
          case Some(element) =>
            element.tagName() shouldBe "input"
        }
      }

      val recipientNameLine1 = formElements.find(_.id() == "recipient_name_line1")
      withClue("advice recipient name line1 field") {
        Inside.inside(adviceSubject) {
          case Some(element) =>
            element.tagName() shouldBe "input"
        }
      }

      val recipientMessageType = formElements.find(_.id() == "messageType")
      withClue("advice messageType field") {
        Inside.inside(adviceSubject) {
          case Some(element) =>
            element.tagName() shouldBe "input"
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
      Jsoup.parse(contentAsString(emptySubject)).getElementsByClass("error-notification").asScala should have size 1
      status(emptySubject) shouldBe BAD_REQUEST

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
      Jsoup.parse(contentAsString(emptyMessage)).getElementsByClass("error-notification").asScala should have size 1
      status(emptyMessage) shouldBe BAD_REQUEST

      val emptyFormFields = controller.submitV2()(FakeRequest())
      Jsoup.parse(contentAsString(emptyFormFields)).getElementsByClass("error-notification").asScala should have size 7
      status(emptyFormFields) shouldBe BAD_REQUEST
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
      val result = controller.successV2("FHDDS_REF", "234234", "1234-223423")(getRequest)
      //   "Advice creation successful", 
      //   "Thanks. Your reply has been successfully received by the customer's Tax Account secure message Inbox."
      // )

      status(result) shouldBe 200
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByTag("header").attr("id") shouldBe "global-header"

      withClue("result page title") {
        document.title() shouldBe "Advice creation successful"
      }
      withClue("result page FHDDS Reference") {
        document.select("ul li").get(0).text() shouldBe s"FHDDS Reference: FHDDS_REF"
      }
      withClue("result page Message Id") {
        document.select("ul li").get(1).text() should startWith regex "Id: [0-9a-f]+"
      }
      withClue("result page External Ref") {
        document.select("ul li").get(2).text() should startWith regex "External Ref: [0-9a-f-]+"
      }
    }
    "contain correct message for duplicate" in {
      val result = controller.duplicateV2("FHDDS_REF")(getRequest)
      status(result) shouldBe 200
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByTag("header").attr("id") shouldBe "global-header"

      withClue("result page title") {
        document.title() shouldBe "Advice already exists"
      }
    }

    "contain correct message for unexpected error" in {
      val result = controller.unexpected("FHDDS_REF")(getRequest)
      status(result) shouldBe 200
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByTag("header").attr("id") shouldBe "global-header"

      withClue("result page title") {
        document.title() shouldBe "Unexpected error"
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
      status(result) shouldBe 303

      redirectLocation(result) match {
        case Some(redirect) => redirect should (startWith(s"/secure-message$url"))
        case _ => fail("redirect location should always be present")
      }

    }
  }

  override val dependenciesPort: Int = 10100
}
