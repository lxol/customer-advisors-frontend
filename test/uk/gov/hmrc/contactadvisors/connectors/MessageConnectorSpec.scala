/*
 * Copyright 2017 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import com.github.tomakehurst.wiremock.http.Fault
import org.scalatest.Inside._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mock.MockitoSugar
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import play.api.http.Status
import play.api.libs.json.Json
import play.test.WithServer
import uk.gov.hmrc.contactadvisors.WSHttp
import uk.gov.hmrc.contactadvisors.domain.{AdviceAlreadyExists, AdviceStored, UnexpectedError}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.utils.SecureMessageCreator

class MessageConnectorSpec extends UnitSpec
  with MockitoSugar
  with ScalaFutures
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with WithFakeApplication
  with TableDrivenPropertyChecks
  with IntegrationPatience {

  val messagePort = 58008
  val wireMockServer = new WireMockServer(wireMockConfig().port(messagePort))

  override def beforeAll() = {
    super.beforeAll()
    wireMockServer.start()
    WireMock.configureFor(messagePort)
  }

  override def beforeEach() = {
    super.beforeEach()
    wireMockServer.resetMappings()
    wireMockServer.resetRequests()
  }

  override def afterAll() = {
    super.afterAll()
    wireMockServer.stop()
  }

  "message connector" should {
    implicit val hc = HeaderCarrier()

    "return the message id from the response" in new TestCase {
      givenThat(post(urlEqualTo(expectedPath)).
        willReturn(aResponse().
          withStatus(Status.CREATED).withBody("""{"id":"12341234"}""")))

      connector.create(secureMessage).futureValue shouldBe AdviceStored("12341234")
    }

    "indicate an invalid message id when the response contains a corrupted message id" in new TestCase {
      givenThat(post(urlEqualTo(expectedPath)).
        willReturn(aResponse().
          withStatus(Status.CREATED)))

      connector.create(secureMessage).futureValue shouldBe UnexpectedError("Missing id in: ''")
    }

    "return MessageAlreadyExists failure with true when the message service returns 409 (conflict) while saving" in
      new TestCase {
        stubFor(post(urlEqualTo(expectedPath)).willReturn(aResponse().withStatus(Status.CONFLICT)))

        connector.create(secureMessage).futureValue shouldBe AdviceAlreadyExists
      }

    forAll(Table("statusCode", 400, 401, 404, 415, 500)) { statusCode: Int =>
      s"return Failure with reason for status=$statusCode" in new TestCase {

        val errorMessage = Json.obj("reason" -> "something went wrong")
        givenThat(post(urlEqualTo(expectedPath))
          .willReturn(aResponse()
            .withStatus(statusCode)
            .withBody(errorMessage.toString())))

        val response = connector.create(secureMessage).futureValue
        response match {
          case UnexpectedError(reason) => {
            reason.toString should include(expectedPath)
            reason.toString should include(statusCode.toString)
            reason.toString should include("'{\"reason\":\"something went wrong\"}'")
          }
        }
      }
    }

    "fail with a reason indicating the status code when message returns 204" in new TestCase {
      val statusCode = 204
      givenThat(post(urlEqualTo(expectedPath))
        .willReturn(aResponse()
          .withStatus(statusCode)))

      val response = connector.create(secureMessage).futureValue
      response shouldBe UnexpectedError(s"Unexpected status : 204 from ${messageServiceBaseUrl + expectedPath}")
    }

    "fail when an IOException occurs when saving" in new TestCase {
      givenThat(post(urlEqualTo(expectedPath))
        .willReturn(aResponse()
          .withFault(Fault.RANDOM_DATA_THEN_CLOSE)))

      val response = connector.create(secureMessage).futureValue
      response shouldBe UnexpectedError("Remotely closed")
    }
  }

  trait TestCase extends WithServer {

    val messageServiceBaseUrl = s"http://localhost:$messagePort"
    val expectedPath = s"/messages"

    val secureMessage = SecureMessageCreator.message
    val fullTaxpayerName = SecureMessageCreator.taxpayerName

    val connector = new MessageConnector {

      lazy val http: HttpPost = WSHttp

      override def serviceUrl: String = messageServiceBaseUrl
    }
  }

}
