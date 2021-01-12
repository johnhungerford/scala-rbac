package org.hungerford.rbac.scalatra

import org.hungerford.rbac.scalatra.exceptions.{FailedAuthenticationException, MissingAuthenticationHeaderException}
import org.hungerford.rbac.{Permissible, Permission, Role, User}
import org.scalatra.ScalatraServlet

trait SecuredController extends ScalatraServlet {

    val authHeaderKey : String

    def authenticate( authHeader : String ) : Boolean =
        throw new FailedAuthenticationException( "Authentication not implemented" )

    def authenticate : Boolean =
        authenticate(
            request.header( authHeaderKey )
              .getOrElse( throw new MissingAuthenticationHeaderException( authHeaderKey ) )
        )

    def authenticateUser( authHeader : String ) : User =
        throw new FailedAuthenticationException( "User authentication not implemented" )

    def authenticateUser : User = {
        authenticateUser(
            request.header( authHeaderKey )
              .getOrElse( throw new MissingAuthenticationHeaderException( authHeaderKey ) )
        )
    }

    def authenticateRole( authHeader : String ) : Role =
        throw new FailedAuthenticationException( "Role authentication not implemented" )

    def authenticateRole : Role = {
        authenticateRole(
            request.header( authHeaderKey )
              .getOrElse( throw new MissingAuthenticationHeaderException( authHeaderKey ) )
        )
    }

    def authenticatePermission( authHeader : String ) : Permission =
        throw new FailedAuthenticationException( "Permission authentication not implemented" )

    def authenticatePermission : Permission = {
        authenticatePermission(
            request.header( authHeaderKey )
              .getOrElse( throw new MissingAuthenticationHeaderException( authHeaderKey ) )
        )
    }

    object Secure {
        def apply( operation : Permissible ) : Secured = new Secured( operation )

        class Secured( operation : Permissible ) {
            def withUser[ T ]( handler : User => T ) : T = {
                implicit val user : User = authenticateUser

                operation.secure( handler( user ) )
            }

            def withRole[ T ]( handler : Role => T ) : T = {
                implicit val role : Role = authenticateRole

                operation.secure( handler( role ) )
            }

            def withPermission[ T ]( handler : Permission => T ) : T = {
                implicit val permission : Permission = authenticatePermission

                operation.secure( handler( permission ) )
            }
        }
    }

    object Authenticate {
        def apply[ T ]( block : =>T ) : T =
            if ( authenticate ) block
            else throw new FailedAuthenticationException( "authentication failed" )

        def withUser[ T ]( handler : User => T ) : T = {
            implicit val user : User = authenticateUser

            handler( user )
        }

        def withRole[ T ]( handler : Role => T ) : T = {
            implicit val role : Role = authenticateRole

            handler( role )
        }

        def withPermission[ T ]( handler : Permission => T ) : T = {
            implicit val permission : Permission = authenticatePermission

            handler( permission )
        }
    }

}
