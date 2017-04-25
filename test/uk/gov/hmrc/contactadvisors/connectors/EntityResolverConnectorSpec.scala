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

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.Inside._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.play.OneServerPerSuite
import play.api.http.Status
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.contactadvisors.WSHttp
import uk.gov.hmrc.contactadvisors.domain.{CustomerIsNotPaperless, TaxIdNotFound, UnexpectedFailure}
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet}
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.utils.WithWiremock


class EntityResolverConnectorSpec extends UnitSpec
    with OneServerPerSuite
    with ScalaFutures
    with WithWiremock
    with TableDrivenPropertyChecks
    with IntegrationPatience {

  implicit val hc = HeaderCarrier()

  override lazy val dependenciesPort = 8015

  "The Entity Resolver connector" should {

    "return true when provided a tax identifier for a valid user, opted-in for paperless" in new TestCase {
      entityResolverReturns(Status.OK, Some(Json.obj(
        "digital" -> true,
        "email" -> Json.obj(
          "email" -> "bbc14eef-97d3-435e-975a-f2ab069af000@TEST.com",
          "mailboxFull" -> false,
          "status" -> "verified"
        )
      )))
      connector.validPaperlessUserWith(utr).futureValue shouldBe true
    }

    "return CustomerCannotReceiveAlerts when provided a tax identifier for a user that has opted out of paperless" in new TestCase {
      entityResolverReturns(Status.OK, Some(Json.obj("digital" -> false)))
      intercept[CustomerIsNotPaperless] { await(connector.validPaperlessUserWith(utr)) }
    }

    "return UnknownTaxId when provided a tax identifier that cannot be resolved" in new TestCase {
      entityResolverReturns(Status.NOT_FOUND)

      intercept[TaxIdNotFound] { await(connector.validPaperlessUserWith(utr)) }
    }

    forAll(Table("statusCode", 400, 401, 403, 415, 500)) { statusCode: Int =>
      s"return unexpected failure when the response has status $statusCode" in new TestCase {
        entityResolverReturns(statusCode)

        inside(connector.validPaperlessUserWith(utr).failed.futureValue) {
          case UnexpectedFailure(msg) =>
            msg should include(statusCode.toString)
        }
      }
    }
  }

  trait TestCase {
    def entityResolverBaseUrl = s"http://localhost:$dependenciesPort"
    def utr = SaUtr("0329u490uwesakdjf")
    def pathToPreferences = s"/portal/preferences/sa/$utr"

    def connector = new EntityResolverConnector {

      override def http: HttpGet = WSHttp

      override def serviceUrl: String = entityResolverBaseUrl

    }

    def entityResolverReturns(status: Int, responseBody: Option[JsObject] = None) =
      givenThat(
        get(urlEqualTo(pathToPreferences)).
          willReturn(

            responseBody.fold(aResponse().withStatus(status).withBody("")) { json =>
              aResponse().withStatus(status).withBody(Json.stringify(json))
            }
          )
        )
  }

}
