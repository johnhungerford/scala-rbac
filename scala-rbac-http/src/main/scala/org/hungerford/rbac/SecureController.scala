package org.hungerford.rbac

import org.hungerford.rbac.http.exceptions.{FailedAuthenticationException, MissingAuthenticationHeaderException}

trait SecureController[ RequestType ] {

    def authenticate( request : RequestType ) : Boolean =
        throw new FailedAuthenticationException( "Authentication not implemented" )

    def authenticateUser( request : RequestType ) : User =
        throw new FailedAuthenticationException( "User authentication not implemented" )

    def authenticateRole( request : RequestType ) : Role =
        throw new FailedAuthenticationException( "Role authentication not implemented" )

    def authenticatePermission( request : RequestType ) : Permission =
        throw new FailedAuthenticationException( "Permission authentication not implemented" )

    protected[rbac] case class Secure( operation : Permissible, request : RequestType ) {
        def withUser[ T ]( handler : User => T ) : T = {
            implicit val user : User = authenticateUser( request )

            operation.secure( handler( user ) )
        }

        def withRole[ T ]( handler : Role => T ) : T = {
            implicit val role : Role = authenticateRole( request )

            operation.secure( handler( role ) )
        }

        def withPermission[ T ]( handler : Permission => T ) : T = {
            implicit val permission : Permission = authenticatePermission( request )

            operation.secure( handler( permission ) )
        }
    }

    protected[rbac] case class Authenticate( request : RequestType ) {
        def apply[ T ]( block : =>T ) : T =
            if ( authenticate( request ) ) block
            else throw new FailedAuthenticationException( "authentication failed" )

        def withUser[ T ]( handler : User => T ) : T = {
            implicit val user : User = authenticateUser( request )

            handler( user )
        }

        def withRole[ T ]( handler : Role => T ) : T = {
            implicit val role : Role = authenticateRole( request )

            handler( role )
        }

        def withPermission[ T ]( handler : Permission => T ) : T = {
            implicit val permission : Permission = authenticatePermission( request )

            handler( permission )
        }
    }

}
