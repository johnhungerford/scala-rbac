package org.hungerford.rbac

import scala.language.postfixOps

/**
 * Defines access to Permissible objects.
 *
 * Its main method is `permits` which defines which "Permissibles"
 * are permitted. The remaining methods define the arithmetic for combining
 * permissions.
 *
 * @see [[AllPermissions]]
 * @see [[NoPermissions]]
 * @see [[PermissionSet]]
 * @see [[PermissionDifference]]
 * @see [[SimplePermission]]
 */
trait Permission extends PartiallyOrdered[ Permission ] {
    /**
     * Determines whether or not a given thing (type Permissible) is permitted.
     * @param permissible Permissible: thing you want to know is permitted or not
     * @return Boolean: whether or not it is permitted
     */
    def permits( permissible : Permissible ) : Boolean

    /**
     * Determines whether or not a given set of permissibles (PermissibleSet)
     * is permitted.
     *
     * Returns true if any of the permissibles in [[AnyPermissibles]] is permitted.
     * Returns true only if all of the permissibles in [[AllPermissibles]] are
     * permitted
     * @param permissibles PermissibleSet
     * @return Boolean
     */
    final def permits( permissibles : PermissibleSet ) : Boolean = permissibles match {
        case _ : AllPermissibles => permissibles.permissibles.forall {
            case Left( permissible ) => permits( permissible )
            case Right( permissibleSet ) => permits( permissibleSet )
        }
        case _ : AnyPermissibles => permissibles.permissibles.exists {
            case Left( permissible ) => permits( permissible )
            case Right( permissibleSet ) => permits( permissibleSet )
        }
    }

    /**
     * Combines permissions into a PermissionSet.
     * @see [[PermissionSet]]
     * @param that Permission
     * @return Permission
     */
    def union( that : Permission ) : Permission

    /**
     * @see [[union(Permission):Permission]]
     */
    def |( that : Permission ) : Permission = this.union( that )

    protected val standardUnion : PartialFunction[ Permission, Permission ] = {
        case p if ( p == this ) => this
        case AllPermissions => AllPermissions
        case NoPermissions => this
    }

    /**
     * The difference between two permissions. The resulting permission
     * will permit what this permission permits but only if the other permission
     * does *not* permit it.
     * @see [[PermissionDifference]]
     * @param that Permission
     * @return Permission
     */
    def diff( that : Permission ) : Permission

    /**
     * @see [[diff(Permission)]]
     * @param that Permission
     * @return Permission
     */
    def -( that : Permission ) : Permission = this.diff( that )

    protected val standardDiff : PartialFunction[ Permission, Permission ] = {
        case p if ( p == this ) => NoPermissions
        case AllPermissions => NoPermissions
        case NoPermissions => this

    }

    /**
     * Determines partial comparison (>, <, >=, <=).
     *
     * "Greater than" means, for permissions, that everything the "lesser" permission permits
     * is permitted by the "greater", as well as more in addition. This default implementation uses
     * the `diff` method to determine what is greater and lesser, but there are edge cases that need
     * to be resolved by overriding this in the various subtypes of Permission.
     *
     * @param that B: Object of comparison
     * @param evidence  B => PartiallyOrdered[B]: Implicit used to convert object to potentially comparable object.
     * @tparam B >: Permission
     * @return Option[Int]
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

    /**
     * Get a role defined by this permission alone.
     *
     * @return Role
     */
    final def toRole : Role = Role.from( this )
}

/**
 * Singleton [[Permission]] that permits all [[Permissible]]s/[[Operation]]s
 */
