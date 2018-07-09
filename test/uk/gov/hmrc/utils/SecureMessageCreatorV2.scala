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

package uk.gov.hmrc.utils

import org.apache.commons.codec.binary.Base64
import uk.gov.hmrc.contactadvisors.connectors.models._
import uk.gov.hmrc.contactadvisors.domain.AdviceV2
import uk.gov.hmrc.time.DateTimeUtils

object SecureMessageCreatorV2 {
  val subject = "This is a response to your HMRC request"
  val uncleanContent = "<p>This is the content of the secure message</p><script>alert('more hax')</script>"
  val recipientTaxidentifierName = "HMRC-OBTDS-ORG"
  val recipientTaxidentifierValue = "XZFH00000100024"
  val recipientEmail = "another@somewhere.co.uk"
  val recipientNameLine1 = "A. N. Other"
  val messageType = "fhddsAlertMessage"
  val adviceWithUncleanContent =
    AdviceV2(
      subject,
      uncleanContent,
      recipientTaxidentifierName,
      recipientTaxidentifierValue,
      recipientEmail,
      recipientNameLine1,
      messageType)

  // val externalReference = ExternalReferenceV2("123412342314", "sees")
  // val messageType = "fhddsAlertMessage"

  // val validFrom = DateTimeUtils.now.toLocalDate
  // val uncleanSubject = "This is a response to your HMRC request<script>alert('hax')</script>"
  // // val content = new String(Base64.encodeBase64("<p>This is the content of the secure message</p>".getBytes("UTF-8")))
  // val content = "<p>This is the content of the secure message</p>"
  // // val uncleanContent = "<p>This is the content of the secure message</p><script>alert('more hax')</script>"
  // val recipientNameLine1 = "Mr. John Smith"
  // val taxpayerName = TaxpayerName(recipientNameLine1)
  // val recipientEmail = "foo@bar.com"
  // val recipientTaxidentifierName = "HMRC-OBTDS-ORG"
  // val recipientTaxidentifierValue = "XZFH00000100024"
  // val taxIdentifier = FHDDSTaxIdentifier(recipientTaxidentifierValue, recipientTaxidentifierName)
  // val recipient = RecipientV2(taxIdentifier, taxpayerName, recipientEmail)

  // val message = SecureMessageV2(recipient, externalReference, messageType, subject, content, validFrom)
  // val uncleanMessage = SecureMessageV2(recipient, externalReference, messageType, uncleanSubject, uncleanContent, validFrom)

}
