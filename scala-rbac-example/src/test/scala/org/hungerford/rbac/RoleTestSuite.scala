package org.hungerford.rbac

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class RoleTestSuite extends AnyFlatSpecLike with Matchers {
    behavior of "Role partial ordering"

    case object FakePermissible1 extends Operation
    case object FakePermissible2 extends Operation

    private val perm1 = SinglePermission( FakePermissible1 )
    private val perm2 = SinglePermission( FakePermissible2 )
    private val perm12 = Permission( perm1, perm2 )

    private val permRole1 = new PermissionsRole { override val permissions : Permission = perm1 }
    private val permRole2 = new PermissionsRole { override val permissions : Permission = perm2 }
    private val permRole12 = new PermissionsRole { override val permissions : Permission = perm12 }

    it should "compare permissions roles correctly" in {
        permRole1 >= permRole2 shouldBe false
        permRole1 > permRole2 shouldBe false
        permRole1 == permRole2 shouldBe false
        permRole1 <= permRole2 shouldBe false
        permRole1 < permRole2 shouldBe false

        permRole2 >= permRole1 shouldBe false
        permRole2 > permRole1 shouldBe false
        permRole2 == permRole1 shouldBe false
        permRole2 <= permRole1 shouldBe false
        permRole2 < permRole1 shouldBe false

        permRole12 >= permRole1 shouldBe true
        permRole12 > permRole1 shouldBe true
        permRole12 == permRole1 shouldBe false
        permRole12 <= permRole1 shouldBe false
        permRole12 < permRole1 shouldBe false

        permRole1 >= permRole12 shouldBe false
        permRole1 > permRole12 shouldBe false
        permRole1 == permRole12 shouldBe false
        permRole1 <= permRole12 shouldBe true
        permRole1 < permRole12 shouldBe true

        permRole12 >= permRole2 shouldBe true
        permRole12 > permRole2 shouldBe true
        permRole12 == permRole2 shouldBe false
        permRole12 <= permRole2 shouldBe false
        permRole12 < permRole2 shouldBe false

        permRole2 >= permRole12 shouldBe false
        permRole2 > permRole12 shouldBe false
        permRole2 == permRole12 shouldBe false
        permRole2 <= permRole12 shouldBe true
        permRole2 < permRole12 shouldBe true
    }

    it should "compare Roles composed of PermissionRoles correctly" in {
        val roles = permRole1 + permRole2

        roles >= permRole1 shouldBe true
        roles > permRole1 shouldBe true
        roles == permRole1 shouldBe false
        roles <= permRole1 shouldBe false
        roles < permRole1 shouldBe false

        permRole1 >= roles shouldBe false
        permRole1 > roles shouldBe false
        permRole1 == roles shouldBe false
        permRole1 <= roles shouldBe true
        permRole1 < roles shouldBe true

        roles >= permRole2 shouldBe true
        roles > permRole2 shouldBe true
        roles == permRole2 shouldBe false
        roles <= permRole2 shouldBe false
        roles < permRole2 shouldBe false

        permRole2 >= roles shouldBe false
        permRole2 > roles shouldBe false
        permRole2 == roles shouldBe false
        permRole2 <= roles shouldBe true
        permRole2 < roles shouldBe true
    }

    case object ComplexRole1 extends Role {
        override def can( permissible : Permissible ) : Boolean = permissible == FakePermissible1

        override def tryCompareTo[ B >: Role ]( that : B )( implicit evidence : B => PartiallyOrdered[ B ] ) : Option[ Int ] = {
            if ( this == that ) Some( 0 )
            else that match {
                case ComplexRole2 => Some( -1 )
                case ComplexRole3 => Some( -1 )
                case _ => super.tryCompareTo( that )
            }
        }
    }

    case object ComplexRole2 extends Role {
        override def can( permissible : Permissible ) : Boolean = permissible == FakePermissible2

        override def tryCompareTo[ B >: Role ]( that : B )( implicit evidence : B => PartiallyOrdered[ B ] ) : Option[ Int ] = {
            if ( this == that ) Some( 0 )
            else that match {
                case ComplexRole1 => Some( 1 )
                case ComplexRole3 => Some( -1 )
                case _ => super.tryCompareTo( that )
            }
        }
    }

    case object ComplexRole3 extends Role {
        override def can( permissible : Permissible ) : Boolean = permissible == FakePermissible1 || permissible == FakePermissible2

        override def tryCompareTo[ B >: Role ]( that : B )( implicit evidence : B => PartiallyOrdered[ B ] ) : Option[ Int ] = {
            if ( this == that ) Some( 0 )
            else that match {
                case ComplexRole1 => Some( 1 )
                case ComplexRole2 => Some( 1 )
                case _ => super.tryCompareTo( that )
            }
        }
    }

    it should "compare Roles composed of complex roles correctly" in {
        ComplexRole3 >= ComplexRole1 shouldBe true
        ComplexRole3 >= ComplexRole2 shouldBe true

        ComplexRole3 >= (ComplexRole2 + ComplexRole1) shouldBe true
        ComplexRole3 > (ComplexRole2 + ComplexRole1) shouldBe true
        ComplexRole3 <= (ComplexRole2 + ComplexRole1) shouldBe false
        ComplexRole3 < (ComplexRole2 + ComplexRole1) shouldBe false
        (ComplexRole1 + ComplexRole2) <= ComplexRole3 shouldBe true
        (ComplexRole1 + ComplexRole2) < ComplexRole3 shouldBe true
        (ComplexRole1 + ComplexRole2) >= ComplexRole3 shouldBe false
        (ComplexRole1 + ComplexRole2) > ComplexRole3 shouldBe false
        ComplexRole3 == (ComplexRole1 + ComplexRole2) shouldBe false

        (permRole1 + ComplexRole3) >= (permRole1 + ComplexRole2 + ComplexRole1) shouldBe true
        (permRole1 + ComplexRole3) > (permRole1 + ComplexRole2 + ComplexRole1) shouldBe true
        (permRole1 + ComplexRole3) <= (permRole1 + ComplexRole2 + ComplexRole1) shouldBe false
        (permRole1 + ComplexRole3) < (permRole1 + ComplexRole2 + ComplexRole1) shouldBe false

        (permRole1 + ComplexRole3) >= (permRole2 + ComplexRole2 + ComplexRole1) shouldBe false
        (permRole1 + ComplexRole3) > (permRole2 + ComplexRole2 + ComplexRole1) shouldBe false
        (permRole1 + ComplexRole3) <= (permRole2 + ComplexRole2 + ComplexRole1) shouldBe false
        (permRole1 + ComplexRole3) < (permRole2 + ComplexRole2 + ComplexRole1) shouldBe false
    }

    it should "compare RoleManagementRoles correctly" in {
        RoleManagementRole( permRole1, Set( Grant, Retrieve ) ) >= RoleManagementRole( permRole1, Set( Grant ) ) shouldBe true
        RoleManagementRole( permRole1, Set( Grant, Retrieve ) ) > RoleManagementRole( permRole1, Set( Grant ) ) shouldBe true
        RoleManagementRole( permRole1, Set( Grant, Retrieve ) ) <= RoleManagementRole( permRole1, Set( Grant ) ) shouldBe false
        RoleManagementRole( permRole1, Set( Grant, Retrieve ) ) < RoleManagementRole( permRole1, Set( Grant ) ) shouldBe false

        RoleManagementRole( permRole1, Set( Grant ) ) >= RoleManagementRole( permRole1, Set( Grant, Retrieve ) ) shouldBe false
        RoleManagementRole( permRole1, Set( Grant ) ) > RoleManagementRole( permRole1, Set( Grant, Retrieve ) ) shouldBe false
        RoleManagementRole( permRole1, Set( Grant ) ) <= RoleManagementRole( permRole1, Set( Grant, Retrieve ) ) shouldBe true
        RoleManagementRole( permRole1, Set( Grant ) ) < RoleManagementRole( permRole1, Set( Grant, Retrieve ) ) shouldBe true

        RoleManagementRole( permRole1 + permRole2, Set( Grant ) ) >= RoleManagementRole( permRole1, Set( Grant ) ) shouldBe true
        RoleManagementRole( permRole1 + permRole2, Set( Grant ) ) > RoleManagementRole( permRole1, Set( Grant ) ) shouldBe true
        RoleManagementRole( permRole1 + permRole2, Set( Grant ) ) <= RoleManagementRole( permRole1, Set( Grant ) ) shouldBe false
        RoleManagementRole( permRole1 + permRole2, Set( Grant ) ) < RoleManagementRole( permRole1, Set( Grant ) ) shouldBe false

        RoleManagementRole( ComplexRole3, Set( Grant ) ) >= RoleManagementRole( ComplexRole1 + ComplexRole2, Set( Grant ) ) shouldBe true
        RoleManagementRole( ComplexRole3, Set( Grant ) ) > RoleManagementRole( ComplexRole1 + ComplexRole2, Set( Grant ) ) shouldBe true
        RoleManagementRole( ComplexRole3, Set( Grant ) ) <= RoleManagementRole( ComplexRole1 + ComplexRole2, Set( Grant ) ) shouldBe false
        RoleManagementRole( ComplexRole3, Set( Grant ) ) < RoleManagementRole( ComplexRole1 + ComplexRole2, Set( Grant ) ) shouldBe false
    }

    it should "compare ReflexiveRoleManagementRoles correctly" in {
        ReflexiveRoleManagementRole( permRole1, Set( Grant, Retrieve ) ) >= ReflexiveRoleManagementRole( permRole1, Set( Grant ) ) shouldBe true
        ReflexiveRoleManagementRole( permRole1, Set( Grant, Retrieve ) ) > ReflexiveRoleManagementRole( permRole1, Set( Grant ) ) shouldBe true
        ReflexiveRoleManagementRole( permRole1, Set( Grant, Retrieve ) ) <= ReflexiveRoleManagementRole( permRole1, Set( Grant ) ) shouldBe false
        ReflexiveRoleManagementRole( permRole1, Set( Grant, Retrieve ) ) < ReflexiveRoleManagementRole( permRole1, Set( Grant ) ) shouldBe false
        ReflexiveRoleManagementRole( permRole1, Set( Grant ) ) >= ReflexiveRoleManagementRole( permRole1, Set( Grant, Retrieve ) ) shouldBe false
        ReflexiveRoleManagementRole( permRole1, Set( Grant ) ) > ReflexiveRoleManagementRole( permRole1, Set( Grant, Retrieve ) ) shouldBe false
        ReflexiveRoleManagementRole( permRole1, Set( Grant ) ) <= ReflexiveRoleManagementRole( permRole1, Set( Grant, Retrieve ) ) shouldBe true
        ReflexiveRoleManagementRole( permRole1, Set( Grant ) ) < ReflexiveRoleManagementRole( permRole1, Set( Grant, Retrieve ) ) shouldBe true
        ReflexiveRoleManagementRole( permRole1 + permRole2, Set( Grant ) ) >= ReflexiveRoleManagementRole( permRole1, Set( Grant ) ) shouldBe true
        ReflexiveRoleManagementRole( permRole1 + permRole2, Set( Grant ) ) > ReflexiveRoleManagementRole( permRole1, Set( Grant ) ) shouldBe true
        ReflexiveRoleManagementRole( permRole1 + permRole2, Set( Grant ) ) <= ReflexiveRoleManagementRole( permRole1, Set( Grant ) ) shouldBe false
        ReflexiveRoleManagementRole( permRole1 + permRole2, Set( Grant ) ) < ReflexiveRoleManagementRole( permRole1, Set( Grant ) ) shouldBe false
        ReflexiveRoleManagementRole( ComplexRole3, Set( Grant ) ) >= ReflexiveRoleManagementRole( ComplexRole1 + ComplexRole2, Set( Grant ) ) shouldBe true
        ReflexiveRoleManagementRole( ComplexRole3, Set( Grant ) ) > ReflexiveRoleManagementRole( ComplexRole1 + ComplexRole2, Set( Grant ) ) shouldBe true
        ReflexiveRoleManagementRole( ComplexRole3, Set( Grant ) ) <= ReflexiveRoleManagementRole( ComplexRole1 + ComplexRole2, Set( Grant ) ) shouldBe false
        ReflexiveRoleManagementRole( ComplexRole3, Set( Grant ) ) < ReflexiveRoleManagementRole( ComplexRole1 + ComplexRole2, Set( Grant ) ) shouldBe false

        ReflexiveRoleManagementRole( permRole1, Set( Grant, Retrieve ) ) >= RoleManagementRole( permRole1, Set( Grant ) ) shouldBe true
        ReflexiveRoleManagementRole( permRole1, Set( Grant, Retrieve ) ) > RoleManagementRole( permRole1, Set( Grant ) ) shouldBe true
        ReflexiveRoleManagementRole( permRole1, Set( Grant, Retrieve ) ) <= RoleManagementRole( permRole1, Set( Grant ) ) shouldBe false
        ReflexiveRoleManagementRole( permRole1, Set( Grant, Retrieve ) ) < RoleManagementRole( permRole1, Set( Grant ) ) shouldBe false
        ReflexiveRoleManagementRole( permRole1, Set( Grant ) ) >= RoleManagementRole( permRole1, Set( Grant, Retrieve ) ) shouldBe false
        ReflexiveRoleManagementRole( permRole1, Set( Grant ) ) > RoleManagementRole( permRole1, Set( Grant, Retrieve ) ) shouldBe false
        ReflexiveRoleManagementRole( permRole1, Set( Grant ) ) <= RoleManagementRole( permRole1, Set( Grant, Retrieve ) ) shouldBe true
        ReflexiveRoleManagementRole( permRole1, Set( Grant ) ) < RoleManagementRole( permRole1, Set( Grant, Retrieve ) ) shouldBe true
        ReflexiveRoleManagementRole( permRole1 + permRole2, Set( Grant ) ) >= RoleManagementRole( permRole1, Set( Grant ) ) shouldBe true
        ReflexiveRoleManagementRole( permRole1 + permRole2, Set( Grant ) ) > RoleManagementRole( permRole1, Set( Grant ) ) shouldBe true
        ReflexiveRoleManagementRole( permRole1 + permRole2, Set( Grant ) ) <= RoleManagementRole( permRole1, Set( Grant ) ) shouldBe false
        ReflexiveRoleManagementRole( permRole1 + permRole2, Set( Grant ) ) < RoleManagementRole( permRole1, Set( Grant ) ) shouldBe false
        ReflexiveRoleManagementRole( ComplexRole3, Set( Grant ) ) >= RoleManagementRole( ComplexRole1 + ComplexRole2, Set( Grant ) ) shouldBe true
        ReflexiveRoleManagementRole( ComplexRole3, Set( Grant ) ) > RoleManagementRole( ComplexRole1 + ComplexRole2, Set( Grant ) ) shouldBe true
        ReflexiveRoleManagementRole( ComplexRole3, Set( Grant ) ) <= RoleManagementRole( ComplexRole1 + ComplexRole2, Set( Grant ) ) shouldBe false
        ReflexiveRoleManagementRole( ComplexRole3, Set( Grant ) ) < RoleManagementRole( ComplexRole1 + ComplexRole2, Set( Grant ) ) shouldBe false

        val rmr = RoleManagementRole( permRole1, Set( Grant ) )
        ReflexiveRoleManagementRole( permRole1, Set( Grant ) ) >= RoleManagementRole( rmr, Set( Grant ) )
        ReflexiveRoleManagementRole( permRole1, Set( Grant ) ) >= RoleManagementRole( ReflexiveRoleManagementRole( permRole1, Set( Grant ) ), Set( Grant ) )
        ReflexiveRoleManagementRole( permRole1, Set( Grant ) ) >= RoleManagementRole( RoleManagementRole( rmr, Set( Grant ) ), Set( Grant ) )
        ReflexiveRoleManagementRole( permRole1, Set( Grant ) ) >= RoleManagementRole( ReflexiveRoleManagementRole( rmr, Set( Grant ) ), Set( Grant ) )

        val rmr2 = RoleManagementRole( ComplexRole1 + ComplexRole2, Set( Grant ) )
        ReflexiveRoleManagementRole( ComplexRole3, Set( Grant ) ) >= RoleManagementRole( (ComplexRole1 + ComplexRole2) + rmr2, Set( Grant ) )
        ReflexiveRoleManagementRole( ComplexRole3, Set( Grant ) ) >= RoleManagementRole( (ComplexRole1 + ComplexRole2) + ReflexiveRoleManagementRole( permRole1, Set( Grant ) ), Set( Grant ) )
        ReflexiveRoleManagementRole( ComplexRole3, Set( Grant ) ) >= RoleManagementRole( RoleManagementRole( (ComplexRole1 + ComplexRole2) + rmr2, Set( Grant ) ), Set( Grant ) )
        ReflexiveRoleManagementRole( ComplexRole3, Set( Grant ) ) >= RoleManagementRole( ReflexiveRoleManagementRole( (ComplexRole1 + ComplexRole2) + rmr2, Set( Grant ) ), Set( Grant ) )
        ReflexiveRoleManagementRole( ComplexRole3, Set( Grant ) ) >= RoleManagementRole( (ComplexRole1 + ComplexRole2) + RoleManagementRole( (ComplexRole1 + ComplexRole2) + rmr2, Set( Grant ) ), Set( Grant ) )
        ReflexiveRoleManagementRole( ComplexRole3, Set( Grant ) ) >= RoleManagementRole( (ComplexRole1 + ComplexRole2) + ReflexiveRoleManagementRole( (ComplexRole1 + ComplexRole2) + rmr2, Set( Grant ) ), Set( Grant ) )
        ReflexiveRoleManagementRole( ComplexRole3, Set( Grant ) ) >= RoleManagementRole( (ComplexRole1 + ComplexRole2) + RoleManagementRole( rmr2, Set( Grant ) ), Set( Grant ) )
        ReflexiveRoleManagementRole( ComplexRole3, Set( Grant ) ) >= RoleManagementRole( (ComplexRole1 + ComplexRole2) + ReflexiveRoleManagementRole(  rmr2, Set( Grant ) ), Set( Grant ) )
    }

    behavior of "ReflexiveRoleManagementRole.can"

    it should "always permit any RoleManagement permissibles whose roles at any level of nesting are subordinate to this one's" in {
        val simpleRefRMR = ReflexiveRoleManagementRole( permRole1, Set( Grant, Retrieve ) )
        val simpleRMR = RoleManagementRole( permRole1, Set( Grant ) )

        simpleRefRMR.can( FakePermissible1 ) shouldBe false
        simpleRefRMR.can( FakePermissible2 ) shouldBe false
        simpleRefRMR.can( RoleManagement( permRole1, Grant ) ) shouldBe true
        simpleRefRMR.can( RoleManagement( simpleRMR, Grant ) ) shouldBe true
        simpleRefRMR.can( RoleManagement( simpleRefRMR, Grant ) ) shouldBe true
        simpleRefRMR.can( RoleManagement( permRole1 + simpleRMR, Grant ) ) shouldBe true
        simpleRefRMR.can( RoleManagement( permRole1 + simpleRefRMR, Grant ) ) shouldBe true
        simpleRefRMR.can( RoleManagement( RoleManagementRole( simpleRMR, Set( Grant ) ), Grant ) ) shouldBe true
        simpleRefRMR.can( RoleManagement( RoleManagementRole( simpleRefRMR, Set( Grant ) ), Grant ) ) shouldBe true
        simpleRefRMR.can( RoleManagement( ReflexiveRoleManagementRole( simpleRMR, Set( Grant ) ), Grant ) ) shouldBe true
        simpleRefRMR.can( RoleManagement( ReflexiveRoleManagementRole( simpleRefRMR, Set( Grant ) ), Grant ) ) shouldBe true
        simpleRefRMR.can( RoleManagement( permRole1 + RoleManagementRole( simpleRMR, Set( Grant ) ), Grant ) ) shouldBe true
        simpleRefRMR.can( RoleManagement( permRole1 + RoleManagementRole( simpleRefRMR, Set( Grant ) ), Grant ) ) shouldBe true
        simpleRefRMR.can( RoleManagement( permRole1 + ReflexiveRoleManagementRole( simpleRMR, Set( Grant ) ), Grant ) ) shouldBe true
        simpleRefRMR.can( RoleManagement( permRole1 + ReflexiveRoleManagementRole( simpleRefRMR, Set( Grant ) ), Grant ) ) shouldBe true

        val complexRefRMR = ReflexiveRoleManagementRole( ComplexRole1 + ComplexRole2, Set( Grant, Retrieve ) )
        val complexRMR = RoleManagementRole( ComplexRole2, Set( Grant ) )

        complexRefRMR.can( FakePermissible1 ) shouldBe false
        complexRefRMR.can( FakePermissible2 ) shouldBe false
        complexRefRMR.can( RoleManagement( ComplexRole2, Grant ) ) shouldBe true
        complexRefRMR.can( RoleManagement( complexRMR, Grant ) ) shouldBe true
        complexRefRMR.can( RoleManagement( complexRefRMR, Grant ) ) shouldBe true
        complexRefRMR.can( RoleManagement( ComplexRole2 + complexRMR, Grant ) ) shouldBe true
        complexRefRMR.can( RoleManagement( ComplexRole2 + complexRefRMR, Grant ) ) shouldBe true
        complexRefRMR.can( RoleManagement( RoleManagementRole( complexRMR, Set( Grant ) ), Grant ) ) shouldBe true
        complexRefRMR.can( RoleManagement( RoleManagementRole( complexRefRMR, Set( Grant ) ), Grant ) ) shouldBe true
        complexRefRMR.can( RoleManagement( ReflexiveRoleManagementRole( complexRMR, Set( Grant ) ), Grant ) ) shouldBe true
        complexRefRMR.can( RoleManagement( ReflexiveRoleManagementRole( complexRefRMR, Set( Grant ) ), Grant ) ) shouldBe true
        complexRefRMR.can( RoleManagement( ComplexRole2 + RoleManagementRole( complexRMR, Set( Grant ) ), Grant ) ) shouldBe true
        complexRefRMR.can( RoleManagement( ComplexRole2 + RoleManagementRole( complexRefRMR, Set( Grant ) ), Grant ) ) shouldBe true
        complexRefRMR.can( RoleManagement( ComplexRole2 + ReflexiveRoleManagementRole( complexRMR, Set( Grant ) ), Grant ) ) shouldBe true
        complexRefRMR.can( RoleManagement( ComplexRole2 + ReflexiveRoleManagementRole( complexRefRMR, Set( Grant ) ), Grant ) ) shouldBe true
    }
}
