package org.hungerford.scalarbac.example.controllers

import org.hungerford.rbac.exceptions.AuthorizationException
import org.hungerford.rbac.http.exceptions.AuthenticationException
import org.hungerford.rbac.{AllPermissions, PermissionSource, Role, SuperUserRole, User}
import org.hungerford.scalarbac.example.services.DocumentUserRepository
import org.hungerford.scalarbac.example.services.DocumentUserRepository.DocUser
import org.hungerford.scalarbac.example.services.RequestParser.parseRole
import org.scalatra.{Created, Forbidden, InternalServerError, Ok, Unauthorized}

import scala.util.{Failure, Success, Try}

object DocumentUserController extends BaseController {
    val su : PermissionSource = PermissionSource.fromPermission( AllPermissions )

    DocumentUserRepository.addUser( "root", SuperUserRole )( PermissionSource.fromPermission( AllPermissions ) )

    get( "/" )( AuthenticateRoute.tryWithUser { userTry : Try[ DocUser ] =>
        userTry map { implicit user =>
            DocumentUserRepository.getUsers.map( _.name ).mkString( "\n" )
        } match {
            case Success( res ) => res
            case Failure( e : AuthenticationException ) => Unauthorized( e.getMessage )
            case Failure( e : AuthorizationException ) => Forbidden( e.getMessage )
            case Failure( e ) => InternalServerError( e.getMessage )
        }
    } )

    get( "/:id" )( AuthenticateRoute.tryWithUser { userTry : Try[ DocUser ] =>
        userTry map { implicit user =>
            val userId : String = params( "id" )
            val retreivedUser = DocumentUserRepository.getUser( userId )
            Ok( s"Name: ${retreivedUser.name}\nRoles: ${retreivedUser.roles}\n" )
        } match {
            case Success( res ) => res
            case Failure( e : AuthenticationException ) => Unauthorized( e.getMessage )
            case Failure( e : AuthorizationException ) => Forbidden( e.getMessage )
            case Failure( e ) => InternalServerError( e.getMessage )
        }
    } )

    post( "/:id" )( AuthenticateRoute.tryWithUser { userTry : Try[ DocUser ] =>
        userTry map { implicit user =>
            val userId : String = params( "id" )
            Try( DocumentUserRepository.getUser( userId ) ) match {
                case Success( _ ) => throw new Exception
                case Failure( _ ) =>
            }
            val roles : Role = request.body.split( '\n' ).map( l => parseRole( l ) ).reduce( _ + _ )
            val newUser = DocumentUserRepository.addUser( userId, roles )
            Created( s"Name: ${newUser.name}\nRoles: ${newUser.roles}\n" )
        } match {
            case Success( res ) => res
            case Failure( e : AuthenticationException ) => Unauthorized( e.getMessage )
            case Failure( e : AuthorizationException ) => Forbidden( e.getMessage )
            case Failure( e ) => InternalServerError( e.getMessage )
        }
    } )
}
