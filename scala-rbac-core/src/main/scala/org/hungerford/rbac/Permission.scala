package org.hungerford.rbac

/**
 * Permission is a class that defines access to Permissible objects.
 * Its main method is `isPermitted` which defines which "Permissibles"
 * are permitted. The remaining methods define the arithmetic for combining
 * permissions.
 *
 * The five core types of Permission are:
 * 1. AllPermissions (singleton), which permits all Permissibles,
 * 2. NoPermissions (singleton), which permits no Permissibles,
 * 4. PermissionSet (class), which is the union of two or more Permissions,
 * 5. PermissionDifference (class), which is the difference between two permissions
 *    (i.e., it permits all operation permitted by the left-operand and none
 *    of the operations permitted by the right-operand), and
 * 6. SimplePermission (trait), which is what all other Permissions should extend
 *
 * Also included are two basic types of SimplePermission:
 * 1. SinglePermission (class) is defined by a single Permissible instance, which it
 *    permits (all other Permissibles it does not permit), and
 * 2. TypePermission (class) is defined by a subtype of Permissible. All Permissibles
 *    of that subtype are permitted, all that are not of that subtype are not
 *    permitted
 */
trait Permission extends PartiallyOrdered[ Permission ] {
    /**
     * Determines whether or not a given thing (type Permissible) is permitted.
     * @param permissible Permissible: thing you want to know is permitted or not
     * @return Boolean: whether or not it is permitted
     */
    def isPermitted( permissible : Permissible ) : Boolean

    def isPermitted( permissibles : PermissibleSet ) : Boolean = permissibles match {
        case _ : AllPermissibles => permissibles.permissibles.forall {
            case Left( permissible ) => isPermitted( permissible )
            case Right( permissibleSet ) => isPermitted( permissibleSet )
        }
        case _ : AnyPermissibles => permissibles.permissibles.exists {
            case Left( permissible ) => isPermitted( permissible )
            case Right( permissibleSet ) => isPermitted( permissibleSet )
        }
    }

    def union( that : Permission ) : Permission

    def |( that : Permission ) : Permission = this.union( that )

    protected val standardUnion : PartialFunction[ Permission, Permission ] = {
        case p if ( p == this ) => this
        case AllPermissions => AllPermissions
        case NoPermissions => this
    }

    def diff( that : Permission ) : Permission

    def -( that : Permission ) : Permission = this.diff( that )

    protected val standardDiff : PartialFunction[ Permission, Permission ] = {
        case p if ( p == this ) => NoPermissions
        case AllPermissions => NoPermissions
        case NoPermissions => this

    }

    /**
     * Default implementation of partial comparison. "Greater than" should mean, for permissions,
     * that everything the "lesser" permission permits is permitted by the "greater", as well as
     * more in addition. This default implementation uses the `diff` method to determine what is
     * greater and lesser, but there are edge cases that need to be resolved by overriding this in
     * the various subtypes of Permission.
     *
     * @param that B: Object of comparison
     * @param evidence  B => PartiallyOrdered[B]: Implicit used to convert object to potentially comparable object.
     * @tparam B >: Permission
     * @return
     */
    override def tryCompareTo[ B >: Permission ]( that : B )( implicit evidence : B => PartiallyOrdered[ B ] ) : Option[ Int ] = {
        if ( this == that ) Some( 0 )
        else that match {
            case thatP : Permission => ( this - thatP ) match {
                case anyP if ( anyP == this ) => Some( 1 )
                case NoPermissions => Some( -1 )
                case PermissionDifference( _, _ ) => ( thatP - this ) match {
                    case anyP if ( anyP == thatP ) => Some( -1 )
                    case NoPermissions => Some( 1 )
                    case PermissionDifference( _, _ ) => None
                }
                case _ => Some( 1 )
            }
        }
    }
}

case object AllPermissions extends Permission {
    override def isPermitted( permissible : Permissible ) : Boolean = true

    override def union( that : Permission ) : Permission = this

    override def diff( that : Permission ) : Permission = {
        if ( that == this ) NoPermissions
        else if ( that == NoPermissions ) this
        else PermissionDifference( this, that )
    }

    override def tryCompareTo[ B >: Permission ]( that : B )( implicit evidence : B => PartiallyOrdered[ B ] ) : Option[ Int ] = {
        that match {
            case AllPermissions => Some( 0 )
            case _ : Permission => Some( 1 )
            case _ => None
        }
    }
}

