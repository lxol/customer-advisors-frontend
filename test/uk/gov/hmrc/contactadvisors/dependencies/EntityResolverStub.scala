/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.http.Status

trait EntityResolverStub {

  def entityResolverEndpoint(saUtr: String) = s"/portal/preferences/sa/$saUtr"

  def givenEntityResolverReturnsNotFound(utr: String): Unit = {
    givenEntityResolverRespondsWith(
      saUtr = utr,
      status = Status.NOT_FOUND,
      body = ""
    )
  }

  def givenEntityResolverReturnsANonPaperlessUser(utr: String): Unit = {
    givenEntityResolverRespondsWith(
      saUtr = utr,
      status = Status.OK,
      body =
        """
          |{
          |  "digital":false
          |}
        """.stripMargin
    )
  }

  def givenEntityResolverReturnsAPaperlessUser(utr: String): Unit = {
    givenEntityResolverRespondsWith(
      saUtr = utr,
      status = Status.OK,
      body =
        """
          |{
          |  "digital":true,
          |  "email":{
          |    "email":"bbc14eef-97d3-435e-975a-f2ab069af000@TEST.com",
          |    "status":"pending",
          |    "mailboxFull":false,
          |    "linkSent":"2015-04-29"
          |  }
          |}
        """.stripMargin
    )
  }

  def givenEntityResolverRespondsWith(saUtr: String, status: Int, body: String = ""): Unit = {
    givenThat(
      get(urlEqualTo(entityResolverEndpoint(saUtr)))
        .willReturn(aResponse().withStatus(status).withBody(body)))
  }
}
