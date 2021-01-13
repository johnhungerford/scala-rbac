package org.hungerford.scalarbac.example.controllers

import org.hungerford.rbac.{SecureScalatraServlet, User}
import org.hungerford.scalarbac.example.services.DocumentUserRepository

trait BaseController extends SecureScalatraServlet {
    override val authHeaderKey : String = "Authorization"

    override def authenticateUser( authHeader : String ) : User = DocumentUserRepository.authenticateWebToken( authHeader )

}
