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

  // "Adding a message" should {
  //   val now = LocalDate.now
  //   "POST addhoc test" in {

  //     val p = resource(s"secure-message/inbox/12367890?message=foomessage2&subject=mysubject")
  //     val response = WS.url(p ).post("")
  //     response.status shouldBe OK
  //   }
  // }

  "Adding a message using v2 API" should {
    "" in {
      val p = resource(s"secure-message/customer-advisors-frontend/submit?content=foomessage2&subject=mysubject&recipientTaxidentifierName=rName&recipientTaxidentifierValue=tValue&recipientEmail=rEmail&recipientNameLine1=rLine1&messageType=mType")
     val response = WS.url(p ).post("")
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