case object AllPermissions extends Permission {
    override def permits( permissible : Permissible ) : Boolean = true

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

/**
 * Singleton [[Permission]] that allows no [[Permissible]]s/[[Operation]]s
 */
case object NoPermissions extends Permission {
    override def permits( permissible : Permissible ) : Boolean = false

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
 * Basic type of Permission that should be extended for any usual custom case.
 *
 * All of the arithmetic is defined here, so all that needs to be implemented is `permits`
 * (and `tryCompareTo` if anything other than the default comparison with other types is
 * desired)
 */
trait SimplePermission extends Permission {
    override final def union( that : Permission ) : Permission = standardUnion.applyOrElse( that, ( t : Permission ) => t match {
        case _ : SimplePermission => Permission( this, t )
        case _ : Permission => t | this
    } )

    override final def diff( that : Permission ) : Permission = standardDiff.applyOrElse( that, ( t : Permission ) => t match {
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

/**
 * Combination of [[Permission]]s
 */
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

    final def toSet : Set[ Permission ] = flatPermissions

    override final def permits( permissible : Permissible ) : Boolean = {
        flatPermissions.exists( _.permits( permissible ) )
    }

    override def toString : String = Permission( this ) match {
        case _ : PermissionSet => s"${flatPermissions.map( _.toString ).mkString( " | " )}"
        case other => other.toString
    }

    override final def equals( that : Any ) : Boolean = {
        that match {
            case thatPs : PermissionSet =>
                this.flatPermissions == thatPs.flatPermissions
            case _ => false
        }
    }

    override final def union( that : Permission ) : Permission = standardUnion.applyOrElse( that, ( t : Permission ) => t match {
        case thatPs : PermissionSet =>
            Permission( this.flatPermissions ++ thatPs.flatPermissions )
        case thatPd : PermissionDifference =>
            thatPd.union( this )
        case _ => Permission( this.flatPermissions + that )
    } )

    override final def diff( that : Permission ) : Permission = standardDiff.applyOrElse( that, ( t : Permission ) => t match {
        case thatPs : PermissionSet =>
            val pIntersect = this.flatPermissions.intersect( thatPs.flatPermissions )
            if ( pIntersect == this.flatPermissions ) NoPermissions
            else PermissionDifference( Permission( this.flatPermissions -- pIntersect ), that )
        case _ =>
            if ( this.flatPermissions.contains( that ) ) PermissionDifference( Permission( this.flatPermissions - that ), that )
            else PermissionDifference( this, that )
    } )

    override final def tryCompareTo[ B >: Permission ]( that : B )( implicit evidence : B => PartiallyOrdered[ B ] ) : Option[ Int ] = {
        if ( this == that ) Some( 0 )
        else that match {
            case thatPs : PermissionSet =>
                if ( this.permissions.contains( thatPs ) ) Some( 1 )
                else if ( this.permissions.forall( p => thatPs.permissions.contains( p ) ) ) Some( -1 )
                else if ( thatPs.permissions.forall( p => this.permissions.contains( p ) ) ) Some( 1 )
                else super.tryCompareTo( that )
            case PermissionDifference( p1, _ ) =>
                if ( this > p1 ) Some( 1 )
                else super.tryCompareTo( that )
            case thatP : Permission if ( this.permissions.forall( p => p == thatP ) ) => Some( 0 )
            case thatP : Permission if ( this.permissions.exists( p => p >= thatP ) ) => Some( 1 )
            case _ => super.tryCompareTo( that )
        }
    }
}

/**
 * The difference between two [[Permission]]s.
 *
 * Generated by [[Permission.diff]] or [[Permission.-]] when the difference cannot be
 * resolved to a simpler form. (E.g., `(perm1 | perm2) - perm1` resolves to `perm1`,
 * which is not necessarily of type [[PermissionDifference]])
 * @param p1 Permission
 * @param p2 Permission
 */
case class PermissionDifference( p1 : Permission, p2 : Permission ) extends Permission {

    override def permits( permissible : Permissible ) : Boolean = {
        p1.permits( permissible ) && !p2.permits( permissible )
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

/**
 * Permits a given [[Permissible]] and nothing else
 *
 * Simplest and most commonly used permission type.
 */
case class SinglePermission( permissible : Permissible ) extends SimplePermission {
    override def permits( p : Permissible ) : Boolean = permissible == p
}

/**
 * Factory object for [[SinglePermission]] and [[PermissibleSet]]
 */
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

/**
 * Subtype of [[Operation]] to be used for permissions having to do with permissions.
 *
 * @see [[PermissionManagementPermission]]
 */
trait PermissionOperation extends Operation

object Grant extends PermissionOperation with RoleOperation
object Retrieve extends PermissionOperation with RoleOperation

/**
 * Defines an operation on a permission or permission-bearer (e.g., [[User]]).
 *
 * E.g., `PermissionManagement(somePermission, [[Grant]])` describes the "granting" of `somePermission` to
 * a [[User]] or some other entity with permissions equal to or less than `somePermission`.
 *
 * @param permissions
 * @param operation
 */
case class PermissionManagement( permissions : Permission, operation : PermissionOperation ) extends Operation

/**
 * Provides permissions for [[PermissionManagement]] operations.
 *
 * Permits [[PermissionManagement]] operation if `this.permissions >= PermissionManagement.permissions`
 * and `this.operationsPermissions.permits(PermissionManagement.operation)`.
 *
 * Partial ordering follows both `permissions` and `operationsPermission` fields.
 *
 * @see [[PermissionManagement]]
 * @param permissionsLevel Permission: level of permissions that permitted PermissionOperations can be performed at
 * @param operationsPermission Permission: permissions for PermissionsOperations
 */
case class PermissionManagementPermission( permissionsLevel : Permission, operationsPermission : Permission ) extends SimplePermission {

    override def permits( permissible : Permissible ) : Boolean = permissible match {
        case PermissionManagement( p : Permission, o : PermissionOperation ) =>
            if ( p <= permissionsLevel && operationsPermission.permits( o ) ) true
            else false
        case _ => false
    }

    override def tryCompareTo[ B >: Permission ]( that : B )( implicit evidence : B => PartiallyOrdered[ B ] ) : Option[ Int ] = {
        if ( this == that ) Some( 0 )
        else that match {
            case PermissionManagementPermission( pl, op ) =>
                if ( permissionsLevel >= pl && operationsPermission >= op ) Some( 1 )
                else if ( permissionsLevel <= pl && operationsPermission <= op ) Some( -1 )
                else None
            case RecursivePermissionManagementPermission( pl, op ) =>
                if ( permissionsLevel <= pl && operationsPermission <= op ) Some( -1 )
                else None
            case _ => super.tryCompareTo( that )
        }
    }
}

/**
 * Version of [[PermissionManagementPermission]] that not only permits [[PermissionManagement]]
 * operations on the given permissions, but also operations on permissions that are equal
 * to or less than `this`.
 *
 * It is necessary to use this or [[RecursiveRoleManagementPermission]] to give a user
 * permissions to perform any user management at their own level of permissions or lower.
 *
 * @see [[PermissionManagementPermission]]
 * @see [[PermissionManagement]]
 * @see [[RecursivePermissionManagementPermission]]
 * @param permissionsLevel
 * @param operationsPermission
 */
case class RecursivePermissionManagementPermission( permissionsLevel : Permission, operationsPermission : Permission ) extends SimplePermission {
    private val PMP = PermissionManagementPermission( permissionsLevel, operationsPermission )

    override def permits( permissible : Permissible ) : Boolean = permissible match {
        case PermissionManagement( p : Permission, o : PermissionOperation ) =>
            this >= PermissionManagementPermission( p, Permission.to( o ) )
        case _ => false
    }

    override def tryCompareTo[ B >: Permission ]( that : B )( implicit evidence : B => PartiallyOrdered[ B ] ) : Option[ Int ] = {
        if ( this == that ) Some( 0 )
        else that match {
            case RecursivePermissionManagementPermission( pl, op ) =>
                this.tryCompareTo( PermissionManagementPermission( pl, op ) )
            case PermissionManagementPermission( pl, op ) => pl match {
                case RecursivePermissionManagementPermission( nextPl, nextOp ) => this.tryCompareTo( PermissionManagementPermission( nextPl, op | nextOp ) )
                case PermissionManagementPermission( nextPl, nextOp ) => this.tryCompareTo( PermissionManagementPermission( nextPl, op | nextOp ) )
                case ps : PermissionSet if ps.permissions.forall( p => this.tryCompareTo( PermissionManagementPermission( p, op ) ).exists( _ >= 0 ) ) => Some( 1 )
                case ps : PermissionSet if ps.permissions.forall( p => this.tryCompareTo( PermissionManagementPermission( p, op ) ).exists( _ <= 0 ) ) => Some( -1 )
                case _ : Permission => PMP.tryCompareTo( that )
            }
            case _ => super.tryCompareTo( that )
        }
    }
}
