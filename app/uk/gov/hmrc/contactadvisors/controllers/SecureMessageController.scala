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
import play.api.data.format.Formats._
import play.api.mvc._
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
      formWithErrors => ???,
      advice => Future.successful(
        Redirect(routes.SecureMessageController.inbox("Success_sending_message").url)
      )
    )
  }

  def adviceForm = Form[Advice](
    mapping(
      "subject" -> of[String],
      "message" -> of[String]
    )(Advice.apply)(Advice.unapply)
  )
}

object SecureMessageController extends SecureMessageController

final case class Advice(subject: String, message: String)