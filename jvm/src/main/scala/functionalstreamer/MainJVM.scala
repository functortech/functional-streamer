package functionalstreamer

import scala.language.postfixOps

import java.io.File

import scala.util.Try

import server._
import server.ServerAPI._

import server.StreamableSyntax._

import org.apache.commons.io.{IOUtils, FilenameUtils}

import io.circe.{ Error => CirceError }
import io.circe.parser.decode
import io.circe.generic.auto._, io.circe.syntax._  // Implicit augmentations & type classes

import cats.instances.either._, cats.instances.option._  // Type class for Bifunctor (which is a superclass of Bitraverse we are importing)
import cats.syntax.all._                                 // Implicit augmentation of types for which Bifunctor is available with Bifunctor operations

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
      case _ if isVideo          => FileType.Video
      case _                     => FileType.Misc
    }

    def parent: Option[File] = Some(file.getParentFile).filter(null !=)
    def parentModel: Option[FileModel] = parent.map(_.toModel.copy(tpe = FileType.Parent))

    def isVideo: Boolean = Set("mp4", "m4v") contains extension

    def extension: String = FilenameUtils.getExtension(file.getName)

    def leftNeighbor  = neighbor(_ => true) { case f :: `file` :: Nil => f }
    def rightNeighbor = neighbor(_ => true) { case `file` :: f :: Nil => f }

    def leftNeighborOfType(allowedTypes: Set[FileType]) =
      neighbor(f => allowedTypes(f.tpe)) { case f :: `file` :: Nil => f }

    def rightNeighborOfType(allowedTypes: Set[FileType]) =
      neighbor(f => allowedTypes(f.tpe)) { case `file` :: f :: Nil => f }

    private[this] def neighbor(filter: File => Boolean)(predicate: PartialFunction[List[File], File]): Either[Throwable, Option[File]] =
      parent.traverse(_.contents).map { mCts: Option[List[File]] =>
        mCts.flatMap(_.filter(filter).sliding(2, 1).collectFirst(predicate))
      }
  }

  implicit class OptionToEither[A](opt: Option[A]) {
    def toEither: Either[ServerError, A] = opt match {
      case Some(x) => Right(x)
      case None    => Left(ServerError(s"Could not extract value from an Option"))
    }
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
        contentsPaths  = contents.map(_.toModel).sortBy(_.tpe != FileType.Directory)
        maybeParent    = path.file.parentModel
      } yield DirContentsResp(contentsPaths, maybeParent)

    case VideoReq(path) =>
      for {
        parent        <- path.file.parentModel.toEither
        maybePrevious <- path.file.leftNeighborOfType (Set(FileType.Video)).map(_.map(_.toModel))
        maybeNext     <- path.file.rightNeighborOfType(Set(FileType.Video)).map(_.map(_.toModel))
        streamPath     = s"/video/$path"
      } yield VideoResp(path.file.getName, streamPath, parent, maybePrevious, maybeNext)

    case _ => Left(ServerError(s"Unknown JSON API request: $request"))
  }
}
