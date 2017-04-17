package functionalstreamer

import server._
import server.ServerAPI._

object MainJVM {
  def main(args: Array[String]): Unit = {
    val server = createServer(8080) {
      case e @ GET -> "/"                  => serveFile(e, "html/index.html"  )
      case e @ GET -> "/js/application.js" => serveFile(e, "js/application.js")
    }
    server.start()
  }
}

