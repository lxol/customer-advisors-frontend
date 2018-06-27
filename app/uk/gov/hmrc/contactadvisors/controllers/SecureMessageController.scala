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

package uk.gov.hmrc.contactadvisors.controllers

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import uk.gov.hmrc.contactadvisors.domain._
import uk.gov.hmrc.contactadvisors.service.SecureMessageService
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.EventKeys
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.{DataEvent, EventTypes}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class SecureMessageController @Inject()(customerAdviceAudit: CustomerAdviceAudit, secureMessageService: SecureMessageService, val messagesApi: MessagesApi)(implicit val appConfig: uk.gov.hmrc.contactadvisors.FrontendAppConfig) extends FrontendController with I18nSupport {

  def inbox = Action.async { implicit request =>
    Future.successful(
      Ok(
        uk.gov.hmrc.contactadvisors.views.html.secureMessage.inbox(
          adviceForm.fill(Advice("", ""))
        )
      )
    )
  }

  def submit() = Action.async { implicit request =>
    adviceForm.bindFromRequest.fold(
      formWithErrors => Future.successful(
        BadRequest(uk.gov.hmrc.contactadvisors.views.html.secureMessage.inbox(formWithErrors))
      ),
      advice => {
        val result = secureMessageService.createMessage(advice, SaUtr("foo utr"))
        customerAdviceAudit.auditAdvice(result, SaUtr("foo utr"))
        result.map {
          handleStorageResult("foo utr")
        }
      }
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
    case AdviceStored(_) => Redirect(routes.SecureMessageController.success(utr))
    case AdviceAlreadyExists => Redirect(routes.SecureMessageController.duplicate(utr))
    case UnknownTaxId => Redirect(routes.SecureMessageController.unknown(utr))
    case UserIsNotPaperless => Redirect(routes.SecureMessageController.notPaperless(utr))
    case UnexpectedError(msg) => Redirect(routes.SecureMessageController.unexpected(utr))
  }
}

@Singleton
class CustomerAdviceAudit @Inject()(auditConnector: AuditConnector) {

  def auditSource: String = "customer-advisors-frontend"

  def auditAdvice(result: Future[StorageResult], taxId: SaUtr)(implicit hc: HeaderCarrier): Unit = {
    def createEvent(messageInfo: Map[String, String], auditType: String, transactionName: String) =
      DataEvent(
        auditSource = auditSource,
        auditType = auditType,
        tags = Map(EventKeys.TransactionName -> transactionName),
        detail = Map(
          taxId.name -> taxId.value
        ) ++ messageInfo
      )

    result.onComplete { res1 =>
      res1.map {
        case AdviceStored(messageId) => createEvent(Map("secureMessageId" -> messageId, "messageId" -> messageId), EventTypes.Succeeded, "Message Stored")
        case AdviceAlreadyExists => createEvent(Map("reason" -> "Duplicate Message Found"), EventTypes.Failed, "Message Not Stored")
        case UnknownTaxId => createEvent(Map("reason" -> "Unknown Tax Id"), EventTypes.Failed, "Message Not Stored")
        case UserIsNotPaperless => createEvent(Map("reason" -> "User is not paperless"), EventTypes.Failed, "Message Not Stored")
        case UnexpectedError(errorMessage) => createEvent(Map("reason" -> s"Unexpected Error: ${errorMessage}"), EventTypes.Failed, "Message Not Stored")
      }.
        recover { case ex =>
          createEvent(Map("reason" -> s"Unexpected Error: ${ex.getMessage}"), EventTypes.Failed, "Message Not Stored")
        }.
        foreach { ev =>
          auditConnector.sendEvent(ev).onFailure {
            case err => Logger.error("Could not audit event", err)
          }
        }
    }
  }
}

