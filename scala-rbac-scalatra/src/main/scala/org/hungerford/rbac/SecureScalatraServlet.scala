package org.hungerford.rbac

import org.hungerford.rbac.http.exceptions.{FailedAuthenticationException, MissingAuthenticationHeaderException}
import org.scalatra.ScalatraServlet

import javax.servlet.http.HttpServletRequest

trait SecureScalatraServlet[ UserType <: User ] extends ScalatraServlet with SecureController[ HttpServletRequest, UserType ] {

    val authHeaderKey : String

    def authenticate( authHeader : String ) : Boolean =
        throw new FailedAuthenticationException( "Authentication not implemented" )

    override def authenticate( req: HttpServletRequest ) : Boolean =
        authenticate(
            req.header( authHeaderKey )
              .getOrElse( throw new MissingAuthenticationHeaderException( authHeaderKey ) )
        )

    def authenticateUser( authHeader : String ) : UserType =
        throw new FailedAuthenticationException( "User authentication not implemented" )

    override def authenticateUser( req: HttpServletRequest ) : UserType = {
        authenticateUser(
            request.header( authHeaderKey )
              .getOrElse( throw new MissingAuthenticationHeaderException( authHeaderKey ) )
        )
    }

    def authenticateRole( authHeader : String ) : Role =
        throw new FailedAuthenticationException( "Role authentication not implemented" )

    override def authenticateRole( req: HttpServletRequest ) : Role = {
        authenticateRole(
            request.header( authHeaderKey )
              .getOrElse( throw new MissingAuthenticationHeaderException( authHeaderKey ) )
        )
    }

    def authenticatePermission( authHeader : String ) : Permission =
        throw new FailedAuthenticationException( "Permission authentication not implemented" )

    override def authenticatePermission( req: HttpServletRequest ) : Permission = {
        authenticatePermission(
            request.header( authHeaderKey )
              .getOrElse( throw new MissingAuthenticationHeaderException( authHeaderKey ) )
        )
    }

    def SecureRoute( permissible: Permissible ) : Secure = Secure( permissible, request )

    def AuthenticateRoute : Authenticate = Authenticate( request )
}
