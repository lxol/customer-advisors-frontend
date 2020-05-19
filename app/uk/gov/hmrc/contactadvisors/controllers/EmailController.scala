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

import javax.inject.{Inject, Singleton}
import play.api._
import play.api.libs.json.{JsObject, JsValue}
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.contactadvisors.connectors.EmailConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class EmailController @Inject()(
  controllerComponents: MessagesControllerComponents,
  val authConnector: AuthConnector,
  val emailConnector: EmailConnector,
  val env: Environment)(implicit val appConfig: uk.gov.hmrc.contactadvisors.FrontendAppConfig)
    extends FrontendController(controllerComponents) with AuthorisedFunctions {

  def sendEmail: Action[AnyContent] = Action.async { implicit request =>
    {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers)
      //authorised(Enrolment("roleOne") and AuthProviders(PrivilegedApplication))

      authorised(AuthProviders(PrivilegedApplication)) {
      //.retrieve(Retrievals.credentials) { creds =>
      //   .retrieve(Retrievals.allEnrolments and Retrievals.authorisedEnrolments and Retrievals.name and Retrievals.email and Retrievals.credentials) {
      request.body.asJson.fold(Future.successful(BadRequest("""{"error": "invalid payload"}""")))(
        (json: JsValue) =>
          (json \ "parameters").toOption
            .collect {
              case x: JsObject if x.keys == Set("name") => emailConnector.send(request.body.asJson.get)
            }
            .getOrElse(Future.successful(BadRequest("""{"error": "invalid parameters"}"""))))
    }
  // .recoverWith {
  //   case _: InsufficientEnrolments =>
  //     Future.successful(Unauthorized("""{"error": "insufficient enrolments"}"""))
  // }
  }
  }

}
