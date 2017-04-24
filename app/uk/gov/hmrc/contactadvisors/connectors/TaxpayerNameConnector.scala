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

package uk.gov.hmrc.contactadvisors.connectors

import play.api.Logger
import play.api.libs.json._
import uk.gov.hmrc.contactadvisors.WSHttp
import uk.gov.hmrc.contactadvisors.connectors.models.TaxpayerName
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

object TaxpayerNameConnector extends TaxpayerNameConnector with ServicesConfig {
  lazy val http = WSHttp
  override lazy val baseUrl: String = baseUrl("taxpayer-data")
}


trait TaxpayerNameConnector {

  def http: HttpGet

  def baseUrl: String

  def url(path: String) = s"$baseUrl$path"

  def taxpayerName(utr: SaUtr)(implicit hc: HeaderCarrier): Future[Option[TaxpayerName]] = {

    implicit val nameFormat = NameFromHods.format


    http.GET[NameFromHods](url(s"/self-assessment/individual/$utr/designatory-details/taxpayer"))
      .map(_.name)
      .recover {
      case notFound: NotFoundException =>
        Logger.warn(s"No taxpayer name found for utr: $utr", notFound)
        None
      case e =>
        Logger.error(s"Unable to get taxpayer name for $utr", e)
        None

    }
  }
}


case class NameFromHods(name: Option[TaxpayerName])

object NameFromHods {

  implicit val taxpayerNameFormat = TaxpayerName.formats
  implicit val format = Json.format[NameFromHods]
}
