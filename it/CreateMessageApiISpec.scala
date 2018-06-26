import java.util.UUID
import org.scalatest.DoNotDiscover

import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status._
import play.api.libs.json.{ JsObject, JsValue, Json }
import play.api.libs.ws.{WS, WSResponse}
import play.api.test.FakeRequest
import uk.gov.hmrc.domain._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import scala.concurrent.duration._
import org.scalatest.BeforeAndAfterAll

import uk.gov.hmrc.play.it.{ExternalServiceRunner, ExternalServicesServer, MongoMicroServiceEmbeddedServer, MongoTestConfiguration, ServiceSpec}
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}

import scala.concurrent.Future

import uk.gov.hmrc.http.HttpResponse
import org.joda.time.LocalDate
import util.FakeRequestBuilder

class CreateMessageApiISpec extends UnitSpec
    with ScalaFutures
    with BeforeAndAfterAll
    with Eventually {
  self =>

  lazy val server:CustomerAdvisorsIntegrationServer =
    CustomerAdvisorsIntegrationServer.server

  def resource(path: String): String = server.resource(path)

  override def beforeAll(): Unit = {
    super.beforeAll()
    if (!CustomerAdvisorsIntegrationServer.serverControlledByItSuite) {
      println(s"******* INTEGRATION SERVER START")
      server.start()
    }
  }

  override def afterAll(): Unit = {
    super.afterAll()
    if (!CustomerAdvisorsIntegrationServer.serverControlledByItSuite) {
      println(s"******* INTEGRATION SERVER STOP")
      server.stop()
    }
  }
  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit lazy val app = play.api.Play.current

  "Adding a message" should {
    val now = LocalDate.now
    "POST addhoc test" in {

      lazy val input = Json.parse(
        s"""
       | {
       |   "message" : "test message content",
       |   "subject" : "statement"
       | }
    """.stripMargin).as[JsObject]
      
      val request = FakeRequest("POST", "/inbox/123456789").withFormUrlEncodedBody(
        "subject" -> "New message subject",
        "message" -> "New message body"
      )
      println(s"***** request: ${request}")

      // val p = resource(s"secure-message/inbox/1234567890?message=foomessage&subject=mysubject")
      val p = resource(s"secure-message/inbox/12367890?message=foomessage2&subject=mysubject")
      println(s"******* path: ${p}")
      val response = WS.url(p ).post("")
      // val response = FakeRequestBuilder(p).post(input)
      response.status shouldBe OK
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

  override protected val additionalConfig:Map[String, Any] = Map(
    "Dev.microservice.services.customer-advisors-frontend.port" -> servicePort,
    "ws.timeout.idle" -> "600000"
  ) ++ scenarioAdditionalConfig

  override protected def startTimeout = 240.seconds
}

object CustomerAdvisorsIntegrationServer {
  val Auth = "auth"
  val AuthLoginApi = "auth-login-api"
  val UserDetails = "user-details"
  val Preferences = "preferences"
  val EntityResolver = "entity-resolver"
  val Email = "email"
  val Mailgun = "mailgun"
  val HmrcDeskPro = "hmrc-deskpro"
  val ExternalDeskPro = "external-deskpro"
  val TaxpayerData = "taxpayer-data"
  val SA = "sa"
  val Datastream = "datastream"
  val HmrcEmailRenderer = "hmrc-email-renderer"
  val Message = "message"
  val allExternalServices = Seq(
    // Auth,
    // AuthLoginApi,
    UserDetails,
    Preferences,
    EntityResolver,
    // Email,
    // Mailgun,
    // HmrcDeskPro,
    // ExternalDeskPro,
    // TaxpayerData,
    SA,
    Datastream,
    Message
    // HmrcEmailRenderer
  )
  var serverControlledByItSuite = false

  lazy val server = new CustomerAdvisorsIntegrationServer("ALL_CUSTOMER_ADVISORS_IT_TESTS", allExternalServices, Seq(), Map())
}

