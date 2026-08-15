package com.simlect.utils;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CollectionCompareTest {

    private static final Function<String, String> SELF_ID = Function.identity();

    @Test
    void compare_detectsAddUpdateDelete() {
        CollectionCompare<String> comparator = new CollectionCompare<>();

        CollectionCompare.CompareResult<String> result =
                comparator.compare(List.of("a", "b", "c"), List.of("b", "c", "d"), SELF_ID);

        assertEquals(List.of("d"), result.addList);
        assertEquals(List.of("a"), result.deleteList);
        assertEquals(List.of("b", "c"), result.updateList);
    }

    @Test
    void compare_emptyOldList_allNewAreAdds() {
        CollectionCompare<String> comparator = new CollectionCompare<>();

        CollectionCompare.CompareResult<String> result =
                comparator.compare(List.of(), List.of("x", "y"), SELF_ID);

        assertEquals(List.of("x", "y"), result.addList);
        assertTrue(result.deleteList.isEmpty());
        assertTrue(result.updateList.isEmpty());
    }

    @Test
    void compare_emptyNewList_allOldAreDeletes() {
        CollectionCompare<String> comparator = new CollectionCompare<>();

        CollectionCompare.CompareResult<String> result =
                comparator.compare(List.of("a", "b"), List.of(), SELF_ID);

        assertEquals(List.of("a", "b"), result.deleteList);
        assertTrue(result.addList.isEmpty());
        assertTrue(result.updateList.isEmpty());
    }

    @Test
    void compare_identicalLists_nothingToChange() {
        CollectionCompare<String> comparator = new CollectionCompare<>();

        CollectionCompare.CompareResult<String> result =
                comparator.compare(List.of("a", "b"), List.of("a", "b"), SELF_ID);

        assertTrue(result.addList.isEmpty());
        assertTrue(result.deleteList.isEmpty());
        assertEquals(List.of("a", "b"), result.updateList);
    }
}
