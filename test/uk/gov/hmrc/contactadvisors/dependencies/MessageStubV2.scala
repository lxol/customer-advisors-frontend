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

package uk.gov.hmrc.contactadvisors.dependencies

import com.github.tomakehurst.wiremock.client.WireMock._
import org.skyscreamer.jsonassert.JSONCompareMode
import play.api.http.Status
import uk.gov.hmrc.contactadvisors.connectors.models.SecureMessageV2
// import uk.gov.hmrc.utils.SecureMessageCreatorV2._
import org.apache.commons.codec.binary.Base64
import play.api.Logger

trait MessageStubV2 {
  val messageEndpoint = "/messages"

  val duplicatedMessage =
    (Status.CONFLICT,
    s"""
       |{
       |  "reason": "Duplicated message content or external reference ID"
       |}
     """.stripMargin)

  // val unknownTaxId =
  //   (Status.BAD_REQUEST,
  //   s"""
  //      |{
  //      |  "reason": "Unknown tax identifier name XYZ"
  //      |}
  //    """.stripMargin)

  val successfulResponse: (Int, String) =
    (Status.CREATED,
      s"""
         |{
         |  "id" : "507f1f77bcf86cd799439011"
         |}
     """.stripMargin)

  def givenMessageRespondsWith(request: SecureMessageV2, response: (Int, String)): Unit = {
    givenThat(
      post(urlEqualTo(messageEndpoint))
        .withRequestBody(
          // matching(".*")
  // val uncleanContent = new String(Base64.encodeBase64("<p>This is the content of the secure message</p><script>alert('more hax')</script>".getBytes("UTF-8")))
          equalToJson(
            {
            val foo = s"""
               |{"recipient":
               | {"taxIdentifier":
               |  {"value":"XZFH00000100024",
               |   "name":"HMRC-OBTDS-ORG"
               |  },
               |  "name":
               |  {"line1":"Mr. John Smith"},
               |  "email":"foo@bar.com"},
               | "externalRef":
               | {"id":"01012625-f783-4b63-a022-e01af0658687",
               |  "source":"customer-advisor"
               | },
               | "messageType":"fhddsAlertMessage",
               | "subject":"This is a response to your HMRC request<script>alert('hax')</script>",
               | "content":"UEhBK1ZHaHBjeUJwY3lCMGFHVWdZMjl1ZEdWdWRDQnZaaUIwYUdVZ2MyVmpkWEpsSUcxbGMzTmhaMlU4TDNBK1BITmpjbWx3ZEQ1aGJHVnlkQ2duYlc5eVpTQm9ZWGduS1R3dmMyTnlhWEIwUGc9PQ==",
               | "validFrom":"2018-07-09",
               | "alertQueue":"PRIORITY"
               |}
         """.stripMargin
              Logger.info(s"****** request to compareTo: ${foo}")
              foo
              },
            JSONCompareMode.LENIENT
          )
        )
        .willReturn(aResponse().withStatus(response._1).withBody(response._2)))
  }
}

               // |{
               // |  "recipient": {
               // |    "taxIdentifier": {
               // |      "value": "${request.recipient.taxIdentifier.value}",
               // |      "name": "${request.recipient.taxIdentifier.name}"
               // |    },
               // |    "name": {
               // |      "line1": "${request.recipient.name.line1}"
               // |    },
               // |    "email":"${request.recipient.email}"
               // |  },
               // |  "externalRef": {
               // |    "id": "${request.externalRef.id}",
               // |    "source": "${request.externalRef.source}"
               // |  },
               // |  "messageType": "${request.messageType}",
               // |  "subject": "${request.subject}",
               // |  "content": "${new String(Base64.encodeBase64(request.content.getBytes("UTF-8")))}",
               // |  "validFrom": "${request.validFrom}",
               // |  "alertQueue":"${request.alertQueue}"
               // |}
