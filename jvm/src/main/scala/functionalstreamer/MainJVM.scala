package functionalstreamer

import scala.language.postfixOps

import java.io.File

import scala.util.Try

import server._
import server.ServerAPI._

import server.StreamableSyntax._

import org.apache.commons.io.IOUtils

import io.circe.{ Error => CirceError }
import io.circe.parser.decode
import io.circe.generic.auto._, io.circe.syntax._  // Implicit augmentations & type classes

import cats.instances.either.catsStdBitraverseForEither  // Type class for Bifunctor (which is a superclass of Bitraverse we are importing)
import cats.syntax.bifunctor.toBifunctorOps              // Implicit augmentation of types for which Bifunctor is available with Bifunctor operations


object MainJVM {
  implicit class FileString(str: String) {
    def assetFile: File = new File(s"assets/$str")
    def file     : File = new File(str)
  }

  implicit class FileAPI(file: File) {
    def contents: Either[Throwable, List[File]] = Try { file.listFiles }.toEither
      .filterOrElse(null !=, ServerError(s"Error occurred while retrieving the contents of the file: $file"))
      .map(_.toList)

    def toModel = FileModel(file.getAbsolutePath, file.getName, file.tpe)

    def tpe: FileType = file match {
      case _ if file.isDirectory => FileType.Directory
      case _                     => FileType.Misc
    }

    def parent: Option[File] = Some(file.getParentFile).filter(null !=)
  }

  def main(args: Array[String]): Unit = {
    val server = createServer(8080) {
      case GET -> "/"                  => Response("html/index.html"  .assetFile.stream, text.html             )
      case GET -> "/js/application.js" => Response("js/application.js".assetFile.stream, application.javascript)
      case e @ POST -> "/api" =>
        (for {
          req      <- Try { IOUtils.toString(e.getRequestBody, defaultEncoding) }.toEither
          decoded  <- decode[APIRequest](req)
          respJson <- handleApi(decoded)
          response  = Response(respJson.asJson.noSpaces.stream, application.json)
        } yield response).leftMap {
          case e: CirceError => Response(s"Error occurred while parsing JSON request: ${e.toString}".stream, responseCode = 400)
          case e: Throwable  => Response(s"Error occurred: ${e.toString}".stream, responseCode = 400)
        }.merge
    }
    server.start()
  }

  def handleApi(request: APIRequest): Either[Throwable, APIResponse] = request match {
    case DirContentsReq(path) =>
      for {
        contents      <- path.file.contents
        contentsPaths  = contents.map(_.toModel)
        maybeParent    = path.file.parent.map(_.toModel.copy(tpe = FileType.Parent))
      } yield DirContentsResp(contentsPaths, maybeParent)

    case _ => Left(ServerError(s"Unknown JSON API request: $request"))
  }
}
