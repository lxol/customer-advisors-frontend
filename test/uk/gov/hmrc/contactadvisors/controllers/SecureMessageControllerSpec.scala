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
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import scala.collection.JavaConverters._

class SecureMessageControllerSpec extends UnitSpec with WithFakeApplication with ScalaFutures {

  val request = FakeRequest("GET", "/")
  val postRequest = FakeRequest("POST", "/")
  val customer_utr = "__asd9887523512"

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
    "redirect to main form view" in {
      val result = SecureMessageController.submit(customer_utr)(
        FakeRequest().withFormUrlEncodedBody(
          "subject" -> "A very interesting subject 123",
          "message" -> "A message sent to the customer. lkasdfjas;ldfjk"
        )
      )

      status(result) shouldBe 303
      redirectLocation(result) match {
        case Some(redirect) => redirect should endWith ("/inbox/Success_sending_message")
        case _ => fail("redirect location should always be present")
      }
    }
  }
}
