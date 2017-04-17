package functionalstreamer

import server._
import server.ServerAPI._

object MainJVM {
  def main(args: Array[String]): Unit = {
    val server = createServer(8080) {
      case GET -> "/"                  => serveFile("html/index.html")
      case GET -> "/js/application.js" => serveFile("js/application.js", "application/javascript")
    }
    server.start()
  }
}

