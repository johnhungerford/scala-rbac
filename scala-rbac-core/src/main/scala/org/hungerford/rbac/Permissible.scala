package org.hungerford.rbac

import org.hungerford.rbac.exceptions.{AuthorizationException, UnpermittedOperationException, UnpermittedOperationsException}

/**
 * Magnet class used by the [[Permissible.secure]] method.
 *
 * @param permitsIn any method evaluating whether a given permissible is permitted
 * @param toStr for logging purposes
 */
sealed class PermissionSource( val permitsIn : Permissible => Boolean, toStr : => String ) extends SimplePermission {
    override def toString : String = toStr

    def permits( permissible : Permissible ) : Boolean = permitsIn( permissible )
}

/**
 * Implicit conversions from common sources of permissions (`Permission`, `User`, `Role`)
 * to the magnet type `PermissionSource`.
 * @see [[PermissionSource]]
 */
object PermissionSource {
    implicit def fromPermission( implicit permission : Permission ) : PermissionSource = new PermissionSource( permission.permits, permission.toString )

    implicit def fromRole( implicit role : Role ) : PermissionSource = new PermissionSource( role.can, role.toString )

    implicit def fromUser( implicit user : User ) : PermissionSource = new PermissionSource( user.can, user.toString )
}

/**
 * Anything that can either be permitted or not permitted.
 *
 * @see [[Permission.permits(Permissible):Boolean]]
 */
trait Permissible {
    private val thisObj = this

    /**
     * Secure a block of code. Requires a source of permissions to execute it, or otherwise
     * throws an `UnpermittedOperationException`.
     *
     * @see [[trySecure()]]
     * @param block code to be executed
     * @param p source of permission, which can be `User`, `Role`, or `Permission`
     * @tparam T return type of code block
     * @throws AuthorizationException if unauthorized
     * @return whatever the code block returns, if permitted
     */
    @throws[ AuthorizationException ]
    def secure[ T ]( block : => T )( implicit p : PermissionSource ) : T = {
        if ( p.permits( this ) ) block
        else throw new UnpermittedOperationException( this, p )
    }

    /**
     * Secure a block of code, letting the code block handle any authorization exception.
     *
     * @see [[secure]]
     * @param block [[ Option[Throwable] ]]=>T: code block that handles exceptions if present.
     * @param p [[PermissionSource]]
     * @tparam T Return type of code block
     * @return
     */
    def trySecure[ T ]( block : Option[ Throwable ] => T )( implicit p : PermissionSource ) : T = {
        if ( p.permits( this ) ) block( None )
        else block( Some( new UnpermittedOperationException( this, p ) ) )
    }

    /**
     * Combine permissibles into a PermissibleSet of type AllPermissibles.
     * @param that Permissible
     * @return PermissibleSet
     */
    def and( that : Permissible ) : PermissibleSet = {
        new AllPermissibles {
            override val permissibles : Set[ Either[ Permissible, PermissibleSet ] ] = Set( Left( thisObj ), Left( that ) )
        }
    }

    /**
     * Combines Permissible with a PermissibleSet, adding to or generating AllPermissibles
     * @param that PermissibleSet
     * @return PermissibleSet
     */
    def and( that : PermissibleSet ) : PermissibleSet = that match {
        case _ : AllPermissibles => new AllPermissibles {
            override val permissibles : Set[ Either[ Permissible, PermissibleSet ] ] = that.permissibles + Left( thisObj )
        }
        case _ : AnyPermissibles => new AllPermissibles {
            override val permissibles : Set[ Either[ Permissible, PermissibleSet ] ] = Set( Left( thisObj ), Right( that ) )
        }
    }

    /**
     * @see [[and(Permissible):PermissibleSet*]]
     * @param that Permissible
     * @return PermissibleSet
     */
    def &( that : Permissible ) : PermissibleSet = and( that )

    /**
     * @see [[and(PermissibleSet):PermissibleSet*]]
     * @param that PermissibleSet
     * @return PermissibleSet
     */
    def &( that : PermissibleSet ) : PermissibleSet = and( that )

    /**
     * Combine permissibles into a PermissibleSet of type AnyPermissibles.
     * @param that Permissible
     * @return PermissibleSet
     */
    def or( that : Permissible ) : PermissibleSet = new AnyPermissibles {
        override val permissibles : Set[ Either[ Permissible, PermissibleSet ] ] = Set( Left( thisObj ), Left( that ) )
    }

