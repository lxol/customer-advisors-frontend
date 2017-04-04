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

import uk.gov.hmrc.play.test.UnitSpec

class HtmlCleanerSpec
  extends UnitSpec
  with HtmlCleaner {


  "html cleaner" should {
    "remove script tags" in {
      val inputHtml = "<p>This is a paragraph</p><script>alert('hax')</script>"
      val expectedOutputHtml = "<p>This is a paragraph</p>"
      cleanHtml(inputHtml) shouldBe expectedOutputHtml
    }

    "remove script attributes" in {
      val inputHtml = """<img src="http://abc" onclick="alert('hax')">"""
      val expectedOutputHtml = """<img src="http://abc">"""
      cleanHtml(inputHtml) shouldBe expectedOutputHtml
    }

    "make no changes" in {
      val inputHtml = "<p>This is a paragraph</p>"
      cleanHtml(inputHtml) shouldBe inputHtml
    }

    "preserve class attributes" in {
      val inputHtml = """<UL class="bullets"><LI>Bullet text</LI></UL>"""
      val expectedOutputHtml = """<UL class="bullets"><LI>Bullet text</LI></UL>"""
      cleanHtml(inputHtml).toLowerCase shouldBe expectedOutputHtml.toLowerCase
    }

    "handle real world scenario and quote unquoted attributes" in {
      val inputHtml = """<DIV class=WordSection1>\r\n<H1>About your Child Benefit</H1>\r\n<P>You contacted us recently to ask about your Child Benefit award.</P>\r\n<H2>Details of your Child Benefit award are shown below</H2>\r\n<TABLE>\r\n<THEAD>\r\n<TR>\r\n<TH><B>Child Benefit was awarded for this child</B></TH>\r\n<TH><B>Date of birth</B></TH>\r\n<TH><B>Higher or lower weekly rate</B></TH>\r\n<TH><B>Awarded from</B></TH>\r\n<TH><B>Ended on</B></TH></TR></THEAD>\r\n<TBODY>\r\n<TR>\r\n<TD>k huji</TD>\r\n<TD>15&nbsp;February&nbsp;1995</TD>\r\n<TD class="right-aligned">Lower</TD>\r\n<TD>28&nbsp;August&nbsp;2015</TD>\r\n<TD>5&nbsp;April&nbsp;2016</TD></TR></TBODY></TABLE>\r\n<P>These details are based on the information we hold at the date of this letter and may change if there is a change in circumstances.</P></DIV>"""
      val expectedOutputHtml = """<DIV class="WordSection1">\r\n<H1>About your Child Benefit</H1>\r\n<P>You contacted us recently to ask about your Child Benefit award.</P>\r\n<H2>Details of your Child Benefit award are shown below</H2>\r\n<TABLE>\r\n<THEAD>\r\n<TR>\r\n<TH><B>Child Benefit was awarded for this child</B></TH>\r\n<TH><B>Date of birth</B></TH>\r\n<TH><B>Higher or lower weekly rate</B></TH>\r\n<TH><B>Awarded from</B></TH>\r\n<TH><B>Ended on</B></TH></TR></THEAD>\r\n<TBODY>\r\n<TR>\r\n<TD>k huji</TD>\r\n<TD>15&nbsp;February&nbsp;1995</TD>\r\n<TD class="right-aligned">Lower</TD>\r\n<TD>28&nbsp;August&nbsp;2015</TD>\r\n<TD>5&nbsp;April&nbsp;2016</TD></TR></TBODY></TABLE>\r\n<P>These details are based on the information we hold at the date of this letter and may change if there is a change in circumstances.</P></DIV>"""
      val actualOutputHtml = cleanHtml(inputHtml)

      expectedOutputHtml.toLowerCase shouldEqual actualOutputHtml.toLowerCase
    }

  }

}
