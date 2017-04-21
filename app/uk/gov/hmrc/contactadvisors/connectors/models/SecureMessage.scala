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

import org.joda.time.LocalDate
import play.api.libs.functional.syntax._
import play.api.libs.json.{Json, _}
import uk.gov.hmrc.domain.SaUtr

final case class Details(formId: String, statutory: Boolean, paperSent: Boolean, batchId: String)
object Details {
  implicit val formats = Json.format[Details]
}

final case class ExternalReference(id: String, source: String)
object ExternalReference {
  implicit val formats = Json.format[ExternalReference]
}

final case class TaxpayerName(title: Option[String] = None, forename: Option[String] = None, secondForename: Option[String] = None, surname: Option[String] = None, honours: Option[String] = None)
object TaxpayerName {
  implicit val formats = Json.format[TaxpayerName]
}

final case class Recipient(taxIdentifier: SaUtr, name: TaxpayerName)
object Recipient {

  implicit val taxIdWrites: Format[SaUtr] = (
    (__ \ "name").format[String] and
      (__ \ "value").format[String]
    ) ( (_, value) => SaUtr(value) , (m => (m.name, m.value)))

  implicit val formats = Json.format[Recipient]
}

case class SecureMessage(recipient: Recipient, externalRef: ExternalReference, messageType: String, subject: String, content: String, validFrom: LocalDate, details: Details)
object SecureMessage {
  implicit val formats = Json.format[SecureMessage]
}