    /**
     * Combines Permissible with a PermissibleSet, adding to or generating AnyPermissibles
     * @param that PermissibleSet
     * @return PermissibleSet
     */
    def or( that : PermissibleSet ) : PermissibleSet = that match {
        case _ : AnyPermissibles => new AnyPermissibles {
            override val permissibles : Set[ Either[ Permissible, PermissibleSet ] ] = that.permissibles + Left( thisObj )
        }
        case _ : AllPermissibles => new AnyPermissibles {
            override val permissibles : Set[ Either[ Permissible, PermissibleSet ] ] = Set( Left( thisObj ), Right( that ) )
        }
    }

    /**
     * @see [[or(Permissible):PermissibleSet*]]
     * @param that Permissible
     * @return PermissibleSet
     */
    def |( that : Permissible ) : PermissibleSet = or( that )

    /**
     * @see [[or(PermissibleSet):PermissibleSet*]]
     * @param that PermissibleSet
     * @return PermissibleSet
     */
    def |( that : PermissibleSet ) : PermissibleSet = or( that )

    /**
     * Get a permission to do perform this permissible.
     *
     * @return Permission
     */
    def permission : Permission = SinglePermission( this )
}

/**
 * Container for multiple Permissibles, used to secure against more than one
 * at once.
 * @see [[AllPermissibles]]
 * @see [[AnyPermissibles]]
 */
sealed trait PermissibleSet {

    /**
     * Permissions belonging to this set, which either permissions
     * themselves, or also permission sets
     */
    val permissibles : Set[ Either[Permissible, PermissibleSet] ]

    /**
     * Combine PermissibleSet with Permissible, adding to or generating [[AllPermissibles]]
     * @param that Permissible
     * @return PermissibleSet
     */
    def and( that : Permissible ) : PermissibleSet

    /**
     * Combine two PermissibleSets, adding to or generating [[AllPermissibles]]
     * @param that PermissibleSet
     * @return PermissibleSet
     */
    def and( that : PermissibleSet ) : PermissibleSet

    /**
     * @see [[and(Permissible):PermissibleSet*]]
     * @param that Permissible
     * @return PermissibleSet
     */
    def &( that : Permissible ) : PermissibleSet = and( that )

    /**
     * @see [[and(PermissibleSet):PermissibleSet]]
     * @param that PermissibleSet
     * @return PermissibleSet
     */
    def &( that : PermissibleSet ) : PermissibleSet = and( that )

    /**
     * Combine PermissibleSet with Permissible, adding to or generating an [[AnyPermissibles]]
     * @param that Permissible
     * @return PermissibleSet
     */
    def or( that : Permissible ) : PermissibleSet

    /**
     * Combine two PermissibleSets, adding to or generating an [[AnyPermissibles]]
     * @param that PermissibleSet
     * @return PermissibleSet
     */
    def or( that : PermissibleSet ) : PermissibleSet

    /**
     * @see [[or(Permissible):PermissibleSet]]
     * @param that Permissible
     * @return PermissibleSet
     */
    def |( that : Permissible ) : PermissibleSet = or( that )

    /**
     * @see [[or(PermissibleSet):PermissibleSet]]
     * @param that Permissible
     * @return PermissibleSet
     */
    def |( that : PermissibleSet ) : PermissibleSet = or( that )

    /**
     * Secures a method against this combination of permissibles.
     * @param block Block to be executed if permitted
     * @param p source of permissions [[PermissionSource]]
     * @tparam T return type of secured code block
     * @return T
     */
    def secure[ T ]( block : => T )( implicit p : PermissionSource ) : T = {
        if ( p.permits( this ) ) block
        else throw new UnpermittedOperationsException( this, p )
    }
}

/**
 * A PermissionSet that can be secured against all of its members.
 * @see [[Permission.permits(PermissibleSet):Boolean]]
 */
sealed trait AllPermissibles extends PermissibleSet {
    private val thisObj = this

    override def and( that : Permissible ) : PermissibleSet = that.and( this )
    override def and( that : PermissibleSet ) : PermissibleSet = that match {
        case _ : AllPermissibles => new AllPermissibles {
            override val permissibles : Set[ Either[ Permissible, PermissibleSet ] ] = thisObj.permissibles ++ that.permissibles
        }
        case _ : AnyPermissibles => new AllPermissibles {
            override val permissibles : Set[ Either[ Permissible, PermissibleSet ] ] = thisObj.permissibles + Right( that )
        }
    }

    override def or( that : Permissible ) : PermissibleSet = that.or( this )
    override def or( that : PermissibleSet ) : PermissibleSet = that match {
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

/**
 * A PermissionSet that can be secured against any of its members.
 * @see [[Permission.permits(PermissibleSet):Boolean]]
 */
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

/**
 * Factory object for [[PermissibleSet]]
 */
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

/**
 * Alias for Permissible.
 *
 * Since pretty much anything that will be secured is an "operation,"
 * this subtype can be used in place of Permissible for greater clarity.
 */
trait Operation extends Permissible

