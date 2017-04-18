package functionalstreamer

import java.io.File

import server._
import server.ServerAPI._

import server.StreamableSyntax._

import org.apache.commons.io.IOUtils

import io.circe.{Error => CirceError}
import io.circe.parser.decode
import io.circe.generic.auto._, io.circe.syntax._  // Implicit augmentations & type classes


object MainJVM {
  implicit class AssefFileString(str: String) {
    def assetFile: File = new File(s"assets/$str")
  }

  def main(args: Array[String]): Unit = {
    val server = createServer(8080) {
      case GET -> "/"                  => Response("html/index.html"  .assetFile.stream, text.html             )
      case GET -> "/js/application.js" => Response("js/application.js".assetFile.stream, application.javascript)
      case e @ POST -> "/api" =>
        val req = IOUtils.toString(e.getRequestBody, defaultEncoding)

        val respOrError: Either[CirceError, Response] =
          decode[EchoReq](req)
            .map { case EchoReq(str) =>
              Response(EchoResp(s"Echo response: $str").asJson.noSpaces.stream, application.json)
            }

        respOrError match {
          case Right(resp) => resp
          case Left (err ) =>
            Response(s"Error occurred while parsing JSON request: ${err.toString}".stream, responseCode = 400)
        }
    }
    server.start()
  }
}

