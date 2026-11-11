package com.netfieldpro.core.json;

public sealed interface JsonValue permits JsonObject, JsonArray, JsonString, JsonInt, JsonBool {
}
