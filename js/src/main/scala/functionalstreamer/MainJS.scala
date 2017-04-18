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

import cats.syntax.all._
import cats.instances.list._, cats.instances.option._
import typeclasses._


object MainJS extends JSApp with AjaxHelpers {
  def placeholder = document.getElementById("body-placeholder")

  def main(): Unit = window.onload = { _ =>
    ajax(DirContentsReq("/")).onComplete(renderResponse)
  }

  def handleApi(response: APIResponse): Either[Throwable, ClientOperation] = response match {
    case resp @ DirContentsResp(files, parent) => view(resp).map(RenderTag)
    case _ => Left(ClientError(s"Can not handle $response"))
  }

  def view(x: Any): Either[ClientError, HtmlTag] = x match {
    case DirContentsResp(files, parent: Option[FileModel]) =>
      (files.traverse(view) |@| parent.traverse(view)).map { (filesViews, maybeParentView) =>
        ul( (maybeParentView ++ filesViews).map { f => li(f) }.toList ) }

    case FileModel(path, name, FileType.Directory) =>
      Right( button(onclick := ajaxCallback(DirContentsReq(path)))(name) )

    case FileModel(path, name, FileType.Misc) => Right(p(name))

    case _ => Left(ClientError(s"Can not render view: $x"))
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

  def ajaxCallback(request: APIRequest): () => Unit =
    () => ajax(request).onComplete(renderResponse)
}
