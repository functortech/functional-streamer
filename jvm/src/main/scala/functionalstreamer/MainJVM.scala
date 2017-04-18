package functionalstreamer

import java.io.File

import scala.util.Try

import server._
import server.ServerAPI._

import server.StreamableSyntax._

import org.apache.commons.io.IOUtils

import io.circe.parser.decode
import io.circe.generic.auto._, io.circe.syntax._  // Implicit augmentations & type classes

import cats.instances.either.catsStdBitraverseForEither  // Type class for Bifunctor (which is a superclass of Bitraverse we are importing)
import cats.syntax.bifunctor.toBifunctorOps              // Implicit augmentation of types for which Bifunctor is available with Bifunctor operations


object MainJVM {
  implicit class AssefFileString(str: String) {
    def assetFile: File = new File(s"assets/$str")
  }

  def main(args: Array[String]): Unit = {
    val server = createServer(8080) {
      case GET -> "/"                  => Response("html/index.html"  .assetFile.stream, text.html             )
      case GET -> "/js/application.js" => Response("js/application.js".assetFile.stream, application.javascript)
      case e @ POST -> "/api" =>
        (for {
          req      <- Try { IOUtils.toString(e.getRequestBody, defaultEncoding) }.toEither
          decoded  <- decode[EchoReq](req)
          respJson  = handleApi(decoded)
          response  = Response(respJson.asJson.noSpaces.stream, application.json)
        } yield response).leftMap { err =>
          Response(s"Error occurred while parsing JSON request: ${err.toString}".stream, responseCode = 400)
        }.merge
    }
    server.start()
  }

  def handleApi(request: EchoReq): EchoResp =
    EchoResp(s"Echo response: ${request.str}")
}
