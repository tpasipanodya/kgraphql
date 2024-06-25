# KGraphQL

KGraphQL is a [Kotlin](https://kotlinlang.org/) implementation of [GraphQL](http://graphql.org/). It provides a rich DSL to set up the GraphQL schema.
Unlike [aPureBase's original implementation](https://github.com/aPureBase/KGraphQL), this fork allows you to propagate
select Coroutine context elements across all Coroutines. An example use-case for this is propagating
ThreadLocalContextElement that are required for use in data fetching operations.


```kotlin
data class Article(val id: Int, val text: String)

fun main() {
    val schema = KGraphQL.schema {
        query("article") {
            resolver { id: Int?, text: String ->
                Article(id ?: -1, text)
            }
        }
        type<Article> {
            property<String>("fullText") {
                resolver { article: Article ->
                    "${article.id}: ${article.text}"
                }
            }
        }
    }

    schema.execute("""
        {
            article(id: 5, text: "Hello World") {
                id
                fullText
            }
        }
    """.trimIndent()).let(::println)
}
```

## Documentation

See the [documentation](https://kgraphql.io/) for a more detailed explanation of the library.

## Contributing

All contributions are welcome. Feel free to open issues and PRs!

## Versioning

The versioning is following [Semantic Versioning](http://semver.org/)

## License

KGraphQL is Open Source software released under the [MIT license](https://opensource.org/licenses/MIT)
