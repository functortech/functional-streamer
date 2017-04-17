package functionalstreamer

import java.io.File
import java.net.InetSocketAddress
import com.sun.net.httpserver.{HttpServer, HttpHandler, HttpExchange}

import org.apache.commons.io.{IOUtils, FileUtils}

object MainJVM {
  def main(args: Array[String]): Unit = {
    val server = HttpServer.create(new InetSocketAddress(8080), 0)

    def serveFile(path: String, contentType: String = "text/html"): HttpHandler = { e: HttpExchange =>
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

    server.createContext("/", serveFile("html/index.html"))
    server.createContext("/js/application.js", serveFile("js/application.js", "text/javascript"))
    server.createContext("/js/application.js.map", serveFile("js/application.js.map", "text/plain"))
    
    server.start()
  }
}
