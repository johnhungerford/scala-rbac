package handlers

import play.api.http.HttpErrorHandler
import play.api.mvc.Results._
import play.api.mvc.{RequestHeader, Result}
import javax.inject.Singleton

import scala.concurrent.Future

@Singleton
class DocumentErrorHandler extends HttpErrorHandler {
    def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
        Future.successful(
            Status(statusCode)("A client error occurred: " + message)
        )
    }

    def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
        Future.successful(
            InternalServerError(exception.getMessage)
        )
    }
}
