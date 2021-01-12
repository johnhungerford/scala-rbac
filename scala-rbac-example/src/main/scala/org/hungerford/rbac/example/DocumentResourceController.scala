package org.hungerford.rbac.example

import org.hungerford.rbac._
import org.hungerford.rbac.example.DocumentUserRepository._
import org.hungerford.rbac.scalatra.SecuredController
import org.scalatra.{Created, Ok}

import scala.util.{Failure, Success, Try}

object DocumentResourceController extends SecuredController {
    override val authHeaderKey : String = "Authorization"

    val root = new DocumentRoot( Set() )

    val su : PermissionSource = PermissionSource.fromPermission( AllPermissions )

    root.+/("dir1")(su).+/("dir2")(su).+/("dir3")(su).+/("dir4")(su)

    def parseRole( role : String )( implicit ps : PermissionSource ) : Role = {
        val rolePattern = """([^:]+):(.+)""".r
        role.trim match {
            case rolePattern( roleType, roleString ) =>
                roleType.trim.toLowerCase match {
                    case "user" => parseUserRole( roleString )
                    case "user+" => parseUserRole( roleString, reflexive = true )
                    case "doc" => parseDocRole( roleString )
                }
        }
    }

    def parseUserRole( role : String, reflexive : Boolean = false )( implicit ps : PermissionSource ) : Role = {
        val rolePattern = """([^;]+);(.+)""".r
        role.trim match {
            case rolePattern( ops : String, rls : String ) =>
                val operations : Array[ RoleOperation ] = ops.split( "," ).map( _.trim.toLowerCase ).collect {
                    case "read" => Array( GetUser.asInstanceOf[ RoleOperation ] )
                    case "r" => Array( GetUser.asInstanceOf[ RoleOperation ] )
                    case "write" => Array( GrantUser.asInstanceOf[ RoleOperation ], AddUser.asInstanceOf[ RoleOperation ], RemoveUser.asInstanceOf[ RoleOperation ] )
                    case "w" => Array( GrantUser.asInstanceOf[ RoleOperation ], AddUser.asInstanceOf[ RoleOperation ], RemoveUser.asInstanceOf[ RoleOperation ] )
                } flatten
                val roles : Role = Roles( rls.split( '|' ).map( r => parseRole( r ) ) )
                if ( reflexive ) ReflexiveRoleManagementRole( roles, operations.toSet )
                else RoleManagementRole( roles, operations.toSet )
        }
    }

    def parseDocRole( role : String )( implicit ps : PermissionSource ) : Role = {
        val rolePattern = """([^;]+);\s*/(.*)""".r
        role.trim match {
            case rolePattern( ops, path ) =>
                val operations = ops.split( "," ).map( _.trim.toLowerCase ).map {
                    case "read" => Read
                    case "r" => Read
                    case "write" => Write
                    case "w" => Write
                    case "execute" => Execute
                    case "x" => Execute
                    case _ => throw new Exception
                }
                val resource : DocumentResource = path.split( "/" ).map( _.trim )
                  .foldLeft( root.asInstanceOf[ DocumentResource ] )( ( res : DocumentResource, pathSection : String ) => {
                      pathSection match {
                          case "" => res
                          case _ => res.asInstanceOf[ DocumentCollection[ _ ] ]( pathSection )
                      }
                  } )
                DocumentAccessRole( resource, operations.toSet )
        }
    }

    DocumentUserRepository.addUser( "root", SuperUserRole )( PermissionSource.fromPermission( AllPermissions ) )

    override def authenticateUser( authHeader : String ) : User = DocumentUserRepository.authenticateWebToken( authHeader )

    get( "/users/:id" )( Authenticate.withUser { implicit user : User =>
        val userId : String = params( "id" )
        val retreivedUser = DocumentUserRepository.getUser( userId )
        Ok( s"Name: ${retreivedUser.name}\nRoles: ${retreivedUser.roles}\n" )
    } )

    get( "/users" )( Authenticate.withUser { implicit user : User =>
        DocumentUserRepository.getUsers.map( _.name ).mkString( "\n" )
    } )

    post( "/users/:id" )( Authenticate.withUser { implicit user : User =>
        val userId : String = params( "id" )
        Try( DocumentUserRepository.getUser( userId ) ) match {
            case Success( _ ) => throw new Exception
            case Failure( _ ) =>
        }
        val roles : Role = Roles( request.body.split( '\n' ).map( l => parseRole( l ) ) )
        val newUser = DocumentUserRepository.addUser( userId, roles )
        Created( s"Name: ${newUser.name}\nRoles: ${newUser.roles}\n" )
    } )

    get( "/resources/**" )( Authenticate.withUser { implicit user : User =>
        val path = requestPath
        val pathSections = path.split( "/" ).drop( 2 )
        val resource : DocumentResource = pathSections
          .foldLeft( root.asInstanceOf[ DocumentResource ] )( ( res : DocumentResource, path : String ) => {
              res.asInstanceOf[ DocumentCollection[ _ ] ]( path )
          } )
        resource match {
            case coll : DocumentCollection[ _ ] => Ok( coll.children.mkString( "\n" ) )
            case doc : Document[ Any ] => Ok( doc.content.toString )
        }
    } )

    post( "/resources/**" )( Authenticate.withUser { implicit user : User =>
        val path = requestPath
        val pathSections = path.split( "/" ).map( _.trim ).drop( 2 )
        val lastSection = pathSections.last
        val firstSections = pathSections.take( pathSections.length - 1 )
        val directories : DocumentResource = firstSections
          .foldLeft( root.asInstanceOf[ DocumentResource ] )( ( res : DocumentResource, pathSect : String ) => {
              res.asInstanceOf[ DocumentCollection[ _ ] ] +/ pathSect
          } )
        val body = request.body
        if ( body.isEmpty ) {
            Created( ( directories.asInstanceOf[ DocumentCollection[ Any ] ] +/ lastSection ).children.mkString( "\n" ) + "\n" )
        } else {
            Created( ( directories.asInstanceOf[ DocumentCollection[ Any ] ] +>[ String ] ( lastSection, body ) ) + "\n" )
        }
    } )
}


