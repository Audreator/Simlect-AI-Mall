package com.simlect.entity.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class PromptTypeEnumTest {

    @Test
    void constants_exposeKeyAndDesc() {
        assertEquals("global", PromptTypeEnum.GLOBAL.getKey());
        assertEquals("全局规则", PromptTypeEnum.GLOBAL.getDesc());
        assertEquals("agent", PromptTypeEnum.AGENT.getKey());
        assertEquals("user_intent", PromptTypeEnum.USER_INTENT.getKey());
        assertEquals("compress", PromptTypeEnum.COMPRESS.getKey());
    }

    @Test
    void getByCode_hitAndMiss() {
        assertEquals(PromptTypeEnum.GLOBAL, PromptTypeEnum.getByCode("GLOBAL"));
        assertEquals(PromptTypeEnum.COMPRESS, PromptTypeEnum.getByCode("COMPRESS"));
        assertNull(PromptTypeEnum.getByCode("NOT_EXIST"));
        assertNull(PromptTypeEnum.getByCode(null));
    }

    @Test
    void getByKey_hitAndMiss() {
        assertEquals(PromptTypeEnum.GLOBAL, PromptTypeEnum.getByKey("global"));
        assertEquals(PromptTypeEnum.PRODUCT_SEARCH, PromptTypeEnum.getByKey("product_search"));
        assertNull(PromptTypeEnum.getByKey("not_exist"));
        assertNull(PromptTypeEnum.getByKey(null));
    }

    @Test
    void getPrompts_returnsListWithValidKeys() {
        var prompts = PromptTypeEnum.getPrompts();
        assertNotNull(prompts);
        for (PromptTypeEnum.Prompt prompt : prompts) {
            assertNotNull(prompt.key());
            assertNotNull(prompt.desc());
            assertFalse(prompt.key().isBlank());
        }
    }
}
