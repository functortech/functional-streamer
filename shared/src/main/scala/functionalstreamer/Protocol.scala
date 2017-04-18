package functionalstreamer

sealed trait APIRequest
case class DirContentsReq(path: String) extends APIRequest

sealed trait APIResponse
case class DirContentsResp(contents: List[FileModel], parent: Option[FileModel]) extends APIResponse
