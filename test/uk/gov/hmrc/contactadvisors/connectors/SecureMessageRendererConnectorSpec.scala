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

package uk.gov.hmrc.contactadvisors.connectors

import org.scalatest.Inside._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mock.MockitoSugar
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import play.api.http.Status
import uk.gov.hmrc.contactadvisors.WSHttp
import uk.gov.hmrc.contactadvisors.dependencies.SecureMessageRenderer
import uk.gov.hmrc.contactadvisors.domain._
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.utils.WithWiremock

class SecureMessageRendererConnectorSpec extends UnitSpec
  with MockitoSugar
  with ScalaFutures
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with WithFakeApplication
  with TableDrivenPropertyChecks
  with IntegrationPatience
  with WithWiremock
  with SecureMessageRenderer {

  "secure message renderer connector" should {
    implicit val hc = HeaderCarrier()

    s"return $AdviceStored saves advice successfully" in new TestCase {
      print(s"AdviceStored: $AdviceStored")
      givenSecureMessageRendererRespondsWith(Status.OK)

      connector.insert(Advice(subject, adviceBody), utr).futureValue shouldBe AdviceStored
    }

    s"return $AdviceAlreadyExists when renderer returns a conflict (409)" in new TestCase {

      givenSecureMessageRendererReturnsDuplicateAdvice()

      connector.insert(Advice(subject, adviceBody), utr).futureValue shouldBe AdviceAlreadyExists
    }

    s"return $UnknownTaxId when renderer returns a not found (404) with a proper body" in
      new TestCase {
      givenSecureMessageRendererCannotFindTheUtr()

      connector.insert(Advice(subject, adviceBody), utr).futureValue shouldBe UnknownTaxId
    }

    s"return $UnexpectedError when renderer returns a not found (404) with an unexpected body" in
      new TestCase {
        givenSecureMessageRendererRespondsWith(Status.NOT_FOUND, "unexpected body")

        inside(connector.insert(Advice(subject, adviceBody), utr).futureValue) {
          case UnexpectedError(msg) =>
            msg should include(Status.NOT_FOUND.toString)
            msg should include("unexpected body")
            msg should include(createAdvicePath)
        }
      }

    s"return $UserIsNotPaperless when renderer returns a precondition failed (412) with a proper body" in
      new TestCase {
      givenSecureMessageRendererFindsThatUserIsNotPaperless()

      connector.insert(Advice(subject, adviceBody), utr).futureValue shouldBe UserIsNotPaperless
    }

    s"return $UnexpectedError when renderer returns a precondition failed (412) with an unexpected body" in
      new TestCase {
        givenSecureMessageRendererRespondsWith(Status.PRECONDITION_FAILED, body = "unexpected body")

        inside(connector.insert(Advice(subject, adviceBody), utr).futureValue) {
          case UnexpectedError(msg) =>
            msg should include(Status.PRECONDITION_FAILED.toString)
            msg should include("unexpected body")
            msg should include(createAdvicePath)
        }
      }

    forAll(Table("statusCode", 202, 400, 401, 404, 415, 500)) { statusCode: Int =>
      s"return $UnexpectedError when response has status $statusCode" in
        new TestCase {
          val errorMsg: String = "There has been an error"
          givenSecureMessageRendererRespondsWith(statusCode, body = errorMsg)

          inside(connector.insert(Advice(subject, adviceBody), utr).futureValue) {
            case UnexpectedError(msg) =>
              msg should include(statusCode.toString)
              msg should include(errorMsg)
              msg should include(createAdvicePath)
          }
        }
    }
  }

  trait TestCase {
    val secureMessageRendererBaseUrl = s"http://localhost:$dependenciesPort"
    val createAdvicePath = "/advice"
    val subject = "This is message subject"
    val adviceBody = "<html>advice body</html>"
    val utr = SaUtr("0329u490uwesakdjf")

    val connector = new SecureMessageRendererConnector {

      override def http: HttpPost = WSHttp

      override def serviceUrl: String = secureMessageRendererBaseUrl
    }
  }

}
