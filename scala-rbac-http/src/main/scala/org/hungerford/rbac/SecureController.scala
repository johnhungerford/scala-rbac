package org.hungerford.rbac

import org.hungerford.rbac.exceptions.AuthorizationException
import org.hungerford.rbac.http.exceptions.{AuthenticationException, FailedAuthenticationException, MissingAuthenticationHeaderException}

import scala.util.{Failure, Try}

/**
 * Provides utility methods for managing authentication and authorization in REST
 * controllers.
 *
 * @tparam RequestType Type of request handled by controller
 * @tparam UserType Subtype of [[User]]
 * @example class MyScalatraServlet extends ScalatraServlet with SecureController[ HttpServletRequest ]
 * @example class SecureAbstractController @Inject() ( cc: ControllerComponents ) extends AbstractController( cc ) with SecureController[ Request[ AnyContent ] ]
 */
trait SecureController[ RequestType, UserType <: User ] {

    /**
     * Authenticates a request without returning user or permissions data.
     *
     * @param request [[RequestType]] incoming http request
     * @throws AuthenticationException Provide information about failure instead of returning false
     * @return Boolean whether authentication was successful
     */
    @throws[ AuthenticationException ]
    def authenticate( request : RequestType ) : Boolean =
        throw new FailedAuthenticationException( "Authentication not implemented" )

    /**
     * Authenticates a request, returning authenticated user.
     *
     * @param request [[RequestType]] incoming http request
     * @throws AuthenticationException Provide information about failure instead of returning false
     * @return [[UserType]] authenticated user object
     */
    @throws[ AuthenticationException ]
    def authenticateUser( request : RequestType ) : UserType =
        throw new FailedAuthenticationException( "User authentication not implemented" )

    /**
     * Authenticates a request, returning authenticated roles.
     *
     * @param request [[RequestType]] incoming http request
     * @throws AuthenticationException Provide information about failure instead of returning false
     * @return [[UserType]] authenticated user object
     */
    @throws[ AuthenticationException ]
    def authenticateRole( request : RequestType ) : Role =
        throw new FailedAuthenticationException( "Role authentication not implemented" )

    /**
     * Authenticates a request, returning authenticated permissions.
     *
     * @param request [[RequestType]] incoming http request
     * @throws AuthenticationException Provide information about failure instead of returning false
     * @return [[UserType]] authenticated user object
     */
    @throws[ AuthenticationException ]
    def authenticatePermission( request : RequestType ) : Permission =
        throw new FailedAuthenticationException( "Permission authentication not implemented" )

    /**
     * Methods for handling requests with authentication and authorization.
     *
     * Secures against a given operation [[Secure.operation]].
     *
     * @param operation [[Permissible]] operation to secure against
     * @param request [[RequestType]] incoming http request
     */
    protected[rbac] case class Secure( operation : Permissible, request : RequestType ) {
        /**
         * Authenticates and authorizes request against an [[operation]],
         * executing a function to handle authenticated user.
         *
         * @param handler [[UserType=>T]]
         * @tparam T Return type of handler
         * @throws AuthenticationException if authentication fails
         * @throws AuthorizationException if authenticated user is not permitted to execute [[operation]]
         * @return T
         */
        @throws[ AuthenticationException ]
        @throws[ AuthorizationException ]
        def withUser[ T ]( handler : UserType => T ) : T = {
            implicit val user : UserType = authenticateUser( request )

            operation.secure( handler( user ) )
        }

        /**
         * Authenticates and authorizes request against an [[operation]],
         * and executes a function to handle either authenticated user
         * or exceptions returned by authentication or authorization.
         *
         * @param handler [[Try[Role]=>T]]
         * @tparam T Return type of handler
         * @return T
         */
        def tryWithUser[ T ]( handler : Try[ UserType ] => T ) : T = {
            val userTry = Try( authenticateUser( request ) )

            if ( userTry.isSuccess ) {
                implicit val ps : User = userTry.get

                operation.trySecure { failOpt : Option[Throwable ] =>
                    handler( failOpt match {
                                 case Some( e ) => Failure( e )
                                 case None => userTry
                             } )
                }
            } else handler( userTry )
        }

        /**
         * Authenticates and authorizes request against an [[operation]],
         * executing a function to handle authenticated role.
         *
         * @param handler [[Role=>T]]
         * @tparam T Return type of handler
         * @throws AuthenticationException if authentication fails
         * @throws AuthorizationException if authenticated role is not permitted to execute [[operation]]
         * @return T
         */
        @throws[ AuthenticationException ]
        @throws[ AuthorizationException ]
        def withRole[ T ]( handler : Role => T ) : T = {
            implicit val role : Role = authenticateRole( request )

            operation.secure( handler( role ) )
        }

        /**
         * Authenticates and authorizes request against an [[operation]],
         * and executes a handler to handle either authenticated roles
         * or exceptions returned by authentication or authorization.
         *
         * @param handler [[Try[Role]=>T]]
         * @tparam T Return type of handler
         * @return T
         */
        def tryWithRole[ T ]( handler : Try[ Role ] => T ) : T = {
            val roleTry = Try( authenticateRole( request ) )

            if ( roleTry.isSuccess ) {
                implicit val ps : Role = roleTry.get

                operation.trySecure { failOpt : Option[ Throwable ] =>
                    handler( failOpt match {
                                 case Some( e ) => Failure( e )
                                 case None => roleTry
                             } )
                }
            } else handler( roleTry )
        }

        /**
         * Authenticates and authorizes request against an [[operation]],
         * executing a function to handle authenticated permissions.
         *
         * @param handler [[Permission=>T]]
         * @tparam T Return type of handler
         * @throws AuthenticationException if authentication fails
         * @throws AuthorizationException if authenticated permissions are not permitted to execute [[operation]]
         * @return T
         */
        @throws[ AuthenticationException ]
        @throws[ AuthorizationException ]
        def withPermission[ T ]( handler : Permission => T ) : T = {
            implicit val permission : Permission = authenticatePermission( request )

            operation.secure( handler( permission ) )
        }

        /**
         * Authenticates and authorizes request against an [[operation]],
         * and executes a handler to handle either authenticated permissions
         * or exceptions returned by authentication or authorization.
         *
         * @param handler [[Try[Permission]=>T]]
         * @tparam T Return type of handler
         * @return T
         */
        def tryWithPermission[ T ]( handler : Try[ Permission ] => T ) : T = {
            val permTry = Try( authenticatePermission( request ) )

            if ( permTry.isSuccess ) {
                implicit val ps : Permission = permTry.get

                operation.trySecure { failOpt : Option[ Throwable ] =>
                    handler( failOpt match {
                                 case Some( e ) => Failure( e )
                                 case None => permTry
                             } )
                }
            } else handler( permTry )
        }
    }