case object NoPermissions extends Permission {
    override def isPermitted( permissible : Permissible ) : Boolean = false

    override def union( that : Permission ) : Permission = that

    override def diff( that : Permission ) : Permission = this

    override def tryCompareTo[ B >: Permission ]( that : B )( implicit evidence : B => PartiallyOrdered[ B ] ) : Option[ Int ] = {
        that match {
            case NoPermissions => Some( 0 )
            case _ : PermissionDifference => None
            case _ : Permission => Some( -1 )
        }
    }
}

/**
 * Basic type of Permission that should be extended for any usual custom case: All of
 * the arithmetic is defined here, so all that needs to be implemented is `isPermitted`
 * (and `tryCompareTo` if anything other than the default comparison with other types is
 * desired)
 */
trait SimplePermission extends Permission {
    override def union( that : Permission ) : Permission = standardUnion.applyOrElse( that, ( t : Permission ) => t match {
        case _ : SimplePermission => Permission( this, t )
        case _ : Permission => t | this
    } )

    override def diff( that : Permission ) : Permission = standardDiff.applyOrElse( that, ( t : Permission ) => t match {
        case ps : PermissionSet if ( ps.toSet.contains( this ) ) => NoPermissions
        case _ => PermissionDifference( this, that )
    } )

    // By default, cannot compare SimplePermissions
    override def tryCompareTo[ B >: Permission ]( that : B )( implicit evidence : B => PartiallyOrdered[ B ] ) : Option[ Int ] = {
        if ( this == that ) Some( 0 )
        else that match {
            case _ : SimplePermission => None
            case _ => that.tryCompareTo( this ).map( _ * -1 )
        }
    }
}

trait PermissionSet extends Permission {
    val permissions : Set[ Permission ]

    private lazy val flatPermissions : Set[ Permission ] = {
        val fp = permissions.foldLeft( Set[ Permission ]() ) {
            ( pSet : Set[ Permission ], p : Permission ) => {
                pSet ++ ( p match {
                    case pSet2 : PermissionSet => pSet2.toSet
                    case _ : Permission => Set( p )
                } )
            }
        } filter ( _ != NoPermissions )

        if ( fp.contains( AllPermissions ) ) throw new IllegalStateException( "PermissionSet cannot include AllPermissions" )
        if ( fp.size == 1 ) throw new IllegalStateException( "PermissionSet cannot have only one element" )
        if ( fp.isEmpty ) throw new IllegalStateException( "PermissionSet cannot have no elements" )

        fp
    }

    def toSet : Set[ Permission ] = flatPermissions

    override def isPermitted( permissible : Permissible ) : Boolean = {
        flatPermissions.exists( _.isPermitted( permissible ) )
    }

    override def toString : String = Permission( this ) match {
        case _ : PermissionSet => s"${flatPermissions.map( _.toString ).mkString( " | " )}"
        case other => other.toString
    }

    override def equals( that : Any ) : Boolean = {
        that match {
            case thatPs : PermissionSet =>
                this.flatPermissions == thatPs.flatPermissions
            case _ => false
        }
    }

    override def union( that : Permission ) : Permission = standardUnion.applyOrElse( that, ( t : Permission ) => t match {
        case thatPs : PermissionSet =>
            Permission( this.flatPermissions ++ thatPs.flatPermissions )
        case thatPd : PermissionDifference =>
            thatPd.union( this )
        case _ => Permission( this.flatPermissions + that )
    } )

    override def diff( that : Permission ) : Permission = standardDiff.applyOrElse( that, ( t : Permission ) => t match {
        case thatPs : PermissionSet =>
            val pIntersect = this.flatPermissions.intersect( thatPs.flatPermissions )
            if ( pIntersect == this.flatPermissions ) NoPermissions
            else PermissionDifference( Permission( this.flatPermissions -- pIntersect ), that )
        case _ =>
            if ( this.flatPermissions.contains( that ) ) PermissionDifference( Permission( this.flatPermissions - that ), that )
            else PermissionDifference( this, that )
    } )

