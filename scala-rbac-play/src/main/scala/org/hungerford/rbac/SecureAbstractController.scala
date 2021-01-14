package org.hungerford.rbac

import org.hungerford.rbac.http.exceptions.{FailedAuthenticationException, MissingAuthenticationHeaderException}
import play.api.mvc._

import scala.util.Try

abstract class SecureAbstractController[ UserType <: User ]( cc: ControllerComponents ) extends AbstractController( cc ) with SecureController[ Request[ _ ], UserType ] {
    val authHeaderKey : String

    def authenticate( authHeader : String ) : Boolean =
        throw new FailedAuthenticationException( "Authentication not implemented" )

    override def authenticate( req: Request[ _ ] ) : Boolean =
        authenticate(
            req.headers.get( authHeaderKey )
              .getOrElse( throw new MissingAuthenticationHeaderException( authHeaderKey ) )
        )

    def authenticateUser( authHeader : String ) : UserType =
        throw new FailedAuthenticationException( "User authentication not implemented" )

    override def authenticateUser( req: Request[ _ ] ) : UserType = {
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
        def withUser( block : (Request[ AnyContent ], UserType) => Result ) : Action[ AnyContent ] = Action {
            req : Request[ AnyContent ] =>
            Secure( permissible, req ).withUser( user => block( req, user ) )
        }

        def tryWithUser( block : (Request[ AnyContent ], Try[ UserType ]) => Result ) : Action[ AnyContent ] = Action {
            req : Request[ AnyContent ] =>
                Secure( permissible, req ).tryWithUser( user => block( req, user ) )
        }

        def withRole( block : (Request[ AnyContent ], Role) => Result ) : Action[ AnyContent ] = Action {
            req : Request[ AnyContent ] =>
                Secure( permissible, req ).withRole( role => block( req, role ) )
        }

        def tryWithRole( block : (Request[ AnyContent ], Try[ Role ]) => Result ) : Action[ AnyContent ] = Action {
            req : Request[ AnyContent ] =>
                Secure( permissible, req ).tryWithRole( role => block( req, role ) )
        }

        def withPermission( block : (Request[ AnyContent ], Permission) => Result ) : Action[ AnyContent ] = Action {
            req : Request[ AnyContent ] =>
                Secure( permissible, req ).withPermission( perm => block( req, perm ) )
        }

        def tryWithPermission( block : (Request[ AnyContent ], Try[ Permission ]) => Result ) : Action[ AnyContent ] = Action {
            req : Request[ AnyContent ] =>
                Secure( permissible, req ).tryWithPermission( perm => block( req, perm ) )
        }
    }

    object AuthenticateAction {
        def apply( block : Request[ AnyContent ] => Result ) : Action[ AnyContent ] = Action {
            req : Request[AnyContent ] =>
                Authenticate( req )( block( req ) )
        }

        def tryTo( block : (Request[ AnyContent ], Option[ Throwable ]) => Result ) : Action[ AnyContent ] = Action {
            req : Request[AnyContent ] =>
                Authenticate( req ).tryTo( failOpt => block( req, failOpt ) )
        }

        def withUser( block : (Request[ AnyContent ], UserType) => Result ) : Action[ AnyContent ] = Action {
            req : Request[ AnyContent ] =>
                Authenticate( req ).withUser( user => block( req, user ) )
        }

        def tryWithUser( block : (Request[ AnyContent ], Try[ UserType ]) => Result ) : Action[ AnyContent ] = Action {
            req : Request[ AnyContent ] =>
                Authenticate( req ).tryWithUser( user => block( req, user ) )
        }

        def withRole( block : (Request[ AnyContent ], Role) => Result ) : Action[ AnyContent ] = Action {
            req : Request[ AnyContent ] =>
                Authenticate( req ).withRole( role => block( req, role ) )
        }

        def tryWithRole( block : (Request[ AnyContent ], Try[ Role ]) => Result ) : Action[ AnyContent ] = Action {
            req : Request[ AnyContent ] =>
                Authenticate( req ).tryWithRole( role => block( req, role ) )
        }

        def withPermission( block : (Request[ AnyContent ], Permission) => Result ) : Action[ AnyContent ] = Action {
            req : Request[ AnyContent ] =>
                Authenticate( req ).withPermission( perm => block( req, perm ) )
        }

        def tryWithPermission( block : (Request[ AnyContent ], Try[ Permission ]) => Result ) : Action[ AnyContent ] = Action {
            req : Request[ AnyContent ] =>
                Authenticate( req ).tryWithPermission( perm => block( req, perm ) )
        }
    }

}


