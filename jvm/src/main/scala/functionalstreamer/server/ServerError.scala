package functionalstreamer.server

case class ServerError(message: String) extends RuntimeException(message)