    override def tryCompareTo[ B >: Permission ]( that : B )( implicit evidence : B => PartiallyOrdered[ B ] ) : Option[ Int ] = {
        if ( this == that ) Some( 0 )
        else that match {
            case thatP : Permission if ( this.permissions.contains( thatP ) ) => Some( 1 )
            case thatPs : PermissionSet =>
                if ( this.permissions.contains( thatPs ) ) Some( 1 )
                else if ( this.permissions.forall( p => thatPs.permissions.contains( p ) ) ) Some( -1 )
                else if ( thatPs.permissions.forall( p => this.permissions.contains( p ) ) ) Some( 1 )
                else super.tryCompareTo( that )
            case PermissionDifference( p1, _ ) =>
                if ( this > p1 ) Some( 1 )
                else super.tryCompareTo( that )
            case _ => super.tryCompareTo( that )
        }
    }
}

case class PermissionDifference( p1 : Permission, p2 : Permission ) extends Permission {

    override def isPermitted( permissible : Permissible ) : Boolean = {
        p1.isPermitted( permissible ) && !p2.isPermitted( permissible )
    }

    override def equals( that : Any ) : Boolean = {
        that match {
            case thatPd : PermissionDifference =>
                this.p1 == thatPd.p1 && this.p2 == thatPd.p2
            case _ => false
        }
    }

    override def union( that : Permission ) : Permission = standardUnion.applyOrElse( that, ( t : Permission ) => {
        if ( p2 == t ) p1 | t
        else if ( p1 == t ) this
        else ( p2 - that ) match {
            case _ : PermissionDifference => Permission( this, t )
            case NoPermissions => t | p1
            case _ => Permission( this, t )
        }
    } )

    override def diff( that : Permission ) : Permission = standardDiff.applyOrElse( that, ( t : Permission ) => {
        if ( p1 == t ) NoPermissions
        else if ( p2 == t ) this
        else p1 - t match {
            case _ : PermissionDifference => PermissionDifference( p1, p2 | t )
            case NoPermissions => NoPermissions
            case pd : Permission => PermissionDifference( pd, p2 )
        }
    } )

    override def tryCompareTo[ B >: Permission ]( that : B )( implicit evidence : B => PartiallyOrdered[ B ] ) : Option[ Int ] = {
        that match {
            case NoPermissions => None
            case _ => super.tryCompareTo( that )
        }
    }
}

case class SinglePermission( permissible : Permissible ) extends SimplePermission {
    override def isPermitted( p : Permissible ) : Boolean = permissible == p
}

object Permission {
    def apply( permissions : Iterable[ Permission ] ) : Permission = {
        if ( permissions.exists( _ == AllPermissions ) ) AllPermissions
        else {
            val permissionSet = permissions.map( {
                case pSet : PermissionSet => pSet.toSet
                case p : Permission => Set( p )
            } )
              .reduceOption( _ ++ _ ) // Can't reduce an empty set...
              .getOrElse( Set[ Permission ]() )
              .filter( _ != NoPermissions )
            if ( permissionSet.size == 1 ) permissionSet.head
            else if ( permissionSet.isEmpty ) NoPermissions
            else new PermissionSet {
                override val permissions : Set[ Permission ] = {
                    permissionSet
                }
            }
        }
    }

    def apply( permissions : Permission* ) : Permission = Permission( permissions )

    def to( permissibles : Iterable[ Permissible ] ) : Permission = {
        val permissibleSet = permissibles.toSet
        if ( permissibleSet.isEmpty ) NoPermissions
        else if ( permissibleSet.size == 1 ) SinglePermission( permissibleSet.head )
        else Permission( permissibleSet.map( p => SinglePermission( p ) ) )
    }

    def to( permissibles : Permissible* ) : Permission = to( permissibles )

    def to( permissibleSet : AnyPermissibles ) : Permission = to {
        def getAnyPermissibles( ap : AnyPermissibles ) : Set[ Permissible ] = {
            permissibleSet.permissibles collect {
                case Left( permissible: Permissible ) => Set( permissible )
                case Right( ps : AnyPermissibles ) => getAnyPermissibles( ps )
            } flatten
        }

        getAnyPermissibles( permissibleSet )
    }
}

