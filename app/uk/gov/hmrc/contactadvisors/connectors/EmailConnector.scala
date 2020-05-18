/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.contactadvisor.connectors

import scala.concurrent.Future

import javax.inject.{ Inject, Named, Singleton }
import uk.gov.hmrc.http.{ HeaderCarrier, HttpReads, HttpResponse }
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import play.api.libs.json.{ JsValue, Json, Writes }

@Singleton
class EmailConnector @Inject()(
  http: HttpClient,
  @Named("email-base-url") baseUrl: String
) {

  def send(message: JsValue)(implicit headerCarrier: HeaderCarrier): Future[Unit] =
    http.POST[JsValue, HttpResponse](s"$baseUrl/hmrc/email", message).map(_ => ())

  implicit def responseHandler: HttpReads[HttpResponse] =
    new HttpReads[HttpResponse] {
      def read(method: String, url: String, response: HttpResponse): HttpResponse =
        response.status match {
          case x if x >= 400 => throw new RuntimeException(response.body)
          case _             => response
        }
    }
}
