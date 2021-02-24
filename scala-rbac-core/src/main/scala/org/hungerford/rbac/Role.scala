package org.hungerford.rbac

import scala.annotation.tailrec

/**
 * A bearer of permissions.
 *
 * It is defined chiefly by its method [[Role.can(Permissible):Boolean]], which is
 * equivalent to [[Permission.permits(Permissible):Boolean]].
 *
 * `Role` is only a slight abstraction from [[Permission]]. It is intended
 * to be used primarily as a container for permissions, via the trait [[PermissionsRole]],
 * but it can be extended directly in much the same way as a [[SimplePermission]], by
 * overriding [[Role.can(Permissible):Boolean]]. Its utility is in providing a layer of
 * separation between permissions and their bearers (such as [[User]]), which helps with
 * things like serialization/deserialization.
 *
 * `Role` differs substantively from [[Permission]] only in having simpler composition
 * arithmetic (see [[Role.and(Role):Roles]]/[[Role.+(Role):Roles]].
 *
 * @see [[Permission]]
 * @see [[Roles]]
 * @see [[PermissionsRole]]
 * @see [[SuperUserRole]]
 */
trait Role extends PartiallyOrdered[ Role ] {
    private val outerThis : Role = this

    /**
     * Is a permissible permitted by this role?
     *
     * @see [[Permission.permits(Permissible):Boolean]]
     * @param permissible Permissible
     * @return Boolean
     */
    def can( permissible : Permissible ) : Boolean

    /**
     * Are a set of permissibles permitted by this role?
     *
     * @see [[Permission.permits(PermissibleSet):Boolean]]
     * @param permissible Permissible
     * @return Boolean
     */
    def can( permissibles: PermissibleSet ) : Boolean = permissibles match {
        case _ : AllPermissibles => permissibles.permissibles.forall {
            case Left( permissible ) => can( permissible )
            case Right( permissibleSet ) => can( permissibleSet )
        }
        case _ : AnyPermissibles => permissibles.permissibles.exists {
            case Left( permissible ) => can( permissible )
            case Right( permissibleSet ) => can( permissibleSet )
        }
    }

    /**
     * Combine this with another [[Role]] into a [[Roles]] instance.
     *
     * @param that Role
     * @return Roles
     */
    def and( that : Role ) : Roles = that match {
        case thatRls : Roles => thatRls + this
        case _ => new Roles {
            override val roles : Set[ Role ] = Set( outerThis, that )
        }
    }

    /**
     * @see [[Role.and(Role):Roles]]
     * @param that Role
     * @return Roles
     */
    def +( that : Role ) : Roles = and( that )

    /**
     * Defines partial ordering of roles.
     *
     * For custom [[Role]]s, this can be overridden to define custom ordering. However, any
     * custom implementation should either reimplement comparison with [[Roles]] or call
     * `super.tryCompareTo` for that case.
     *
     * @param that
     * @param evidence
     * @tparam B
     * @return
     */
    override def tryCompareTo[ B >: Role ]( that : B )( implicit evidence : B => PartiallyOrdered[ B ] ) : Option[ Int ] = {
        that match {
            case SuperUserRole =>
                if ( this == SuperUserRole ) Some( 0 ) else Some( -1 )
            case NoRole =>
                if ( this == NoRole ) Some( 0 ) else Some( 1 )
            case _ : Roles => that.tryCompareTo( this ).map( _ * -1 )
            case _ : PermissionsRole => that.tryCompareTo( this ).map( _ * -1 )
            case _ => None
        }
    }
}

/**
 * Most common type of role, defined by one or more permissions.
 *
 * This type of Role is ordered by its permissions, so, e.g., if `this.permissions >= that.permissions`,
 * then `this >= that`.
 */
trait PermissionsRole extends Role {
    val permissions : Permission

    override def can( permissible : Permissible ) : Boolean = permissions.permits( permissible )

    override def toString : String = s"PermissionsRole(${permissions.toString})"

