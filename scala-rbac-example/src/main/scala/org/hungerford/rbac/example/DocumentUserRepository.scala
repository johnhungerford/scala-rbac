package org.hungerford.rbac.example

import org.hungerford.rbac.{Permissible, PermissionSource, Role, RoleManagement, RoleOperation, User}

object DocumentUserRepository {

    private val userMap : scala.collection.mutable.Map[ String, User ] = scala.collection.mutable.Map[ String, User ]()

    trait UserOperation extends RoleOperation

    case object AddUser extends UserOperation

    case object GetUser extends UserOperation

    case object RemoveUser extends UserOperation

    case object GrantUser extends UserOperation

    def userOps( user : User, op : UserOperation ) : Permissible = {
        RoleManagement( user.roles, op )
    }

    def roleOps( role : Role, op : UserOperation ) : Permissible = RoleManagement( role, op )

    def addUser( id : String, rls : Role )( implicit ps : PermissionSource ) : User = {
        RoleManagement( rls, AddUser ).secure {
            userMap( id ) = new User {
                override val name : String = id
                override val roles : Role = rls
            }
            userMap( id )
        }
    }

    def getUser( id : String )( implicit ps : PermissionSource ) : User = {
        val user = userMap( id : String )
        userOps( user, GetUser ).secure( user )
    }

    def getUsers( implicit ps : PermissionSource ) : List[ User ] = {
        userMap.keys.toList
          .filter( id => ps permits RoleManagement( userMap( id ).roles, GetUser ) )
          .map( id => getUser( id ) )
    }

    def removeUser( id : String )( implicit ps : PermissionSource ) : Unit = {
        val user = getUser( id )
        userOps( user, RemoveUser ).secure( userMap - id )
    }

    def grantUser( id : String, roles : Role )( implicit ps : PermissionSource ) : User = {
        val user = getUser( id )
        val newUser = new User {
            override val name : String = user.name
            override val roles : Role = user.roles + roles
        }
        userOps( newUser, GrantUser ).secure {
            userMap( id ) = newUser
            userMap( id )
        }
    }

    def authenticateWebToken( token : String ) : User = {
        userMap( token )
    }

}
