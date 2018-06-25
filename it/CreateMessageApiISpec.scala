
import java.util.UUID
import org.scalatest.DoNotDiscover

import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status._
import play.api.libs.json.{ JsObject, JsValue, Json }
import play.api.libs.ws.{WS, WSResponse}
// import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.domain._
import uk.gov.hmrc.http.HeaderCarrier
// import uk.gov.hmrc.message.connectors.NameFromHods
// import uk.gov.hmrc.message.controllers.model.{ExternalRef, Recipient}
// import uk.gov.hmrc.message.it.util.MessageFixtures._
// import uk.gov.hmrc.message.model.Message
import uk.gov.hmrc.play.test.UnitSpec
// import uk.gov.hmrc.message.util.CustomerAdvisorsIntegrationServer.{Email, _}
// import uk.gov.hmrc.message.util._
import scala.concurrent.duration._
import org.scalatest.BeforeAndAfterAll

import uk.gov.hmrc.play.it.{ExternalServiceRunner, ExternalServicesServer, MongoMicroServiceEmbeddedServer, MongoTestConfiguration, ServiceSpec}
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}

import scala.concurrent.Future

import uk.gov.hmrc.http.HttpResponse
import org.joda.time.LocalDate
// import uk.gov.hmrc.play.microservice.bootstrap.ErrorResponse

// @DoNotDiscover
class CreateMessageApiISpec extends MessagingISpec  {
  self =>

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "Adding a message" should {
    val now = LocalDate.now
    "asdfa df asd fa dsf" in new TestCase {
      lazy val input = Json.parse(
        s"""
        | {
        |   "id" : "asdfasdf",
        |   "recipient" : { "regime" : "sa", "identifier" : { "sautr" : "$ctUtr" } },
        |   "subject" : "statement",
        |   "validFrom": "${"%04d-%02d-%02d".format(now.getYear, now.getMonthOfYear, now.getDayOfMonth)}",
        |   "alertDetails": {
        |       "alertFrom": "${"%04d-%02d-%02d".format(now.getYear, now.getMonthOfYear, now.getDayOfMonth)}",
        |       "data": {},
        |       "templateId": "testAlertTemplate"
        |    },
        |   "body": {
        |      "type": "ats-message-renderer"
        |   },
        |   "contentParameters": {
        |      "data": {},
        |      "templateId": "ats_v2"
        |   },
        |   "hash": "ABCDEFGHIJ",
        |   "statutory": false,
        |   "renderUrl": {
        |      "service": "ats-message-renderer",
        |      "url": "/ats-message-renderer/message/{messageId}"
        |   }
        | }
     """.stripMargin).as[JsObject] 
      val messageCreationResponse: WSResponse = `/inbox`.post(input).futureValue
      // val messageId: String = (messageCreationResponse.json \ "id").as[String]
      // val messageResponse: WSResponse = getMessageBy(messageId, authorisedTokenFor(utr)).futureValue
      // val messageJson: JsValue = messageResponse.json

      messageCreationResponse.status shouldBe CREATED
      // messageResponse.status shouldBe OK
      // (messageResponse.json \ "id").as[String] shouldBe messageId
      // (messageResponse.json \ "subject").as[String] shouldBe (messageJson \ "subject").as[String]
      // (messageResponse.json \ "validFrom").as[String] shouldBe (messageJson \ "validFrom").as[String]
      // (messageResponse.json \ "sentInError").as[Boolean] shouldBe false
    }

  }


  trait TaxpayerDataPrimer {

    lazy val `/test-only/self-assessment/individual/:utr/designatory-details/taxpayer` = new {

      private def formatPayload(payload: String) = {
        Json.parse(payload)
      }

      def post(utr: SaUtr, jsonPayload: String): Future[WSResponse] = {
        val payload = formatPayload(jsonPayload)
        val url = WS.url(server.externalResource("taxpayer-data", s"/test-only/self-assessment/individual/$utr/designatory-details/taxpayer"))
        url.post(payload)
      }
    }
  }

