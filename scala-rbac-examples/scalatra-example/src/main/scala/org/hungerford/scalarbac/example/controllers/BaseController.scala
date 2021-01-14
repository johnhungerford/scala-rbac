package org.hungerford.scalarbac.example.controllers

import org.hungerford.rbac.http.exceptions.AuthenticationException
import org.hungerford.rbac.{SecureScalatraServlet}
import org.hungerford.scalarbac.example.services.DocumentUserRepository
import org.hungerford.scalarbac.example.services.DocumentUserRepository.DocUser

import scala.util.Try

trait BaseController extends SecureScalatraServlet[ DocUser ] {
    override val authHeaderKey : String = "Authorization"

    override def authenticateUser( authHeader : String ) : DocUser = Try( DocumentUserRepository.authenticateWebToken( authHeader ) )
      .getOrElse( throw new AuthenticationException( s"User $authHeader does not exist" ) )
}
