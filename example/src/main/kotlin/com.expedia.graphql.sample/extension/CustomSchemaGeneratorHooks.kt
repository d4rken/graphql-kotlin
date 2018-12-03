package com.expedia.graphql.sample.extension

import com.expedia.graphql.sample.directives.DirectiveWiringFactory
import com.expedia.graphql.sample.validation.DataFetcherExecutionValidator
import com.expedia.graphql.schema.generator.directive.DirectiveWiringHelper
import com.expedia.graphql.schema.hooks.DataFetcherExecutionPredicate
import com.expedia.graphql.schema.hooks.SchemaGeneratorHooks
import graphql.language.StringValue
import graphql.scalars.url.UrlScalar
import graphql.schema.Coercing
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLType
import java.net.URL
import java.util.*
import javax.validation.Validator
import kotlin.reflect.KClass
import kotlin.reflect.KType

/**
 * Schema generator hook that adds additional scalar types.
 */
class CustomSchemaGeneratorHooks(validator: Validator, wiringFactory: DirectiveWiringFactory) : SchemaGeneratorHooks {
    private val directiveWiringHelper = DirectiveWiringHelper(wiringFactory)

    val urlScalar = UrlScalar()
    /**
     * Register additional GraphQL scalar types.
     */
    override fun willGenerateGraphQLType(type: KType): GraphQLType? = when (type.classifier as? KClass<*>) {
        UUID::class -> graphqlUUIDType
        URL::class -> urlScalar
        else -> null
    }

    override fun onRewireGraphQLType(type: KType, generatedType: GraphQLType): GraphQLType {
        return directiveWiringHelper.onWire(generatedType)
    }

    override val dataFetcherExecutionPredicate: DataFetcherExecutionPredicate? = DataFetcherExecutionValidator(validator)
}

internal val graphqlUUIDType = GraphQLScalarType("UUID",
        "A type representing a formatted java.util.UUID",
        UUIDCoercing
)

private object UUIDCoercing : Coercing<UUID, String> {
    override fun parseValue(input: Any?): UUID = UUID.fromString(
            serialize(
                    input
            )
    )

    override fun parseLiteral(input: Any?): UUID? {
        val uuidString = (input as? StringValue)?.value
        return UUID.fromString(uuidString)
    }

    override fun serialize(dataFetcherResult: Any?): String = dataFetcherResult.toString()
}
