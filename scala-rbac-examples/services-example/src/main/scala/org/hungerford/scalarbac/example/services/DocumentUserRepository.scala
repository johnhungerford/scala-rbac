package org.hungerford.scalarbac.example.services

import org.hungerford.rbac.{Permissible, PermissionSource, Role, RoleManagement, RoleOperation, User}

object DocumentUserRepository {

    case class DocUser( name : String, override val roles : Role ) extends User

    private val userMap : scala.collection.mutable.Map[ String, DocUser ] = scala.collection.mutable.Map[ String, DocUser ]()

    trait UserOperation extends RoleOperation

    case object AddUser extends UserOperation

    case object GetUser extends UserOperation

    case object RemoveUser extends UserOperation

    case object GrantUser extends UserOperation

    def userOps( user : User, op : UserOperation ) : Permissible = {
        RoleManagement( user.roles, op )
    }

    def roleOps( role : Role, op : UserOperation ) : Permissible = RoleManagement( role, op )

    def addUser( id : String, rls : Role )( implicit ps : PermissionSource ) : DocUser = {
        RoleManagement( rls, AddUser ).secure {
            userMap( id ) = DocUser( id, rls )
            userMap( id )
        }
    }

    def getUser( id : String )( implicit ps : PermissionSource ) : DocUser = {
        val user = userMap( id : String )
        userOps( user, GetUser ).secure( user )
    }

    def getUsers( implicit ps : PermissionSource ) : List[ DocUser ] = {
        userMap.keys.toList
          .filter( id => ps permits RoleManagement( userMap( id ).roles, GetUser ) )
          .map( id => getUser( id ) )
    }

    def removeUser( id : String )( implicit ps : PermissionSource ) : Unit = {
        val user = getUser( id )
        userOps( user, RemoveUser ).secure( userMap - id )
    }

    def grantUser( id : String, roles : Role )( implicit ps : PermissionSource ) : DocUser = {
        val user = getUser( id )
        val newUser = user.copy( roles = user.roles + roles )

        userOps( newUser, GrantUser ).secure {
            userMap( id ) = newUser
            userMap( id )
        }
    }

    def authenticateWebToken( token : String ) : DocUser = {
        userMap( token )
    }

}
