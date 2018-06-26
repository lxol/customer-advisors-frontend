package util

import play.api.Application
import play.api.http.Writeable
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.{ AnyContentAsEmpty, AnyContentAsJson }
import play.api.test.FakeRequest
import scala.util.Try
import play.api.test.Helpers
import play.api.test.Helpers._

final case class FakeResponse(status: Int, body: String, allHeaders: Map[String, String] = Map.empty) {
  val json: JsValue = Try(Json.parse(body)).getOrElse(Json.obj())
}

final case class FakeRequestBuilder(
  url: String,
  headers: Map[String, String] = Map.empty,
  queryParameters: Map[String, String] = Map.empty
) {

  def withHeaders(params: (String, String)*): FakeRequestBuilder =
    this.copy(headers = this.headers ++ params)

  def withQueryString(params: (String, String)*): FakeRequestBuilder =
    this.copy(queryParameters = this.queryParameters ++ params)

  def post(body: JsValue)(implicit app: Application): FakeResponse = {
    val req: FakeRequest[AnyContentAsJson] = FakeRequest("POST", url)
      .withHeaders(headers.toList: _*)
      .withJsonBody(body)

    val result = route(app, req)
      .getOrElse(throw new RuntimeException(s"POST request to $url failed"))

    FakeResponse(Helpers.status(result), contentAsString(result), Helpers.headers(result))
  }

  def get()(implicit app: Application): FakeResponse = {
    val req = FakeRequest("GET", url)
      .withHeaders(headers.toList: _*)

    val result = route(app, req)
      .getOrElse(throw new RuntimeException(s"GET request to $url failed"))

    FakeResponse(Helpers.status(result), contentAsString(result), Helpers.headers(result))
  }

}
