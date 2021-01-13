package org.hungerford.scalarbac.example.controllers

import org.hungerford.rbac.{AllPermissions, PermissionSource, Role, SecureScalatraServlet, SuperUserRole, User}
import org.hungerford.scalarbac.example.services.RequestParser.parseRole
import org.hungerford.scalarbac.example.services.{DocumentRoot, DocumentUserRepository}
import org.scalatra.{Created, Ok}

import scala.util.{Failure, Success, Try}

object DocumentUserController extends BaseController {
    val su : PermissionSource = PermissionSource.fromPermission( AllPermissions )

    DocumentUserRepository.addUser( "root", SuperUserRole )( PermissionSource.fromPermission( AllPermissions ) )

    get( "/" )( AuthenticateRoute.withUser { implicit user : User =>
        DocumentUserRepository.getUsers.map( _.name ).mkString( "\n" )
    } )

    get( "/:id" )( AuthenticateRoute.withUser { implicit user : User =>
        val userId : String = params( "id" )
        val retreivedUser = DocumentUserRepository.getUser( userId )
        Ok( s"Name: ${retreivedUser.name}\nRoles: ${retreivedUser.roles}\n" )
    } )

    post( "/:id" )( AuthenticateRoute.withUser { implicit user : User =>
        val userId : String = params( "id" )
        Try( DocumentUserRepository.getUser( userId ) ) match {
            case Success( _ ) => throw new Exception
            case Failure( _ ) =>
        }
        val roles : Role = request.body.split( '\n' ).map( l => parseRole( l ) ).reduce( _ + _ )
        val newUser = DocumentUserRepository.addUser( userId, roles )
        Created( s"Name: ${newUser.name}\nRoles: ${newUser.roles}\n" )
    } )
}
