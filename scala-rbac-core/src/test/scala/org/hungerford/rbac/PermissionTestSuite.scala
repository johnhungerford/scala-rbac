package org.hungerford.rbac

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class PermissionTestSuite extends AnyFlatSpecLike with Matchers {

    object TestOperation1 extends Operation
    object TestOperation2 extends Operation
    object TestOperation3 extends Operation
    object TestOperation4 extends Operation

    val perm1 = SinglePermission( TestOperation1 )
    val perm2 = SinglePermission( TestOperation2 )
    val perm3 = SinglePermission( TestOperation3 )
    val perm4 = SinglePermission( TestOperation4 )

    behavior of "Permission.union"

    it should "generate a PermissionSet from SimplePermissions" in {
        perm1 | perm2 match {
            case ps : PermissionSet =>
                ps.toSet should have size ( 2 )
                ps.toSet should contain( perm1 )
                ps.toSet should contain( perm2 )
            case _ => fail( "perm1 | perm2 was not PermissionSet" )
        }

        perm1 | perm2 | perm3 | perm4 match {
            case ps : PermissionSet => ps.toSet should have size ( 4 )
            case _ => fail( "union of SimplePermission did not produce PermissionSet" )
        }
    }

    it should "remove duplicate SimplePermissions" in {
        perm1 | perm1 | perm2 shouldBe perm1 | perm2
        ( perm1 | perm1 | perm2 ).asInstanceOf[ PermissionSet ].toSet should have size ( 2 )
    }

    it should "combine non-intersecting PermissionSets of SimplePermissions" in {
        Permission( perm1, perm2 ) | Permission( perm3, perm4 ) shouldBe Permission( perm1, perm2, perm3, perm4 )
    }

    it should "combine intersecting PermissionSets without repeating intersection" in {
        Permission( perm1, perm2, perm3 ) | Permission( perm2, perm3, perm4 ) shouldBe (perm1 | perm2 | perm3 | perm4)
        (Permission( perm1, perm2, perm3 ) | Permission( perm2, perm3, perm4 ))
          .asInstanceOf[ PermissionSet ].toSet should have size( 4 )
    }

    it should "remove the difference of a PermissionDifference if it is unioned with its difference" in {
        PermissionDifference( perm1, perm2 ) | perm2 shouldBe perm1 | perm2
        PermissionDifference( perm1, Permission( perm2, perm3 ) ) | (perm2 | perm3) shouldBe (perm1 | perm2 | perm3)
        PermissionDifference( perm1, PermissionDifference( perm2, perm3 ) ) | PermissionDifference( perm2, perm3 ) shouldBe (PermissionDifference( perm2, perm3 ) | perm1)
    }

    it should "return AllPermissions when AllPermissions is one of the elements" in {
        perm1 | perm2 | (perm3 - perm4) | AllPermissions | (perm4 - perm2) shouldBe AllPermissions
    }

    it should "filter out NoPermissions" in {
        perm1 | NoPermissions shouldBe perm1
        NoPermissions | perm3 shouldBe perm3
        perm1 | perm3 | NoPermissions | (perm2 - perm4) shouldBe perm1 | (perm2 - perm4) | perm3
    }

    behavior of "Permission.diff"

    it should "return NoPermissions if subject and object are same" in {
        AllPermissions - AllPermissions shouldBe NoPermissions
        perm1 - perm1 shouldBe NoPermissions
        (perm1 | perm3 | perm4) - (perm3 | perm1 | perm4) shouldBe NoPermissions
        ((perm3 - perm4) - (perm3- perm4)) shouldBe NoPermissions
    }

    it should "return subject when object is NoPermissions" in {
        AllPermissions - NoPermissions shouldBe AllPermissions
        NoPermissions - NoPermissions shouldBe NoPermissions
        perm1 - NoPermissions shouldBe perm1
        (perm1 | perm2 | perm3) - NoPermissions shouldBe perm1 | perm2 | perm3
        PermissionDifference(perm1, perm2) - NoPermissions shouldBe (perm1 - perm2)
    }

    it should "return NoPermissions if object is AllPermissions" in {
        NoPermissions - AllPermissions shouldBe NoPermissions
        AllPermissions - AllPermissions shouldBe NoPermissions
        perm1 - AllPermissions shouldBe NoPermissions
        (perm1 | perm2 | perm3) - AllPermissions shouldBe NoPermissions
        PermissionDifference( perm1, perm3 ) - AllPermissions shouldBe NoPermissions
        PermissionDifference( (perm1 | perm4), (perm2 | perm3)) - AllPermissions shouldBe NoPermissions
    }

    it should "filter a PermissionSet's intersecting permissions, but still make a PermissionDifference" in {
        (perm1 | perm2 | perm3 | perm4) - (perm2 | perm3) shouldBe PermissionDifference( (perm1 | perm4), (perm2 | perm3) )
    }

    it should "subtract from a PermissionDifference by replacing its difference (p2) with that difference's union with the subtracted Permission" in {
        ( perm1 - perm2 ) - perm3 shouldBe PermissionDifference( perm1, perm2 | perm3 )
        ( perm2 - ( perm3 - perm4 ) ) - perm4 shouldBe PermissionDifference( perm2, perm3 | perm4 )
    }

    behavior of "Permission.permits"

    it should "return true only for specific operation for OperationPermission" in {
        perm1.permits( TestOperation1 ) shouldBe true
        perm1.permits( TestOperation2 ) shouldBe false
        perm1.permits( TestOperation3 ) shouldBe false
        perm1.permits( TestOperation4 ) shouldBe false

        perm3.permits( TestOperation3 ) shouldBe true
        perm3.permits( TestOperation1 ) shouldBe false
        perm3.permits( TestOperation2 ) shouldBe false
        perm3.permits( TestOperation4 ) shouldBe false
    }

    it should "return true for any operation whose associated OperationPermission is in a PermissionSet" in {
        val permSet = perm1 | perm2
        permSet.permits( TestOperation1 ) shouldBe true
        permSet.permits( TestOperation2 ) shouldBe true
        permSet.permits( TestOperation3 ) shouldBe false
        permSet.permits( TestOperation4 ) shouldBe false
    }

    it should "return true for any operation when called from AllPermissions" in {
        AllPermissions.permits( TestOperation1 ) shouldBe true
        AllPermissions.permits( TestOperation2 ) shouldBe true
        AllPermissions.permits( TestOperation3 ) shouldBe true
        AllPermissions.permits( TestOperation4 ) shouldBe true
    }

    it should "return false for any operation when called from NoPermissions" in {
        NoPermissions.permits( TestOperation1 ) shouldBe false
        NoPermissions.permits( TestOperation2 ) shouldBe false
        NoPermissions.permits( TestOperation3 ) shouldBe false
        NoPermissions.permits( TestOperation4 ) shouldBe false
    }

    val complexPerm1 = new SimplePermission {
        override def permits( permissible : Permissible ) : Boolean = permissible match {
            case TestOperation1 => true
            case TestOperation2 => true
            case TestOperation3 => true
            case _ => false
        }
    }

    val complexPerm2 = new SimplePermission {
        override def permits( permissible : Permissible ) : Boolean = permissible match {
            case TestOperation2 => true
            case TestOperation3 => true
            case TestOperation4 => true
            case _ => false
        }
    }

    it should "return true only for those operations permitted by first parameter of PermissionDifference that aren't permitted by second parameter" in {
        (complexPerm1 - complexPerm2).permits( TestOperation1 ) shouldBe true
        (complexPerm1 - complexPerm2).permits( TestOperation2 ) shouldBe false
        (complexPerm1 - complexPerm2).permits( TestOperation3 ) shouldBe false
        (complexPerm1 - complexPerm2).permits( TestOperation4 ) shouldBe false

        (complexPerm2 - complexPerm1).permits( TestOperation1 ) shouldBe false
        (complexPerm2 - complexPerm1).permits( TestOperation2 ) shouldBe false
        (complexPerm2 - complexPerm1).permits( TestOperation3 ) shouldBe false
        (complexPerm2 - complexPerm1).permits( TestOperation4 ) shouldBe true

        (complexPerm1 - perm2).permits( TestOperation1 ) shouldBe true
        (complexPerm1 - perm2).permits( TestOperation2 ) shouldBe false
        (complexPerm1 - perm2).permits( TestOperation3 ) shouldBe true
        (complexPerm1 - perm2).permits( TestOperation4 ) shouldBe false

        (complexPerm2 - (perm2 | perm4)).permits( TestOperation1 ) shouldBe false
        (complexPerm2 - (perm2 | perm4)).permits( TestOperation2 ) shouldBe false
        (complexPerm2 - (perm2 | perm4)).permits( TestOperation3 ) shouldBe true
        (complexPerm2 - (perm2 | perm4)).permits( TestOperation4 ) shouldBe false
    }

    behavior of "Permission partial ordering"

    it should "show AllPermissions as always being greater than or equal to everthing else" in {
        ( AllPermissions > NoPermissions ) shouldBe true
        ( NoPermissions < AllPermissions ) shouldBe true
        ( AllPermissions > perm1 ) shouldBe true
        ( perm1 < AllPermissions ) shouldBe true
        ( AllPermissions == AllPermissions ) shouldBe true
        ( AllPermissions > (perm1 | perm2 | perm3 ) ) shouldBe true
        ( ( perm1 | perm2 | perm3 ) < AllPermissions ) shouldBe true
        ( AllPermissions > (perm1 - perm2) ) shouldBe true
        ( (perm1 - perm2) < AllPermissions ) shouldBe true
        ( AllPermissions > ( ( perm1 | perm2 ) - ( perm3 | perm4 ) ) ) shouldBe true
        ( ( ( perm1 | perm2 ) - ( perm3 | perm4 ) ) < AllPermissions ) shouldBe true
        ( AllPermissions > complexPerm1 ) shouldBe true
        ( complexPerm1 < AllPermissions ) shouldBe true
    }

    it should "show NoPermissions as always being less than everything else or equal to itself, except a PermissionDifference (which is unknown...)" in {
        ( AllPermissions > NoPermissions ) shouldBe true
        ( NoPermissions < AllPermissions ) shouldBe true
        ( NoPermissions < perm1 ) shouldBe true
        ( perm1 > NoPermissions ) shouldBe true
        ( NoPermissions < perm1 ) shouldBe true
        ( NoPermissions == NoPermissions ) shouldBe true
        ( NoPermissions < (perm1 | perm2 | perm3 ) ) shouldBe true
        ( ( perm1 | perm2 | perm3 ) > NoPermissions ) shouldBe true
        ( NoPermissions < complexPerm1 ) shouldBe true
        ( complexPerm1 > NoPermissions ) shouldBe true

        // NoPermissions should not be comparable with PermissionDifference
        ( NoPermissions < (perm1 - perm2) ) shouldBe false
        ( NoPermissions > (perm1 - perm2) ) shouldBe false
        ( (perm1 - perm2) > NoPermissions ) shouldBe false
        ( (perm1 - perm2) < NoPermissions ) shouldBe false
        ( NoPermissions < ( ( perm1 | perm2 ) - ( perm3 | perm4 ) ) ) shouldBe false
        ( NoPermissions > ( ( perm1 | perm2 ) - ( perm3 | perm4 ) ) ) shouldBe false
        ( ( ( perm1 | perm2 ) - ( perm3 | perm4 ) ) > NoPermissions ) shouldBe false
        ( ( ( perm1 | perm2 ) - ( perm3 | perm4 ) ) < NoPermissions ) shouldBe false
    }

    it should "return false when incomparable permissions are compared" in {
        ( perm1 > perm2 ) shouldBe false
        ( perm1 < perm2 ) shouldBe false
        ( perm1 <= perm2 ) shouldBe false
        ( perm1 >= perm2 ) shouldBe false

        ( complexPerm1 > complexPerm2 ) shouldBe false
        ( complexPerm1 < complexPerm2 ) shouldBe false
        ( complexPerm1 <= complexPerm2 ) shouldBe false
        ( complexPerm1 >= complexPerm2 ) shouldBe false
    }

    it should "show a PermissionSet as being greater than a subset of permissions within it" in {
        ( ( perm1 | perm2 | perm3 | complexPerm1 ) > perm2 ) shouldBe true
        ( perm2 < ( perm1 | perm2 | perm3 | complexPerm1 ) ) shouldBe true
        ( ( perm1 | perm2 | perm3 | complexPerm1 ) > ( perm1 | complexPerm1 ) ) shouldBe true
        ( ( perm1 | complexPerm1 ) < ( perm1 | perm2 | perm3 | complexPerm1 ) ) shouldBe true
    }

    it should "show a PermissionDifference as being greater than its difference (p2)" in {
        ( ( perm1 - perm2 ) > perm2 ) shouldBe true
        ( perm2 < ( perm1 - perm2 ) ) shouldBe true
    }

    it should "show a PermissionDifference where the difference (p2) is a PermissionSet greater than any Permission within that set" in {
        ( ( perm1 - ( perm2 | perm3 ) ) > perm3 ) shouldBe true
        ( perm3 < ( perm1 - ( perm2 | perm3 ) ) ) shouldBe true
    }

    behavior of "PermissionManagementPermission"

    it should "permit a permission management operation on defined permissions or lower" in {
        val permPerm = PermissionManagementPermission( perm1 | perm2 | perm3, Permission.to( Grant, Retrieve ) )

        permPerm.permits( PermissionManagement( perm1, Grant ) ) shouldBe true
        permPerm.permits( PermissionManagement( perm2, Retrieve ) ) shouldBe true
        permPerm.permits( PermissionManagement( perm1 | perm3, Grant ) ) shouldBe true
        permPerm.permits( PermissionManagement( perm1 | perm2 | perm3, Retrieve ) ) shouldBe true
    }

    it should "not permit a permission management operation on permissions orthogonal to its own" in {
        val permPerm = PermissionManagementPermission( perm1 | perm2 | perm3, Permission.to( Grant, Retrieve ) )
        case object TestOp extends PermissionOperation

        permPerm.permits( PermissionManagement( perm4, Grant ) ) shouldBe false
        permPerm.permits( PermissionManagement( perm1, TestOp ) ) shouldBe false
        permPerm.permits( PermissionManagement( perm4, TestOp ) ) shouldBe false
    }

    behavior of "RecursivePermissionManagementPermission"

    it should "permit a permission management operation on defined permissions or lower" in {
        val permPerm = RecursivePermissionManagementPermission( perm1 | perm2 | perm3, Permission.to( Grant, Retrieve ) )

        permPerm.permits( PermissionManagement( perm1, Grant ) ) shouldBe true
        permPerm.permits( PermissionManagement( perm2, Retrieve ) ) shouldBe true
        permPerm.permits( PermissionManagement( perm1 | perm3, Grant ) ) shouldBe true
        permPerm.permits( PermissionManagement( perm1 | perm2 | perm3, Retrieve ) ) shouldBe true
    }

    it should "not permit a permission management operation on permissions orthogonal to its own" in {
        val permPerm = RecursivePermissionManagementPermission( perm1 | perm2 | perm3, Permission.to( Grant, Retrieve ) )
        case object TestOp extends PermissionOperation

        permPerm.permits( PermissionManagement( perm4, Grant ) ) shouldBe false
        permPerm.permits( PermissionManagement( perm1, TestOp ) ) shouldBe false
        permPerm.permits( PermissionManagement( perm4, TestOp ) ) shouldBe false
    }

    it should "permit a permission management operation defined on itself or equivalent non-recursive version of itself with permission operations it permits" in {
        val permPerm = RecursivePermissionManagementPermission( perm1 | perm2 | perm3, Permission.to( Grant, Retrieve ) )
        val permNonRec = PermissionManagementPermission( perm1 | perm2 | perm3, Permission.to( Grant, Retrieve ) )

        permPerm.permits( PermissionManagement( permPerm, Grant ) ) shouldBe true
        permPerm.permits( PermissionManagement( permNonRec, Grant ) ) shouldBe true
        permPerm.permits( PermissionManagement( permPerm, Retrieve ) ) shouldBe true
        permPerm.permits( PermissionManagement( permNonRec, Retrieve ) ) shouldBe true
    }

    it should "not permit a permission management operation defined on itself or equivalent non-recursive version of itself with permission operations it does not permit" in {
        val permPerm = RecursivePermissionManagementPermission( perm1 | perm2 | perm3, Permission.to( Grant, Retrieve ) )
        val permNonRec = PermissionManagementPermission( perm1 | perm2 | perm3, Permission.to( Grant, Retrieve ) )
        case object TestOp extends PermissionOperation

        permPerm.permits( PermissionManagement( permPerm, TestOp ) ) shouldBe false
        permPerm.permits( PermissionManagement( permNonRec, TestOp ) ) shouldBe false
    }

    it should "permit a permission management operation defined on itself or equivalent non-recursive version of itself and other permissions it is defined by with permission operations it permits" in {
        val permPerm = RecursivePermissionManagementPermission( perm1 | perm2 | perm3, Permission.to( Grant, Retrieve ) )
        val permNonRec = PermissionManagementPermission( perm1 | perm2 | perm3, Permission.to( Grant, Retrieve ) )

        permPerm.permits( PermissionManagement( perm1 | permPerm, Grant ) ) shouldBe true
        permPerm.permits( PermissionManagement( perm1 | permNonRec, Grant ) ) shouldBe true
        permPerm.permits( PermissionManagement( perm2 | perm3 | permPerm, Retrieve ) ) shouldBe true
        permPerm.permits( PermissionManagement( perm2 | perm3 | permNonRec, Retrieve ) ) shouldBe true
    }

    it should "not permit a permission management operation defined on itself or equivalent non-recursive version of itself and other permissions it is not defined by, even with permission operations it permits" in {
        val permPerm = RecursivePermissionManagementPermission( perm1 | perm2 | perm3, Permission.to( Grant, Retrieve ) )
        val permNonRec = PermissionManagementPermission( perm1 | perm2 | perm3, Permission.to( Grant, Retrieve ) )

        permPerm.permits( PermissionManagement( perm4 | permPerm, Grant ) ) shouldBe false
        permPerm.permits( PermissionManagement( perm4 | permNonRec, Grant ) ) shouldBe false
        permPerm.permits( PermissionManagement( perm2 | perm4 | permPerm, Retrieve ) ) shouldBe false
        permPerm.permits( PermissionManagement( perm2 | perm4 | permNonRec, Retrieve ) ) shouldBe false
    }

    it should "permit a permission management operation with complicated nested versions of itself" in {
        val permPerm = RecursivePermissionManagementPermission( perm1 | perm2 | perm3, Permission.to( Grant, Retrieve ) )
        val permNonRec = PermissionManagementPermission( perm1 | perm2 | perm3, Permission.to( Grant, Retrieve ) )

        permPerm.permits( PermissionManagement( perm1 | PermissionManagementPermission( perm2 | permPerm, Permission.to( Retrieve ) ), Grant ) ) shouldBe true
        permPerm.permits( PermissionManagement( perm1 | PermissionManagementPermission( perm4 | permPerm, Permission.to( Retrieve ) ), Grant ) ) shouldBe false
    }

    it should "permit a permission management operation with a PermissionOperation that is not also a RoleOperation" in {
        case object TestOp extends PermissionOperation

        RecursivePermissionManagementPermission( perm1, Permission.to( TestOp ) )
          .permits( PermissionManagement( perm1, TestOp ) ) shouldBe true
    }
}
