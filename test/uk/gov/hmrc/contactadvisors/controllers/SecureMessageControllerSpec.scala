/*
 * Copyright 2016 HM Revenue & Customs
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
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.contactadvisors.dependencies.SecureMessageRenderer
import uk.gov.hmrc.play.it.{ExternalService, MicroServiceEmbeddedServer}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.utils.WithWiremock

import scala.collection.JavaConverters._
import scala.concurrent.Future

class SecureMessageControllerSpec extends {
  val testName: String = "SecureMessageControllerSpec"
} with UnitSpec
  with WithFakeApplication
  with ScalaFutures
  with IntegrationPatience
  with MicroServiceEmbeddedServer
  with WithWiremock
  with SecureMessageRenderer {

  val getRequest = FakeRequest("GET", "/")
  val postRequest = FakeRequest("POST", "/")
  val customer_utr = "__asd9887523512"


  override def beforeAll() = {
    super.beforeAll()
    start()
  }

  override def afterAll() = {
    super.afterAll()
    stop()
  }

  "GET /inbox/:utr" should {
    "return 200" in {
      val result = SecureMessageController.inbox(customer_utr)(getRequest)
      status(result) shouldBe Status.OK
    }

    "return HTML" in {
      val result = SecureMessageController.inbox(customer_utr)(getRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
    }

    "show main banner" in {
      val result = SecureMessageController.inbox(customer_utr)(getRequest)
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByTag("nav").attr("id") shouldBe "proposition-menu"
    }

    "have the expected elements on the form" in {
      val result = SecureMessageController.inbox(customer_utr)(getRequest)
      status(result) shouldBe 200

      val document = Jsoup.parse(contentAsString(result))
      val form = document.select("form#form-submit-customer-advice").get(0)
      val formElements = form.getAllElements.asScala

      val adviceSubject = formElements.find(_.id() == "subject")
      withClue("advice subject field") {
        Inside.inside(adviceSubject) {
          case Some(element) =>
            element.tagName() shouldBe "input"
            element.attr("type") shouldBe "hidden"
            element.`val`() shouldBe "Response to your enquiry from HMRC customer services"
        }
      }

      val adviceMessage = formElements.find(_.id() == "message")
      withClue("advice message field") {
        adviceMessage.map(_.tagName()) shouldBe Some("textarea")
      }

      val submitAdvice = formElements.find(_.id() == "submit-advice")
      withClue("submit advice button") {
        submitAdvice.map(_.tagName()) shouldBe Some("button")
        submitAdvice.map(_.text()) shouldBe Some("Send")
      }
    }
  }

  "POST /inbox/:utr" should {
    "indicate a bad request when any of the form elements are empty" in {
      val emptySubject = SecureMessageController.submit(customer_utr)(
        FakeRequest().withFormUrlEncodedBody(
          "message" -> "A message success to the customer. lkasdfjas;ldfjk"
        )
      )
      Jsoup.parse(contentAsString(emptySubject)).getElementsByClass("error-notification").asScala should have size 1
      status(emptySubject) shouldBe BAD_REQUEST

      val emptyMessage = SecureMessageController.submit(customer_utr)(
        FakeRequest().withFormUrlEncodedBody(
          "subject" -> "subject"
        )
      )
      Jsoup.parse(contentAsString(emptyMessage)).getElementsByClass("error-notification").asScala should have size 1
      status(emptyMessage) shouldBe BAD_REQUEST

      val emptyFormFields = SecureMessageController.submit(customer_utr)(FakeRequest())
      Jsoup.parse(contentAsString(emptyFormFields)).getElementsByClass("error-notification").asScala should have size 2
      status(emptyFormFields) shouldBe BAD_REQUEST
    }

    "redirect to the success page when the form submission is successful" in {
      givenSecureMessageRendererRespondsSuccessfully()

      submissionOfCompletedForm() returnsRedirectTo s"/inbox/$utr/success"
    }

    "redirect and indicate a duplicate message submission" in {
      givenSecureMessageRendererReturnsDuplicateAdvice()

      submissionOfCompletedForm() returnsRedirectTo s"/inbox/$utr/duplicate"
    }

    "redirect and indicate an unexpected error has occurred when processing the submission" in {
      givenSecureMessageRendererRespondsWith(Status.BAD_REQUEST)

      submissionOfCompletedForm() returnsRedirectTo s"/inbox/$utr/unexpected"
    }

    "redirect and indicate that the user has not opted in for paperless communications" in {
      givenSecureMessageRendererFindsThatUserIsNotPaperless()

      submissionOfCompletedForm() returnsRedirectTo s"/inbox/$utr/not-paperless"
    }


    "redirect and indicate a submission for unknown utr" in {
      givenSecureMessageRendererCannotFindTheUtr()

      submissionOfCompletedForm() returnsRedirectTo s"/inbox/$utr/unknown"
    }

  }

  "submission result page" should {
    "contain correct message for success" in {
      SecureMessageController.success(utr.value)(getRequest) shouldContainPageWithTitleAndMessage(
        "Advice creation successful",
        "Thanks. We have received your reply and the customer will now be able to see the " +
          s"new message in the Personal Tax Account secure message Inbox for user with SA-UTR ${utr.value}."
      )
    }
    "contain correct message for duplicate" in {
      SecureMessageController.duplicate(utr.value)(getRequest) shouldContainPageWithTitleAndMessage(
        "Advice already exists",
        "This message appears to be a duplicate of a message somebody sent earlier. We haven't saved it for that reason."
      )
    }
    "contain correct message for unknown taxid" in {
      SecureMessageController.unknown(utr.value)(getRequest) shouldContainPageWithTitleAndMessage(
        "Unknown UTR",
        "The SA-UTR provided is not recognised by the MDTP (Personal Tax Account and/or Business Tax Account)."
      )
    }
    "contain correct message for not paperless user" in {
      SecureMessageController.notPaperless(utr.value)(getRequest) shouldContainPageWithTitleAndMessage(
        "User is not paperless",
        s"The user with SA-UTR ${utr.value} is not registered for paperless communications " +
           "(and so we can't send them a secure message)."
      )
    }
    "contain correct message for unexpected error" in {
      SecureMessageController.unexpected(utr.value)(getRequest) shouldContainPageWithTitleAndMessage(
        "Unexpected error",
        "There is an unexpected problem. There may be an issue with the connection. Please try again."
      )
    }
  }

  def submissionOfCompletedForm() = SecureMessageController.submit(utr.value)(
    FakeRequest().withFormUrlEncodedBody(
      "subject" -> subject,
      "message" -> adviceBody
    )
  )

  implicit class ShouldContainPageWithMessage(result: Future[Result]) {
    def shouldContainPageWithTitleAndMessage(title: String, message: String) = {
      status(result) shouldBe 200
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")

      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByTag("nav").attr("id") shouldBe "proposition-menu"

      withClue("result page title") {
        document.title() shouldBe title

      }

      withClue("result message") {
        val creationResult = document.select("h1")
        creationResult should have size 1
        creationResult.get(0).text() shouldBe message
      }
    }
  }

  implicit class ReturnsRedirectTo(result: Future[Result]) {
    def returnsRedirectTo(url: String) = {
      status(result) shouldBe 303

      redirectLocation(result) match {
        case Some(redirect) => redirect should startWith (s"/secure-message$url")
        case _ => fail("redirect location should always be present")
      }

    }
  }

  override val dependenciesPort: Int = 9847

  override protected val externalServices: Seq[ExternalService] = List.empty


}
