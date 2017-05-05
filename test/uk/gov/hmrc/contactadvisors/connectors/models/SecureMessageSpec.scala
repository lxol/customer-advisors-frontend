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

package uk.gov.hmrc.contactadvisors.connectors.models

import org.joda.time.DateTime
import play.api.libs.json.Json
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.test.UnitSpec

class SecureMessageSpec extends UnitSpec {

  "SecureMessage.format" should {

    "convert an AdviceCreationBody into the expected json" in {
      val recipient = Recipient(SaUtr("987654321"))
      val externalReference = ExternalReference("123412342314", "customer-advisor")
      val messageType = "advisor-reply"
      val subject = "This is a response to your HMRC request"
      val content = "This is the content of the secure message"
      val validFrom = DateTime.parse("2017-04-21").toLocalDate
      val details = Details(formId = "CA001", statutory = true, paperSent = false, batchId = None)
      val message = SecureMessage(recipient, externalReference, messageType, subject, content, validFrom, details)

      val expectedJson =
        s"""
           |{
           |  "recipient": {
           |    "taxIdentifier": {
           |      "name": "sautr",
           |      "value": "987654321"
           |    }
           |  },
           |  "externalRef": {
           |    "id": "123412342314",
           |    "source": "customer-advisor"
           |  },
           |  "messageType": "advisor-reply",
           |  "subject": "This is a response to your HMRC request",
           |  "content": "This is the content of the secure message",
           |  "validFrom": "2017-04-21",
           |  "details": {
           |    "formId": "CA001",
           |    "statutory": true,
           |    "paperSent": false
           |  }
           |}
         """.stripMargin

      Json.toJson(message) shouldBe Json.parse(expectedJson)
    }
  }

}