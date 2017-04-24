package uk.gov.hmrc.contactadvisors.dependencies

import com.github.tomakehurst.wiremock.client.WireMock._
import org.skyscreamer.jsonassert.JSONCompareMode
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.contactadvisors.connectors.models.SecureMessage

trait MessageStub {
  val messageEndpoint = "/messages"

  val duplicatedMessage =
    (Status.CONFLICT,
    s"""
       |{
       |  "reason": "Duplicated message content or external reference ID"
       |}
     """.stripMargin)

  val unknownTaxId =
    (Status.BAD_REQUEST,
    s"""
       |{
       |  "reason": "Unknown tax identifier name XYZ"
       |}
     """.stripMargin)

  val successfulResponse: (Int, String) =
    (Status.CREATED,
      s"""
         |{
         |  "id" : "507f1f77bcf86cd799439011"
         |}
     """.stripMargin)

  def givenMessageRespondsWith(request: SecureMessage, response: (Int, String)): Unit = {
    givenThat(
      post(urlEqualTo(messageEndpoint))
        .withRequestBody(
          equalToJson(
            s"""
               |{
               |  "recipient": {
               |    "taxIdentifier": {
               |      "name": "sautr",
               |      "value": "${request.recipient.taxIdentifier.value}"
               |    },
               |    "name": {
               |      "title": "${request.recipient.name.title}",
               |      "forename": "${request.recipient.name.forename}",
               |      "surname": "${request.recipient.name.surname}"
               |    }
               |  },
               |  "externalRef": {
               |    "source": "customer-advisor"
               |  },
               |  "messageType": "advisor-reply",
               |  "subject": "${request.subject}",
               |  "content": "${request.content}",
               |  "validFrom": "${request.validFrom}",
               |  "details": {
               |    "formId": "${request.details.formId}",
               |    "statutory": ${request.details.statutory},
               |    "paperSent": ${request.details.paperSent}
               |  }
               |}
         """.stripMargin,
            JSONCompareMode.LENIENT
          )
        )
        .willReturn(aResponse().withStatus(response._1).withBody(response._2)))
  }
}