package org.hungerford.rbac

sealed trait PermissibleResource extends Permissible with PartiallyOrdered[ PermissibleResource ] {
    def childOf( resource : PermissibleResource ) : Boolean

    override def tryCompareTo[ B >: PermissibleResource ]( that : B )( implicit evidence : B => PartiallyOrdered[ B ] ) : Option[ Int ] = {
        if ( this == that ) Some( 0 )
        else that match {
            case thatPr : PermissibleResource =>
                if ( this.childOf( thatPr ) ) {
                    if ( thatPr.childOf( this ) ) Some( 0 )
                    else Some( -1 )
                } else if ( thatPr.childOf( this ) ) Some( 1 )
                else None
        }
    }
}

case object AllResources extends PermissibleResource {
    override def childOf( resource : PermissibleResource ) : Boolean = resource == AllResources

    override def tryCompareTo[ B >: PermissibleResource ]( that : B )( implicit evidence : B => PartiallyOrdered[ B ] ) : Option[ Int ] = {
        that match {
            case AllResources => Some( 0 )
            case _ : PermissibleResource => Some( 1 )
            case _ => None
        }
    }
}

sealed trait ResourceWithParent extends PermissibleResource {
    val parent : PermissibleResource

    if ( parent == this ) throw new IllegalArgumentException( "A resource cannot be it's own parent" )

    override def childOf( resource : PermissibleResource ) : Boolean = {
        if ( resource == this ) true
        else parent.childOf( resource )
    }
}

trait ResourceType extends ResourceWithParent

trait Resource[ +T <: PermissibleResource ] extends ResourceWithParent {
    override val parent : T
}

trait ResourceOperation extends Permissible {
    val resource : PermissibleResource
    val operation : Operation
}

case class ResourceOperationPermission( val resource : PermissibleResource, val operationPermission : Permission ) extends SimplePermission {
    override def isPermitted( permissible : Permissible ) : Boolean = {
        permissible match {
            case rOp : ResourceOperation =>
                if ( rOp.resource <= resource && operationPermission.isPermitted( rOp.operation ) ) true
                else false
            case _ => false
        }
    }

    override def tryCompareTo[ B >: Permission ]( that : B )( implicit evidence : B => PartiallyOrdered[ B ] ) : Option[ Int ] = {
        that match {
            case rOpPerm : ResourceOperationPermission =>
                ( for {
                    resourceComp <- this.resource.tryCompareTo( rOpPerm.resource )
                    operationComp <- this.operationPermission.tryCompareTo( rOpPerm.operationPermission )
                } yield {
                    if ( resourceComp == 0 && operationComp == 0 ) 0
                    else if ( resourceComp > 0 && operationComp == 0 ) 1
                    else if ( resourceComp == 0 && operationComp > 0 ) 1
                    else if ( resourceComp > 0 && operationComp > 0 ) 1
                    else -1
                } )
            case _ => super.tryCompareTo( that )
        }
    }
}
