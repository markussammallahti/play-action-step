package mrks.play.mvc

import play.api.data.Form
import play.api.libs.json.{JsError, JsResult, JsSuccess}
import play.api.mvc.Result

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}


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

  implicit class FutureStepBuilder[A](future: Future[A])(implicit ec: ExecutionContext) extends StepBuilder[A, Throwable] {
    override def build(onError: Handler[Throwable]): Step[A] = {
      Step(fromFuture(onError, future))
    }
  }

  implicit class BooleanStepBuilder(boolean: Boolean)(implicit ec: ExecutionContext) extends StepBuilder[Unit, Unit] {
    override def build(onError: Handler[Unit]): Step[Unit] = {
      Step(fromBoolean(onError)(boolean))
    }
  }

  implicit class FutureBooleanStepBuilder(future: Future[Boolean])(implicit ec: ExecutionContext) extends StepBuilder[Unit, Unit] {
    override def build(onError: Handler[Unit]): Step[Unit] = {
      Step(future.flatMap(fromBoolean(onError)))
    }
  }

  implicit class OptionStepBuilder[A](option: Option[A])(implicit ec: ExecutionContext) extends StepBuilder[A, Unit] {
    override def build(onError: Handler[Unit]): Step[A] = {
      Step(fromOption(onError)(option))
    }
  }

  implicit class FutureOptionStepBuilder[A](futureOption: Future[Option[A]])(implicit ec: ExecutionContext) extends StepBuilder[A, Unit] {
    override def build(onError: Handler[Unit]): Step[A] = {
      Step(futureOption.flatMap(fromOption(onError)))
    }
  }

  implicit class EitherStepBuilder[A, B](either: Either[B, A])(implicit ec: ExecutionContext) extends StepBuilder[A, B] {
    override def build(onError: Handler[B]): Step[A] = {
      Step(fromEither(onError)(either))
    }
  }

  implicit class FutureEitherStepBuilder[A, B](futureEither: Future[Either[B, A]])(implicit ec: ExecutionContext) extends StepBuilder[A, B] {
    override def build(onError: Handler[B]): Step[A] = {
      Step(futureEither.flatMap(fromEither(onError)))
    }
  }

  implicit class TryStepBuilder[A](result: Try[A])(implicit ec: ExecutionContext) extends StepBuilder[A, Throwable] {
    override def build(onError: Handler[Throwable]): Step[A] = {
      Step(fromFuture(onError, Future.fromTry(result)))
    }
  }

  implicit class FormStepBuilder[A](form: Form[A])(implicit ec: ExecutionContext) extends StepBuilder[A, Form[A]] {
    override def build(onError: Handler[Form[A]]): Step[A] = {
      Step(fromForm(onError)(form))
    }
  }

  implicit class FutureFormStepBuilder[A](futureForm: Future[Form[A]])(implicit ec: ExecutionContext) extends StepBuilder[A, Form[A]] {
    override def build(onError: Handler[Form[A]]): Step[A] = {
      Step(futureForm.flatMap(fromForm(onError)))
    }
  }

  implicit class JsResultStepBuilder[A](result: JsResult[A])(implicit ec: ExecutionContext) extends StepBuilder[A, JsError] {
    override def build(onError: Handler[JsError]): Step[A] = {
      Step(fromJsResult(onError)(result))
    }
  }

  implicit class FutureJsResultStepBuilder[A](futureResult: Future[JsResult[A]])(implicit ec: ExecutionContext) extends StepBuilder[A, JsError] {
    override def build(onError: Handler[JsError]): Step[A] = {
      Step(futureResult.flatMap(fromJsResult(onError)))
    }
  }

  private def fromBoolean(onError: Handler[Unit])(boolean: Boolean)(implicit ec: ExecutionContext): Future[Either[Result, Unit]] = {
    if (boolean) {
      Future.successful(Right(()))
    }
    else {
      onError(()).map(Left(_))
    }
  }

  private def fromOption[A](onError: Handler[Unit])(option: Option[A])(implicit ec: ExecutionContext): Future[Either[Result, A]] = {
    option match {
      case Some(value) =>
        Future.successful(Right(value))

      case _ =>
        onError(()).map(Left(_))
    }
  }

  private def fromEither[A, B](onError: Handler[B])(either: Either[B, A])(implicit ec: ExecutionContext): Future[Either[Result, A]] = {
    either match {
      case Right(value) =>
        Future.successful(Right(value))

      case Left(value) =>
        onError(value).map(Left(_))
    }
  }

  private def fromForm[A](onError: Handler[Form[A]])(form: Form[A])(implicit ec: ExecutionContext): Future[Either[Result, A]] = {
    form.fold(
      hasErrors => onError(hasErrors).map(Left(_)),
      data      => Future.successful(Right(data))
    )
  }

  private def fromJsResult[A](onError: Handler[JsError])(result: JsResult[A])(implicit ec: ExecutionContext): Future[Either[Result, A]] = {
    result match {
      case JsSuccess(value, _) =>
        Future.successful(Right(value))

      case e: JsError =>
        onError(e).map(Left(_))
    }
  }

  private def fromFuture[A](onError: Handler[Throwable], future: Future[A])(implicit ec: ExecutionContext): Future[Either[Result, A]] = {
    future.transformWith {
      case Success(value) =>
        Future.successful(Right(value))

      case Failure(e) =>
        onError(e).map(Left(_))
    }
  }
}
