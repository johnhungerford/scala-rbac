package controllers

import org.hungerford.rbac.{SecureAbstractController, User}
import org.hungerford.scalarbac.example.services.DocumentUserRepository
import play.api.mvc.ControllerComponents

abstract class BaseController( cc : ControllerComponents ) extends SecureAbstractController( cc : ControllerComponents ) {
    override val authHeaderKey : String = "Authorization"

    override def authenticateUser( authHeader : String ) : User = DocumentUserRepository.authenticateWebToken( authHeader )
}
