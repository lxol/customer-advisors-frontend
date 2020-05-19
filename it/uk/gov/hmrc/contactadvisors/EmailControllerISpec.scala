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

package uk.gov.hmrc.contactadvisors

import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.{ Eventually, ScalaFutures }
import org.scalatestplus.play.PlaySpec
import play.api.Mode
import play.api.http.Status._
import play.api.libs.json.{ JsValue, Json }
import play.api.libs.ws.{ WSClient, WSResponse }
import uk.gov.hmrc.contactadvisors.util.AuthHelper
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.integration.ServiceSpec

import scala.concurrent.Future
import scala.concurrent.duration._

class EmailControllerISpec extends PlaySpec with ScalaFutures with BeforeAndAfterAll with Eventually with ServiceSpec {

  override val applicationMode = Mode.Test
  def externalServices: Seq[String] = Seq.empty

  protected def startTimeout = 240.seconds

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "POST /customer-advisors-frontend/email" should {
    val emailUrl = resource("/secure-message/hmrc/email")
    /*
          "sdfafasdf" in new TestCase {
            val content = DateTime.now().toString
            val fhddsRef = "XFH00000100024"
            //val wsClient = app.injector.instanceOf[WSClient]
            println(s"""**** ${resource("secure-message/hmrc/email")}""")
            val strideToken = authHelper.buildStrideToken().futureValue
            println(s"**** $strideToken")
            val response = wsClient
              .url(resource("/secure-message/hmrc/email"))
              .withHttpHeaders(("authorization", strideToken))
              // .withHttpHeaders(("Csrf-Token", "nocheck"))
              .post(Map(
                "content"                     -> s"${content}21",
                "subject"                     -> "mysubject",
                "recipientTaxidentifierName"  -> "sautr",
                "recipientTaxidentifierValue" -> fhddsRef,
                "recipientEmail"              -> "test@test.com",
                "recipientNameLine1"          -> "line1",
                "messageType"                 -> "mType"
              ))
              .futureValue

            response.status must be(OK)

            // val body = response.body

            // val document = Jsoup.parse(body)

            /*
                withClue("result page title") {
                    document.title() must be("Advice creation successful")
                }
                withClue("result page FHDDS Reference") {
                    document.select("ul li").get(0).text() must be(s"FHDDS Reference: $fhddsRef")
                }
                withClue("result page Message Id") {
                    document.select("ul li").get(1).text() must startWith regex "Id: [0-9a-f]+"
                }
                withClue("result page External Ref") {
                    document.select("ul li").get(2).text() must startWith regex "External Ref: [0-9a-f-]+"
                }
     */
          }
     */
    "send an email with valid payload and entitlements" in new TestCase {
      val response = wsClient
        .url(emailUrl)
        .withHttpHeaders(("authorization", validStrideToken))
        .post(validJson)
        .futureValue
      response.status must be(ACCEPTED)
    }
    "return BadRequest if payload is not json" in new TestCase {
      val response = wsClient
        .url(emailUrl)
        .withHttpHeaders(("authorization", validStrideToken))
        .post("notjson")
        .futureValue
      response.status must be(BAD_REQUEST)
      response.body must be("""{"error": "invalid payload"}""")
    }

    "return BadRequest if payload has extra parameters " in new TestCase {
      val response = wsClient
        .url(emailUrl)
        .withHttpHeaders(("authorization", validStrideToken))
        .post(extraParameters)
        .futureValue
      response.status must be(BAD_REQUEST)
      response.body must be("""{"error": "invalid parameters"}""")
    }

    "return FORBIDDEN if insufficient entitlements" in new TestCase {
      val response = wsClient
        .url(emailUrl)
        .withHttpHeaders(("authorization", invalidStrideToken))
        .post(validJson)
        .futureValue
      response.status must be(FORBIDDEN)
    }
    "return UNAUTHORIZED if not logged in" in new TestCase {
      val response = wsClient
        .url(emailUrl)
        .post(validJson)
        .futureValue
      response.status must be(UNAUTHORIZED)
    }
    //   "redirect to the unexpected page when the form submission is unsuccessful" in {

    //     val content = DateTime.now().toString
    //     val fhddsRef = "XZFH00000100024"
    //     val wrongEmail = "foobar"
    //     val wsClient = app.injector.instanceOf[WSClient]

    //     val response = wsClient
    //       .url(resource("secure-message/customer-advisors-frontend/submit"))
    //       .post(Map(
    //         "content"                     -> s"${content}21",
    //         "subject"                     -> "mysubject",
    //         "recipientTaxidentifierName"  -> "sautr",
    //         "recipientTaxidentifierValue" -> s"$fhddsRef",
    //         "recipientEmail"              -> s"$wrongEmail",
    //         "recipientNameLine1"          -> "rLine1",
    //         "messageType"                 -> "mType"
    //       ))
    //       .futureValue

    //     response.status must be(OK)
    //     val body = response.body
    //     val document = Jsoup.parse(body)
    //     withClue("result page title") {
    //       document.title() must be("Unexpected error")
    //     }

    //     withClue("result page title") {
    //       document.title() must be("Unexpected error")
    //     }
    //     withClue("result page h2") {
    //       document.select("h2").text().trim must include(s"Failed")
    //     }
    //     withClue("result page alert message") {
    //       document.select("p.alert__message").text() must include(s"$fhddsRef")
    //     }
    //   }
    // }
  }
  trait TestCase {

    val wsClient = app.injector.instanceOf[WSClient]
    lazy val authHelper = app.injector.instanceOf[AuthHelper]

    val validStrideToken = authHelper.buildStrideToken().futureValue
    val invalidStrideToken = authHelper.authorisedTokenFor(Nino("AB123456C")).futureValue
    val validJson = Json.parse("""
                                 |{
                                 |  "to": ["example@domain.com"],
                                 |  "templateId": "seiss_claim_now",
                                 |  "parameters": {
                                 |        "name": "Mr Joe Bloggs"
                                 |  },
                                 |  "clientId": "ba6219ed-d6be-48e1-8612-ed5e793274f7"
                                 |}
        """.stripMargin)

    val extraParameters = Json.parse("""
                                       |{
                                       |  "to": ["example@domain.com"],
                                       |  "templateId": "seiss_claim_now",
                                       |  "parameters": {
                                       |        "name": "Mr Joe Bloggs",
                                       |        "secret": "secret value"
                                       |  },
                                       |  "clientId": "ba6219ed-d6be-48e1-8612-ed5e793274f7"
                                       |}
        """.stripMargin)

    val invalidJson = Json.parse("""{"key": "value"}""")
    def sendEmail(payload: JsValue): Future[WSResponse] =
      wsClient
        .url(resource(s"/email"))
        .withHttpHeaders(("authorization", authHelper.buildStrideToken().futureValue))
        .post(payload)

  }
}
