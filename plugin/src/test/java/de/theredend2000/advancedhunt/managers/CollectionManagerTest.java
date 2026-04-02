package de.theredend2000.advancedhunt.managers;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CollectionManagerTest {

    @Test
    public void normalizeCollectionNameLowercasesSpacesAndNulls() {
        assertEquals("", CollectionManager.normalizeCollectionName(null));
        assertEquals("spring_event", CollectionManager.normalizeCollectionName("Spring Event"));
        assertEquals("already_slugged", CollectionManager.normalizeCollectionName("Already_Slugged"));
    }
}