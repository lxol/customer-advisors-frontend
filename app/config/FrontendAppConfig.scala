/*
 * Copyright 2018 HM Revenue & Customs
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

package config

import javax.inject.{Inject, Singleton}
import play.api.{Configuration, Environment, Mode}
import uk.gov.hmrc.play.config.ServicesConfig

trait AppConfig {

  val btaHost: String
  val btaBaseUrl: String
  val ptaHost: String
  val ptaBaseUrl: String
  val announcementFrontEndUrl: String
  val showAnnouncements: Boolean

  def getPortalPath(pathKey: String): String
}

@Singleton
class FrontendAppConfig @Inject() (val runModeConfiguration: Configuration, environment: Environment) extends AppConfig with ServicesConfig {

  protected def mode: Mode.Mode = environment.mode

  lazy val btaHost                 = s"${runModeConfiguration.getString(s"govuk-tax.$env.business-account.host").getOrElse("")}"
  lazy val btaBaseUrl              = s"$btaHost/business-account"
  lazy val ptaHost                 = s"${runModeConfiguration.getString(s"govuk-tax.$env.personal-account.host").getOrElse("")}"
  lazy val ptaBaseUrl              = s"$ptaHost/personal-account"
  lazy val showAnnouncements       = runModeConfiguration.getBoolean(s"$env.featureToggle.button.switch").getOrElse(false)
  lazy val announcementFrontEndUrl = envPath("/announcement/sa-filing-notice-2018")(other = baseUrl("announcement-frontend"), prod = "")

  def getPortalPath(pathKey: String): String = runModeConfiguration.getString(s"govuk-tax.$env.portal.destinationPath.$pathKey").getOrElse("")
}
