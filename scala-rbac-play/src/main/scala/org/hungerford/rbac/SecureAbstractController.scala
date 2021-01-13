package org.hungerford.rbac

import org.hungerford.rbac.http.exceptions.{FailedAuthenticationException, MissingAuthenticationHeaderException}
import play.api.mvc._

abstract class SecureAbstractController( cc: ControllerComponents ) extends AbstractController( cc ) with SecureController[ Request[ _ ] ] {
    val authHeaderKey : String

    def authenticate( authHeader : String ) : Boolean =
        throw new FailedAuthenticationException( "Authentication not implemented" )

    override def authenticate( req: Request[ _ ] ) : Boolean =
        authenticate(
            req.headers.get( authHeaderKey )
              .getOrElse( throw new MissingAuthenticationHeaderException( authHeaderKey ) )
        )

    def authenticateUser( authHeader : String ) : User =
        throw new FailedAuthenticationException( "User authentication not implemented" )

    override def authenticateUser( req: Request[ _ ] ) : User = {
        authenticateUser (
            req.headers.get( authHeaderKey )
              .getOrElse( throw new MissingAuthenticationHeaderException( authHeaderKey ) )
        )
    }

    def authenticateRole( authHeader : String ) : Role =
        throw new FailedAuthenticationException( "Role authentication not implemented" )

    override def authenticateRole( req: Request[ _ ] ) : Role = {
        authenticateRole(
            req.headers.get( authHeaderKey )
              .getOrElse( throw new MissingAuthenticationHeaderException( authHeaderKey ) )
        )
    }

    def authenticatePermission( authHeader : String ) : Permission =
        throw new FailedAuthenticationException( "Permission authentication not implemented" )

    override def authenticatePermission( req: Request[ _ ] ) : Permission = {
        authenticatePermission(
            req.headers.get( authHeaderKey )
              .getOrElse( throw new MissingAuthenticationHeaderException( authHeaderKey ) )
        )
    }

    case class SecureAction( permissible: Permissible ) {
        def withUser( block : (Request[ AnyContent ], User) => Result ) : Action[ AnyContent ] = Action {
            req : Request[ AnyContent ] =>
            Secure( permissible, req ).withUser( user => block( req, user ) )
        }

        def withRole( block : (Request[ AnyContent ], Role) => Result ) : Action[ AnyContent ] = Action {
            req : Request[ AnyContent ] =>
                Secure( permissible, req ).withRole( role => block( req, role ) )
        }

        def withPermission( block : (Request[ AnyContent ], Permission) => Result ) : Action[ AnyContent ] = Action {
            req : Request[ AnyContent ] =>
                Secure( permissible, req ).withPermission( perm => block( req, perm ) )
        }
    }

    object AuthenticateAction {
        def apply( block : Request[ AnyContent ] => Result ) : Action[ AnyContent ] = Action {
            req : Request[AnyContent ] =>
                Authenticate( req )( block( req ) )
        }

        def withUser( block : (Request[ AnyContent ], User) => Result ) : Action[ AnyContent ] = Action {
            req : Request[ AnyContent ] =>
                Authenticate( req ).withUser( user => block( req, user ) )
        }

        def withRole( block : (Request[ AnyContent ], Role) => Result ) : Action[ AnyContent ] = Action {
            req : Request[ AnyContent ] =>
                Authenticate( req ).withRole( role => block( req, role ) )
        }

        def withPermission( block : (Request[ AnyContent ], Permission) => Result ) : Action[ AnyContent ] = Action {
            req : Request[ AnyContent ] =>
                Authenticate( req ).withPermission( perm => block( req, perm ) )
        }
    }

}


