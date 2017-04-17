package functionalstreamer.server

import java.io.File
import java.net.InetSocketAddress
import com.sun.net.httpserver.{HttpServer, HttpExchange}

import org.apache.commons.io.{IOUtils, FileUtils}

object ServerAPI extends ServerAPI
trait ServerAPI {
  def createServer(port: Int)(handler: Handler): HttpServer = {
    val server = HttpServer.create(new InetSocketAddress(port), 0)
    server.createContext("/", handler)
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
