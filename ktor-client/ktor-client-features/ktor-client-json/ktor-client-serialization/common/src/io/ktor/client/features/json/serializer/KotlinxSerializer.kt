/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.features.json.serializer

import io.ktor.client.call.*
import io.ktor.client.features.json.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.utils.io.core.*
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*

/**
 * A [JsonSerializer] implemented for kotlinx [Serializable] classes.
 */
@OptIn(
    ImplicitReflectionSerializer::class, UnstableDefault::class
)
public class KotlinxSerializer(
    private val json: Json = Json(DefaultJsonConfiguration)
) : JsonSerializer {

    override fun write(data: Any, contentType: ContentType): OutgoingContent {
        @Suppress("UNCHECKED_CAST")
        return TextContent(writeContent(data), contentType)
    }

    internal fun writeContent(data: Any): String = json.stringify(buildSerializer(data, json.context) as KSerializer<Any>, data)

    override fun read(type: TypeInfo, body: Input): Any {
        val text = body.readText()
        val deserializationStrategy = json.context.getContextual(type.type)
        val mapper = deserializationStrategy ?: (type.kotlinType?.let { serializer(it) } ?: type.type.serializer())
        return json.parse(mapper, text)!!
    }

    public companion object {
        /**
         * Default [Json] configuration for [KotlinxSerializer].
         */
        public val DefaultJsonConfiguration: JsonConfiguration = JsonConfiguration(
            isLenient = false,
            ignoreUnknownKeys = false,
            serializeSpecialFloatingPointValues = true,
            useArrayPolymorphism = false
        )
    }
}

@Suppress("UNCHECKED_CAST")
@OptIn(ImplicitReflectionSerializer::class)
private fun buildSerializer(value: Any, module: SerialModule): KSerializer<*> = when (value) {
    is JsonElement -> JsonElementSerializer
    is List<*> -> value.elementSerializer(module).list
    is Array<*> -> value.firstOrNull()?.let { buildSerializer(it, module) } ?: String.serializer().list
    is Set<*> -> value.elementSerializer(module).set
    is Map<*, *> -> {
        val keySerializer = value.keys.elementSerializer(module) as KSerializer<Any>
        val valueSerializer = value.values.elementSerializer(module) as KSerializer<Any>
        MapSerializer(keySerializer, valueSerializer)
    }
    else -> module.getContextualOrDefault(value::class)
}

@OptIn(ImplicitReflectionSerializer::class)
private fun Collection<*>.elementSerializer(module: SerialModule): KSerializer<*> {
    val serializers: List<KSerializer<*>> =
        filterNotNull().map { buildSerializer(it, module) }.distinctBy { it.descriptor.serialName }

    if (serializers.size > 1) {
        error(
            "Serializing collections of different element types is not yet supported. " +
                "Selected serializers: ${serializers.map { it.descriptor.serialName }}"
        )
    }

    val selected = serializers.singleOrNull() ?: String.serializer()

    if (selected.descriptor.isNullable) {
        return selected
    }

    @Suppress("UNCHECKED_CAST")
    selected as KSerializer<Any>

    if (any { it == null }) {
        return selected.nullable
    }

    return selected
}
