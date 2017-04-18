package functionalstreamer

import scalatags.JsDom.all.HtmlTag

sealed trait ClientOperation
case class RenderString(str: String) extends ClientOperation
case class RenderTag(tag: HtmlTag) extends ClientOperation
