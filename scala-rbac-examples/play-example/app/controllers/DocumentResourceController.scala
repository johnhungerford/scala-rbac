package controllers

import org.hungerford.rbac.exceptions.AuthorizationException
import org.hungerford.rbac.http.exceptions.AuthenticationException
import org.hungerford.rbac.{AllPermissions, PermissionSource, User}
import org.hungerford.scalarbac.example.services.{Document, DocumentCollection, DocumentResource, DocumentRoot, Root}
import play.api.mvc.{Action, AnyContent, ControllerComponents, Request}

import javax.inject.{Inject, Singleton}
import scala.util.{Failure, Success, Try}

@Singleton
class DocumentResourceController @Inject() ( cc : ControllerComponents ) extends BaseController( cc ) {

    val su : PermissionSource = PermissionSource.fromPermission( AllPermissions )

    Root.+/( "dir1" )( su ).+/( "dir2" )( su ).+/( "dir3" )( su ).+/( "dir4" )( su )

    def getResource : Action[ AnyContent ] = AuthenticateAction.tryWithUser { ( req : Request[ AnyContent ], userTry : Try[ User ] ) =>
        userTry map { implicit user =>
            val path = req.path
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

    }

    def postResource : Action[ AnyContent ] = AuthenticateAction.tryWithUser { ( req : Request[ AnyContent ], userTry : Try[ User ] ) =>
        userTry map { implicit user =>
            val path = req.path
            val pathSections = path.split( "/" ).map( _.trim ).drop( 2 )
            val lastSection = pathSections.last
            val firstSections = pathSections.take( pathSections.length - 1 )
            val directories : DocumentResource = firstSections
              .foldLeft( Root.asInstanceOf[ DocumentResource ] )( ( res : DocumentResource, pathSect : String ) => {
                  res.asInstanceOf[ DocumentCollection[ _ ] ] +/ pathSect
              } )
            val body = req.body.asText

            body match {
                case None =>
                    Created( ( directories.asInstanceOf[ DocumentCollection[ Any ] ] +/ lastSection ).children.mkString( "\n" ) + "\n" )
                case Some( "" ) =>
                    Created( ( directories.asInstanceOf[ DocumentCollection[ Any ] ] +/ lastSection ).children.mkString( "\n" ) + "\n" )
                case Some( text ) =>
                    Created( ( directories.asInstanceOf[ DocumentCollection[ Any ] ] +>[ String ](lastSection, text) ) + "\n" )
            }
        } match {
            case Success( res ) => res
            case Failure( e : AuthenticationException ) => Unauthorized( e.getMessage )
            case Failure( e : AuthorizationException ) => Forbidden( e.getMessage )
            case Failure( e ) => InternalServerError( e.getMessage )
        }
    }
}
