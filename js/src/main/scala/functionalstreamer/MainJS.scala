package functionalstreamer

import scala.util.{Try, Success, Failure}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import scala.scalajs.js.JSApp
import org.scalajs.dom.{document, window, Element}
import org.scalajs.dom.ext.{Ajax, AjaxException}
import scalatags.JsDom.all._

import io.circe.parser.decode
import io.circe.generic.auto._, io.circe.syntax._  // Implicit augmentations & type classes

object MainJS extends JSApp with AjaxHelpers {
  def placeholder = document.getElementById("body-placeholder")

  def main(): Unit = window.onload = { _ =>
    ajax(DirContentsReq("/")).onComplete(renderResponse)
  }

  def handleApi(response: APIResponse): Either[Throwable, ClientOperation] = response match {
    case DirContentsResp(files, parent) =>
      val filesViews: List[HtmlTag] = files.map {
        case FileModel(path, name, FileType.Directory) =>
          button(onclick := { () => ajax(DirContentsReq(path)).onComplete(renderResponse) })(name)

        case FileModel(path, name, _) =>
          p(name)
      }

      val maybeParent: Option[HtmlTag] = parent.map { f =>
        button(onclick := { () => ajax(DirContentsReq(f.path)).onComplete(renderResponse) })("..")
      }

      val listItems: List[HtmlTag] = (maybeParent.toList ++ filesViews).map { f => li(f) }

      Right(RenderTag( ul(listItems) ))

    case _ => Left(ClientError(s"Can not handle $response"))
  }
}

trait AjaxHelpers {
  def placeholder: Element

  def handleApi(response: APIResponse): Either[Throwable, ClientOperation]

  def ajax(request: APIRequest): Future[ClientOperation] =
    for {
      response  <- Ajax.post(url = "/api", data = request.asJson.noSpaces)
      respText   = response.responseText
      decoded   <- decode[APIResponse](respText).toFuture
      operation <- handleApi(decoded).toFuture
    } yield operation

  def renderResponse(response: Try[ClientOperation]): Unit = response match {
    case Success(RenderString(str)) => placeholder.innerHTML = str
    case Success(RenderTag   (tag)) => placeholder.innerHTML = ""
                                       placeholder.appendChild(tag.render)

    case Failure(err: AjaxException) => placeholder.innerHTML = s"Ajax exception: ${err.xhr.responseText}"
    case Failure(err) => placeholder.innerHTML = s"Unknown error: ${err.toString}"
  }
}
