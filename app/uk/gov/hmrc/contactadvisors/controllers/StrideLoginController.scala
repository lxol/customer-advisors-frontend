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

package uk.gov.hmrc.contactadvisors.controllers

import play.api.libs.json.{JsValue, Json, Reads}
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.contactadvisors.WSHttp
import uk.gov.hmrc.contactadvisors.connectors.UserDetailsConnector
import uk.gov.hmrc.contactadvisors.connectors.models.UserDetails
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.http.HttpPost
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

import scala.concurrent.Future

trait StrideLoginController extends FrontendController with AuthorisedFunctions {

  val advisor = Enrolment("authorisedHelper")
  val userDetailsUri: Retrieval[Option[String]] = OptionalRetrieval("userDetailsUri", Reads.StringReads)

  val userDetailsConnector : UserDetailsConnector

  private type HelpdeskAction = Request[AnyContent] => UserDetails => Future[Result]

  object AuthoriseHelpdeskAction {

    def asyncWithRoles(roles: Predicate)(action: HelpdeskAction): Action[AnyContent] = {
      Action.async { implicit request =>
        authorised(roles and AuthProviders(PrivilegedApplication)).retrieve(userDetailsUri) { userDetailsUri =>
                  userDetailsConnector.getDetails(userDetailsUri).flatMap { userDetails =>
                    action(request)(userDetails)
          }
        }
      }
    }

    def async(action: HelpdeskAction): Action[AnyContent] = {
      Action.async { implicit request =>
        authorised(AuthProviders(PrivilegedApplication)).retrieve(userDetailsUri) { userDetailsUri =>
          userDetailsConnector.getDetails(userDetailsUri).flatMap { userDetails =>
            action(request)(userDetails)
          }
        }
      }
    }
  }


  def withRole(): Action[AnyContent] = AuthoriseHelpdeskAction.asyncWithRoles(advisor) {
    implicit request =>
      implicit userDetails =>
        Future.successful(Ok(uk.gov.hmrc.contactadvisors.views.html.error_template("Success", "", "")))
  }

  def withoutRole(): Action[AnyContent] = AuthoriseHelpdeskAction.async {
    implicit request => implicit userDetails =>
      Future.successful(Ok(uk.gov.hmrc.contactadvisors.views.html.error_template("Without Auth", "", "")))
  }

}


object StrideLoginController extends StrideLoginController {
  override val userDetailsConnector: UserDetailsConnector = UserDetailsConnector

  override def authConnector: AuthConnector = new PlayAuthConnector {
    override val serviceUrl: String = baseUrl("auth")

    override def http: HttpPost = WSHttp
  }

}

case class AuthProviders(providers: AuthProvider*) extends Predicate {
  def toJson: JsValue = Json.obj("authProviders" -> providers.map(_.getClass.getSimpleName.dropRight(1)))
}

