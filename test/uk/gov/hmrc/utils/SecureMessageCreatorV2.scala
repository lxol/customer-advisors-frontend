/*
 * Copyright 2020 HM Revenue & Customs
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

import uk.gov.hmrc.contactadvisors.domain.AdviceV2

object SecureMessageCreatorV2 {
  val subject = "This is a response to your HMRC request"
  val uncleanContent = "<p>This is the content of the secure message</p><script>alert('more hax')</script>"
  val cleanContent = "his is the content of the secure message"
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

  val adviceWithCleanContent =
    AdviceV2(
      subject,
      cleanContent,
      recipientTaxidentifierName,
      recipientTaxidentifierValue,
      recipientEmail,
      recipientNameLine1,
      messageType)

}
