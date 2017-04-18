package functionalstreamer

import scala.util.{Success, Failure}
import scala.concurrent.ExecutionContext.Implicits.global

import scala.scalajs.js.JSApp
import org.scalajs.dom.{document, window}
import org.scalajs.dom.ext.{Ajax, AjaxException}

import io.circe.parser.decode
import io.circe.generic.auto._, io.circe.syntax._  // Implicit augmentations & type classes

object MainJS extends JSApp {
  def main(): Unit = window.onload = { _ =>
    val placeholder = document.getElementById("body-placeholder")

    val req = EchoReq("Hello from Ajax")

    Ajax.post(url = "/api", data = req.asJson.noSpaces)
      .map(_.responseText).map(decode[EchoResp]).flatMap(_.toFuture)
      .onComplete {
        case Success(EchoResp(str)) => placeholder.innerHTML = str
        case Failure(err: AjaxException) => placeholder.innerHTML = err.xhr.responseText
        case Failure(err) => placeholder.innerHTML = s"Unknown error: ${err.toString}"
      }
  }
}
