package org.hungerford.rbac

class PermissionException( msg : String ) extends Exception( msg )

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

sealed class PermissionSource( val permitsIn : Permissible => Boolean, toStr : => String ) extends SimplePermission {
    override def toString : String = toStr

    def permits( permissible : Permissible ) : Boolean = permitsIn( permissible )
}

object PermissionSource {
    implicit def fromPermission( implicit permission : Permission ) : PermissionSource = new PermissionSource( permission.permits, permission.toString )

    implicit def fromRole( implicit role : Role ) : PermissionSource = new PermissionSource( role.can, role.toString )

    implicit def fromUser( implicit user : User ) : PermissionSource = new PermissionSource( user.can, user.toString )
}

trait Permissible {
    private val thisObj = this

    def secure[ T ]( block : => T )( implicit p : PermissionSource ) : T = {
        if ( p.permits( this ) ) block
        else throw new UnpermittedOperationException( this, p )
    }

    def and( that : Permissible ) : PermissibleSet = {
        new AllPermissibles {
            override val permissibles : Set[ Either[ Permissible, PermissibleSet ] ] = Set( Left( thisObj ), Left( that ) )
        }
    }
    def and( that : PermissibleSet ) : PermissibleSet = that match {
        case _ : AllPermissibles => new AllPermissibles {
            override val permissibles : Set[ Either[ Permissible, PermissibleSet ] ] = that.permissibles + Left( thisObj )
        }
        case _ : AnyPermissibles => new AllPermissibles {
            override val permissibles : Set[ Either[ Permissible, PermissibleSet ] ] = Set( Left( thisObj ), Right( that ) )
        }
    }
    def &( that : Permissible ) : PermissibleSet = and( that )
    def &( that : PermissibleSet ) : PermissibleSet = and( that )

    def or( that : Permissible ) : PermissibleSet = new AnyPermissibles {
        override val permissibles : Set[ Either[ Permissible, PermissibleSet ] ] = Set( Left( thisObj ), Left( that ) )
    }
    def or( that : PermissibleSet ) : PermissibleSet = that match {
        case _ : AnyPermissibles => new AnyPermissibles {
            override val permissibles : Set[ Either[ Permissible, PermissibleSet ] ] = that.permissibles + Left( thisObj )
        }
        case _ : AllPermissibles => new AnyPermissibles {
            override val permissibles : Set[ Either[ Permissible, PermissibleSet ] ] = Set( Left( thisObj ), Right( that ) )
        }
    }
    def |( that : Permissible ) : PermissibleSet = or( that )
    def |( that : PermissibleSet ) : PermissibleSet = or( that )
}

sealed trait PermissibleSet {
    val permissibles : Set[ Either[Permissible, PermissibleSet] ]
    def and( that : Permissible ) : PermissibleSet
    def and( that : PermissibleSet ) : PermissibleSet
    def &( that : Permissible ) : PermissibleSet = and( that )
    def &( that : PermissibleSet ) : PermissibleSet = and( that )
    def or( that : Permissible ) : PermissibleSet
    def or( that : PermissibleSet ) : PermissibleSet
    def |( that : Permissible ) : PermissibleSet = or( that )
    def |( that : PermissibleSet ) : PermissibleSet = or( that )

    def secure[ T ]( block : => T )( implicit p : PermissionSource ) : T = {
        if ( p.permits( this ) ) block
        else throw new UnpermittedOperationsException( this, p )
    }
}

sealed trait AllPermissibles extends PermissibleSet {
    private val thisObj = this

    def and( that : Permissible ) : PermissibleSet = that.and( this )
    def and( that : PermissibleSet ) : PermissibleSet = that match {
        case _ : AllPermissibles => new AllPermissibles {
            override val permissibles : Set[ Either[ Permissible, PermissibleSet ] ] = thisObj.permissibles ++ that.permissibles
        }
        case _ : AnyPermissibles => new AllPermissibles {
            override val permissibles : Set[ Either[ Permissible, PermissibleSet ] ] = thisObj.permissibles + Right( that )
        }
    }

    def or( that : Permissible ) : PermissibleSet = that.or( this )
    def or( that : PermissibleSet ) : PermissibleSet = that match {
        case _ : AnyPermissibles => new AnyPermissibles {
            override val permissibles : Set[ Either[ Permissible, PermissibleSet ] ] = that.permissibles + Right( thisObj )
        }
        case _ : AllPermissibles => new AnyPermissibles {
            override val permissibles : Set[ Either[ Permissible, PermissibleSet ] ] = Set( Right( thisObj ), Right( that ) )
        }
    }

    override def toString : String = {
        val internalString = permissibles collect {
            case Left( p ) => p.toString
            case Right( ps ) => ps.toString
        } mkString(" & ")
        s"($internalString)"
    }
}

sealed trait AnyPermissibles extends PermissibleSet {
    private val thisObj = this

    def and( that : Permissible ) : PermissibleSet = that.and( this )
    def and( that : PermissibleSet ) : PermissibleSet = that match {
        case _ : AllPermissibles => new AllPermissibles {
            override val permissibles : Set[ Either[ Permissible, PermissibleSet ] ] = that.permissibles + Right( thisObj )
        }
        case _ : AnyPermissibles => new AllPermissibles {
            override val permissibles : Set[ Either[ Permissible, PermissibleSet ] ] = Set( Right( thisObj ), Right( that ) )
        }
    }

    def or( that : Permissible ) : PermissibleSet = that.or( this )
    def or( that : PermissibleSet ) : PermissibleSet = that match {
        case _ : AnyPermissibles => new AnyPermissibles {
            override val permissibles : Set[ Either[ Permissible, PermissibleSet ] ] = thisObj.permissibles ++ that.permissibles
        }
        case _ : AllPermissibles => new AnyPermissibles {
            override val permissibles : Set[ Either[ Permissible, PermissibleSet ] ] = thisObj.permissibles + Right( that )
        }
    }

    override def toString : String = {
        val internalString = permissibles collect {
            case Left( p ) => p.toString
            case Right( ps ) => ps.toString
        } mkString(" | ")
        s"($internalString)"
    }
}

object Permissible {
    def all( ps : Iterable[ Permissible ] ) : AllPermissibles = {
        new AllPermissibles {
            override val permissibles : Set[ Either[ Permissible, PermissibleSet ] ] = ps.toSet.map( ( p : Permissible ) => Left( p ) )
        }
    }
    def all( ps : Permissible* ) : AllPermissibles = all( ps )

    def any( ps : Iterable[ Permissible ] ) : AnyPermissibles = {
        new AnyPermissibles {
            override val permissibles : Set[ Either[ Permissible, PermissibleSet ] ] = ps.toSet.map( ( p : Permissible ) => Left( p ) )
        }
    }
    def any( ps : Permissible* ) : AnyPermissibles = any( ps )
}

trait Operation extends Permissible

