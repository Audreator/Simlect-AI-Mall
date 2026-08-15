package com.simlect.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonUtilsTest {

    @Test
    void toJson_null_returnsNull() {
        assertNull(JsonUtils.toJson(null));
    }

    @Test
    void toJson_string_passthrough() {
        assertEquals("already-json", JsonUtils.toJson("already-json"));
    }

    @Test
    void toJson_object_serializes() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "sim");
        map.put("count", 3);

        String json = JsonUtils.toJson(map);

        assertTrue(json.contains("\"name\":\"sim\""));
        assertTrue(json.contains("\"count\":3"));
    }

    @Test
    void toJson_unserializableValue_throwsIllegalState() {
        Map<String, Object> map = new HashMap<>();
        map.put("stream", new java.io.ByteArrayInputStream(new byte[0]));
        assertThrows(IllegalStateException.class, () -> JsonUtils.toJson(map));
    }

    @Test
    void parseObject_classRoundTrip() {
        Map<String, Object> map = JsonUtils.parseObject("{\"name\":\"sim\"}", Map.class);

        assertNotNull(map);
        assertEquals("sim", map.get("name"));
    }

    @Test
    void parseObject_blankOrNull_returnsNull() {
        assertNull(JsonUtils.parseObject(null, Map.class));
        assertNull(JsonUtils.parseObject("  ", Map.class));
    }

    @Test
    void parseObject_invalidJson_throwsIllegalState() {
        assertThrows(IllegalStateException.class, () -> JsonUtils.parseObject("{broken", Map.class));
    }

    @Test
    void parseObject_typeReference() {
        Map<String, Object> map = JsonUtils.parseObject("{\"a\":1}", new TypeReference<Map<String, Object>>() {
        });

        assertNotNull(map);
        assertEquals(1, map.get("a"));
    }

    @Test
    void parseObject_typeReference_blankReturnsNull() {
        assertNull(JsonUtils.parseObject("", new TypeReference<Map<String, Object>>() {
        }));
    }

    @Test
    void parseArray_numbers() {
        List<Integer> list = JsonUtils.parseArray("[1,2,3]", Integer.class);

        assertEquals(List.of(1, 2, 3), list);
    }

    @Test
    void parseArray_blankReturnsNull() {
        assertNull(JsonUtils.parseArray(null, Integer.class));
    }

    @Test
    void parseArray_invalid_throwsIllegalState() {
        assertThrows(IllegalStateException.class, () -> JsonUtils.parseArray("[1,2", Integer.class));
    }

    @Test
    void parse_objectJson_returnsMap() {
        Object parsed = JsonUtils.parse("{\"k\":\"v\"}");

        assertInstanceOf(Map.class, parsed);
        assertEquals("v", ((Map<?, ?>) parsed).get("k"));
    }

    @Test
    void parse_scalar_returnsScalar() {
        assertEquals(123, JsonUtils.parse("123"));
        assertEquals("abc", JsonUtils.parse("\"abc\""));
    }

    @Test
    void parse_blankOrInvalid() {
        assertNull(JsonUtils.parse(""));
        assertThrows(IllegalStateException.class, () -> JsonUtils.parse("{bad"));
    }

    @Test
    void parseTree_readsNode() {
        JsonNode node = JsonUtils.parseTree("{\"name\":\"sim\"}");

        assertNotNull(node);
        assertEquals("sim", node.get("name").asText());
    }

    @Test
    void parseTree_blankReturnsNull() {
        assertNull(JsonUtils.parseTree("  "));
    }

    @Test
    void createObjectNode_roundTrip() {
        ObjectNode node = JsonUtils.createObjectNode();
        node.put("key", "value");

        assertEquals("value", JsonUtils.parseTree(JsonUtils.toJson(node)).get("key").asText());
    }
}
