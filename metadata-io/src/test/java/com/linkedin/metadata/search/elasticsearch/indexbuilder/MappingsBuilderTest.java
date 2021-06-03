package com.linkedin.metadata.search.elasticsearch.indexbuilder;

import com.google.common.collect.ImmutableMap;
import com.linkedin.metadata.TestEntitySpecBuilder;
import java.util.Map;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;


public class MappingsBuilderTest {

  @Test
  public void testMappingsBuilder() {
    Map<String, Object> result = MappingsBuilder.getMappings(TestEntitySpecBuilder.getSpec());
    assertEquals(result.size(), 1);
    Map<String, Object> properties = (Map<String, Object>) result.get("properties");
    assertEquals(properties.size(), 10);
    assertEquals(properties.get("urn"), ImmutableMap.of("type", "keyword"));
    assertTrue(properties.containsKey("browsePaths"));
    // KEYWORD
    assertEquals(properties.get("keyPart3"), ImmutableMap.of("type", "keyword", "normalizer", "keyword_normalizer"));

    // TEXT
    Map<String, Object> nestedArrayStringField = (Map<String, Object>) properties.get("nestedArrayStringField");
    assertEquals(nestedArrayStringField.get("type"), "keyword");
    assertEquals(nestedArrayStringField.get("normalizer"), "keyword_normalizer");
    Map<String, Object> nestedArrayStringFieldSubfields = (Map<String, Object>) nestedArrayStringField.get("fields");
    assertEquals(nestedArrayStringFieldSubfields.size(), 1);
    assertTrue(nestedArrayStringFieldSubfields.containsKey("delimited"));

    // TEXT with addToFilters
    Map<String, Object> textField = (Map<String, Object>) properties.get("textFieldOverride");
    assertEquals(textField.get("type"), "keyword");
    assertEquals(textField.get("normalizer"), "keyword_normalizer");
    Map<String, Object> textFieldSubfields = (Map<String, Object>) textField.get("fields");
    assertEquals(textFieldSubfields.size(), 2);
    assertTrue(textFieldSubfields.containsKey("delimited"));
    assertTrue(textFieldSubfields.containsKey("keyword"));

    // TEXT_PARTIAL
    Map<String, Object> textArrayField = (Map<String, Object>) properties.get("textArrayField");
    assertEquals(textArrayField.get("type"), "keyword");
    assertEquals(textArrayField.get("normalizer"), "keyword_normalizer");
    Map<String, Object> textArrayFieldSubfields = (Map<String, Object>) textArrayField.get("fields");
    assertEquals(textArrayFieldSubfields.size(), 2);
    assertTrue(textArrayFieldSubfields.containsKey("delimited"));
    assertTrue(textArrayFieldSubfields.containsKey("ngram"));

    // URN
    Map<String, Object> foreignKey = (Map<String, Object>) properties.get("foreignKey");
    assertEquals(foreignKey.get("type"), "text");
    assertEquals(foreignKey.get("analyzer"), "urn_component");
    Map<String, Object> foreignKeySubfields = (Map<String, Object>) foreignKey.get("fields");
    assertTrue(foreignKeySubfields.isEmpty());

    // URN_PARTIAL
    Map<String, Object> nestedForeignKey = (Map<String, Object>) properties.get("nestedForeignKey");
    assertEquals(nestedForeignKey.get("type"), "text");
    assertEquals(nestedForeignKey.get("analyzer"), "urn_component");
    Map<String, Object> nestedForeignKeySubfields = (Map<String, Object>) nestedForeignKey.get("fields");
    assertEquals(nestedForeignKeySubfields.size(), 1);
    assertTrue(nestedForeignKeySubfields.containsKey("ngram"));
  }
}
