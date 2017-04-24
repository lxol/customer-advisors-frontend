package uk.gov.hmrc.utils

import org.joda.time.DateTime
import uk.gov.hmrc.contactadvisors.connectors.models._
import uk.gov.hmrc.domain.SaUtr

object SecureMessageCreator {
  val recipient = Recipient(SaUtr("987654321"), TaxpayerName(title = Some("Mr"), forename = Some("John"), surname = Some("Smith")))
  val externalReference = ExternalReference("123412342314", "customer-advisor")
  val messageType = "advisor-reply"
  val subject = "This is a response to your HMRC request"
  val content = "This is the content of the secure message"
  val validFrom = DateTime.parse("2017-04-21").toLocalDate
  val details = Details(formId = "CA001", statutory = true, paperSent = false, batchId = None)
  val message = SecureMessage(recipient, externalReference, messageType, subject, content, validFrom, details)
}
