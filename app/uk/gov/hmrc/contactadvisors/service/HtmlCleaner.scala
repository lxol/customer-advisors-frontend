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

package uk.gov.hmrc.contactadvisors.service

import org.jsoup.Jsoup
import org.jsoup.nodes.Document.OutputSettings
import org.jsoup.safety.Whitelist


trait HtmlCleaner {
  def cleanHtml(dirtyHtml: String): String = {
    val settings = new OutputSettings().prettyPrint(false)
    return Jsoup.clean(dirtyHtml, "", relaxedWhitelistWithClassAttributes(), settings)
  }

  private def relaxedWhitelistWithClassAttributes(): Whitelist = Whitelist.relaxed()
    .addAttributes("a", "class")
    .addAttributes("div", "class")
    .addAttributes("ol", "class")
    .addAttributes("table", "class")
    .addAttributes("td", "class")
    .addAttributes("th", "class")
    .addAttributes("ul", "class")
}

object HtmlCleaner extends HtmlCleaner
