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
