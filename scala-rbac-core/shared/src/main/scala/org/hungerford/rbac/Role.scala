package org.hungerford.rbac

import scala.annotation.tailrec

/**
 * A bearer of permissions.
 *
 * It is defined by its field [[Role.permissions]].
 *
 * `Role` is only a slight abstraction from [[Permission]] of which it is essentially
 * a container. Its utility is in providing a layer of separation between permissions
 * and their bearers (such as [[User]]), which helps with things like
 * serialization/deserialization.
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
    val permissions : Permission

    private val outerThis : Role = this

    /**
     * Is a permissible permitted by this role?
     *
     * @see [[Permission.permits(Permissible):Boolean]]
     * @param permissible Permissible
     * @return Boolean
     */
    final def can( permissible : Permissible ) : Boolean = permissions.permits( permissible )

    /**
     * Are a set of permissibles permitted by this role?
     *
     * @see [[Permission.permits(PermissibleSet):Boolean]]
     * @param permissible Permissible
     * @return Boolean
     */
    final def can( permissibles: PermissibleSet ) : Boolean = permissibles match {
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
            case thatRole : Role =>
                this.permissions.tryCompareTo( thatRole.permissions )
            case _ => None
        }
    }

    override def equals( obj : Any ) : Boolean = obj match {
        case thatRole : Role => permissions.equals( thatRole.permissions )
        case _ => false
    }
}

/**
 * Can perform any operation.
 *
 * @see [[AllPermissions]]
 */
case object SuperUserRole extends Role {
    override lazy val permissions : Permission = AllPermissions

    override def toString : String = "SuperUserRole"
}


/**
 * The absence of a role: can perform no operations.
 *
 * @see [[NoPermissions]]
 */
case object NoRole extends Role {
    override lazy val permissions : Permission = NoPermissions

    override def toString : String = "NoRole"
}

/**
 * Describes permission to perform an [[Operation]] on a [[PermissibleResource]].
 *
 * Wrapper for [[ResourceOperationPermission]]
 * @see [[ResourceOperationPermission]]
 * @see [[PermissibleResource]]
 */
trait ResourceRole extends Role {
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

    override lazy val permissions : Permission = Permission( roles.map( _.permissions ) )

    override def +( that : Role ) : Roles = that match {
        case thatRls : Roles => new Roles {
            override val roles : Set[ Role ] = outerThis.roles ++ thatRls.roles
        }
        case _ => new Roles {
            override val roles : Set[ Role ] = outerThis.roles + that
        }
    }

    override def toString : String = s"Roles(${roles.mkString( ", " )})"

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
    def from( perms : Iterable[Permission] ) : Role = new Role {
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
    def forOp( operations : Iterable[ Permissible ] ) : Role = new Role {
        override val permissions : Permission = Permission.to( operations )
    }

    /**
     * Generates a [[Role]] permitting one or more [[Permissible]]s (comma-separated).
     *
     * @param operations [[Permissible]]*
     * @return [[Role]]
     */
    def forOp( operations : Permissible* ) : Role = new Role {
        override val permissions : Permission = Permission.to( operations )
    }
}

/**
 * Subtype of [[Operation]] to be used for permissions having to do with roles.
 * All out of the box objects of this type are also [[PermissionOperation]]s.
 *
 * @see [[PermissionOperation]]
 * @see [[RoleManagementPermission]]
 */
trait RoleOperation extends Operation

/**
 * Defines an operation on a role or role-bearer (e.g., [[User]]).
 *
 * E.g., `RoleManagement(someRole, [[Grant]])` describes the "granting" of `someRole` to
 * a [[User]] or some other entity with roles equal to or less than `someRole`.
 * @see [[PermissionManagement]]
 * @param role
 * @param operation
 */
case class RoleManagement( role : Role, operation : RoleOperation ) extends Operation

/**
 * Provides permissions for [[RoleManagement]] operations.
 *
 * Permits [[RoleManagement]] operation if `this.role >= RoleManagement.role`
 * and `this.operationsPermissions.permits(PermissionManagement.operation)`.
 *
 * Partial ordering follows both `roleLevel` and `operationsPermission` fields.
 *
 * @see [[RoleManagement]]
 * @see [[PermissionManagementPermission]]
 * @param roleLevel Permission: level of permissions that permitted PermissionOperations can be performed at
 * @param operationsPermission Permission: permissions for PermissionsOperations
 */
case class RoleManagementPermission( roleLevel : Role, operationsPermission : Permission ) extends SimplePermission {

    override def permits( permissible : Permissible ) : Boolean = permissible match {
        case RoleManagement( r : Role, o : RoleOperation ) =>
            if ( r <= roleLevel && operationsPermission.permits( o ) ) true
            else false
        case _ => false
    }

    override def tryCompareTo[ B >: Permission ]( that : B )( implicit evidence : B => PartiallyOrdered[ B ] ) : Option[ Int ] = {
        if ( this == that ) Some( 0 )
        else that match {
            case RoleManagementPermission( rl, op ) =>
                if ( roleLevel >= rl && operationsPermission >= op ) Some( 1 )
                else if ( roleLevel <= rl && operationsPermission <= op ) Some( -1 )
                else None
            case RecursiveRoleManagementPermission( rl, op ) =>
                if ( roleLevel <= rl && operationsPermission <= op ) Some( -1 )
                else None
            case _ => super.tryCompareTo( that )
        }
    }
}

/**
 * Version of [[RoleManagementPermission]] that not only permits [[RoleManagement]] operations
 * on the given roles, but also operations on roles that have permissions equal to or less
 * than `this`.
 *
 * It is necessary to use this or [[RecursivePermissionManagementPermission]] to give a user
 * permissions to perform any user management at their own level of permissions or lower.
 *
 * @see [[RecursivePermissionManagementPermission]]
 * @see [[RoleManagementPermission]]
 * @see [[RoleManagement]]
 * @param roleLevel
 * @param operationsPermission
 */
case class RecursiveRoleManagementPermission( roleLevel : Role, operationsPermission : Permission ) extends SimplePermission {
    private val PMP = RoleManagementPermission( roleLevel, operationsPermission )

    override def permits( permissible : Permissible ) : Boolean = permissible match {
        case RoleManagement( r : Role, o : RoleOperation ) =>
            this >= RoleManagementPermission( r, Permission.to( o ) )
        case _ => false
    }

    override def tryCompareTo[ B >: Permission ]( that : B )( implicit evidence : B => PartiallyOrdered[ B ] ) : Option[ Int ] = {
        if ( this == that ) Some( 0 )
        else that match {
            case RecursiveRoleManagementPermission( rl : Role, op ) =>
                this.tryCompareTo( RoleManagementPermission( rl, op ) )
            case RoleManagementPermission( rl, op ) => rl.permissions match {
                case RecursiveRoleManagementPermission( nextRl : Role, nextOp ) => this.tryCompareTo( RoleManagementPermission( nextRl, op | nextOp ) )
                case RoleManagementPermission( nextRl : Role, nextOp ) => this.tryCompareTo( RoleManagementPermission( nextRl, op | nextOp ) )
                case ps : PermissionSet if ps.permissions.forall( p => this.tryCompareTo( RoleManagementPermission( Role.from( p ), op ) ).exists( _ >= 0 ) ) => Some( 1 )
                case ps : PermissionSet if ps.permissions.forall( p => this.tryCompareTo( RoleManagementPermission( Role.from( p ), op ) ).exists( _ <= 0 ) ) => Some( -1 )
                case _ : Permission => PMP.tryCompareTo( that )
            }
            case _ => super.tryCompareTo( that )
        }
    }
}