    override def tryCompareTo[ B >: Role ]( that : B )( implicit evidence : B => PartiallyOrdered[ B ] ) : Option[ Int ] = that match {
        case SuperUserRole =>
            if ( permissions == AllPermissions ) Some( 0 ) else Some( -1 )
        case NoRole =>
            if ( permissions == NoPermissions ) Some( 0 ) else Some( 1 )
        case thatPr : PermissionsRole => this.permissions.tryCompareTo( thatPr.permissions )
        case _ => super.tryCompareTo( that )
    }

    override def equals( that : Any ) : Boolean = that match {
        case thatPr : PermissionsRole => this.permissions == thatPr.permissions
        case _ => false
    }
}

/**
 * Can perform any operation.
 *
 * @see [[AllPermissions]]
 */
case object SuperUserRole extends Role {
    override def can( permissible : Permissible ) : Boolean = true

    override def tryCompareTo[ B >: Role ]( that : B )( implicit evidence : B => PartiallyOrdered[ B ] ) : Option[ Int ] = that match {
        case SuperUserRole => Some( 0 )
        case pr : PermissionsRole => pr.permissions match {
            case AllPermissions => Some( 0 )
            case _ => Some( 1 )
        }
        case _ : Role =>
            println( "comparing" )
            Some( 1 )
        case _ => None
    }

    override def toString : String = "SuperUserRole"
}


/**
 * The absence of a role: can perform no operations.
 *
 * @see [[NoPermissions]]
 */
case object NoRole extends Role {
    override def can( permissible : Permissible ) : Boolean = false

    override def tryCompareTo[ B >: Role ]( that : B )( implicit evidence : B => PartiallyOrdered[ B ] ) : Option[ Int ] = that match {
        case NoRole => Some( 0 )
        case pr : PermissionsRole => pr.permissions match {
            case NoPermissions => Some( 0 )
            case _ => Some( -1 )
        }
        case _ : Role => Some( -1 )
        case _ => None
    }

    override def toString : String = "SuperUserRole"
}

/**
 * Describes permission to perform an [[Operation]] on a [[PermissibleResource]].
 *
 * Wrapper for [[ResourceOperationPermission]]
 * @see [[ResourceOperationPermission]]
 * @see [[PermissibleResource]]
 */
trait ResourceRole extends PermissionsRole {
    val resource : PermissibleResource
    val operations : Set[ Operation ]

    lazy val permissions : Permission = {
        ResourceOperationPermission(
            resource,
            Permission( operations.map( op => SinglePermission( op ) ) )
        )
    }

    override def toString : String = s"ResourceRole( Resource: ${resource}; Permitted Operations: ${operations.mkString( ", " )})"
}

/**
 * Container for multiple instances of [[Role]], permitting what each of them permits.
 *
 * @see [[Role.and(Role):Roles]]
 * @see [[Role.+(Role):Roles]]
 */
trait Roles extends Role {
    private val outerThis = this

    val roles : Set[ Role ]

    override def can( permissible : Permissible ) : Boolean = roles.exists( _.can( permissible ) )

    override def +( that : Role ) : Roles = that match {
        case thatRls : Roles => new Roles {
            override val roles : Set[ Role ] = outerThis.roles ++ thatRls.roles
        }
        case _ => new Roles {
            override val roles : Set[ Role ] = outerThis.roles + that
        }
    }

    override def toString : String = s"Roles(${roles.mkString( ", " )})"

    override def tryCompareTo[ B >: Role ]( that : B )( implicit evidence : B => PartiallyOrdered[ B ] ) : Option[ Int ] = {
        if ( roles.size == 1 ) roles.head.tryCompareTo( that )
        that match {
            case thatRls : Roles =>
                if ( this.roles == thatRls.roles ) Some( 0 )
                else if ( thatRls.roles subsetOf this.roles ) Some( 1 )
                else if ( this.roles subsetOf thatRls.roles ) Some( -1 )
                else if ( thatRls.roles.forall( r => this.roles.exists( tr => tr >= r ) ) && this.roles.forall( r => thatRls.roles.exists( tr => tr >= r ) ) ) Some( 0 )
                else if ( thatRls.roles.forall( r => this.roles.exists( tr => tr >= r ) ) ) Some( 1 )
                else if ( this.roles.forall( r => thatRls.roles.exists( tr => tr >= r ) ) ) Some( -1 )
                else None
            case thatR : Role =>
                if ( this.roles contains thatR ) Some( 1 )
                else if ( roles.exists( r => r >= thatR ) ) Some( 1 )
                else if ( roles.forall( r => r <= thatR ) ) Some( -1 )
                else None
        }
    }

