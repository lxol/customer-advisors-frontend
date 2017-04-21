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

import play.api.libs.json.Json
import uk.gov.hmrc.contactadvisors.domain.Advice
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.test.UnitSpec

class AdviceCreationBodySpec extends UnitSpec {

  "AdviceCreationBody.from" should {

    "create an AdviceCreationBody from a the three parameters subject, adviceBody, taxId" in {
      val advice = Advice("This is a response to your HMRC request", "This is the response body")
      val saUtr = SaUtr("987654321")

      val adviceCreationBody = AdviceCreationBody.from(advice, saUtr)

      adviceCreationBody.subject shouldBe "This is a response to your HMRC request"
      adviceCreationBody.taxId shouldBe TaxIdBody("sautr", "987654321")
    }

  }

  "AdviceCreationBody.format" should {

    "convert an AdviceCreationBody into the expected json" in {
      val advice = Advice("This is a response to your HMRC request", "This is the response body")
      val saUtr = SaUtr("987654321")

      val expectedJson =
        s"""
           |{
           |  "subject":"This is a response to your HMRC request",
           |  "adviceBody":"This is the response body",
           |  "taxId":{
           |    "idType":"sautr",
           |    "id":"987654321"
           |  }
           |}
         """.stripMargin

      val adviceCreationBody = AdviceCreationBody.from(advice, saUtr)

      Json.toJson(adviceCreationBody) shouldBe Json.parse(expectedJson)
    }
  }
}


//{
//  "recipient": {
//    "taxIdentifier": {
//      "name": "sautr",
//      "value": "1234567890"
//    },
//    "name": {
//      "title": "Mr",
//      "forename": "William",
//      "secondForename": "Harry",
//      "surname": "Smith",
//      "honours": "OBE"
//    }
//  },
//  "externalRef": {
//    "id": "123412342314",
//    "source": "gmc"
//  },
//  "messageType": "mailout-batch",
//  "subject": "Reminder to file a Self Assessment return",
//  "content": "Some base64-encoded HTML",
//  "validFrom": "2017-02-14",
//  "details": {
//    "formId": "SA300",
//    "statutory": true,
//    "paperSent": false,
//    "batchId": "1234567"
//  }
//}
