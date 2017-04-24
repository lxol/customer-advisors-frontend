package uk.gov.hmrc.contactadvisors.service

import org.apache.commons.codec.binary.Base64
import org.mockito.ArgumentCaptor
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers._
import uk.gov.hmrc.contactadvisors.connectors.models.{SecureMessage, TaxpayerName}
import uk.gov.hmrc.contactadvisors.connectors.{EntityResolverConnector, MessageConnector, TaxpayerNameConnector}
import uk.gov.hmrc.contactadvisors.domain._
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class SecureMessageServiceSpec extends UnitSpec with MockitoSugar with ScalaFutures {

  "createMessageWithTaxpayerName" should {

    import scala.concurrent.ExecutionContext.Implicits.global
    implicit val hc = HeaderCarrier()

    "get taxpayer name details when saUtr is paperless, store the message and return an AdviceStored result" in new TestCase {

      when(entityResolverConnectorMock.validPaperlessUserWith(validSaUtr)).thenReturn(Future.successful(true))
      when(taxpayerNameConnectorMock.taxpayerName(validSaUtr)).thenReturn(Future.successful(Some(validTaxpayerName)))
      when(messageConnectorMock.create(messageCaptor.capture())(any())).thenReturn(Future.successful(AdviceStored("messageId-1234")))

      val storageResult = secureMessageService.createMessageWithTaxpayerName(advice, validSaUtr).futureValue

      storageResult shouldBe AdviceStored("messageId-1234")
      val storedMessage = messageCaptor.getValue
      storedMessage.recipient.name shouldBe validTaxpayerName
      storedMessage.content shouldBe encodedAdviceBody
    }

    "get no taxpayer details when saUtr is paperless, creates a message with empty recipient name store the message and return an AdviceStored result" in new TestCase {

      when(entityResolverConnectorMock.validPaperlessUserWith(validSaUtr)).thenReturn(Future.successful(true))
      when(taxpayerNameConnectorMock.taxpayerName(validSaUtr)).thenReturn(Future.successful(None))
      when(messageConnectorMock.create(messageCaptor.capture())(any())).thenReturn(Future.successful(AdviceStored("messageId-1234")))

      val storageResult = secureMessageService.createMessageWithTaxpayerName(advice, validSaUtr).futureValue

      storageResult shouldBe AdviceStored("messageId-1234")
      val storedMessage = messageCaptor.getValue
      storedMessage.recipient.name shouldBe emptyTaxpayerName
      storedMessage.content shouldBe encodedAdviceBody
    }

    "get taxpayer name details when saUtr is paperless, and handle failure in creating the message" in new TestCase {

      when(entityResolverConnectorMock.validPaperlessUserWith(validSaUtr)).thenReturn(Future.successful(true))
      when(taxpayerNameConnectorMock.taxpayerName(validSaUtr)).thenReturn(Future.successful(Some(validTaxpayerName)))
      when(messageConnectorMock.create(messageCaptor.capture())(any())).thenReturn(Future.successful(AdviceAlreadyExists))

      val storageResult = secureMessageService.createMessageWithTaxpayerName(advice, validSaUtr).futureValue

      storageResult shouldBe AdviceAlreadyExists
    }

    "handle case when taxpayer is not paperless" in new TestCase {

      when(entityResolverConnectorMock.validPaperlessUserWith(validSaUtr)).thenReturn(Future.failed(CustomerIsNotPaperless(s"Customer with tax id ${validSaUtr.value} has not opted in to paperless")))

      val storageResult = secureMessageService.createMessageWithTaxpayerName(advice, validSaUtr).futureValue

      verify(taxpayerNameConnectorMock, never()).taxpayerName(any())(any())
      verify(messageConnectorMock, never()).create(any())(any())
      storageResult shouldBe UserIsNotPaperless
    }

    "handle case when taxpayer is not found" in new TestCase {

      when(entityResolverConnectorMock.validPaperlessUserWith(validSaUtr)).thenReturn(Future.failed(TaxIdNotFound(s"unknown tax id [${validSaUtr.value}] provided")))

      val storageResult = secureMessageService.createMessageWithTaxpayerName(advice, validSaUtr).futureValue

      verify(taxpayerNameConnectorMock, never()).taxpayerName(any())(any())
      verify(messageConnectorMock, never()).create(any())(any())
      storageResult shouldBe UnknownTaxId
    }

    "handle case when paperless check fails" in new TestCase {

      when(entityResolverConnectorMock.validPaperlessUserWith(validSaUtr)).thenReturn(Future.failed(UnexpectedFailure("Could not determine if user with utr is paperless")))

      val storageResult = secureMessageService.createMessageWithTaxpayerName(advice, validSaUtr).futureValue

      verify(taxpayerNameConnectorMock, never()).taxpayerName(any())(any())
      verify(messageConnectorMock, never()).create(any())(any())
      storageResult shouldBe UnexpectedError("Creation of the advice failed. Reason: Could not determine if user with utr is paperless")
    }
  }

  trait TestCase {
    val validTaxpayerName = TaxpayerName(title = Some("Mr"), forename = Some("John"), surname = Some("Smith"))
    val emptyTaxpayerName = TaxpayerName()

    val plainTextAdviceBody = "Advice message body"
    val encodedAdviceBody = new String(Base64.encodeBase64(plainTextAdviceBody.getBytes("UTF-8")))

    val advice = Advice("Advice subject", plainTextAdviceBody)
    val validSaUtr = SaUtr("1234")

    val messageCaptor = ArgumentCaptor.forClass(classOf[SecureMessage])

    val taxpayerNameConnectorMock: TaxpayerNameConnector = mock[TaxpayerNameConnector]
    val messageConnectorMock: MessageConnector = mock[MessageConnector]
    val entityResolverConnectorMock: EntityResolverConnector = mock[EntityResolverConnector]

    val secureMessageService = new SecureMessageService {
      override def messageConnector: MessageConnector = messageConnectorMock

      override def taxpayerNameConnector: TaxpayerNameConnector = taxpayerNameConnectorMock

      override def entityResolverConnector: EntityResolverConnector = entityResolverConnectorMock
    }
  }

}
