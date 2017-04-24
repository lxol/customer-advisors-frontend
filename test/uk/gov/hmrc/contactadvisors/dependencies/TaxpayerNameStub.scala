package uk.gov.hmrc.contactadvisors.dependencies

import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.contactadvisors.connectors.models.TaxpayerName

trait TaxpayerNameStub {

  def taxpayerNameEndpoint(saUtr: String) = s"/self-assessment/individual/$saUtr/designatory-details/taxpayer"

  def givenTaxpayerNameReturnsNoBodyWith(status: Int, saUtr: String) =
    givenThat(
      get(urlEqualTo(taxpayerNameEndpoint(saUtr)))
        .willReturn(aResponse().withStatus(status)
        )
    )

  def taxpayerNameBody(taxpayerNameModel: TaxpayerName): String = {
    s"""
       |{
       |    "name": {
       |        "title": "${taxpayerNameModel.title}",
       |        "forename": "${taxpayerNameModel.forename}",
       |        "secondForename": "${taxpayerNameModel.secondForename}",
       |        "surname": "${taxpayerNameModel.surname}",
       |        "honours": "${taxpayerNameModel.honours}"
       |    }
       |}
     """.stripMargin
  }

  def givenTaxpayerNameRespondsWith(status: Int, saUtr: String, taxpayerName: TaxpayerName): Unit = {
    givenThat(
      get(urlEqualTo(taxpayerNameEndpoint(saUtr)))
        .willReturn(aResponse().withStatus(status).withBody(taxpayerNameBody(taxpayerName))))
  }
}
