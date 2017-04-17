package functionalstreamer

import scala.scalajs.js.JSApp
import org.scalajs.dom.{document, window}

object MainJS extends JSApp {
  def main(): Unit = window.onload = { _ =>
    val placeholder = document.getElementById("body-placeholder")
    placeholder.innerHTML = "Hello World from JS"
  }
}