    override def equals( that : Any ) : Boolean = that match {
        case thatRls : Roles => this.roles == thatRls.roles
        case thatR : Role => this.roles.size == 1 && this.roles.head == that
        case _ => false
    }
}

/**
 * Factory object for [[Role]]
 */
object Role {
    /**
     * Generates [[Roles]] by combining a collection of [[Role]] instances
     *
     * @param rls : Iterable[ Role ]
     * @return Role (will be a [[Roles]] if multiple, or a single [[Role]] if only one)
     */
    def join( rls : Iterable[ Role ] ) : Role = {
        if ( rls.isEmpty ) throw new IllegalArgumentException( "Cannot construct Roles without any roles" )
        else if ( rls.size == 1 ) rls.head
        else new Roles {
            override val roles : Set[ Role ] = rls.toSet
        }
    }

    /**
     * Generates [[Roles]] by combining multiple [[Role]] instances
     *
     * @param rls : var-args of type Role
     * @return Role (will be a [[Roles]] if multiple, or a single [[Role]] if only one)
     */
    def join( roles : Role* ) : Role = join( roles )

    /**
     * Generates [[Role]] from a collection of permission.
     *
     * Multiple [[Role]]s will generate [[Roles]], a collection of only one [[Role]] will
     * return that one.
     *
     * @param perms [[ Iterable[Permission] ]]
     * @return [[Role]]
     */
    def from( perms : Iterable[Permission] ) : Role = new PermissionsRole {
        override val permissions : Permission = Permission( perms )
    }

    /**
     * Generates [[Role]] from a multiple permissions (comma-separated).
     *
     * Multiple [[Role]]s will generate [[Roles]], while one [[Role]] will
     * return that one.
     *
     * @param perms [[Permission]]*
     * @return [[Role]]
     */
    def from( perms : Permission* ) : Role = from( perms )

    /**
     * Generates a [[Role]] permitting a collection of [[Permissible]]s.
     *
     * @param operations [[ Iterable[Permissible] ]]
     * @return [[Role]]
     */
    def forOp( operations : Iterable[ Permissible ] ) : Role = new PermissionsRole {
        override val permissions : Permission = Permission.to( operations )
    }

    /**
     * Generates a [[Role]] permitting one or more [[Permissible]]s (comma-separated).
     *
     * @param operations [[Permissible]]*
     * @return [[Role]]
     */
    def forOp( operations : Permissible* ) : Role = new PermissionsRole {
        override val permissions : Permission = Permission.to( operations )
    }
}

/**
 * Subtype of [[Operation]] to be used for permissions having to do with roles.
 *
 * @see [[RoleManagementPermission]]
 */
trait RoleOperation extends Operation

object Grant extends RoleOperation
object Retrieve extends RoleOperation

/**
 * Defines an operation on a role or role-bearer (e.g., [[User]]).
 *
 * E.g., `RoleManagement(someRole, [[Grant]])` describes the "granting" of `someRole` to
 * a [[User]] or some other entity with roles equal to or less than `someRole`.
 * @param role
 * @param operation
 */
case class RoleManagement( role : Role, operation : RoleOperation ) extends Operation

/**
 * Provides permissions for [[RoleManagement]] operations.
 *
 * Permits [[RoleManagement]] operation if `this.role >= RoleManagement.role`
 * and `this.operationsPermissions.permits(RoleManagement.operation)`.
 *
 * Partial ordering follows both `role` and `operationsPermission` fields.
 *
 * @see [[RoleManagement]]
 * @param role Role
 * @param operationsPermission Permission
 */
