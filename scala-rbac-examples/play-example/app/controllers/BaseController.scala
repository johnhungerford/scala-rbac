package controllers

import org.hungerford.rbac.http.exceptions.AuthenticationException
import org.hungerford.rbac.{SecureAbstractController, User}
import org.hungerford.scalarbac.example.services.DocumentUserRepository
import org.hungerford.scalarbac.example.services.DocumentUserRepository.DocUser
import play.api.mvc.ControllerComponents

import scala.util.Try

abstract class BaseController( cc : ControllerComponents ) extends SecureAbstractController[ DocUser ]( cc : ControllerComponents ) {
    override val authHeaderKey : String = "Authorization"

    override def authenticateUser( authHeader : String ) : DocUser = Try( DocumentUserRepository.authenticateWebToken( authHeader ) )
      .getOrElse( throw new AuthenticationException( s"User $authHeader does not exist" ) )
}
