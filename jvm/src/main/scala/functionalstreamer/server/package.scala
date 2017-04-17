package functionalstreamer

import com.sun.net.httpserver.{HttpHandler, HttpExchange}

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
    else ServerAPI.serveString(exchange, "Not Found", 404)
  }
}