case class RoleManagementPermission( role : Role, operationsPermission : Permission ) extends SimplePermission {
    /**
     * Determines whether or not a given thing (type Permissible) is permitted.
     *
     * @param permissible Permissible: thing you want to know is permitted or not
     * @return Boolean: whether or not it is permitted
     */
    override def permits( permissible : Permissible ) : Boolean = permissible match {
        case RoleManagement( r : Role, o : RoleOperation ) =>
            if ( r <= role && operationsPermission.permits( o ) ) true
            else false
        case _ => false
    }

    override def tryCompareTo[ B >: Permission ]( that : B )( implicit evidence : B => PartiallyOrdered[ B ] ) : Option[ Int ] = {
        if ( this == that ) Some( 0 )
        else that match {
            case RoleManagementPermission( r, op ) =>
                if ( role >= r && operationsPermission >= op ) Some( 1 )
                else if ( role <= r && operationsPermission <= op ) Some( -1 )
                else None
            case _ => super.tryCompareTo( that )
        }
    }
}

/**
 * Based on [[RoleManagementPermission]], permitting (or not) given [[RoleManagement]] operations.
 *
 * Use [[RecursiveRoleManagementRole]] for roles permitting management of themselves in addition
 * to the roles they are defined on.
 *
 * @see [[RoleManagementPermission]]
 * @see [[RoleManagement]]
 * @see [[RecursiveRoleManagementRole]]
 * @param role Role
 * @param operations Set[RoleOperation]
 */
case class RoleManagementRole( role : Role, operations : Set[ RoleOperation ] ) extends PermissionsRole {
    override val permissions : Permission = RoleManagementPermission( role, Permission( operations.map( op => SinglePermission( op ) ) ) )

    override def tryCompareTo[ B >: Role ]( that : B )( implicit evidence : B => PartiallyOrdered[ B ] ) : Option[ Int ] = {
        if ( this == that ) Some( 0 )
        else that match {
            case _ : RecursiveRoleManagementRole => that.tryCompareTo( this ).map( _ * -1 )
            case _ => super.tryCompareTo( that )
        }
    }
}

/**
 * Permits management of given roles and also of itself (with the same [[RoleOperation]]s)
 *
 * Is greater than both the roles it is defined on and the non-recursive version of itself
 * (i.e., [[RoleManagementRole]])
 *
 * @see [[RoleManagementRole]]
 * @see [[RoleManagement]]
 * @see [[RoleManagementPermission]]
 * @param role
 * @param operations
 */
case class RecursiveRoleManagementRole( role : Role, operations : Set[ RoleOperation ] ) extends Role {
    private val thisRMR = RoleManagementRole( role, operations )

    override def can( permissible : Permissible ) : Boolean = permissible match {
        case RoleManagement( r, ops ) => this >= RoleManagementRole( r, Set( ops ) )
        case _ => false
    }

    override def tryCompareTo[ B >: Role ]( that : B )( implicit evidence : B => PartiallyOrdered[ B ] ) : Option[ Int ] = {
        if ( this == that ) Some( 0 )
        else that match {
            case RecursiveRoleManagementRole( r, ops ) =>
                this.tryCompareTo( RoleManagementRole( r, ops ) )
            case RoleManagementRole( r, ops ) => r match {
                case RecursiveRoleManagementRole( nextR, nextOps ) => this.tryCompareTo( RoleManagementRole( nextR, ops ++ nextOps ) )
                case RoleManagementRole( nextR, nextOps ) => this.tryCompareTo( RoleManagementRole( nextR, ops ++ nextOps ) )
                case rls : Roles =>
                    if ( rls.roles.forall( rl => this >= rl || role >= rl ) ) Some( 1 )
                    else None
                case _ : Role => thisRMR.tryCompareTo( that )
            }
            case _ => super.tryCompareTo( that )
        }
    }
}
