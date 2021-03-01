package org.hungerford.rbac

import org.hungerford.rbac.test.tags.scala.Tags.WipTest
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class RoleTestSuite extends AnyFlatSpecLike with Matchers {
    behavior of "Role partial ordering"

    case object FakePermissible1 extends Operation
    case object FakePermissible2 extends Operation

    private val perm1 = SinglePermission( FakePermissible1 )
    private val perm2 = SinglePermission( FakePermissible2 )
    private val perm12 = Permission( perm1, perm2 )

    private val permRole1 = new Role { override val permissions : Permission = perm1 }
    private val permRole2 = new Role { override val permissions : Permission = perm2 }
    private val permRole12 = new Role { override val permissions : Permission = perm12 }

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

    it should "compare RoleManagementPermissions correctly" in {
        RoleManagementPermission( permRole1, Permission.to( Grant, Retrieve ) ) >= RoleManagementPermission( permRole1, Permission.to( Grant ) ) shouldBe true
        RoleManagementPermission( permRole1, Permission.to( Grant, Retrieve ) ) > RoleManagementPermission( permRole1, Permission.to( Grant ) ) shouldBe true
        RoleManagementPermission( permRole1, Permission.to( Grant, Retrieve ) ) <= RoleManagementPermission( permRole1, Permission.to( Grant ) ) shouldBe false
        RoleManagementPermission( permRole1, Permission.to( Grant, Retrieve ) ) < RoleManagementPermission( permRole1, Permission.to( Grant ) ) shouldBe false

        RoleManagementPermission( permRole1, Permission.to( Grant ) ) >= RoleManagementPermission( permRole1, Permission.to( Grant, Retrieve ) ) shouldBe false
        RoleManagementPermission( permRole1, Permission.to( Grant ) ) > RoleManagementPermission( permRole1, Permission.to( Grant, Retrieve ) ) shouldBe false
        RoleManagementPermission( permRole1, Permission.to( Grant ) ) <= RoleManagementPermission( permRole1, Permission.to( Grant, Retrieve ) ) shouldBe true
        RoleManagementPermission( permRole1, Permission.to( Grant ) ) < RoleManagementPermission( permRole1, Permission.to( Grant, Retrieve ) ) shouldBe true

        RoleManagementPermission( permRole1 + permRole2, Permission.to( Grant ) ) >= RoleManagementPermission( permRole1, Permission.to( Grant ) ) shouldBe true
        RoleManagementPermission( permRole1 + permRole2, Permission.to( Grant ) ) > RoleManagementPermission( permRole1, Permission.to( Grant ) ) shouldBe true
        RoleManagementPermission( permRole1 + permRole2, Permission.to( Grant ) ) <= RoleManagementPermission( permRole1, Permission.to( Grant ) ) shouldBe false
        RoleManagementPermission( permRole1 + permRole2, Permission.to( Grant ) ) < RoleManagementPermission( permRole1, Permission.to( Grant ) ) shouldBe false
    }

    it should "compare RecursiveRoleManagementPermissions correctly" in {
        RecursiveRoleManagementPermission( permRole1, Permission.to( Grant, Retrieve ) ) >= RecursiveRoleManagementPermission( permRole1, Permission.to( Grant ) ) shouldBe true
        RecursiveRoleManagementPermission( permRole1, Permission.to( Grant, Retrieve ) ) > RecursiveRoleManagementPermission( permRole1, Permission.to( Grant ) ) shouldBe true
        RecursiveRoleManagementPermission( permRole1, Permission.to( Grant, Retrieve ) ) <= RecursiveRoleManagementPermission( permRole1, Permission.to( Grant ) ) shouldBe false
        RecursiveRoleManagementPermission( permRole1, Permission.to( Grant, Retrieve ) ) < RecursiveRoleManagementPermission( permRole1, Permission.to( Grant ) ) shouldBe false
        RecursiveRoleManagementPermission( permRole1, Permission.to( Grant ) ) >= RecursiveRoleManagementPermission( permRole1, Permission.to( Grant, Retrieve ) ) shouldBe false
        RecursiveRoleManagementPermission( permRole1, Permission.to( Grant ) ) > RecursiveRoleManagementPermission( permRole1, Permission.to( Grant, Retrieve ) ) shouldBe false
        RecursiveRoleManagementPermission( permRole1, Permission.to( Grant ) ) <= RecursiveRoleManagementPermission( permRole1, Permission.to( Grant, Retrieve ) ) shouldBe true
        RecursiveRoleManagementPermission( permRole1, Permission.to( Grant ) ) < RecursiveRoleManagementPermission( permRole1, Permission.to( Grant, Retrieve ) ) shouldBe true
        RecursiveRoleManagementPermission( permRole1 + permRole2, Permission.to( Grant ) ) >= RecursiveRoleManagementPermission( permRole1, Permission.to( Grant ) ) shouldBe true
        RecursiveRoleManagementPermission( permRole1 + permRole2, Permission.to( Grant ) ) > RecursiveRoleManagementPermission( permRole1, Permission.to( Grant ) ) shouldBe true
        RecursiveRoleManagementPermission( permRole1 + permRole2, Permission.to( Grant ) ) <= RecursiveRoleManagementPermission( permRole1, Permission.to( Grant ) ) shouldBe false
        RecursiveRoleManagementPermission( permRole1 + permRole2, Permission.to( Grant ) ) < RecursiveRoleManagementPermission( permRole1, Permission.to( Grant ) ) shouldBe false

        RecursiveRoleManagementPermission( permRole1, Permission.to( Grant, Retrieve ) ) >= RoleManagementPermission( permRole1, Permission.to( Grant ) ) shouldBe true
        RecursiveRoleManagementPermission( permRole1, Permission.to( Grant, Retrieve ) ) > RoleManagementPermission( permRole1, Permission.to( Grant ) ) shouldBe true
        RecursiveRoleManagementPermission( permRole1, Permission.to( Grant, Retrieve ) ) <= RoleManagementPermission( permRole1, Permission.to( Grant ) ) shouldBe false
        RecursiveRoleManagementPermission( permRole1, Permission.to( Grant, Retrieve ) ) < RoleManagementPermission( permRole1, Permission.to( Grant ) ) shouldBe false
        RecursiveRoleManagementPermission( permRole1, Permission.to( Grant ) ) >= RoleManagementPermission( permRole1, Permission.to( Grant, Retrieve ) ) shouldBe false
        RecursiveRoleManagementPermission( permRole1, Permission.to( Grant ) ) > RoleManagementPermission( permRole1, Permission.to( Grant, Retrieve ) ) shouldBe false
        RecursiveRoleManagementPermission( permRole1, Permission.to( Grant ) ) <= RoleManagementPermission( permRole1, Permission.to( Grant, Retrieve ) ) shouldBe true
        RecursiveRoleManagementPermission( permRole1, Permission.to( Grant ) ) < RoleManagementPermission( permRole1, Permission.to( Grant, Retrieve ) ) shouldBe true
        RecursiveRoleManagementPermission( permRole1 + permRole2, Permission.to( Grant ) ) >= RoleManagementPermission( permRole1, Permission.to( Grant ) ) shouldBe true
        RecursiveRoleManagementPermission( permRole1 + permRole2, Permission.to( Grant ) ) > RoleManagementPermission( permRole1, Permission.to( Grant ) ) shouldBe true
        RecursiveRoleManagementPermission( permRole1 + permRole2, Permission.to( Grant ) ) <= RoleManagementPermission( permRole1, Permission.to( Grant ) ) shouldBe false
        RecursiveRoleManagementPermission( permRole1 + permRole2, Permission.to( Grant ) ) < RoleManagementPermission( permRole1, Permission.to( Grant ) ) shouldBe false

        val rmr = RoleManagementPermission( permRole1, Permission.to( Grant ) )
        RecursiveRoleManagementPermission( permRole1, Permission.to( Grant ) ) >= RoleManagementPermission( Role.from( rmr ), Permission.to( Grant ) )
        RecursiveRoleManagementPermission( permRole1, Permission.to( Grant ) ) >= RoleManagementPermission( Role.from( RecursiveRoleManagementPermission( permRole1, Permission.to( Grant ) ) ), Permission.to( Grant ) )
        RecursiveRoleManagementPermission( permRole1, Permission.to( Grant ) ) >= RoleManagementPermission( Role.from( RoleManagementPermission( Role.from( rmr ), Permission.to( Grant ) ) ), Permission.to( Grant ) )
        RecursiveRoleManagementPermission( permRole1, Permission.to( Grant ) ) >= RoleManagementPermission( Role.from( RecursiveRoleManagementPermission( Role.from( rmr ), Permission.to( Grant ) ) ), Permission.to( Grant ) )
    }

    behavior of "RecursiveRoleManagementPermission.can"

    it should "always permit any RoleManagement permissibles whose roles at any level of nesting are subordinate to this one's" in {
        val simpleRefRMR = Role.from( RecursiveRoleManagementPermission( permRole1, Permission.to( Grant, Retrieve ) ) )
        val simpleRMR = Role.from( RoleManagementPermission( permRole1, Permission.to( Grant ) ) )

        simpleRefRMR.can( FakePermissible1 ) shouldBe false
        simpleRefRMR.can( FakePermissible2 ) shouldBe false
        simpleRefRMR.can( RoleManagement( permRole1, Grant ) ) shouldBe true
        simpleRefRMR.can( RoleManagement( simpleRMR, Grant ) ) shouldBe true
        simpleRefRMR.can( RoleManagement( simpleRefRMR, Grant ) ) shouldBe true
        simpleRefRMR.can( RoleManagement( permRole1 + simpleRMR, Grant ) ) shouldBe true
        simpleRefRMR.can( RoleManagement( permRole2 + simpleRMR, Grant ) ) shouldBe false
        simpleRefRMR.can( RoleManagement( permRole1 + simpleRefRMR, Grant ) ) shouldBe true
        simpleRefRMR.can( RoleManagement( Role.from( RoleManagementPermission( simpleRMR, Permission.to( Grant ) ) ), Grant ) ) shouldBe true
        simpleRefRMR.can( RoleManagement( Role.from( RoleManagementPermission( simpleRefRMR, Permission.to( Grant ) ) ), Grant ) ) shouldBe true
        simpleRefRMR.can( RoleManagement( Role.from( RecursiveRoleManagementPermission( simpleRMR, Permission.to( Grant ) ) ), Grant ) ) shouldBe true
        simpleRefRMR.can( RoleManagement( Role.from( RecursiveRoleManagementPermission( simpleRefRMR, Permission.to( Grant ) ) ), Grant ) ) shouldBe true
        simpleRefRMR.can( RoleManagement( permRole1 + Role.from( RoleManagementPermission( simpleRMR, Permission.to( Grant ) ) ), Grant ) ) shouldBe true
        simpleRefRMR.can( RoleManagement( permRole2 + Role.from( RoleManagementPermission( simpleRMR, Permission.to( Grant ) ) ), Grant ) ) shouldBe false
        simpleRefRMR.can( RoleManagement( permRole1 + Role.from( RoleManagementPermission( simpleRefRMR, Permission.to( Grant ) ) ), Grant ) ) shouldBe true
        simpleRefRMR.can( RoleManagement( permRole1 + Role.from( RecursiveRoleManagementPermission( simpleRMR, Permission.to( Grant ) ) ), Grant ) ) shouldBe true
        simpleRefRMR.can( RoleManagement( permRole1 + Role.from( RecursiveRoleManagementPermission( simpleRefRMR, Permission.to( Grant ) ) ), Grant ) ) shouldBe true
        simpleRefRMR.can( RoleManagement( permRole2 + Role.from( RecursiveRoleManagementPermission( simpleRefRMR, Permission.to( Grant ) ) ), Grant ) ) shouldBe false
    }

    behavior of "SuperUserRole"

    it should "be gte and lte but not gt and lt itself and Role.from(AllPermissions)" taggedAs ( WipTest ) in {
        SuperUserRole >= SuperUserRole shouldBe true
        SuperUserRole <= SuperUserRole shouldBe true
        SuperUserRole > SuperUserRole shouldBe false
        SuperUserRole < SuperUserRole shouldBe false
        SuperUserRole >= Role.from( AllPermissions ) shouldBe true
        SuperUserRole <= Role.from( AllPermissions ) shouldBe true
        SuperUserRole > Role.from( AllPermissions ) shouldBe false
        SuperUserRole < Role.from( AllPermissions ) shouldBe false
        Role.from( AllPermissions ) >= SuperUserRole shouldBe true
        Role.from( AllPermissions ) <= SuperUserRole shouldBe true
        Role.from( AllPermissions ) > SuperUserRole shouldBe false
        Role.from( AllPermissions ) < SuperUserRole shouldBe false
    }

    it should "be greater than everything else" in {
        SuperUserRole >= permRole1 shouldBe true
        permRole1 <= SuperUserRole shouldBe true
    }
    
    behavior of "NoRole"

    it should "be gte and lte but not gt and lt itself and Role.from(NoPermissions)" taggedAs ( WipTest ) in {
        NoRole >= NoRole shouldBe true
        NoRole <= NoRole shouldBe true
        NoRole > NoRole shouldBe false
        NoRole < NoRole shouldBe false
        NoRole >= Role.from( NoPermissions ) shouldBe true
        NoRole <= Role.from( NoPermissions ) shouldBe true
        NoRole > Role.from( NoPermissions ) shouldBe false
        NoRole < Role.from( NoPermissions ) shouldBe false
        Role.from( NoPermissions ) >= NoRole shouldBe true
        Role.from( NoPermissions ) <= NoRole shouldBe true
        Role.from( NoPermissions ) > NoRole shouldBe false
        Role.from( NoPermissions ) < NoRole shouldBe false
    }

    it should "be less than everything else" in {
        NoRole <= permRole1 shouldBe true
        permRole1 >= NoRole shouldBe true
    }

}
