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

import com.google.inject.AbstractModule
import config.{AppConfig, FrontendAppConfig}
//import connectors.FrontendAuthConnector
import net.codingwell.scalaguice.ScalaModule
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.http.{DefaultHttpClient, HttpClient}

class FrontendModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    //bind(classOf[AuthConnector]).to(classOf[FrontendAuthConnector])
    bind(classOf[AppConfig]).to(classOf[FrontendAppConfig])
    bind(classOf[HttpClient]).to(classOf[DefaultHttpClient])
  }
}
