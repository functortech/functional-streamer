import scala.concurrent.Future

package object functionalstreamer {
  implicit class EitherToFuture[A <: Throwable, B](e: Either[A, B]) {
    def toFuture: Future[B] = e match {
      case Right(b) => Future.successful(b)
      case Left (e) => Future.failed    (e)
    }
  }
}