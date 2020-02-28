# play-action-step

Step monad for [Play Framework](https://www.playframework.com/) controller actions.
Clone of [play-monadic-actions](https://github.com/Kanaka-io/play-monadic-actions) with few features added and few missing.

**Added**
* Async error handler operator `?>`
* Scala 2.13 support
* Implemented as trait

**Missing**
* Cats support
* Scalaz support
* withFilter support

## Usage

build.sbt
```
resolvers += Resolver.bintrayRepo("mrks", "maven")
libraryDependencies += "mrks" %% "play-action-step" % "1.0"
```

Controller.scala
```scala
package controllers

import mrks.play.mvc.ActionSteps
import play.api.mvc.{AbstractController, ControllerComponents}

import scala.concurrent.{ExecutionContext, Future}

class ExampleController(implicit ec: ExecutionContext) extends Controller with ActionSteps {

  private def findUser(id: String): Future[Option[User]] = ???
  private def save(user: User, data: Data): Future[Either[Error, Data]] = ???

  def action(id: String) = Action.async(parse.json) { request =>
    for {
      user   <- findUser(id)                ?| NotFound
      data   <- request.body.validate[Data] ?| (e => BadRequest(JsError.toJson(e)))
      result <- save(user, data)            ?| Unauthorized
    } yield {
      Ok(result)
    }
  }
}
```

### Supported step builders

|      Value     |      |         Source         | Operator |            Handler            |
|----------------|:----:|------------------------|:--------:|-------------------------------|
| `A`            | `<-` | `Future[A]`            |   `?\|`   | `Throwable => Result`         |
| `A`            | `<-` | `Future[A]`            |   `?>`   | `Throwable => Result`         |
| `Unit`         | `<-` | `Boolean`              |   `?\|`   | `=> Result`                   |
| `Unit`         | `<-` | `Boolean`              |   `?>`   | `=> Future[Result]`           |
| `Unit`         | `<-` | `Future[Boolean]`      |   `?\|`   | `=> Result`                   |
| `Unit`         | `<-` | `Future[Boolean]`      |   `?>`   | `=> Future[Result]`           |
| `A`            | `<-` | `Option[A]`            |   `?\|`   | `=> Result`                   |
| `A`            | `<-` | `Option[A]`            |   `?>`   | `=> Future[Result]`           |
| `A`            | `<-` | `Future[Option[A]]`    |   `?\|`   | `=> Result`                   |
| `A`            | `<-` | `Future[Option[A]]`    |   `?>`   | `=> Future[Result]`           |
| `A`            | `<-` | `Either[B, A]`         |   `?\|`   | `B => Result`                 |
| `A`            | `<-` | `Either[B, A]`         |   `?>`   | `B => Future[Result]`         |
| `A`            | `<-` | `Future[Either[B, A]]` |   `?\|`   | `B => Result`                 |
| `A`            | `<-` | `Future[Either[B, A]]` |   `?>`   | `B => Future[Result]`         |
| `A`            | `<-` | `Try[A]`               |   `?\|`   | `Throwable => Result`         |
| `A`            | `<-` | `Try[A]`               |   `?>`   | `Throwable => Future[Result]` |
| `A`            | `<-` | `Form[A]`              |   `?\|`   | `Form[A] => Result`           |
| `A`            | `<-` | `Form[A]`              |   `?>`   | `Form[A] => Future[Result]`   |
| `A`            | `<-` | `Future[Form[A]]`      |   `?\|`   | `Form[A] => Result`           |
| `A`            | `<-` | `Future[Form[A]]`      |   `?>`   | `Form[A] => Future[Result]`   |
| `A`            | `<-` | `JsResult[A]`          |   `?\|`   | `JsError => Result`           |
| `A`            | `<-` | `JsResult[A]`          |   `?>`   | `JsError => Future[Result]`   |
| `Option[A]`    | `<-` | `Option[A]`            |   `-\|`   | `escalate`                    |
| `Either[B, A]` | `<-` | `Either[B, A]`         |   `-\|`   | `escalate`                    |
| `Option[A]`    | `<-` | `Future[Option[A]]`    |   `-\|`   | `escalate`                    |
| `Either[B, A]` | `<-` | `Future[Either[B, A]]` |   `-\|`   | `escalate`                    |
