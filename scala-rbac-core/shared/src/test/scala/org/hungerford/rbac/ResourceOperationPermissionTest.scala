package org.hungerford.rbac

import org.hungerford.rbac.test.tags.scala.Tags.WipTest
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class ResourceOperationPermissionTest extends AnyFlatSpecLike with Matchers {

    behavior of "ResourceOperationPermission partial ordering"

    case object TestResource1 extends ResourceType {
        override val parent : PermissibleResource = AllResources
    }

    case object TestResource2 extends ResourceType {
        override val parent : PermissibleResource = TestResource1
    }

    case object TestResource3 extends ResourceType {
        override val parent : PermissibleResource = TestResource2
    }

    case object TestOp1 extends Operation
    case object TestOp2 extends Operation

    val testPerm1 = SinglePermission( TestOp1 )
    val testPerm2 = SinglePermission( TestOp1 )
    val testPerm12 = testPerm1 | testPerm2

    it should "compare based on the comparison of resources when operation permissions are the same" taggedAs( WipTest ) in {
        ResourceOperationPermission( TestResource1, testPerm1 ) >= ResourceOperationPermission( TestResource2, testPerm1 ) shouldBe true
    }

}
