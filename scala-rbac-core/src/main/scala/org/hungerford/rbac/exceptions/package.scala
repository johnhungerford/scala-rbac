package org.hungerford.rbac

package object exceptions {
    class RbacException( msg : String ) extends Exception( msg )

    class PermissionException( msg : String ) extends RbacException( msg )

    class UnpermittedOperationException( operation : Permissible, permissionSource : PermissionSource )
      extends PermissionException( s"\n\tUnpermitted operation: ${operation.toString}\n\tPermission source: ${permissionSource.toString}")

    class UnpermittedOperationsException( operations : PermissibleSet, permissionSource: PermissionSource )
      extends PermissionException( {
          val oneOrAll : String = operations match {
              case _ : AllPermissibles => "all"
              case _ : AnyPermissibles => "any"
          }
          val operationsString = operations.permissibles.mkString( ", " )
          s"\n\tUnpermitted operations: $oneOrAll of the following: $operationsString\n\tPermission source: ${permissionSource.toString}"
      } )

    class MissingCredentialsException( msg : String ) extends PermissionException( msg )
}
