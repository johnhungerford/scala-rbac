package org.hungerford.rbac

/**
 * A partially ordered type for representing hierarchical resources with permissions.
 *
 * Singly linked list that necessarily terminates at singleton [[AllResources]]. Linkage
 * determines partial ordering.
 *
 * Pattern for defining a type of resource:
 *
 * `trait MyResourceType extends [[ResourceType]]`
 *
 * `class MyResourceRoot extends MyResourceType`
 *
 * `class MyResourceNode(override val parent : MyResourceType) extends MyResourceType with Resource[MyResourceType]`
 *
 * @see [[AllResources]]
 * @see [[ResourceType]]
 * @see [[Resource[+T]]
 * @see [[ResourceOperation]]
 * @see [[ResourceOperationPermission]]
 */
sealed trait PermissibleResource extends Permissible with PartiallyOrdered[ PermissibleResource ] {
    /**
     * Test if this is child of another [[PermissibleResource]] instance
     *
     * @param resource : [[PermissibleResource]]
     * @return Boolean
     */
    def childOf( resource : PermissibleResource ) : Boolean

    /**
     * Determines partial ordering (`<,>,<=,>=`).
     *
     * Uses [[PermissibleResource.childOf(PermissibleResource):Boolean]] to determine ordering:
     * `child < parent` is `true` and vice versa.
     *
     * @param that [[PermissibleResource]]
     * @param evidence B => [[PartiallyOrdered]]
     * @tparam B for resolving partial ordering of different but comparable types
     * @return [[ Option[Int] ]]
     */
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

/**
 * Object that all other [[PermissibleResource]] instances are children of.
 */
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

/**
 * [[PermissibleResource]] with a linked `parent`.
 *
 * All public subtypes of [[PermissibleResource]] are of this type and thus have to
 * be linked to another instance of [[PermissibleResource]]. The only available
 * "out-of-the-box" instance is [[AllResources]], so all [[PermissibleResource]]s
 * must eventually link to that.
 */
sealed trait ResourceWithParent extends PermissibleResource {
    val parent : PermissibleResource

    if ( parent == this ) throw new IllegalArgumentException( "A resource cannot be it's own parent" )

    override def childOf( resource : PermissibleResource ) : Boolean = {
        if ( resource == this ) true
        else parent.childOf( resource )
    }
}

/**
 * Any custom sub-type of [[PermissibleResource]] should extend/mix this trait.
 */
trait ResourceType extends ResourceWithParent

/**
 * Defines the node of a [[ResourceType]], restricting its parent to other [[PermissibleResource]]s
 * of its own type.
 *
 * @tparam T Some subtype of [[ResourceType]]
 */
trait Resource[ +T <: ResourceType ] extends ResourceWithParent { this : T =>
    override val parent : T
}

/**
 * Defines an operation on a resource.
 */
trait ResourceOperation extends Permissible {
    val resource : PermissibleResource
    val operation : Operation
}

/**
 * Defines permission to perform certain operations on certain resources.
 *
 * Permits any [[ResourceOperation]] `resOp` where `this.resource >= resOp.resource` and
 * `this.operationPermission.permits(resOp.operation)`.
 *
 * @param resource : [[PermissibleResource]]
 * @param operationPermission : [[Permission]]
 */
case class ResourceOperationPermission( val resource : PermissibleResource, val operationPermission : Permission ) extends SimplePermission {
    override def permits( permissible : Permissible ) : Boolean = {
        permissible match {
            case rOp : ResourceOperation =>
                if ( rOp.resource <= resource && operationPermission.permits( rOp.operation ) ) true
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
