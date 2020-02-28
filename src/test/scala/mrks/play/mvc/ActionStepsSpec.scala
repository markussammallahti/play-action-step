package mrks.play.mvc

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{MustMatchers, WordSpec}
import play.api.data.{Form, Forms}
import play.api.http.{HeaderNames, Status}
import play.api.i18n.MessagesProvider
import play.api.libs.json.{JsError, Json}
import play.api.mvc.{Result, Results}
import play.api.test.{DefaultAwaitTimeout, ResultExtractors, StubMessagesFactory}

import scala.concurrent.Future
import scala.util.Try


class ActionStepsSpec extends WordSpec
    with MustMatchers
    with ScalaFutures
    with ActionSteps
    with DefaultAwaitTimeout
    with StubMessagesFactory
    with ResultExtractors
    with HeaderNames
    with Status
    with Results {

  import scala.concurrent.ExecutionContext.Implicits.global

  private implicit val messages: MessagesProvider = stubMessages()

  private def run(block: => Future[Result]) = {
    val result = block
    (status(result), contentAsString(result))
  }

  private val form = Form(Forms.single("f", Forms.nonEmptyText))
  private val json = Json.obj("k1" -> "a", "k2" -> "b")

  "escalate" should {
    "support Future" in {
      run(for {
        a <- Future(Either.cond(false, "a", "b")) -| escalate
      } yield {
        Ok(a.merge)
      }) mustBe (OK, "b")
    }
    "support Either" in {
      run(for {
        a <- Either.cond(true, "a", "b")  -| escalate
        b <- Either.cond(false, "a", "b") -| escalate
      } yield {
        Ok(a.merge + b.merge)
      }) mustBe (OK, "ab")
    }
    "support Option" in {
      run(for {
        a <- Some("a")            -| escalate
        b <- Option.empty[String] -| escalate
      } yield {
        Ok(a.fold("empty")(identity) + b.fold("empty")(identity))
      }) mustBe (OK, "aempty")
    }
  }

  "action steps" when {
    "Future" should {
      "run error handler on failed" in {
        run(for {
          _ <- Future.successful(1)                   ?| BadRequest("successful")
          _ <- Future.failed[Int](new Exception("e")) ?| (e => BadRequest(s"failed ${e.getMessage}"))
        } yield {
          Ok
        }) mustBe (BAD_REQUEST, "failed e")
      }
      "return result on successful" in {
        run(for {
          a <- Future("a") ?| BadRequest
          b <- Future("b") ?| BadRequest
        } yield {
          Ok(a + b)
        }) mustBe (OK, "ab")
      }
    }
    "Boolean" should {
      "run error handler on false" in {
        run(for {
          _ <- true   ?| BadRequest("true")
          _ <- false  ?| BadRequest("false")
        } yield {
          Ok
        }) mustBe (BAD_REQUEST, "false")
      }
      "return result on true" in {
        run(for {
          _ <- true ?| BadRequest
        } yield {
          Ok("true")
        }) mustBe (OK, "true")
      }
    }
    "Future[Boolean]" should {
      "run error handler on false" in {
        run(for {
          _ <- Future(true)  ?| BadRequest("true")
          _ <- Future(false) ?| BadRequest("false")
        } yield {
          Ok
        }) mustBe (BAD_REQUEST, "false")
      }
      "return result on true" in {
        run(for {
          _ <- Future(true) ?| BadRequest
        } yield {
          Ok("true")
        }) mustBe (OK, "true")
      }
    }
    "Option" should {
      "run error handler on None" in {
        run(for {
          _ <- Some(1)            ?| BadRequest("some")
          _ <- Option.empty[Int]  ?> Future(BadRequest("none"))
        } yield {
          Ok
        }) mustBe (BAD_REQUEST, "none")
      }
      "return result on Some" in {
        run(for {
          a <- Some("a") ?| BadRequest
          b <- Some("b") ?| BadRequest
        } yield {
          Ok(a + b)
        }) mustBe (OK, "ab")
      }
    }
    "Future[Option]" should {
      "run error handler on None" in {
        run(for {
          _ <- Future(Some(1))              ?> Future(BadRequest("some"))
          _ <- Future(Option.empty[String]) ?| BadRequest("none")
        } yield {
          Ok
        }) mustBe (BAD_REQUEST, "none")
      }
      "return result on Some" in {
        run(for {
          a <- Future(Some("a")) ?| BadRequest
          b <- Future(Some("b")) ?| BadRequest
        } yield {
          Ok(a + b)
        }) mustBe (OK, "ab")
      }
    }
    "Try" should {
      "run error handler on Failure" in {
        run(for {
          _ <- Try(1 + 1) ?| BadRequest("success")
          _ <- Try(1 / 0) ?| (e => BadRequest(s"failure ${e.getMessage}"))
        } yield {
          Ok
        }) mustBe (BAD_REQUEST, "failure / by zero")
      }
      "return result on Success" in {
        run(for {
          a <- Try("a") ?| BadRequest
          b <- Try("b") ?| BadRequest
        } yield {
          Ok(a + b)
        }) mustBe (OK, "ab")
      }
    }
    "Either" should {
      "run error handler on Left" in {
        run(for {
          _ <- Either.cond(true, 1, "a")  ?| BadRequest("right")
          _ <- Either.cond(false, 2, "b") ?> (e => Future(BadRequest(e)))
        } yield {
          Ok
        }) mustBe (BAD_REQUEST, "b")
      }
      "return result on Right" in {
        run(for {
          a <- Either.cond(true, "a", 1) ?> Future(BadRequest)
          b <- Either.cond(true, "b", 2) ?| BadRequest
        } yield {
          Ok(a + b)
        }) mustBe (OK, "ab")
      }
    }
    "Future[Either]" should {
      "run error handler on Left" in {
        run(for {
          _ <- Future(Either.cond(true, 1, "a"))  ?| BadRequest("right")
          _ <- Future(Either.cond(false, 2, "b")) ?> (e => Future(BadRequest(e)))
        } yield {
          Ok
        }) mustBe (BAD_REQUEST, "b")
      }
      "return result on Right" in {
        run(for {
          a <- Future(Either.cond(true, "a", 1)) ?| BadRequest
          b <- Future(Either.cond(true, "b", 2)) ?| BadRequest
        } yield {
          Ok(a + b)
        }) mustBe (OK, "ab")
      }
    }
    "Form" should {
      "run error handler on form with errors" in {
        run(for {
          _ <- form.bind(Map("f" -> "a")) ?| BadRequest("valid")
          _ <- form.withError("f", "err") ?| (f => BadRequest(f.errorsAsJson))
        } yield {
          Ok
        }) mustBe (BAD_REQUEST, """{"f":["err"]}""")
      }
      "return result on form without errors" in {
        run(for {
          a <- form.bind(Map("f" -> "a")) ?| BadRequest
          b <- form.bind(Map("f" -> "b")) ?| BadRequest
        } yield {
          Ok(a + b)
        }) mustBe (OK, "ab")
      }
    }
    "Future[Form]" should {
      "run error handler on form with errors" in {
        run(for {
          _ <- Future(form.bind(Map("f" -> "a"))) ?| BadRequest("valid")
          _ <- Future(form.withError("f", "err")) ?> (f => Future(BadRequest(f.errorsAsJson)))
        } yield {
          Ok
        }) mustBe (BAD_REQUEST, """{"f":["err"]}""")
      }
      "return result on form without errors" in {
        run(for {
          a <- Future(form.bind(Map("f" -> "a"))) ?| BadRequest
          b <- Future(form.bind(Map("f" -> "b"))) ?| BadRequest
        } yield {
          Ok(a + b)
        }) mustBe (OK, "ab")
      }
    }
    "JsResult" should {
      "run error handler on JsError" in {
        run(for {
          _ <- (json \ "k1").validate[String] ?| BadRequest("string")
          _ <- (json \ "k1").validate[Int]    ?| (e => BadRequest(JsError.toJson(e)))
        } yield {
          Ok
        }) mustBe (BAD_REQUEST, """{"obj":[{"msg":["error.expected.jsnumber"],"args":[]}]}""")
      }
      "return result on JsSuccess" in {
        run(for {
          a <- (json \ "k1").validate[String] ?| BadRequest
          b <- (json \ "k2").validate[String] ?| BadRequest
        } yield {
          Ok(a + b)
        }) mustBe (OK, "ab")
      }
    }
    "Future[JsResult]" should {
      "run error handler on JsError" in {
        run(for {
          _ <- Future((json \ "k1").validate[String]) ?| BadRequest("string")
          _ <- Future((json \ "k1").validate[Int])    ?> (e => Future(BadRequest(JsError.toJson(e))))
        } yield {
          Ok
        }) mustBe (BAD_REQUEST, """{"obj":[{"msg":["error.expected.jsnumber"],"args":[]}]}""")
      }
      "return result on JsSuccess" in {
        run(for {
          a <- Future((json \ "k1").validate[String]) ?| BadRequest
          b <- Future((json \ "k2").validate[String]) ?| BadRequest
        } yield {
          Ok(a + b)
        }) mustBe (OK, "ab")
      }
    }
  }
}
