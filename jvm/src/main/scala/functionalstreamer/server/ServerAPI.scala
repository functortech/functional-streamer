package functionalstreamer.server

import scala.collection.JavaConverters._

import java.io.File
import java.net.InetSocketAddress
import com.sun.net.httpserver.{HttpServer, HttpExchange}

import org.apache.commons.io.{IOUtils, FileUtils}

object ServerAPI extends ServerAPI
trait ServerAPI {
  val defaultEncoding = "utf8"

  def createServer(port: Int)(handler: PartialHandler, errorHandler: TotalHandler = defaultErrorHandler): HttpServer = {
    val server = HttpServer.create(new InetSocketAddress(port), 0)
    
    server.createContext("/", { e: HttpExchange =>
      val Response(payloadIsGenerator, contentType, responseCode) = handler.applyOrElse(e, errorHandler)
      
      // Write the content type in the headers
      val headers = e.getResponseHeaders
      headers.put("Content-Type" , List(contentType).asJava)
      
      // Get the payload and response body streams
      val is = payloadIsGenerator()
      val os = e.getResponseBody

      try {
        e.sendResponseHeaders(responseCode, 0) // Send the status code
        IOUtils.copy(is, os)                   // Write the payload
      } finally {
        is.close()
        os.close()
      }
    })
    
    server
  }

  val defaultErrorHandler: TotalHandler = _ => serveString("Not Found", 404)

  def serveFile(path: String, contentType: String = "text/html"): Response =
    Response( () => FileUtils.openInputStream(new File(s"assets/$path")), contentType, 200 )

  def serveString(str: String, responseCode: Int): Response =
    Response( () => IOUtils.toInputStream(str, defaultEncoding), "text/plain", responseCode )
}
