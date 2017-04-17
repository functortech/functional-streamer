package functionalstreamer.server

import java.io.File
import java.net.InetSocketAddress
import com.sun.net.httpserver.{HttpServer, HttpExchange}

import org.apache.commons.io.{IOUtils, FileUtils}

object ServerAPI extends ServerAPI
trait ServerAPI {
  def createServer(port: Int)(handler: PartialHandler, errorHandler: TotalHandler = defaultErrorHandler): HttpServer = {
    val server = HttpServer.create(new InetSocketAddress(port), 0)
    server.createContext("/", { e: HttpExchange =>
      if (handler.isDefinedAt(e)) handler(e)
      else errorHandler(e)
    })
    server
  }

  val defaultErrorHandler: TotalHandler = serveString(_, "Not Found", 404)

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

  def serveString(e: HttpExchange, str: String, responseCode: Int): Unit = {
    e.sendResponseHeaders(responseCode, 0)
    val os = e.getResponseBody()
    try IOUtils.write(str, os)
    finally os.close()
  }
}
