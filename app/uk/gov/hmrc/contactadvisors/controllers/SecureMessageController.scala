/*
 * Copyright 2016 HM Revenue & Customs
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

import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import uk.gov.hmrc.contactadvisors.connectors.SecureMessageRendererConnector
import uk.gov.hmrc.contactadvisors.domain._
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

trait SecureMessageController extends FrontendController {

  def secureMessageRenderer: AdviceRepository

  def inbox(utr: String) = Action.async { implicit request =>
    Future.successful(
      Ok(uk.gov.hmrc.contactadvisors.views.html.secureMessage.inbox(utr, adviceForm))
    )
  }

  def submit(utr: String) = Action.async { implicit request =>
    adviceForm.bindFromRequest.fold(
      formWithErrors => Future.successful(
        BadRequest(uk.gov.hmrc.contactadvisors.views.html.secureMessage.inbox(utr, formWithErrors))
      ),
      advice =>
        secureMessageRenderer.insert(advice, SaUtr(utr)).map { handleStorageResult(utr) }
    )
  }

  def success(utr: String) = Action.async { implicit request =>
    Future.successful(
      Ok(uk.gov.hmrc.contactadvisors.views.html.secureMessage.success(utr))
    )
  }

  def duplicate(utr: String) = Action.async { implicit request =>
    Future.successful(
      Ok(uk.gov.hmrc.contactadvisors.views.html.secureMessage.duplicate(utr))
    )
  }

  def unexpected(utr: String) = Action.async { implicit request =>
    Future.successful(
      Ok(uk.gov.hmrc.contactadvisors.views.html.secureMessage.unexpected(utr))
    )
  }

  def unknown(utr: String) = Action.async { implicit request =>
    Future.successful(
      Ok(uk.gov.hmrc.contactadvisors.views.html.secureMessage.unknown(utr))
    )
  }

  def notPaperless(utr: String) = Action.async { implicit request =>
    Future.successful(
      Ok(uk.gov.hmrc.contactadvisors.views.html.secureMessage.not_paperless(utr))
    )
  }

  def adviceForm = Form[Advice](
    mapping(
      "subject" -> nonEmptyText,
      "message" -> nonEmptyText
    )(Advice.apply)(Advice.unapply)
  )

  private def handleStorageResult(utr: String): StorageResult => Result = {
    case AdviceStored => Redirect(routes.SecureMessageController.success(utr))
    case AdviceAlreadyExists => Redirect(routes.SecureMessageController.duplicate(utr))
    case UnknownTaxId => Redirect(routes.SecureMessageController.unknown(utr))
    case UserIsNotPaperless => Redirect(routes.SecureMessageController.notPaperless(utr))
    case UnexpectedError(msg) => Redirect(routes.SecureMessageController.unexpected(utr))
  }
}

object SecureMessageController extends SecureMessageController {
  lazy val secureMessageRenderer: SecureMessageRendererConnector = SecureMessageRendererConnector
}
