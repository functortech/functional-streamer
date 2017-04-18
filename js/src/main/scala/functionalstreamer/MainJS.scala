package functionalstreamer

import scala.util.{Try, Success, Failure}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import scala.scalajs.js.JSApp
import org.scalajs.dom.{document, window}
import org.scalajs.dom.ext.{Ajax, AjaxException}

import io.circe.parser.decode
import io.circe.generic.auto._, io.circe.syntax._  // Implicit augmentations & type classes

object MainJS extends JSApp {
  def placeholder = document.getElementById("body-placeholder")

  def main(): Unit = window.onload = { _ =>
    ajax(DirContentsReq("/")).onComplete(renderResponse)
  }

  def handleApi(response: APIResponse): Either[Throwable, ClientOperation] = response match {
    case DirContentsResp(files) =>
      val html =
        s"""<ul>
           |  ${(for (f <- files) yield s"<li>$f</li>").mkString("\n")}
           |</ul>""".stripMargin

      Right(RenderString(html))

    case _ => Left(ClientError(s"Can not handle $response"))
  }

  def ajax(request: APIRequest): Future[ClientOperation] =
    for {
      response  <- Ajax.post(url = "/api", data = request.asJson.noSpaces)
      respText   = response.responseText
      decoded   <- decode[APIResponse](respText).toFuture
      operation <- handleApi(decoded).toFuture
    } yield operation

  def renderResponse(response: Try[ClientOperation]): Unit = response match {
    case Success(RenderString(str)) => placeholder.innerHTML = str

    case Failure(err: AjaxException) => placeholder.innerHTML = s"Ajax exception: ${err.xhr.responseText}"
    case Failure(err) => placeholder.innerHTML = s"Unknown error: ${err.toString}"
  }
}
