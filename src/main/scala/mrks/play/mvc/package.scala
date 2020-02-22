package mrks.play

import play.api.mvc.Result

import scala.concurrent.Future

package object mvc {
  type Handler[A] = A => Future[Result]
}

