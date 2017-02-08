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

import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.contactadvisors.domain.Advice
import uk.gov.hmrc.domain.SaUtr

final case class TaxIdBody(idType: String, id: String)
object TaxIdBody {
  implicit val formats: Format[TaxIdBody] = Json.format[TaxIdBody]
}

final case class AdviceCreationBody(subject: String, adviceBody: String, taxId: TaxIdBody)
object AdviceCreationBody {
  def from(advice: Advice, utr: SaUtr): AdviceCreationBody = {
    AdviceCreationBody(
      subject = advice.subject,
      adviceBody = advice.message,
      taxId = TaxIdBody(utr.name, utr.value)
    )
  }

  implicit val formats = Json.format[AdviceCreationBody]
}
