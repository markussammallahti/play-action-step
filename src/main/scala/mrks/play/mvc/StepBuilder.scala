package mrks.play.mvc

import play.api.mvc.Result

import scala.concurrent.Future


trait StepBuilder[A, B] {
  def build(onError: Handler[B]): Step[A]

  def ?> (onError: B => Future[Result]): Step[A] = build(onError)
  def ?> (onError: => Future[Result]): Step[A] = build(_ => onError)
  def ?| (onError: B => Result): Step[A] = build(b => Future.successful(onError(b)))
  def ?| (onError: => Result): Step[A] = build(_ => Future.successful(onError))
}
