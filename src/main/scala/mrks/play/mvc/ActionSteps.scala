package mrks.play.mvc

import play.api.data.Form
import play.api.libs.json.{JsError, JsResult, JsSuccess}
import play.api.mvc.Result

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import scala.language.implicitConversions


trait ActionSteps {
  case object escalate

  implicit class FutureEscalate[A](future: Future[A]) {
    def -| (esc: escalate.type)(implicit ec: ExecutionContext): Step[A] = Step(future.map(Right(_)))
  }

  implicit class EitherEscalate[A, B](either: Either[B, A]) {
    def -| (esc: escalate.type): Step[Either[B, A]] = Step(Future.successful(Right(either)))
  }

  implicit class OptionEscalate[A](option: Option[A]) {
    def -| (esc: escalate.type): Step[Option[A]] = Step(Future.successful(Right(option)))
  }

  implicit def futureToOps[A](future: Future[A])(implicit ec: ExecutionContext): StepOps[A, Throwable] = {
    handler => Step(fromFuture(handler, future))
  }

  implicit def booleanToOps(boolean: Boolean)(implicit ec: ExecutionContext): StepOps[Unit, Unit] = {
    handler => Step(fromBoolean(handler)(boolean))
  }

  implicit def futureBooleanToOps(future: Future[Boolean])(implicit ec: ExecutionContext): StepOps[Unit, Unit] = {
    handler => Step(future.flatMap(fromBoolean(handler)))
  }

  implicit def optionToOps[A](option: Option[A])(implicit ec: ExecutionContext): StepOps[A, Unit] = {
    handler => Step(fromOption(handler)(option))
  }

  implicit def futureOptionToOps[A](futureOption: Future[Option[A]])(implicit ec: ExecutionContext): StepOps[A, Unit] = {
    handler => Step(futureOption.flatMap(fromOption(handler)))
  }

  implicit def eitherToOps[A, B](either: Either[B, A])(implicit ec: ExecutionContext): StepOps[A, B] = {
    handler => Step(fromEither(handler)(either))
  }

  implicit def futureEitherToOps[A, B](futureEither: Future[Either[B, A]])(implicit ec: ExecutionContext): StepOps[A, B] = {
    handler => Step(futureEither.flatMap(fromEither(handler)))
  }

  implicit def tryToOps[A](result: Try[A])(implicit ec: ExecutionContext): StepOps[A, Throwable] = {
    handler => Step(fromFuture(handler, Future.fromTry(result)))
  }

  implicit def formToOps[A](form: Form[A])(implicit ec: ExecutionContext): StepOps[Form[A], Form[A]] = {
    handler => Step(fromForm(handler)(form))
  }

  implicit def futureFormToOps[A](futureForm: Future[Form[A]])(implicit ec: ExecutionContext): StepOps[Form[A], Form[A]] = {
    handler => Step(futureForm.flatMap(fromForm(handler)))
  }

  implicit def jsResultOps[A](result: JsResult[A])(implicit ec: ExecutionContext): StepOps[A, JsError] = {
    handler => Step(fromJsResult(handler)(result))
  }

  implicit def futureJsResultOps[A](futureResult: Future[JsResult[A]])(implicit ec: ExecutionContext): StepOps[A, JsError] = {
    handler => Step(futureResult.flatMap(fromJsResult(handler)))
  }

  private def fromBoolean(handler: Handler[Unit])(boolean: Boolean)(implicit ec: ExecutionContext): Future[Either[Result, Unit]] = {
    if (boolean) {
      Future.successful(Right(()))
    }
    else {
      handler(()).map(Left(_))
    }
  }

  private def fromOption[A](handler: Handler[Unit])(option: Option[A])(implicit ec: ExecutionContext): Future[Either[Result, A]] = {
    option match {
      case Some(value) =>
        Future.successful(Right(value))

      case _ =>
        handler(()).map(Left(_))
    }
  }

  private def fromEither[A, B](handler: Handler[B])(either: Either[B, A])(implicit ec: ExecutionContext): Future[Either[Result, A]] = {
    either match {
      case Right(value) =>
        Future.successful(Right(value))

      case Left(value) =>
        handler(value).map(Left(_))
    }
  }

  private def fromForm[A](handler: Handler[Form[A]])(form: Form[A])(implicit ec: ExecutionContext): Future[Either[Result, Form[A]]] = {
    if (form.hasErrors) {
      handler(form).map(Left(_))
    }
    else {
      Future.successful(Right(form))
    }
  }

  private def fromJsResult[A](handler: Handler[JsError])(result: JsResult[A])(implicit ec: ExecutionContext): Future[Either[Result, A]] = {
    result match {
      case JsSuccess(value, _) =>
        Future.successful(Right(value))

      case e: JsError =>
        handler(e).map(Left(_))
    }
  }

  private def fromFuture[A](handler: Handler[Throwable], future: Future[A])(implicit ec: ExecutionContext): Future[Either[Result, A]] = {
    future.transformWith {
      case Success(value) =>
        Future.successful(Right(value))

      case Failure(e) =>
        handler(e).map(Left(_))
    }
  }
}