  trait TestCase extends ScalaFutures
    // with SaIntegrationPatience
    with TaxpayerDataPrimer {

    // implicit lazy val app = play.api.Play.current
    // lazy val server: CustomerAdvisorsIntegrationServer = CustomerAdvisorsIntegrationServer.server
    def resource(path: String): String = server.resource(path)
    // val utr: SaUtr = GenerateRandomForIT.utr()
    // val nino: Nino = GenerateRandomForIT.nino()
    val ctUtr: CtUtr = CtUtr("123456789")

    // def `/messages` = new {
    //   def post(body: JsValue) =  WS.url(resource(s"/messages")).post(body)
    // }
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
    "Dev.microservice.services.message.port" -> servicePort,
    "Dev.scheduling.sendAlerts.initialDelay" -> "1 day", // Deliberate - effectively disabled
    "Dev.scheduling.processNotifications.initialDelay" -> "1 day", // Deliberate - effectively disabled
    "Dev.messages.delayMessageProcessing" -> "false", // Deliberate - effectively disabled
    "ws.timeout.idle" -> "600000"
  ) ++ (externalServicePorts.get(CustomerAdvisorsIntegrationServer.Datastream)
                            .map(dsPort => "Dev.auditing.consumer.baseUri.port" -> dsPort)
        ++ scenarioAdditionalConfig)

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
    // UserDetails,
    Preferences,
    EntityResolver,
    // Email,
    // Mailgun,
    // HmrcDeskPro,
    // ExternalDeskPro,
    // Taxpaye,
    SA,
    Datastream,
    // HmrcEmailRenderer,
    Message
  )

  var serverControlledByItSuite = false

  lazy val server = new CustomerAdvisorsIntegrationServer("ALL_MESSAGE_IT_TESTS", allExternalServices, Seq(), Map())
}

