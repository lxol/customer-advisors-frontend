package uk.gov.hmrc.utils

import org.apache.commons.codec.binary.Base64
import uk.gov.hmrc.contactadvisors.connectors.models._
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.time.DateTimeUtils

object SecureMessageCreator {
  val utr = SaUtr("123456789")
  val taxpayerName = TaxpayerName(title = Some("Mr"), forename = Some("John"), surname = Some("Smith"))

  def recipient(identifier: SaUtr = utr, name: TaxpayerName = taxpayerName) = {
    Recipient(identifier, name)
  }

  val externalReference = ExternalReference("123412342314", "customer-advisor")
  val messageType = "advisor-reply"
  val validFrom = DateTimeUtils.now.toLocalDate
  val subject = "This is a response to your HMRC request"
  val content = new String(Base64.encodeBase64("<p>This is the content of the secure message</p>".getBytes("UTF-8")))
  val details = Details(formId = "CA001", statutory = false, paperSent = false, batchId = None)
  val message = SecureMessage(recipient(), externalReference, messageType, subject, content, validFrom, details)
}
