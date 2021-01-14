package controllers

;

import org.hungerford.rbac.exceptions.AuthorizationException
import org.hungerford.rbac.http.exceptions.AuthenticationException
import org.hungerford.rbac.{AllPermissions, PermissionSource, Role, SuperUserRole, User}
import org.hungerford.scalarbac.example.services.DocumentUserRepository
import org.hungerford.scalarbac.example.services.RequestParser.parseRole
import play.api.mvc._

import javax.inject._
import scala.util.{Failure, Success, Try}

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class DocumentUserController @Inject()( cc : ControllerComponents ) extends BaseController( cc ) {
    val su : PermissionSource = PermissionSource.fromPermission( AllPermissions )

    DocumentUserRepository.addUser( "root", SuperUserRole )( PermissionSource.fromPermission( AllPermissions ) )

    def getUsers : Action[ AnyContent ] = AuthenticateAction.tryWithUser { ( _, userTry ) =>
        userTry map { implicit user =>
            Ok( DocumentUserRepository.getUsers.map( _.name ).mkString( "\n" ) )
        } match {
            case Success( res ) => res
            case Failure( e : AuthenticationException ) => Unauthorized( e.getMessage )
            case Failure( e : AuthorizationException ) => Forbidden( e.getMessage )
            case Failure( e ) => InternalServerError( e.getMessage )
        }
    }

    def getUser( userId : String ) : Action[ AnyContent ] = AuthenticateAction.tryWithUser { ( _, userTry ) =>
        userTry map { implicit user =>
            val retreivedUser = DocumentUserRepository.getUser( userId )
            Ok( s"Name: ${retreivedUser.name}\nRoles: ${retreivedUser.roles}\n" )
        } match {
            case Success( res ) => res
            case Failure( e : AuthenticationException ) => Unauthorized( e.getMessage )
            case Failure( e : AuthorizationException ) => Forbidden( e.getMessage )
            case Failure( e ) => InternalServerError( e.getMessage )
        }
    }

    def postUser( userId : String ) : Action[ AnyContent ] = AuthenticateAction.tryWithUser { ( req, userTry ) =>
        userTry map { implicit user : User =>
            Try( DocumentUserRepository.getUser( userId ) ) match {
                case Success( _ ) => throw new Exception( s"User $userId already exists" )
                case Failure( _ ) =>
            }
            val roles : Role = req.body.asText.getOrElse( "" ).split( '\n' ).map( l => parseRole( l ) ).reduce( _ + _ )
            val newUser = DocumentUserRepository.addUser( userId, roles )
            Created( s"Name: ${newUser.name}\nRoles: ${newUser.roles}\n" )
        } match {
            case Success( res ) => res
            case Failure( e : AuthenticationException ) => Unauthorized( e.getMessage )
            case Failure( e : AuthorizationException ) => Forbidden( e.getMessage )
            case Failure( e ) => InternalServerError( e.getMessage )
        }
    }

}
