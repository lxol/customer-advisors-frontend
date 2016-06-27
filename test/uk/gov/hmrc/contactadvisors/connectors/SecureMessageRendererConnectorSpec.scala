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

package uk.gov.hmrc.contactadvisors.connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.Inside._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mock.MockitoSugar
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.skyscreamer.jsonassert.JSONCompareMode
import play.api.http.Status
import play.api.libs.json.Json
import play.test.WithServer
import uk.gov.hmrc.contactadvisors.WSHttp
import uk.gov.hmrc.contactadvisors.domain._
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.utils.WithWiremock

class SecureMessageRendererConnectorSpec extends UnitSpec
  with MockitoSugar
  with ScalaFutures
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with WithFakeApplication
  with TableDrivenPropertyChecks
  with IntegrationPatience
  with WithWiremock {

  "secure message renderer connector" should {
    implicit val hc = HeaderCarrier()

    "return AdviceStored saves advice successfully" in new TestCase {
      givenSecureMessageRendererRespondsWith(Status.OK)

      connector.saveNew(Advice(subject, adviceBody), utr).futureValue shouldBe AdviceStored
    }

    "return AdviceAlreadyExists when renderer returns a conflict (409)" in new TestCase {
      givenSecureMessageRendererRespondsWith(Status.CONFLICT)

      connector.saveNew(Advice(subject, adviceBody), utr).futureValue shouldBe AdviceAlreadyExists
    }

    forAll(Table("statusCode", 400, 401, 404, 415, 500)) { statusCode: Int =>
      s"return UnexpectedError when response has status $statusCode" in
        new TestCase {
          val errorMsg: String = "There has been an error"
          givenSecureMessageRendererRespondsWith(statusCode, body = errorMsg)

          inside(connector.saveNew(Advice(subject, adviceBody), utr).futureValue) {
            case UnexpectedError(msg) =>
              msg should include(statusCode.toString)
              msg should include(errorMsg)
              msg should include(createAdvicePath)
          }
        }
    }
  }

  trait TestCase extends WithServer {
    val secureMessageRendererBaseUrl = s"http://localhost:$dependenciesPort"
    val createAdvicePath = "/advice"
    val subject = "This is message subject"
    val adviceBody = "<html>advice body</html>"
    val utr = "0329u490uwesakdjf"

    val connector = new SecureMessageRendererConnector {

      override def http: HttpPost = WSHttp

      override def serviceUrl: String = secureMessageRendererBaseUrl

    }

    def givenSecureMessageRendererRespondsWith(status: Int, body: String = ""): Unit = {
      givenThat(
        post(urlEqualTo(createAdvicePath))
          .withRequestBody(
            equalToJson(
              Json.stringify(
                Json.obj(
                  "subject" -> subject,
                  "adviceBody" -> adviceBody,
                  "taxId" -> Json.obj(
                    "idType" -> "sautr",
                    "id" -> utr
                  )
                )
              ),
              JSONCompareMode.LENIENT
            )
          )
          .willReturn(aResponse().withStatus(status).withBody(body)))
    }
  }

}
