/*
 * Copyright 2016 HM Revenue & Customs
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
import play.api.libs.json.Json
import uk.gov.hmrc.domain.SaUtr

trait SecureMessageRenderer {
  val createAdvicePath = "/advice"
  val subject = "This is message subject"
  val adviceBody = "<html>advice body</html>"
  val utr = SaUtr("0329u490uwesakdjf")

  def givenSecureMessageRendererRespondsSuccessfully(): Unit = {
    givenSecureMessageRendererRespondsWith(Status.OK)
  }

  def givenSecureMessageRendererReturnsDuplicateAdvice(): Unit = {
    givenSecureMessageRendererRespondsWith(Status.CONFLICT)

  }

  def givenSecureMessageRendererCannotFindTheUtr(): Unit = {
    givenSecureMessageRendererRespondsWith(
      status = Status.NOT_FOUND,
      body =
        """
          | {
          |     "reason": "TAX_ID_NOT_RECOGNISED"
          | }
        """.stripMargin
    )
  }

  def givenSecureMessageRendererFindsThatUserIsNotPaperless(): Unit = {
    givenSecureMessageRendererRespondsWith(
      status = Status.PRECONDITION_FAILED,
      body =
        """
          | {
          |     "reason": "USER_NOT_PAPERLESS"
          | }
        """.stripMargin
    )
  }

  def givenSecureMessageRendererRespondsWith(status: Int, body: String = ""): Unit = {
    givenThat(
      post(urlEqualTo(createAdvicePath))
        .withRequestBody(
          equalToJson(
            Json.stringify(
              Json.obj(
                "subject" -> subject,
                "adviceBody" -> adviceBody,
                "taxId" -> Json.obj(
                  "idType" -> "sautr",
                  "id" -> utr.value
                )
              )
            ),
            JSONCompareMode.LENIENT
          )
        )
        .willReturn(aResponse().withStatus(status).withBody(body)))
  }
}
