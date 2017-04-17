package functionalstreamer

import com.sun.net.httpserver.{HttpHandler, HttpExchange}
import org.apache.commons.io.{IOUtils, FileUtils}

package object server {
  type Handler = PartialFunction[HttpExchange, Unit]
  type Path    = String

  // Converters between the native API types and the overlay types we defined
  object -> {
    def unapply(exchange: HttpExchange): Option[(Method, Path)] =
      exchange.getRequestMethod.toMethod.map { _ -> exchange.getRequestURI.getPath }
  }

  implicit class MethodString(str: String) {
    def toMethod: Option[Method] = str.toLowerCase match {
      case "get"  => Some(GET)
      case "post" => Some(POST)
      case _      => None
    }
  }

  implicit def toNativeHandler(handler: Handler): HttpHandler = { exchange: HttpExchange =>
    if (handler.isDefinedAt(exchange)) handler(exchange)
    else {
      exchange.sendResponseHeaders(404, 0)
      val os = exchange.getResponseBody()
      try IOUtils.write("Not found", os)
      finally os.close()
    }
  }
}
