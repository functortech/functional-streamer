package functionalstreamer

sealed trait APIRequest
case class EchoReq (str: String) extends APIRequest

sealed trait APIResponse
case class EchoResp(str: String) extends APIResponse
