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

import org.joda.time.DateTime
import org.jsoup.Jsoup
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import play.api.http.Status._
import play.api.libs.ws.WS
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.it.{ExternalServiceRunner, MongoMicroServiceEmbeddedServer}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.duration._

class ApiISpec extends UnitSpec
  with ScalaFutures
  with BeforeAndAfterAll
  with Eventually {
  self =>

  lazy val server: CustomerAdvisorsIntegrationServer =
    CustomerAdvisorsIntegrationServer.server

  def resource(path: String): String = server.resource(path)

  override def beforeAll(): Unit = {
    super.beforeAll()
    if (!CustomerAdvisorsIntegrationServer.serverControlledByItSuite) {
      server.start()
    }
  }

  override def afterAll(): Unit = {
    super.afterAll()
    if (!CustomerAdvisorsIntegrationServer.serverControlledByItSuite) {
      server.stop()
    }
  }

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit lazy val app = play.api.Play.current

  "POST /customer-advisors-frontend/submit" should {
    "redirect to the success page when the form submission is successful" in {

      val content = DateTime.now().toString
      val fhddsRef = "XZFH00000100024"
      val p = resource(s"secure-message/customer-advisors-frontend/submit?content=${content}21&subject=mysubject&recipientTaxidentifierName=sautr&recipientTaxidentifierValue=${fhddsRef}&recipientEmail=foo@domain.com&recipientNameLine1=rLine1&messageType=mType")
      val response = WS.url(p).post("")
      response.status shouldBe OK
      val body = response.futureValue.body
      val document = Jsoup.parse(body)
      withClue("result page title") {
        document.title() shouldBe "Advice creation successful"
      }
      withClue("result page FHDDS Reference") {
        document.select("ul li").get(0).text() shouldBe s"FHDDS Reference: ${fhddsRef}"
      }
      withClue("result page Message Id") {
        document.select("ul li").get(1).text() should startWith regex "Id: [0-9a-f]+"
      }
      withClue("result page External Ref") {
        document.select("ul li").get(2).text() should startWith regex "External Ref: [0-9a-f-]+"
      }
    }
    "redirect to the unexpected page when the form submission is unsuccessful" in {

      val content = DateTime.now().toString
      val fhddsRef = "XZFH00000100024"
      val wrongEmail = "foobar"
      val p = resource(s"secure-message/customer-advisors-frontend/submit?content=${content}21&subject=mysubject&recipientTaxidentifierName=sautr&recipientTaxidentifierValue=${fhddsRef}&recipientEmail=${wrongEmail}&recipientNameLine1=rLine1&messageType=mType")
      val response = WS.url(p).post("")
      response.status shouldBe OK
      val body = response.futureValue.body
      val document = Jsoup.parse(body)
      withClue("result page title") {
        document.title() shouldBe "Unexpected error"
      }

      withClue("result page title") {
        document.title() shouldBe "Unexpected error"
      }
      withClue("result page h2") {
        document.select("h2").text().trim shouldBe s"Failed"
      }
      withClue("result page alert message") {
        document.select("p.alert__message").text() should include (s"${fhddsRef}")
      }
    }
  }
}

class CustomerAdvisorsIntegrationServer(override val testName: String,
                                        servicesFromJar: Seq[String],
                                        servicesFromSource: Seq[String],
                                        scenarioAdditionalConfig: Map[String, Any])
  extends MongoMicroServiceEmbeddedServer {

  override val externalServices = servicesFromJar.map(ExternalServiceRunner.
    runFromJar(_)) ++ servicesFromSource.map(ExternalServiceRunner.runFromSource(_))

  override protected val additionalConfig: Map[String, Any] = Map(
    "Dev.microservice.services.customer-advisors-frontend.port" -> servicePort,
    "ws.timeout.idle" -> "600000"
  ) ++ scenarioAdditionalConfig

  override protected def startTimeout = 240.seconds
}

object CustomerAdvisorsIntegrationServer {
  val UserDetails = "user-details"
  val Preferences = "preferences"
  val EntityResolver = "entity-resolver"
  val HmrcDeskPro = "hmrc-deskpro"
  val SA = "sa"
  val Datastream = "datastream"
  val Message = "message"
  val allExternalServices = Seq(
    UserDetails,
    Preferences,
    EntityResolver,
    SA,
    Datastream,
    Message
  )
  var serverControlledByItSuite = false

  lazy val server = new CustomerAdvisorsIntegrationServer("ALL_CUSTOMER_ADVISORS_IT_TESTS", allExternalServices, Seq(), Map())
}
