package org.hungerford.scalarbac.example.controllers

import org.hungerford.rbac.exceptions.AuthorizationException
import org.hungerford.rbac.http.exceptions.AuthenticationException
import org.hungerford.rbac.{AllPermissions, PermissionSource, User}
import org.hungerford.scalarbac.example.services.DocumentUserRepository.DocUser
import org.hungerford.scalarbac.example.services.{Document, DocumentCollection, DocumentResource, Root}
import org.scalatra.{Created, Forbidden, InternalServerError, Ok, Unauthorized}

import scala.util.{Failure, Success, Try}

object DocumentResourceController extends BaseController {

    val su : PermissionSource = PermissionSource.fromPermission( AllPermissions )

    Root.+/( "dir1" )( su ).+/( "dir2" )( su ).+/( "dir3" )( su ).+/( "dir4" )( su )

    get( "/**" )( AuthenticateRoute.tryWithUser { userTry : Try[ DocUser ] =>
      userTry map { implicit user =>
          val path = requestPath
          val pathSections = path.split( "/" ).drop( 2 )
          val resource : DocumentResource = pathSections
            .foldLeft( Root.asInstanceOf[ DocumentResource ] )( ( res : DocumentResource, path : String ) => {
                res.asInstanceOf[ DocumentCollection[ _ ] ]( path )
            } )
          resource match {
              case coll : DocumentCollection[ _ ] => Ok( coll.children.mkString( "\n" ) )
              case doc : Document[ _ ] => Ok( doc.content.toString )
          }
      } match {
          case Success( res ) => res
          case Failure( e : AuthenticationException ) => Unauthorized( e.getMessage )
          case Failure( e : AuthorizationException ) => Forbidden( e.getMessage )
          case Failure( e ) => InternalServerError( e.getMessage )
      }
    } )

    post( "/**" )( AuthenticateRoute.tryWithUser { userTry : Try[ DocUser ] =>
      userTry map { implicit user =>
          val path = requestPath
          val pathSections = path.split( "/" ).map( _.trim ).drop( 2 )
          val lastSection = pathSections.last
          val firstSections = pathSections.take( pathSections.length - 1 )
          val directories : DocumentResource = firstSections
            .foldLeft( Root.asInstanceOf[ DocumentResource ] )( ( res : DocumentResource, pathSect : String ) => {
                res.asInstanceOf[ DocumentCollection[ _ ] ] +/ pathSect
            } )
          val body = request.body
          if ( body.isEmpty ) {
              Created( ( directories.asInstanceOf[ DocumentCollection[ Any ] ] +/ lastSection ).children.mkString( "\n" ) + "\n" )
          } else {
              Created( ( directories.asInstanceOf[ DocumentCollection[ Any ] ] +>[ String ](lastSection, body) ) + "\n" )
          }
      } match {
          case Success( res ) => res
          case Failure( e : AuthenticationException ) => Unauthorized( e.getMessage )
          case Failure( e : AuthorizationException ) => Forbidden( e.getMessage )
          case Failure( e ) => InternalServerError( e.getMessage )
      }
    } )
}
