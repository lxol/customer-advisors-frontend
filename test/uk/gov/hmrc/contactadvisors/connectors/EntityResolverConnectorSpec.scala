/*
 * Copyright 2020 HM Revenue & Customs
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
import javax.inject.{Inject, Singleton}
import org.scalatest.Inside._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.libs.json.{JsObject, Json}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.contactadvisors.domain.UnexpectedFailure
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.utils.WithWiremock

@Singleton
class TestEntityResolverConnector @Inject()(http: HttpClient,
                                            val runModeConfiguration: Configuration,
                                            servicesConfig: ServicesConfig,
                                            val environment: Environment)
  extends EntityResolverConnector(http, runModeConfiguration, servicesConfig, environment) {
  override lazy val serviceUrl: String = s"http://localhost:8015"
}

class EntityResolverConnectorSpec extends PlaySpec
  with GuiceOneAppPerSuite
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
      connector.validPaperlessUserWith(utr).futureValue must be(Some(PaperlessPreference(true)))
    }

    "return CustomerCannotReceiveAlerts when provided a tax identifier for a user that has opted out of paperless" in new TestCase {
      entityResolverReturns(Status.OK, Some(Json.obj("digital" -> false)))

      connector.validPaperlessUserWith(utr).futureValue must be(Some(PaperlessPreference(false)))
    }

    "return UnknownTaxId when provided a tax identifier that cannot be resolved" in new TestCase {
      entityResolverReturns(Status.NOT_FOUND)

      connector.validPaperlessUserWith(utr).futureValue must be(None)
    }

    forAll(Table("statusCode", 400, 401, 403, 415, 500)) { statusCode: Int =>
      s"return unexpected failure when the response has status $statusCode" in new TestCase {
        entityResolverReturns(statusCode)

        inside(connector.validPaperlessUserWith(utr).failed.futureValue) {
          case UnexpectedFailure(msg) =>
            msg must include(statusCode.toString)
        }
      }
    }
  }

  trait TestCase {
    def utr = SaUtr("0329u490uwesakdjf")

    def pathToPreferences = s"/portal/preferences/sa/$utr"

    def connector = app.injector.instanceOf(classOf[TestEntityResolverConnector])

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
