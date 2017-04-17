package functionalstreamer.server

import java.io.{InputStream, File}
import org.apache.commons.io.{IOUtils, FileUtils}

trait Streamable[A] {
  def stream(a: A): () => InputStream
}

object Streamable {
  implicit val streamableFile: Streamable[File] = f =>
    () => FileUtils.openInputStream(f)

  implicit val streamableString: Streamable[String] = str =>
    () => IOUtils.toInputStream(str, defaultEncoding)
}

object StreamableSyntax {
  implicit class StreamableOps[A](a: A)(implicit typeclass: Streamable[A]) {
    def stream: () => InputStream = typeclass.stream(a)
  }
}
