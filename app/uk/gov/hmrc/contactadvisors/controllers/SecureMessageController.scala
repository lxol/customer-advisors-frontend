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

import java.util.UUID

import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import uk.gov.hmrc.contactadvisors.domain.Advice
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

trait SecureMessageController extends FrontendController {
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
      advice => Future.successful(
        Redirect(routes.SecureMessageController.sent(utr, UUID.randomUUID().toString))
      )
    )
  }

  def sent(utr: String, messageId: String) = Action.async { implicit request =>
    Future.successful(
      Ok(uk.gov.hmrc.contactadvisors.views.html.secureMessage.sent(utr))
    )
  }

  def adviceForm = Form[Advice](
    mapping(
      "subject" -> nonEmptyText,
      "message" -> nonEmptyText
    )(Advice.apply)(Advice.unapply)
  )
}

object SecureMessageController extends SecureMessageController


