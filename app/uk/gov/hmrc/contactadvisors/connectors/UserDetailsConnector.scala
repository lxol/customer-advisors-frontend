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
import uk.gov.hmrc.contactadvisors.WSHttp
import uk.gov.hmrc.contactadvisors.connectors.models.UserDetails
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait UserDetailsConnector {

  def http: HttpGet

  def getDetails(location: Option[String])(implicit hc: HeaderCarrier): Future[UserDetails]

}

object UserDetailsConnector extends UserDetailsConnector with ServicesConfig {

  override def http: HttpGet = WSHttp

  lazy val serviceUrl = s"${baseUrl("user-details")}"

  def getDetails(location: Option[String])(implicit hc: HeaderCarrier): Future[UserDetails] = {
    if(location.isDefined) {
      http.GET[UserDetails](s"$serviceUrl${location.get}").map {
        details => details
      }.recover {
        case e =>
          Logger.error(s"Silent Error: ${e.getMessage}")
          UserDetails()
      }
    }
    else {
      Logger.error("Silent Error: No user details link found in session record")
      Future.successful(UserDetails())
    }
  }

}
