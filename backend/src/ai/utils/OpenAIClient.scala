package delivery.ai.utils

import cats.effect.{IO, Resource}
import cats.syntax.all.*
import io.circe.*
import io.circe.parser.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.CirceEntityCodec.given
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.headers.{Authorization, `Content-Type`}
import org.typelevel.ci.CIString
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.duration.*

object OpenAIClient:

  private given Logger[IO] = Slf4jLogger.getLogger[IO]

  private val apiKey: Option[String] = sys.env.get("OPENAI_API_KEY")
  private val baseUrl: String = sys.env.get("OPENAI_BASE_URL").getOrElse("https://api.openai.com/v1")
  private val model: String = sys.env.get("OPENAI_MODEL").getOrElse("gpt-4o-mini")

  def configured: IO[Boolean] = IO.pure(apiKey.isDefined)

  def chatCompletion(systemPrompt: String, userMessage: String, maxRetries: Int = 3): IO[Json] =
    apiKey match
      case None =>
        IO.raiseError(new RuntimeException("OPENAI_API_KEY 环境变量未配置，无法调用 AI 服务"))
      case Some(key) =>
        val requestJson = Json.obj(
          "model" -> model.asJson,
          "messages" -> List(
            Json.obj("role" -> "system".asJson, "content" -> systemPrompt.asJson),
            Json.obj("role" -> "user".asJson, "content" -> userMessage.asJson)
          ).asJson,
          "temperature" -> 0.7.asJson,
          "max_tokens" -> 2000.asJson
        )

        def attempt(remaining: Int): IO[Json] =
          val action: IO[Json] =
            EmberClientBuilder.default[IO].withTimeout(30.seconds).build.use { client =>
              for
                req <- IO.pure(
                  Request[IO](Method.POST, Uri.unsafeFromString(s"$baseUrl/chat/completions"))
                    .withHeaders(
                      Authorization(Credentials.Token(AuthScheme.Bearer, key)),
                      `Content-Type`(MediaType.application.json)
                    )
                    .withEntity(requestJson)
                )
                resp <- client.expect[Json](req)
                content <- extractContent(resp)
                parsed <- parseJsonContent(content)
              yield parsed
            }

          IO.race(action, IO.sleep(30.seconds)).flatMap {
            case Left(json) => IO.pure(json)
            case Right(_) => IO.raiseError(new RuntimeException("OpenAI 请求超时"))
          }.handleErrorWith { error =>
            if remaining > 0 then
              Logger[IO].warn(s"OpenAI 调用失败（剩余重试 $remaining 次）：${error.getMessage}") *>
                attempt(remaining - 1)
            else
              IO.raiseError(new RuntimeException(s"AI 服务暂时不可用，请稍后再试：${error.getMessage}"))
          }

        attempt(maxRetries)

  private def extractContent(response: Json): IO[String] =
    IO.fromOption(
      response.hcursor.downField("choices").downArray.downField("message").downField("content").as[String].toOption
    )(new RuntimeException("OpenAI 返回格式异常：无法提取 content"))

  private def parseJsonContent(content: String): IO[Json] =
    val cleaned = content.trim.stripPrefix("```json").stripPrefix("```").stripSuffix("```").trim
    IO.fromEither(parse(cleaned).left.map(e => new RuntimeException(s"AI 返回内容非有效 JSON：${e.getMessage}")))

end OpenAIClient
