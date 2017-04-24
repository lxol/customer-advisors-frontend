package uk.gov.hmrc.contactadvisors.service

import org.mockito.ArgumentCaptor
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers._
import uk.gov.hmrc.contactadvisors.connectors.models.{SecureMessage, TaxpayerName}
import uk.gov.hmrc.contactadvisors.connectors.{MessageConnector, TaxpayerNameConnector}
import uk.gov.hmrc.contactadvisors.domain.{Advice, AdviceStored}
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class SecureMessageServiceSpec extends UnitSpec with MockitoSugar with ScalaFutures {

  "createMessageWithTaxpayerName" should {

    import scala.concurrent.ExecutionContext.Implicits.global
    implicit val hc = HeaderCarrier()

    "get taxpayer name details, store the message and return an AdviceStored result" in new TestCase {

      val advice = Advice("Advice subject", "Advice message body")
      val saUtr = SaUtr("1234")
      val messageCaptor = ArgumentCaptor.forClass(classOf[SecureMessage])

      when(taxpayerNameConnectorMock.taxpayerName(saUtr)).thenReturn(Future.successful(Some(validTaxpayerName)))
      when(messageConnectorMock.create(messageCaptor.capture())(any())).thenReturn(Future.successful(AdviceStored))

      val storageResult = secureMessageService.createMessageWithTaxpayerName(advice, saUtr).futureValue

      storageResult shouldBe AdviceStored
      val storedMessage = messageCaptor.getValue
      storedMessage.recipient.name shouldBe validTaxpayerName
    }

    "get no taxpayer details, creates a message with empty recipient name store the message and return an AdviceStored result" in new TestCase {
      val advice = Advice("Advice subject", "Advice message body")
      val saUtr = SaUtr("1234")
      val messageCaptor = ArgumentCaptor.forClass(classOf[SecureMessage])

      when(taxpayerNameConnectorMock.taxpayerName(saUtr)).thenReturn(Future.successful(None))
      when(messageConnectorMock.create(messageCaptor.capture())(any())).thenReturn(Future.successful(AdviceStored))

      val storageResult = secureMessageService.createMessageWithTaxpayerName(advice, saUtr).futureValue

      storageResult shouldBe AdviceStored
      val storedMessage = messageCaptor.getValue
      storedMessage.recipient.name shouldBe emptyTaxpayerName
    }

  }

  trait TestCase {
    val validTaxpayerName = TaxpayerName(title = Some("Mr"), forename = Some("John"), surname = Some("Smith"))
    val emptyTaxpayerName = TaxpayerName()

    val taxpayerNameConnectorMock: TaxpayerNameConnector = mock[TaxpayerNameConnector]
    val messageConnectorMock: MessageConnector = mock[MessageConnector]

    val secureMessageService = new SecureMessageService {
      override def messageConnector: MessageConnector = messageConnectorMock

      override def taxpayerNameConnector: TaxpayerNameConnector = taxpayerNameConnectorMock
    }
  }

}
