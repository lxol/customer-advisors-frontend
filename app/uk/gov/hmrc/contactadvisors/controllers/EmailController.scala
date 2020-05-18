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

package uk.gov.hmrc.contactadvisors.controllers

import java.util.UUID

import play.api.libs.json.{ JsObject, Json }
import javax.inject.{ Inject, Singleton }
import play.api.Logger
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.mvc._
import uk.gov.hmrc.contactadvisors.connectors.models.ExternalReferenceV2
import uk.gov.hmrc.contactadvisors.domain._
import uk.gov.hmrc.contactadvisors.service.SecureMessageService
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.EventKeys
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.{ DataEvent, EventTypes }
import uk.gov.hmrc.play.bootstrap.controller.{ FrontendController }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Future }

import javax.inject.{ Inject, Singleton }
import play.api._
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.{ Credentials, Name, ~ }
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import play.api.libs.json.{ JsValue, Json, Writes }

import scala.concurrent.Future

@Singleton
class EmailController @Inject()(
  controllerComponents: MessagesControllerComponents,
  customerAdviceAudit: CustomerAdviceAudit,
  secureMessageService: SecureMessageService,
  val authConnector: AuthConnector,
  val env: Environment,
  messagesApi: MessagesApi)(implicit val appConfig: uk.gov.hmrc.contactadvisors.FrontendAppConfig)
    extends FrontendController(controllerComponents) with I18nSupport with AuthorisedFunctions {

  def sendEmail: Action[AnyContent] = Action.async { implicit request =>
    {
      authorised(Enrolment("roleOne") and AuthProviders(PrivilegedApplication))
        .retrieve(Retrievals.allEnrolments and Retrievals.authorisedEnrolments and Retrievals.name and Retrievals.email and Retrievals.credentials) {
          case allEnrolments ~ authorisedEnrolments ~ name ~ email ~ credentials =>
            //request.body
            ((request.body.asJson.get \ "parameters").toOption.collect { case x: JsObject if x.keys == Set("name") => true
              // connector send email
              }).getOrElse(BadRequest("invalid paramters"))
            //buildPage(name, email, credentials, allEnrolments, authorisedEnrolments, "roleOne")
            ???
        }
        .recoverWith {
          case _: NoActiveSession =>
            Future.successful(
              Ok
              // toStrideLogin(
              //   if (env.mode.equals(Mode.Dev)) {
              //     s"http://${request.host}${request.uri}"
              //   }
              //   else {
              //     s"${request.uri}"
              //   })
            )
          case _: InsufficientEnrolments =>
            Future.successful(SeeOther(???))
        }
      ???
    }
  }

}
