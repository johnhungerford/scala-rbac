package org.hungerford.rbac

trait Role extends PartiallyOrdered[ Role ] {
    private val outerThis : Role = this

    def can( permissible : Permissible ) : Boolean

    def and( that : Role ) : Roles = that match {
        case thatRls : Roles => thatRls + this
        case _ => new Roles {
            override val roles : Set[ Role ] = Set( outerThis, that )
        }
    }

    def +( that : Role ) : Roles = and( that )

    override def tryCompareTo[ B >: Role ]( that : B )( implicit evidence : B => PartiallyOrdered[ B ] ) : Option[ Int ] = {
        that match {
            case _ : Roles => that.tryCompareTo( this ).map( _ * -1 )
            case _ : PermissionsRole => that.tryCompareTo( this ).map( _ * -1 )
            case _ => None
        }
    }
}

trait PermissionsRole extends Role {
    val permissions : Permission

    override def can( permissible : Permissible ) : Boolean = permissions.isPermitted( permissible )

    override def toString : String = s"PermissionsRole(${permissions.toString})"

    override def tryCompareTo[ B >: Role ]( that : B )( implicit evidence : B => PartiallyOrdered[ B ] ) : Option[ Int ] = that match {
        case thatPr : PermissionsRole => this.permissions.tryCompareTo( thatPr.permissions )
        case _ => super.tryCompareTo( that )
    }

    override def equals( that : Any ) : Boolean = that match {
        case thatPr : PermissionsRole => this.permissions == thatPr.permissions
        case _ => false
    }
}

case object SuperUserRole extends PermissionsRole {
    override val permissions : Permission = AllPermissions

    override def toString : String = "SuperUserRole"
}

trait ResourceRole extends PermissionsRole {
    val resource : PermissibleResource
    val operations : Set[ Operation ]

    lazy val permissions : Permission = new ResourceOperationPermission( resource, Permission( operations.map( op => SinglePermission( op ) ) ) )

    override def toString : String = s"ResourceRole( Resource: ${resource}; Permitted Operations: ${operations.mkString( ", " )})"
}

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

object Roles {
    def apply( rls : Iterable[ Role ] ) : Role = {
        if ( rls.isEmpty ) throw new IllegalArgumentException( "Cannot construct Roles without any roles" )
        else if ( rls.size == 1 ) rls.head
        else new Roles {
            override val roles : Set[ Role ] = rls.toSet
        }
    }

    def apply( roles : Role* ) : Role = apply( roles )

    def allRoleOp( roles : Iterable[ Role ], operation : RoleOperation ) : PermissibleSet = {
        Permissible.all( roles.map( r => RoleManagement( r, operation ) ) )
    }
    def anyRoleOp( roles : Iterable[ Role ], operation : RoleOperation ) : PermissibleSet = {
        Permissible.any( roles.map( r => RoleManagement( r, operation ) ) )
    }
}

trait RoleOperation extends Operation

object Grant extends RoleOperation
object Retrieve extends RoleOperation

case class RoleManagement( role : Role, operation : RoleOperation ) extends Operation

case class RoleManagementPermission( role : Role, operationsPermission : Permission ) extends SimplePermission {
    /**
     * Determines whether or not a given thing (type Permissible) is permitted.
     *
     * @param permissible Permissible: thing you want to know is permitted or not
     * @return Boolean: whether or not it is permitted
     */
    override def isPermitted( permissible : Permissible ) : Boolean = permissible match {
        case RoleManagement( r : Role, o : RoleOperation ) =>
            if ( r <= role && operationsPermission.isPermitted( o ) ) true
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
            case _ => None
        }
    }
}

case class RoleManagementRole( role : Role, operations : Set[ RoleOperation ] ) extends PermissionsRole {
    override val permissions : Permission = RoleManagementPermission( role, Permission( operations.map( op => SinglePermission( op ) ) ) )

    override def tryCompareTo[ B >: Role ]( that : B )( implicit evidence : B => PartiallyOrdered[ B ] ) : Option[ Int ] = {
        if ( this == that ) Some( 0 )
        else that match {
            case _ : ReflexiveRoleManagementRole => that.tryCompareTo( this ).map( _ * -1 )
            case _ => super.tryCompareTo( that )
        }
    }
}

case class ReflexiveRoleManagementRole( role : Role, operations : Set[ RoleOperation ] ) extends Role {
    private val thisRMR = RoleManagementRole( role, operations )

    override def can( permissible : Permissible ) : Boolean = permissible match {
        case RoleManagement( r, ops ) => this >= RoleManagementRole( r, Set( ops ) )
        case _ => false
    }

    override def tryCompareTo[ B >: Role ]( that : B )( implicit evidence : B => PartiallyOrdered[ B ] ) : Option[ Int ] = {
        if ( this == that ) Some( 0 )
        else that match {
            case ReflexiveRoleManagementRole( r, ops ) =>
                this.tryCompareTo( RoleManagementRole( r, ops ) )
            case RoleManagementRole( r, ops ) => r match {
                case ReflexiveRoleManagementRole( nextR, nextOps ) => this.tryCompareTo( RoleManagementRole( nextR, ops ++ nextOps ) )
                case RoleManagementRole( nextR, nextOps ) => this.tryCompareTo( RoleManagementRole( nextR, ops ++ nextOps ) )
                case rls : Roles =>
                    if ( rls.roles.forall( rl => this >= rl || role >= rl ) ) Some( 1 )
                    else None
                case _ : Role => thisRMR.tryCompareTo( that )
            }
            case _ => None
        }
    }
}
