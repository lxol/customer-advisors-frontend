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

package uk.gov.hmrc.contactadvisors.connectors

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.{Level, Logger => LogbackLogger}
import ch.qos.logback.core.read.ListAppender
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.slf4j.LoggerFactory
import play.api.libs.json._
import uk.gov.hmrc.contactadvisors.connectors.models.TaxpayerName
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.http.hooks.HttpHook
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpResponse}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class TaxpayerNameConnectorSpec extends UnitSpec with ScalaFutures with LogCapturing with Eventually {

  val fullTaxpayerName = TaxpayerName(title = Some("Mr"),
    forename = Some("Erbert"),
    secondForename = Some("Donaldson"),
    surname = Some("Ducking"),
    honours = Some("KCBE"))

  val utr = SaUtr("12345678990")

  "Parsing from JSON to a" should {


    implicit val headerCarrier: HeaderCarrier = new HeaderCarrier()
    "work for more complete Taxpayer data JSON" in {
      val json = Some(Json.parse(
        """{
          |    "name" : {
          |        "title": "Mr",
          |        "forename": "Erbert",
          |        "secondForename": "Donaldson",
          |        "surname": "Ducking",
          |        "honours": "KCBE"
          |    },
          |    "address": {
          |        "addressLine1": "42 Somewhere's Street",
          |        "addressLine2": "London",
          |        "addressLine3": "Greater London",
          |        "addressLine4": "",
          |        "addressLine5": "",
          |        "postcode": "WO9H 8AA",
          |        "foreignCountry": null,
          |        "returnedLetter": true,
          |        "additionalDeliveryInformation": "Leave by door"
          |    },
          |    "contact": {
          |        "telephone": {
          |            "daytime": "02654321#1235",
          |            "evening": "027123456",
          |            "mobile": "07676767",
          |            "fax": "0209798969"
          |        },
          |        "email": {
          |            "primary": "erbert@notthere.co.uk"
          |        },
          |        "other": {}
          |    }
          |}
          | """.stripMargin))

      val result = connectorWithResponse(json).taxpayerName(utr)

      result.futureValue should be(Some(fullTaxpayerName))
    }

    "work for Taxpayer JSON which holds no name field" in {
      val json = Some(Json.parse(
        """{
          |    "address": {
          |        "addressLine1": "42 Somewhere's Street",
          |        "addressLine2": "London",
          |        "addressLine3": "Greater London",
          |        "postcode": "WO9H 8AA",
          |        "foreignCountry": null,
          |        "returnedLetter": true,
          |        "additionalDeliveryInformation": "Leave by door"
          |    },
          |    "contact": {
          |        "telephone": {
          |            "mobile": "07676767"
          |        },
          |        "email": {
          |            "primary": "erbert@notthere.co.uk"
          |        },
          |        "other": {}
          |    }
          |}
          | """.stripMargin))

      val result = connectorWithResponse(json).taxpayerName(utr)

      result.futureValue should be(None)
    }

    "work for empty Taxpayer JSON" in {
      val result = connectorWithResponse(Some(Json.parse("{}"))).taxpayerName(utr)

      result.futureValue should be(None)
    }

    "work for JSON which only holds the name data" in {
      val json = Some(Json.parse(
        """{
          |"name" : {
          |        "title": "Mr",
          |        "forename": "Erbert",
          |        "secondForename": "Donaldson",
          |        "surname": "Ducking",
          |        "honours": "KCBE"
          |    }
          |}""".stripMargin))

      val result =
        connectorWithResponse(json).
          taxpayerName(utr
          )

      result.futureValue should be(Some(fullTaxpayerName))
    }
  }

  "Taxpayer connector" should {
    implicit val hc = HeaderCarrier()
    "log an error and return empty TaxpayerName on 5** or non 404 4** error" in {
      withCaptureOfLoggingFrom(logger) {
        logEvents => {
          connectorWithResponse(None, 500).taxpayerName(utr).futureValue should be(None)

          connectorWithResponse(None, 501).taxpayerName(utr).futureValue should be(None)

          connectorWithResponse(None, 401).taxpayerName(utr).futureValue should be(None)

          logEvents.count(_.getLevel == Level.ERROR) should be(3)
        }
      }
    }

    "log an warn level message and return empty TaxpayerName on 404 error" in {
      withCaptureOfLoggingFrom(logger) {
        logEvents => {
          connectorWithResponse(None, 404).taxpayerName(utr).futureValue should be(None)
          logEvents.head.getLevel should be(Level.WARN)
          logEvents.head.getMessage should include(utr.value)
        }
      }
    }
  }

  val logger = play.api.Logger.underlyingLogger.asInstanceOf[LogbackLogger]

  def connectorWithResponse(json: Option[JsValue], status: Int = 200) = {
    new TaxpayerNameConnector {
      override def http: HttpGet = new HttpGet {
        override protected def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
          Future.successful(HttpResponse(status, json))
        }

        val hooks: Seq[HttpHook] = Seq()
      }

      override def baseUrl: String = ""
    }
  }
}

trait LogCapturing {

  import scala.collection.JavaConverters._
  import scala.reflect._

  def withCaptureOfLoggingFrom[T: ClassTag](body: (=> List[ILoggingEvent]) => Any): Any = {
    val logger = LoggerFactory.getLogger(classTag[T].runtimeClass).asInstanceOf[LogbackLogger]
    withCaptureOfLoggingFrom(logger)(body)
  }

  def withCaptureOfLoggingFrom(logger: LogbackLogger)(body: (=> List[ILoggingEvent]) => Any): Any = {
    val appender = new ListAppender[ILoggingEvent]()
    appender.setContext(logger.getLoggerContext)
    appender.start()
    logger.addAppender(appender)
    logger.setLevel(Level.ALL)
    logger.setAdditive(true)
    body(appender.list.asScala.toList)
  }
}
