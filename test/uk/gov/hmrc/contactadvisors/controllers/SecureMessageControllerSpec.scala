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

  val request = FakeRequest("GET", "/")
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
      val result = SecureMessageController.inbox(customer_utr)(request)
      status(result) shouldBe Status.OK
    }

    "return HTML" in {
      val result = SecureMessageController.inbox(customer_utr)(request)
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
    }

    "show main banner" in {
      val result = SecureMessageController.inbox(customer_utr)(request)
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByTag("nav").attr("id") shouldBe "proposition-menu"
    }

    "have the expected elements on the form" in {
      val result = SecureMessageController.inbox(customer_utr)(request)
      status(result) shouldBe 200

      val document = Jsoup.parse(contentAsString(result))
      val form = document.select("form#form-submit-customer-advice").get(0)
      val formElements = form.getAllElements.asScala

      val adviceSubject = formElements.find(_.id() == "subject")
      withClue("advice subject field") {
        adviceSubject.map(_.tagName()) shouldBe Some("input")
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
          "message" -> "A message sent to the customer. lkasdfjas;ldfjk"
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

  def submissionOfCompletedForm() = SecureMessageController.submit(utr.value)(
    FakeRequest().withFormUrlEncodedBody(
      "subject" -> subject,
      "message" -> adviceBody
    )
  )

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
