/*
 * Copyright 2017 HM Revenue & Customs
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
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.time.DateTimeUtils

object SecureMessageCreator {
  val utr = SaUtr("123456789")
  val externalReference = ExternalReference("123412342314", "customer-advisor")
  val messageType = "advisor-reply"
  val validFrom = DateTimeUtils.now.toLocalDate
  val subject = "This is a response to your HMRC request"
  val uncleanSubject = "This is a response to your HMRC request<script>alert('hax')</script>"
  val content = new String(Base64.encodeBase64("<p>This is the content of the secure message</p>".getBytes("UTF-8")))
  val uncleanContent = new String(Base64.encodeBase64("<p>This is the content of the secure message</p><script>alert('more hax')</script>".getBytes("UTF-8")))
  val details = Details(formId = "CA001", statutory = false, paperSent = false, batchId = None)
  val message = SecureMessage(Recipient(utr), externalReference, messageType, subject, content, validFrom, details)
  val uncleanMessage = SecureMessage(Recipient(utr), externalReference, messageType, uncleanSubject, uncleanContent, validFrom, details)
}
