package com.expedia.graphql.sample

import com.expedia.graphql.DirectiveWiringHelper
import com.expedia.graphql.SchemaGeneratorConfig
import com.expedia.graphql.TopLevelObject
import com.expedia.graphql.sample.context.MyGraphQLContextBuilder
import com.expedia.graphql.sample.dataFetchers.SpringDataFetcherFactory
import com.expedia.graphql.sample.directives.DirectiveWiringFactory
import com.expedia.graphql.sample.directives.LowercaseDirectiveWiring
import com.expedia.graphql.sample.exceptions.CustomDataFetcherExceptionHandler
import com.expedia.graphql.sample.extension.CustomSchemaGeneratorHooks
import com.expedia.graphql.sample.mutation.Mutation
import com.expedia.graphql.sample.query.Query
import com.expedia.graphql.toSchema
import com.fasterxml.jackson.module.kotlin.KotlinModule
import graphql.execution.AsyncExecutionStrategy
import graphql.schema.GraphQLSchema
import graphql.schema.idl.SchemaPrinter
import graphql.servlet.DefaultExecutionStrategyProvider
import graphql.servlet.GraphQLErrorHandler
import graphql.servlet.GraphQLInvocationInputFactory
import graphql.servlet.GraphQLObjectMapper
import graphql.servlet.GraphQLQueryInvoker
import graphql.servlet.ObjectMapperConfigurer
import graphql.servlet.SimpleGraphQLHttpServlet
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.web.servlet.ServletRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import javax.servlet.http.HttpServlet
import javax.validation.Validator

@SpringBootApplication
@ComponentScan("com.expedia.graphql")
class Application {

    private val logger = LoggerFactory.getLogger(Application::class.java)

    @Bean
    fun wiringFactory() = DirectiveWiringFactory()

    @Bean
    fun schemaConfig(dataFetcherFactory: SpringDataFetcherFactory, validator: Validator, wiringFactory: DirectiveWiringFactory): SchemaGeneratorConfig = SchemaGeneratorConfig(
        supportedPackages = listOf("com.expedia"),
        hooks = CustomSchemaGeneratorHooks(validator, DirectiveWiringHelper(wiringFactory, mapOf("lowercase" to LowercaseDirectiveWiring()))),
        dataFetcherFactory = dataFetcherFactory
    )

    @Bean
    fun schema(
            queries: List<Query>,
            mutations: List<Mutation>,
            schemaConfig: SchemaGeneratorConfig
    ): GraphQLSchema {
        fun List<Any>.toTopLevelObjectDefs() = this.map {
            TopLevelObject(it)
        }

        val schema = toSchema(
                queries = queries.toTopLevelObjectDefs(),
                mutations = mutations.toTopLevelObjectDefs(),
                config = schemaConfig
        )
        logger.info(SchemaPrinter(
                SchemaPrinter.Options.defaultOptions()
                        .includeScalarTypes(true)
                        .includeExtendedScalarTypes(true)
                        .includeSchemaDefintion(true)
            ).print(schema)
        )
        return schema
    }

    @Bean
    fun contextBuilder() = MyGraphQLContextBuilder()

    @Bean
    fun graphQLInvocationInputFactory(
            schema: GraphQLSchema,
            contextBuilder: MyGraphQLContextBuilder
    ): GraphQLInvocationInputFactory = GraphQLInvocationInputFactory.newBuilder(schema)
            .withGraphQLContextBuilder(contextBuilder)
            .build()

    @Bean
    fun graphQLQueryInvoker(): GraphQLQueryInvoker {
        val exceptionHandler = CustomDataFetcherExceptionHandler()
        val executionStrategyProvider = DefaultExecutionStrategyProvider(AsyncExecutionStrategy(exceptionHandler))

        return GraphQLQueryInvoker.newBuilder()
                .withExecutionStrategyProvider(executionStrategyProvider)
                .build()
    }

    @Bean
    fun graphQLObjectMapper(): GraphQLObjectMapper = GraphQLObjectMapper.newBuilder()
            .withObjectMapperConfigurer(ObjectMapperConfigurer { it.registerModule(KotlinModule()) })
            .withGraphQLErrorHandler(GraphQLErrorHandler { it })
            .build()

    @Bean
    fun graphQLServlet(
            invocationInputFactory: GraphQLInvocationInputFactory,
            queryInvoker: GraphQLQueryInvoker,
            objectMapper: GraphQLObjectMapper
    ): SimpleGraphQLHttpServlet = SimpleGraphQLHttpServlet.newBuilder(invocationInputFactory)
            .withQueryInvoker(queryInvoker)
            .withObjectMapper(objectMapper)
            .build()

    @Bean
    fun graphQLServletRegistration(graphQLServlet: HttpServlet) = ServletRegistrationBean(graphQLServlet, "/graphql")
}

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