    /**
     * Methods for handling requests with authentication.
     *
     * @param request [[RequestType]]
     */
    protected[rbac] case class Authenticate( request : RequestType ) {
        /**
         * Authenticates a request and either executes a given code block or throws
         * an exception.
         *
         * @param block code you want executed
         * @tparam T code block result type
         * @throws AuthenticationException if authentication fails
         * @return
         */
        @throws[ AuthenticationException ]
        def apply[ T ]( block : =>T ) : T =
            if ( authenticate( request ) ) block
            else throw new FailedAuthenticationException( "authentication failed" )

        /**
         * Authenticates a request and executes a given function that can handle
         * auth exceptions if they arise.
         *
         * @param block [[Option[Throwable]=>T]] code you want executed that handles auth exceptions
         * @tparam T code block result type
         * @return
         */
        def tryTo[ T ]( block : Option[ Throwable ] => T ) : T = {
            val failOpt = Try( authenticate( request ) )

            if ( failOpt.isFailure ) block( Some( failOpt.failed.get ) )
            else if ( failOpt.get ) block( None )
            else block( Some( new FailedAuthenticationException( "authentication failed" ) ) )
        }

        /**
         * Authenticates a request and passes retrieved user to a given code block
         * or throws an exception if authentication fails.
         *
         * @param handler [[User=>T]] code you want executed
         * @tparam T code block result type
         * @throws AuthenticationException if authentication fails
         * @return
         */
        @throws[ AuthenticationException ]
        def withUser[ T ]( handler : UserType => T ) : T = {
            implicit val user : UserType = authenticateUser( request )

            handler( user )
        }

        /**
         * Authenticates a request and executes a given code block, passing to it
         * either the authenticated user instance or an exception if authentication
         * fails
         *
         * @param handler [[Try[User]=>T]] code you want executed
         * @tparam T code block result type
         * @return
         */
        def tryWithUser[ T ]( handler : Try[ UserType ] => T ) : T = {
            val userTry = Try( authenticateUser( request ) )

            handler( userTry )
        }

        /**
         * Authenticates a request and passes retrieved role to a given code block
         * or throws an exception if authentication fails.
         *
         * @param handler [[Role=>T]] code you want executed
         * @tparam T code block result type
         * @throws AuthenticationException if authentication fails
         * @return
         */
        @throws[ AuthenticationException ]
        def withRole[ T ]( handler : Role => T ) : T = {
            implicit val role : Role = authenticateRole( request )

            handler( role )
        }

        /**
         * Authenticates a request and executes a given code block, passing to it
         * either the authenticated role instance or an exception if authentication
         * fails
         *
         * @param handler [[Try[Role]=>T]] code you want executed
         * @tparam T code block result type
         * @return
         */
        def tryWithRole[ T ]( handler : Try[ Role ] => T ) : T = {
            val roleTry = Try( authenticateRole( request ) )

            handler( roleTry )
        }

        /**
         * Authenticates a request and passes retrieved permission to a given code block
         * or throws an exception if authentication fails.
         *
         * @param handler [[Permission=>T]] code you want executed
         * @tparam T code block result type
         * @throws AuthenticationException if authentication fails
         * @return
         */
        @throws[ AuthenticationException ]
        def withPermission[ T ]( handler : Permission => T ) : T = {
            implicit val permission : Permission = authenticatePermission( request )

            handler( permission )
        }

        /**
         * Authenticates a request and executes a given code block, passing to it
         * either an authenticated permission instance or an exception if authentication
         * fails
         *
         * @param handler [[Try[Permission]=>T]] code you want executed
         * @tparam T code block result type
         * @return
         */
        def tryWithPermission[ T ]( handler : Try[ Permission ] => T ) : T = {
            val permTry = Try( authenticatePermission( request ) )

            handler( permTry )
        }
    }

}
