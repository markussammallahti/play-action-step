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

import javax.inject.Inject
import mrks.play.mvc.ActionSteps
import play.api.mvc.{AbstractController, ControllerComponents}

import scala.concurrent.{ExecutionContext, Future}

class ExampleController @Inject()(cc: ControllerComponents)(implicit ec: ExecutionContext)
    extends AbstractController(cc) with ActionSteps {

  private def findUser(id: String): Future[Option[User]] = ???
  private def save(data: Data): Future[Either[Error, Data]] = ???

  def action(id: String) = Action.async(parse.json) { request =>
    for {
      user <- findUser(id)                ?| NotFound
      data <- request.body.validate[Data] ?| (e => BadRequest(JsError.toJson(e))
      _    <- save(data)                  ?| Unauthorized
    } yield {
      Ok
    }
  }
}
```
