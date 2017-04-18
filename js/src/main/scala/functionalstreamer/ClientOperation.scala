package functionalstreamer

sealed trait ClientOperation
case class RenderString(str: String) extends ClientOperation
