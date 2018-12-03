package com.expedia.graphql.sample.query

import com.expedia.graphql.annotations.GraphQLDescription
import com.expedia.graphql.annotations.GraphQLIgnore
import com.expedia.graphql.sample.directives.CustomDirective
import org.springframework.stereotype.Component
import java.net.URL
import java.util.Random

/**
 * Example queries.
 */
@Component
class SimpleQuery: Query {

    @Deprecated(message = "this query is deprecated", replaceWith = ReplaceWith("shinyNewQuery"))
    @GraphQLDescription("old query that should not be used always returns false")
    fun simpleDeprecatedQuery(): Boolean = false

    @GraphQLDescription("new query that always returns true")
    fun shinyNewQuery(): Boolean = true

    @GraphQLDescription("echoes back the msg")
    fun experimentalEcho(msg: String): String = msg

    @GraphQLIgnore
    fun notPartOfSchema() = "ignore me!"

    private fun privateFunctionsAreNotVisible() = "ignored private function"

    @GraphQLDescription("performs some operation")
    @CustomDirective
    fun doSomething(@GraphQLDescription("super important value")
                    value: Int): Boolean = true

    @GraphQLDescription("generates pseudo random int and returns it if it is less than 50")
    fun generateNullableNumber(): Int? {
        val num = Random().nextInt(100)
        return if (num < 50) num else null
    }

    @GraphQLDescription("generates pseudo random int")
    fun generateNumber(): Int = Random().nextInt(100)

    @GraphQLDescription("generates pseudo random list of ints")
    fun generateList(): List<Int> {
        val random = Random()
        return (1..10).map { random.nextInt(100) }.toList()
    }

    @GraphQLDescription("query with optional input")
    fun doSomethingWithOptionalInput(
            @GraphQLDescription("this field is required") requiredValue: Int,
            @GraphQLDescription("this field is optional") optionalValue: Int? = null)
            = "required value=$requiredValue, optional value=$optionalValue"

    fun someUrlScalar() = URL("https://github.com")
}