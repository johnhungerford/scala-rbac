package org.hungerford.rbac

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class PermissibleResourceTestSuite extends AnyFlatSpecLike with Matchers {

    behavior of "PermissibleResource.childOf"

    it should "return true for AllResources.childOf( AllResources )" in {
        AllResources.childOf( AllResources ) shouldBe true
    }

    trait TestResource extends ResourceType

    case object TopLevelTestResource extends TestResource {
        override val parent : PermissibleResource = AllResources
    }

    case object SubResource1 extends TestResource with Resource[ TestResource ] {
        override val parent : TestResource = TopLevelTestResource
    }

    case object SubResource2 extends TestResource with Resource[ TestResource ] {
        override val parent : TestResource = TopLevelTestResource
    }

    case object SubResource1Sub1 extends TestResource with Resource[ TestResource ] {
        override val parent : TestResource = SubResource1
    }

    case object SubResource1Sub2 extends TestResource with Resource[ TestResource ] {
        override val parent : TestResource = SubResource1
    }

    case object SubResource2Sub1 extends TestResource with Resource[ TestResource ] {
        override val parent : TestResource = SubResource2
    }

    case object SubResource2Sub2 extends TestResource with Resource[ TestResource ] {
        override val parent : TestResource = SubResource2
    }

    case object SubResource2Sub2Sub1 extends TestResource with Resource[ TestResource ] {
        override val parent : TestResource = SubResource2Sub2
    }

    it should "return false for AllResources.childOf anything else" in {
        AllResources.childOf( SubResource1Sub2 ) shouldBe false
        AllResources.childOf( SubResource1 ) shouldBe false
        AllResources.childOf( SubResource2Sub2Sub1 ) shouldBe false
    }

    it should "return true for anything.childOf( AllPermissions )" in {
        SubResource1Sub2.childOf( AllResources ) shouldBe true
        SubResource1.childOf( AllResources ) shouldBe true
        SubResource2Sub2Sub1.childOf( AllResources ) shouldBe true
    }

    it should "return true for child.childOf(parent)" in {
        SubResource1Sub2.childOf( SubResource1 ) shouldBe true
        SubResource2Sub1.childOf( SubResource2 ) shouldBe true
    }

    it should "return false for parent.childOf(child)" in {
        SubResource1.childOf( SubResource1Sub2 ) shouldBe false
        SubResource2.childOf( SubResource2Sub1 ) shouldBe false
    }

    it should "return false for sibling.childOf(sibling)" in {
        SubResource1.childOf(SubResource2) shouldBe false
    }

    it should "return false for cousin.childOf(cousin)" in {
        SubResource2Sub1.childOf(SubResource1Sub2) shouldBe false
    }

    it should "return false for nephew.childOf(uncle)" in {
        SubResource1.childOf(SubResource2Sub1)
    }

    it should "return true for grandchild.childOf(grandparent)" in {
        SubResource2Sub2Sub1.childOf(SubResource2)
    }

    behavior of "PermissibleResource partial ordering"

    it should "compare AllResources to itself properly" in {
        AllResources <= AllResources shouldBe true
        AllResources >= AllResources shouldBe true
        AllResources == AllResources shouldBe true
        AllResources > AllResources shouldBe false
        AllResources < AllResources shouldBe false
    }

    it should "compare other resources to AllResource properly" in {
        AllResources >= TopLevelTestResource shouldBe true
        AllResources > TopLevelTestResource shouldBe true
        TopLevelTestResource <= AllResources shouldBe true
        TopLevelTestResource < AllResources shouldBe true
        AllResources == TopLevelTestResource shouldBe false
        AllResources <= TopLevelTestResource shouldBe false
        AllResources < TopLevelTestResource shouldBe false
        TopLevelTestResource >= AllResources shouldBe false
        TopLevelTestResource > AllResources shouldBe false

        AllResources >= SubResource2Sub2Sub1 shouldBe true
        AllResources > SubResource2Sub2Sub1 shouldBe true
        ( SubResource2Sub2Sub1 <= AllResources ) shouldBe true
        SubResource2Sub2Sub1 < AllResources shouldBe true
        AllResources == SubResource2Sub2Sub1 shouldBe false
        AllResources <= SubResource2Sub2Sub1 shouldBe false
        AllResources < SubResource2Sub2Sub1 shouldBe false
        ( SubResource2Sub2Sub1 >= AllResources ) shouldBe false
        SubResource2Sub2Sub1 > AllResources shouldBe false
    }

    it should "compare children and parents and grandchildren and grandparents properly" in {
        ( TopLevelTestResource >= SubResource1 ) shouldBe true
        TopLevelTestResource > SubResource1 shouldBe true
        SubResource1 <= TopLevelTestResource shouldBe true
        SubResource1 < TopLevelTestResource shouldBe true
        TopLevelTestResource == SubResource1 shouldBe false
        ( TopLevelTestResource <= SubResource1 ) shouldBe false
        TopLevelTestResource < SubResource1 shouldBe false
        SubResource1 >= TopLevelTestResource shouldBe false
        SubResource1 > TopLevelTestResource shouldBe false

        ( SubResource2 >= SubResource2Sub2Sub1 ) shouldBe true
        SubResource2 > SubResource2Sub2Sub1 shouldBe true
        ( SubResource2Sub2Sub1 <= SubResource2 ) shouldBe true
        SubResource2Sub2Sub1 < SubResource2 shouldBe true
        SubResource2 == SubResource2Sub2Sub1 shouldBe false
        ( SubResource2 <= SubResource2Sub2Sub1 ) shouldBe false
        SubResource2 < SubResource2Sub2Sub1 shouldBe false
        ( SubResource2Sub2Sub1 >= SubResource2 ) shouldBe false
        SubResource2Sub2Sub1 > SubResource2 shouldBe false
    }

    it should "not compare siblings and cousins" in {
        ( SubResource1 >= SubResource2 ) shouldBe false
        ( SubResource1 <= SubResource2 ) shouldBe false
        ( SubResource1 == SubResource2 ) shouldBe false
        ( SubResource1 > SubResource2 ) shouldBe false
        ( SubResource1 < SubResource2 ) shouldBe false

        ( SubResource1Sub1 >= SubResource2Sub1 ) shouldBe false
        ( SubResource1Sub1 <= SubResource2Sub1 ) shouldBe false
        ( SubResource1Sub1 == SubResource2Sub1 ) shouldBe false
        ( SubResource1Sub1 > SubResource2Sub1 ) shouldBe false
        ( SubResource1Sub1 < SubResource2Sub1 ) shouldBe false
    }

    it should "not compare uncles and removed cousins" in {
        ( SubResource1 >= SubResource2Sub1 ) shouldBe false
        ( SubResource1 <= SubResource2Sub1 ) shouldBe false
        ( SubResource1 == SubResource2Sub1 ) shouldBe false
        ( SubResource1 > SubResource2Sub1 ) shouldBe false
        ( SubResource1 < SubResource2Sub1 ) shouldBe false

        ( SubResource1Sub1 >= SubResource2Sub2Sub1 ) shouldBe false
        ( SubResource1Sub1 <= SubResource2Sub2Sub1 ) shouldBe false
        ( SubResource1Sub1 == SubResource2Sub2Sub1 ) shouldBe false
        ( SubResource1Sub1 > SubResource2Sub2Sub1 ) shouldBe false
        ( SubResource1Sub1 < SubResource2Sub2Sub1 ) shouldBe false
    }

}
