package org.hungerford.scalarbac.example.services

import org.hungerford.rbac.{PermissionSource, RecursiveRoleManagementRole, Role, RoleManagementRole, RoleOperation}
import org.hungerford.scalarbac.example.services.DocumentUserRepository.{AddUser, GetUser, GrantUser, RemoveUser}

object RequestParser {

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
                val roles : Role = Role.join( rls.split( '|' ).map( r => parseRole( r ) ) )
                if ( reflexive ) RecursiveRoleManagementRole( roles, operations.toSet )
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
                  .foldLeft( Root.asInstanceOf[ DocumentResource ] )( ( res : DocumentResource, pathSection : String ) => {
                      pathSection match {
                          case "" => res
                          case _ => res.asInstanceOf[ DocumentCollection[ _ ] ]( pathSection )
                      }
                  } )
                DocumentAccessRole( resource, operations.toSet )
        }
    }
}
