package uk.gov.hmrc.contactadvisors.controllers

import org.mockito.ArgumentCaptor
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import uk.gov.hmrc.contactadvisors.domain.AdviceStored
import uk.gov.hmrc.contactadvisors.service.{HtmlCleaner, SecureMessageService}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class CustomerAdviceAuditSpec extends UnitSpec with ScalaFutures with IntegrationPatience {
  "SecureMessageController"  should {
    "audit the successful event" ignore new TestCase {
      when(secureMessageServiceMock.createMessageWithTaxpayerName(any(), any())(any(), any()))
        .thenReturn(Future.successful(AdviceStored("1234")))

      await(controller.submit("123456789"))

      verify(auditConnectorMock).sendEvent(dataEventCaptor.capture())
      dataEventCaptor.getValue shouldBe DataEvent
    }
  }
}

trait TestCase extends MockitoSugar {

  val secureMessageServiceMock = mock[SecureMessageService]
  val auditConnectorMock = mock[AuditConnector]

  val controller = new SecureMessageController {
    override val secureMessageService: SecureMessageService = secureMessageServiceMock
    override def htmlCleaner: HtmlCleaner = ???
    override def auditSource: String = "customer-advisors-frontend"
    override def auditConnector: AuditConnector = auditConnectorMock
  }
  val dataEventCaptor = ArgumentCaptor.forClass(classOf[DataEvent])
}
