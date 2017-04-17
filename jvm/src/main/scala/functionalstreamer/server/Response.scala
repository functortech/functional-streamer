package functionalstreamer.server

import java.io.InputStream

case class Response(
  payload: () => InputStream
, contentType: String = "text/plain"
, responseCode: Int   = 200
)