abstract class MessagingISpec(
  customMessagingServer: Option[CustomerAdvisorsIntegrationServer] = None
) extends UnitSpec
    // with IntegrationPatience
    with ScalaFutures
    // with MessageRepositoryForIntegrationTest
    // with ResponseMatchers
    // with SaIntegrationPatience
    // with AuthHelper
    with BeforeAndAfterAll
    with Eventually {

  lazy val server:CustomerAdvisorsIntegrationServer =
    customMessagingServer.getOrElse(CustomerAdvisorsIntegrationServer.server)

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

  implicit lazy val app = play.api.Play.current

  implicit lazy val httpClient = WS.client

  def authResource(path: String) = server.externalResource("auth-login-api", path)

  def processEmailQueueNow() = WS.url(server.externalResource("email", "test-only/hmrc/email-admin/process-email-queue")).post("")

  // protected def extractErrorResponse(request: => Future[HttpResponse], expectedStatusCode: Int, expectedErrorMessage: Option[String]): HttpResponse = {

  //   import play.api.libs.json.Json

  //   implicit val errorResponseFormat = Json.format[ErrorResponse]

  //   val response: HttpResponse = await(request)

  //   withClue(s"Response body: ${response.body} - ") {
  //     response.status shouldBe expectedStatusCode

  //     expectedErrorMessage.foreach {
  //       expectedError: String =>
  //       Try(Json.parse(response.body).validate[ErrorResponse]) match {
  //         case Success(JsSuccess(err, _)) => err.message shouldBe expectedError
  //         case other => fail(s"Expected '$expectedError', got: $other")
  //       }
  //     }
  //   }

  //   response
  // }

  protected def verifyStatusCodeOnly(request: => Future[HttpResponse], expectedStatus: Int)(implicit timeout: scala.concurrent.duration.Duration): HttpResponse = {
    val response = await(request)(timeout)

    withClue(s"Response body: ${response.body} - ") {
      response.status shouldBe expectedStatus
    }

    response
  }

  protected def verifyStatusCodeWithPayload(request: => Future[HttpResponse], expectedStatus: Int, expectedPayload: JsValue) {
    val response = await(request)

    withClue(s"Response body: ${response.body} - ") {
      response.status shouldBe expectedStatus
      Json.parse(response.body) shouldBe expectedPayload
    }
  }

  // def clearNotifications() {
  //   WS.url(server.externalResource("notification", "/admin/notifications")).delete() should have(status(200))
  // }

  // def clearPreferences() = {
  //   WS.url(server.externalResource("preferences", "/preferences-admin/sa/individual/print-suppression")).delete() should have(status(200))
  // }

  final case class TaxIdBody(idType: String, id: String)
  object TaxIdBody {
    implicit val format = Json.format[TaxIdBody]
  }

  final case class CustomerAdviceRequest(taxId: TaxIdBody, adviceBody: String)
  object CustomerAdviceRequest {
    implicit val format = Json.format[CustomerAdviceRequest]
  }

  def `/message/system/process-event/:id`(id: String) = new {
    def post(body: JsValue) = WS.url(resource(s"/message/system/process-event/$id")).post(body)
  }

  def `/message/system/:id/send-alert`(id: String) = new {
    def get = WS.url(resource(s"/message/system/$id/send-alert")).get()
  }

  def  `/admin/message/re-queue` = new {
    def post(messageIds: String*) = WS.url(resource(s"/admin/message/re-queue")).post(Json.toJson(messageIds))
  }

  def `/admin/message/add-rescindment` = new {
    def post(messageIds: String*) = WS.url(resource(s"/admin/message/add-rescindment")).post(Json.toJson(messageIds))
  }

  def `/admin/message/add-extra-alert` = new {
    def post(body: JsValue) = WS.url(resource(s"/admin/message/add-extra-alert")).post(body)
  }

  def `/v1/messages` =  new {
    def post(body: JsValue) =  WS.url(resource(s"/v1/messages")).post(body)
  }

  def `/v2/messages` = new {
    def post(body: JsValue) =  WS.url(resource(s"/v2/messages")).post(body)
  }

  def `/inbox` = new {
    def post(body: JsValue) =  WS.url(resource(s"/secure-message/inbox")).post(body)
  }

  def getMessageFromUrl(url:String) = WS.url(resource(url)).get()

  def getAllMessagesFor(
    authorisationToken: Future[String],
    countOnly: Boolean
  ): Future[WSResponse] = {
    WS.url(resource(s"/messages?countOnly=$countOnly")).
      withHeaders(("authorization", authorisationToken)).
      get()
  }

  def getAllFilteredMessagesFor(
    authorisationToken: Future[String],
    countOnly: Boolean, taxIdentifiers: List[String]
  ): Future[WSResponse] = {
    val tIds = taxIdentifiers.fold("")((a,b) => a + s"&taxIdentifiers=$b")
    WS.url(resource(s"/messages?countOnly=$countOnly$tIds")).
      withHeaders(("authorization", authorisationToken)).
      get()
  }

  def getMessageBy(id: String, authorisationToken: Future[String]) = {
    WS.url(resource(s"/messages/$id")).
      withHeaders(("authorization", authorisationToken)).
      get()
  }

  def getContentBy(id: String, authorisationToken: Future[String]) = {
    WS.url(resource(s"/messages/$id/content")).
      withHeaders(("authorization", authorisationToken)).
      get()
  }

  // def updateReadTimeFor(taxId: TaxIdWithName, messageId: BSONObjectID, authorisationToken: Future[String]): Future[WSResponse] =
  //   WS.url(resource(s"/messages/${messageId.stringify}/read-time")).
  //     withHeaders(("authorization", authorisationToken)).
  //     post(EmptyContent())


  // trait TestCaseWithEmailSupport extends EmailSupport with SaIntegrationPatience {

  //   protected def dispatchNewMessageAlerts() = {
  //     val response: Future[WSResponse] = WS.url(resource("/admin/send-alerts")).post(EmptyContent())
  //     response should have(status(200))
  //     response
  //   }

  //   protected def dispatchExtraAlerts() = {
  //     val response: Future[WSResponse] = WS.url(resource("/admin/send-extra-alerts")).post(EmptyContent())
  //     response should have(status(200))
  //     response
  //   }

  //   protected def withReceivedEmails(expectedCount: Int)(assertions: List[Email] => Unit) {
  //     implicit val patienceConfig = PatienceConfig(timeout = scaled(Span(60, Seconds)), interval = scaled(Span(400, Millis)))
  //     val listOfMails = eventually {
  //       val emailList = emails()
  //       emailList should have size expectedCount
  //       emailList
  //     }
  //     assertions(listOfMails)
  //   }
  // }

  // trait NotificationsTestCase {
  //   val today = new LocalDate(ISOChronology.getInstanceUTC)

  //   def formId = "SA300 Cumbernauld"

  //   def utr = "1555369043"

  //   def sa300NotificationWith(detailsId: String) = new {
  //     val id = utr
  //     val idType = "utr"
  //     val suppressedAt = "2013-09-16"
  //     val toJson = Json.parse(
  //       s"""{ "id": "$id", "idType": "$idType", "form": "$formId", "suppressedAt": "$suppressedAt", "dispatchOn": "22/11/2013", "detailsId": "$detailsId"}""").as[JsObject]
  //   }

  //   def randomDetailsId = "C" + Random.alphanumeric.filter(_.isDigit).take(16).toSeq.mkString

  //   val notification = sa300NotificationWith(detailsId = "C0123456781234568")

  //   val `/notification/sa/print-suppression` = WS.url(server.externalResource("notification", "/notification/sa/print-suppression"))
  // }

  // trait TestCaseWithATodoNotification extends NotificationsTestCase {
  //   clearNotifications()
  //   `/notification/sa/print-suppression`.put(JsArray(Seq(notification.toJson))) should have(status(200))
  // }

  // case class AuthorisationHeader(value: Option[String]) {
  //   def asHeader: Seq[(String, String)] = value.fold(Seq.empty[(String, String)])(v => Seq(HeaderNames.AUTHORIZATION -> v))
  // }
}
