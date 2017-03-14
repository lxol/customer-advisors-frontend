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

package uk.gov.hmrc.contactadvisors

import com.typesafe.config.Config
import controllers.routes
import net.ceedubs.ficus.Ficus._
import play.api.mvc.{Request, RequestHeader, Result}
import play.api._
import play.twirl.api.Html
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.play.audit.filters.FrontendAuditFilter
import uk.gov.hmrc.play.config.{AppName, ControllerConfig, RunMode}
import uk.gov.hmrc.play.filters.MicroserviceFilterSupport
import uk.gov.hmrc.play.frontend.bootstrap.DefaultFrontendGlobal
import uk.gov.hmrc.play.http.logging.filters.FrontendLoggingFilter
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import play.api.mvc.Results.{Redirect, SeeOther}
import uk.gov.hmrc.auth.core.{InsufficientEnrolments, NoActiveSession}


object FrontendGlobal
  extends DefaultFrontendGlobal {

  override val auditConnector = FrontendAuditConnector
  override val loggingFilter = LoggingFilter
  override val frontendAuditFilter = AuditFilter

  override def onStart(app: Application) {
    super.onStart(app)
    ApplicationCrypto.verifyConfiguration()
  }

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit rh: Request[_]): Html =
    uk.gov.hmrc.contactadvisors.views.html.error_template(pageTitle, heading, message)

  override def microserviceMetricsConfig(implicit app: Application): Option[Configuration] = app.configuration.getConfig(s"microservice.metrics")

  override def resolveError(rh: RequestHeader, ex: Throwable): Result = ex match {
    //TODO add failed authentication redirect
    case _: NoActiveSession => toStrideLogin(s"${rh.uri}" )
    case ex : InsufficientEnrolments => SeeOther(routes.StrideLoginController.withoutRole().url)
    case _ => super.resolveError(rh, ex)
  }

  private def host(service: String): String = "localhost:9000"

  def origin: String = "localhost"

  def strideLoginUrl: String = host("stride-auth-frontend") + "/stride/sign-in"

  def toStrideLogin(successUrl: String, failureUrl: Option[String] = None): Result = Redirect(strideLoginUrl, Map(
    "successURL" -> Seq(successUrl),
    "origin" -> Seq(origin)
  ) ++ failureUrl.map(f => Map("failureURL" -> Seq(f))).getOrElse(Map()))
}

object ControllerConfiguration extends ControllerConfig {
  lazy val controllerConfigs = Play.current.configuration.underlying.as[Config]("controllers")
}

object LoggingFilter extends FrontendLoggingFilter with MicroserviceFilterSupport {
  override def controllerNeedsLogging(controllerName: String) = ControllerConfiguration.paramsForController(controllerName).needsLogging
}

object AuditFilter extends FrontendAuditFilter with MicroserviceFilterSupport with RunMode with AppName {

  override lazy val maskedFormFields = Seq("password")

  override lazy val applicationPort = None

  override lazy val auditConnector = FrontendAuditConnector

  override def controllerNeedsAuditing(controllerName: String) = ControllerConfiguration.paramsForController(controllerName).needsAuditing
}
