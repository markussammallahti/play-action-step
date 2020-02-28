package mrks.play.mvc

import play.api.mvc.Result

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions


final case class Step[A](value: Future[Either[Result, A]]) {
  def map[B](f: A => B)(implicit ec: ExecutionContext): Step[B] = {
    Step(value.map(_.map(f)))
  }

  def flatMap[B](f: A => Step[B])(implicit ec: ExecutionContext): Step[B] = {
    Step(value.flatMap {
      case Left(result) =>
        Future.successful(Left(result))

      case Right(value) =>
        f(value).value
    })
  }
}


object Step {
  implicit def stepToResult[A <: Result](step: Step[A])(implicit ec: ExecutionContext): Future[Result] = {
    step.value.map(_.merge)
  }
}
