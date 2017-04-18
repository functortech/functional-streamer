package functionalstreamer

case class ClientError(message: String) extends RuntimeException(message)
