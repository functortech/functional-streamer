package functionalstreamer

import java.io.File
import java.net.InetSocketAddress
import com.sun.net.httpserver.{HttpServer, HttpHandler, HttpExchange}

import org.apache.commons.io.{IOUtils, FileUtils}

object ServerAPI extends ServerAPI
trait ServerAPI {
  // Types we will use to describe the Server
  type Handler = PartialFunction[HttpExchange, Unit]
  type Path    = String

  sealed trait Method
  case object GET  extends Method
  case object POST extends Method


  // Converters between the native API types and the overlay types we defined
  object -> {
    def unapply(exchange: HttpExchange): Option[(Method, Path)] =
      toMethod(exchange.getRequestMethod).map { _ -> exchange.getRequestURI.getPath }
  }

  def toMethod(str: String): Option[Method] = str.toLowerCase match {
    case "get"  => Some(GET)
    case "post" => Some(POST)
    case _      => None
  }

  def toNativeHandler(handler: Handler): HttpHandler = { exchange: HttpExchange =>
    if (handler.isDefinedAt(exchange)) handler(exchange)
    else {
      exchange.sendResponseHeaders(404, 0)
      val os = exchange.getResponseBody()
      try IOUtils.write("Not found", os)
      finally os.close()
    }
  }


  // The API itself
  def createServer(port: Int)(handler: Handler): HttpServer = {
    val server = HttpServer.create(new InetSocketAddress(port), 0)
    val nativeHandler: HttpHandler = toNativeHandler(handler)
    server.createContext("/", nativeHandler)
    server
  }

  def serveFile(e: HttpExchange, path: String, contentType: String = "text/html"): Unit = {
    val file   = new File(s"assets/$path")
    val fileIs = FileUtils.openInputStream(file)
    val os     = e.getResponseBody
    try {
      e.sendResponseHeaders(200, 0)
      IOUtils.copy(fileIs, os)
      os.close()
    } finally {
      os.close()
      fileIs.close()
    }
  }
}
