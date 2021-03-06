package com.expedia.graphql.generator.extensions

import com.expedia.graphql.exceptions.CouldNotGetNameOfKClassException
import com.expedia.graphql.hooks.NoopSchemaGeneratorHooks
import com.expedia.graphql.hooks.SchemaGeneratorHooks
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Suppress("Detekt.UnusedPrivateClass")
open class KClassExtensionsTest {

    @Suppress("Detekt.FunctionOnlyReturningConstant", "Detekt.UnusedPrivateMember")
    private class MyTestClass(
        val publicProperty: String = "public",
        val filteredProperty: String = "filtered",
        private val privateVal: String = "hidden"
    ) : TestInterface {
        fun publicFunction() = "public function"

        fun filteredFunction() = "filtered function"

        private fun privateTestFunction() = "private function"
    }

    internal class MyInternalClass

    protected class MyProtectedClass

    class MyPublicClass

    internal class UnionSuperclass : TestInterface

    internal class InterfaceSuperclass : InvalidFunctionUnionInterface {
        override fun getTest() = 2
    }

    private enum class MyTestEnum {
        ONE,
        TWO
    }

    private class EmptyConstructorClass {
        val id = 1
    }

    interface TestInterface

    private interface InvalidPropertyUnionInterface {
        val test: Int
            get() = 1
    }

    @Suppress("Detekt.FunctionOnlyReturningConstant")
    interface InvalidFunctionUnionInterface {
        fun getTest() = 1
    }

    private class FilterHooks : SchemaGeneratorHooks {
        override fun isValidProperty(property: KProperty<*>) =
            property.name.contains("filteredProperty").not()

        override fun isValidFunction(function: KFunction<*>) =
            function.name.contains("filteredFunction").not()

        override fun isValidSuperclass(kClass: KClass<*>) =
            kClass.simpleName?.contains("InvalidFunctionUnionInterface")?.not().isTrue()
    }

    private val noopHooks = NoopSchemaGeneratorHooks()

    @Test
    fun `test getting valid properties with no hooks`() {
        val properties = MyTestClass::class.getValidProperties(noopHooks)
        assertEquals(listOf("filteredProperty", "publicProperty"), properties.map { it.name })
    }

    @Test
    fun `test getting valid properties with filter hooks`() {
        val properties = MyTestClass::class.getValidProperties(FilterHooks())
        assertEquals(listOf("publicProperty"), properties.map { it.name })
    }

    @Test
    fun `test getting valid functions with no hooks`() {
        val properties = MyTestClass::class.getValidFunctions(noopHooks)
        assertEquals(listOf("filteredFunction", "publicFunction"), properties.map { it.name })
    }

    @Test
    fun `test getting valid functions with filter hooks`() {
        val properties = MyTestClass::class.getValidFunctions(FilterHooks())
        assertEquals(listOf("publicFunction"), properties.map { it.name })
    }

    @Test
    fun `test getting valid superclass with no hooks`() {
        val superclasses = InterfaceSuperclass::class.getValidSuperclasses(noopHooks)
        assertEquals(listOf("InvalidFunctionUnionInterface"), superclasses.map { it.simpleName })
    }

    @Test
    fun `test getting valid superclass with filter hooks`() {
        val superclasses = InterfaceSuperclass::class.getValidSuperclasses(FilterHooks())
        assertTrue(superclasses.isEmpty())
    }

    @Test
    fun `test getting invalid superclass with no hooks`() {
        val superclasses = UnionSuperclass::class.getValidSuperclasses(noopHooks)
        assertTrue(superclasses.isEmpty())
    }

    @Test
    fun `test findConstructorParamter`() {
        assertNotNull(MyTestClass::class.findConstructorParamter("publicProperty"))
        assertNull(MyTestClass::class.findConstructorParamter("foobar"))
        assertNull(EmptyConstructorClass::class.findConstructorParamter("id"))
        assertNull(TestInterface::class.findConstructorParamter("foobar"))
    }

    @Test
    fun `test enum extension`() {
        assertTrue(MyTestEnum::class.isEnum())
        assertFalse(MyTestClass::class.isEnum())
    }

    @Test
    fun `test list extension`() {
        assertTrue(listOf(1)::class.isList())
        assertTrue(arrayListOf(1)::class.isList())
        assertFalse(arrayOf(1)::class.isList())
        assertFalse(MyTestClass::class.isList())
    }

    @Test
    fun `test array extension`() {
        assertTrue(arrayOf(1)::class.isArray())
        assertTrue(intArrayOf(1)::class.isArray())
        assertFalse(listOf(1)::class.isArray())
        assertFalse(MyTestClass::class.isArray())
    }

    @Test
    fun `test listType extension`() {
        assertTrue(arrayOf(1)::class.isListType())
        assertTrue(intArrayOf(1)::class.isListType())
        assertTrue(listOf(1)::class.isListType())
        assertFalse(MyTestClass::class.isListType())
    }

    @Test
    fun `test graphql interface extension`() {
        assertTrue(TestInterface::class.isInterface())
        assertFalse(MyTestClass::class.isInterface())
    }

    @Test
    fun `test graphql union extension`() {
        assertTrue(TestInterface::class.isUnion())
        assertFalse(InvalidPropertyUnionInterface::class.isUnion())
        assertFalse(InvalidFunctionUnionInterface::class.isUnion())
    }

    @Test
    fun `test class simple name`() {
        assertEquals("MyTestClass", MyTestClass::class.getSimpleName())
        assertFailsWith(CouldNotGetNameOfKClassException::class) {
            object { }::class.getSimpleName()
        }
    }

    @Test
    fun `test input class name`() {
        assertEquals("MyTestClassInput", MyTestClass::class.getSimpleName(true))
    }

    @Test
    fun getQualifiedName() {
        assertEquals("com.expedia.graphql.generator.extensions.KClassExtensionsTest.MyTestClass", MyTestClass::class.getQualifiedName())
        assertEquals("", object { }::class.getQualifiedName())
    }

    @Test
    fun isPublic() {
        assertTrue(MyPublicClass::class.isPublic())
        assertFalse(MyInternalClass::class.isPublic())
        assertFalse(MyProtectedClass::class.isPublic())
        assertFalse(MyTestClass::class.isPublic())
    }
}
