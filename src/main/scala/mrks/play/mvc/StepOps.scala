package mrks.play.mvc

import play.api.mvc.Result

import scala.concurrent.Future


trait StepOps[A, B] {
  def recover(handler: Handler[B]): Step[A]

  def ?> (handler: B => Future[Result]): Step[A] = recover(handler)
  def ?> (handler: => Future[Result]): Step[A] = recover(_ => handler)
  def ?| (handler: B => Result): Step[A] = recover(b => Future.successful(handler(b)))
  def ?| (handler: => Result): Step[A] = recover(_ => Future.successful(handler))
}
