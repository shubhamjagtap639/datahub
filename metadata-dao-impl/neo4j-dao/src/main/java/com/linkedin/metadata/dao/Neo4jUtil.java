package com.linkedin.metadata.dao;

import com.linkedin.common.urn.CorpGroupUrn;
import com.linkedin.common.urn.DatasetUrn;
import com.linkedin.common.urn.Urn;
import com.linkedin.common.urn.CorpuserUrn;
import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.metadata.dao.exception.ModelConversionException;
import com.linkedin.metadata.dao.utils.RecordUtils;
import com.linkedin.metadata.entity.CorpGroupEntity;
import com.linkedin.metadata.entity.CorpUserEntity;
import com.linkedin.metadata.entity.DatasetEntity;
import com.linkedin.metadata.query.Condition;
import com.linkedin.metadata.query.Criterion;
import com.linkedin.metadata.query.CriterionArray;
import com.linkedin.metadata.query.Filter;
import com.linkedin.metadata.query.RelationshipDirection;
import com.linkedin.metadata.query.RelationshipFilter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.ClassUtils;
import org.neo4j.driver.v1.types.Node;
import org.neo4j.driver.v1.types.Relationship;


public class Neo4jUtil {

  public static final String URN_FIELD = "urn";
  public static final String SOURCE_FIELD = "source";
  public static final String DESTINATION_FIELD = "destination";

  private Neo4jUtil() {
    // Util class
  }

  // TODO: relationship model change or auto generate by scanning all entity models
  static final Map<String, String> URN_TO_ENTITY_TYPE = Collections.unmodifiableMap(new HashMap<String, String>() {

    {
      put(CorpuserUrn.ENTITY_TYPE, getType(CorpUserEntity.class));
      put(CorpGroupUrn.ENTITY_TYPE, getType(CorpGroupEntity.class));
      put(DatasetUrn.ENTITY_TYPE, getType(DatasetEntity.class));

      // For unit testing only
      // TODO: auto generate through models, and make 1-1 mapping for testing urn and entity type
      put("entityFoo", "`com.linkedin.testing.EntityFoo`");
      put("entityBar", "`com.linkedin.testing.EntityBar`");
      put("entityBaz", "`com.linkedin.testing.EntityBaz`");
    }
  });

  /**
   * Converts ENTITY to node (field:value map)
   *
   * @param entity ENTITY defined in models
   * @return unmodifiable field value map
   */
  @Nonnull
  public static <ENTITY extends RecordTemplate> Map<String, Object> entityToNode(@Nonnull ENTITY entity) {
    final Map<String, Object> fields = new HashMap<>();

    // put all field values
    entity.data().forEach((k, v) -> fields.put(k, toValueObject(v)));

    return Collections.unmodifiableMap(fields);
  }

  /**
   * Converts RELATIONSHIP to edge (field:value map), excluding source and destination
   *
   * @param relationship RELATIONSHIP defined in models
   * @return unmodifiable field value map
   */
  @Nonnull
  public static <RELATIONSHIP extends RecordTemplate> Map<String, Object> relationshipToEdge(
      @Nonnull RELATIONSHIP relationship) {
    final Map<String, Object> fields = new HashMap<>();

    // put all field values except source and destination
    relationship.data().forEach((k, v) -> {
      if (!SOURCE_FIELD.equals(k) && !DESTINATION_FIELD.equals(k)) {
        fields.put(k, toValueObject(v));
      }
    });

    return Collections.unmodifiableMap(fields);
  }

  /**
   * Converts RELATIONSHIP to cypher matching criteria, excluding source and destination, e.g. {key: "value"}
   *
   * @param relationship RELATIONSHIP defined in models
   * @return Criteria String, or "" if no additional fields in relationship
   */
  @Nonnull
  public static <RELATIONSHIP extends RecordTemplate> String relationshipToCriteria(
      @Nonnull RELATIONSHIP relationship) {
    final StringJoiner joiner = new StringJoiner(",", "{", "}");

    // put all field values except source and destination
    relationship.data().forEach((k, v) -> {
      if (!SOURCE_FIELD.equals(k) && !DESTINATION_FIELD.equals(k)) {
        joiner.add(toCriterionString(k, v));
      }
    });

    return joiner.length() <= 2 ? "" : joiner.toString();
  }

  // Returns self if primitive type, otherwise, return toString()
  @Nonnull
  private static Object toValueObject(@Nonnull Object obj) {
    if (ClassUtils.isPrimitiveOrWrapper(obj.getClass())) {
      return obj;
    }

    return obj.toString();
  }

  // Returns "key:value" String, if value is not primitive, then use toString() and double quote it
  @Nonnull
  private static String toCriterionString(@Nonnull String key, @Nonnull Object value) {
    if (ClassUtils.isPrimitiveOrWrapper(value.getClass())) {
      return key + ":" + value;
    }

    return key + ":\"" + value.toString() + "\"";
  }

  /**
   * Converts {@link Filter} to neo4j query criteria, filter criterion condition requires to be EQUAL
   *
   * @param filter Query Filter
   * @return Neo4j criteria string
   */
  @Nonnull
  public static String filterToCriteria(@Nonnull Filter filter) {
    return criterionToString(filter.getCriteria());
  }

  /**
   * Converts {@link CriterionArray} to neo4j query string
   *
   * @param criterionArray CriterionArray in a Filter
   * @return Neo4j criteria string
   */
  @Nonnull
  public static String criterionToString(@Nonnull CriterionArray criterionArray) {
    if (!criterionArray.stream().allMatch(criterion -> Condition.EQUAL.equals(criterion.getCondition()))) {
      throw new RuntimeException("Neo4j query filter only support EQUAL condition " + criterionArray);
    }

    final StringJoiner joiner = new StringJoiner(",", "{", "}");

    criterionArray.forEach(criterion -> joiner.add(toCriterionString(criterion.getField(), criterion.getValue())));

    return joiner.length() <= 2 ? "" : joiner.toString();
  }

  /**
   * Converts node (field:value map) to ENTITY
   *
   * @param entityClass Class of Entity
   * @param node Neo4j Node of entityClass type
   * @return ENTITY
   */
  @Nonnull
  public static <ENTITY extends RecordTemplate> ENTITY nodeToEntity(@Nonnull Class<ENTITY> entityClass,
      @Nonnull Node node) {
    return RecordUtils.toRecordTemplate(entityClass, new DataMap(node.asMap()));
  }

  /**
   * Converts node (field:value map) to ENTITY RecordTemplate
   *
   * @param node Neo4j Node of entityClass type
   * @return RecordTemplate
   */
  @Nonnull
  public static RecordTemplate nodeToEntity(@Nonnull Node node) {

    final String className = node.labels().iterator().next();
    return RecordUtils.toRecordTemplate(className, new DataMap(node.asMap()));
  }

  /**
   * Converts edge (source-relationship->destination) to RELATIONSHIP
   *
   * @param relationshipClass Class of RELATIONSHIP
   * @param source Neo4j source Node
   * @param destination Neo4j destination Node
   * @param relationship Neo4j relationship
   * @return ENTITY
   */
  @Nonnull
  public static <RELATIONSHIP extends RecordTemplate> RELATIONSHIP edgeToRelationship(
      @Nonnull Class<RELATIONSHIP> relationshipClass, @Nonnull Node source, @Nonnull Node destination,
      @Nonnull Relationship relationship) {

    final DataMap dataMap = relationshipDataMap(source, destination, relationship);
    return RecordUtils.toRecordTemplate(relationshipClass, dataMap);
  }

  /**
   * Converts edge (source-relationship->destination) to RELATIONSHIP RecordTemplate
   *
   * @param source Neo4j source Node
   * @param destination Neo4j destination Node
   * @param relationship Neo4j relationship
   * @return ENTITY RecordTemplate
   */
  @Nonnull
  public static RecordTemplate edgeToRelationship(@Nonnull Node source, @Nonnull Node destination,
      @Nonnull Relationship relationship) {

    final String className = relationship.type();
    final DataMap dataMap = relationshipDataMap(source, destination, relationship);
    return RecordUtils.toRecordTemplate(className, dataMap);
  }

  @Nonnull
  private static DataMap relationshipDataMap(@Nonnull Node source, @Nonnull Node destination,
      @Nonnull Relationship relationship) {

    final DataMap dataMap = new DataMap(relationship.asMap());
    dataMap.put(SOURCE_FIELD, source.get(URN_FIELD).asString());
    dataMap.put(DESTINATION_FIELD, destination.get(URN_FIELD).asString());
    return dataMap;
  }

  /**
   * Extracts Urn field from a record
   *
   * @param record extends RecordTemplate
   * @param fieldName urn field name in record
   * @return Urn
   */
  @Nonnull
  public static <T extends RecordTemplate> Urn getUrn(@Nonnull T record, @Nonnull String fieldName) {
    return getUrn(record.data().getString(fieldName));
  }

  @Nonnull
  private static Urn getUrn(@Nonnull String urn) {
    try {
      return Urn.createFromString(urn);
    } catch (Exception ex) {
      throw new ModelConversionException("Unable to deserialize URN " + urn, ex);
    }
  }

  // Gets the Node/Edge type from an Entity/Relationship, using the backtick-quoted FQCN
  @Nonnull
  public static String getType(@Nullable RecordTemplate record) {
    return record == null ? "" : getType(record.getClass());
  }

  // Gets the Node/Edge type from an Entity/Relationship class, return empty string if null
  @Nonnull
  public static String getTypeOrEmptyString(@Nullable Class<? extends RecordTemplate> recordClass) {
    return recordClass == null ? "" : ":" + getType(recordClass);
  }

  // Gets the Node/Edge type from an Entity/Relationship class, using the backtick-quoted FQCN
  @Nonnull
  public static String getType(@Nonnull Class<? extends RecordTemplate> recordClass) {
    return new StringBuilder("`").append(recordClass.getCanonicalName()).append("`").toString();
  }

  // Gets node type from Urn
  @Nonnull
  public static String getNodeType(@Nonnull Urn urn) {
    return ":" + URN_TO_ENTITY_TYPE.getOrDefault(urn.getEntityType(), "UNKNOWN");
  }

  /**
   * Create {@link RelationshipFilter} using filter and relationship direction
   *
   * @param filter {@link Filter} filter
   * @param relationshipDirection {@link RelationshipDirection} relationship direction
   * @return RelationshipFilter
   */
  @Nonnull
  public static RelationshipFilter createRelationshipFilter(@Nonnull Filter filter,
      @Nonnull RelationshipDirection relationshipDirection) {
    return new RelationshipFilter().setCriteria(filter.getCriteria()).setDirection(relationshipDirection);
  }

  /**
   * Create {@link RelationshipFilter} using filter conditions and relationship direction
   *
   * @param field field to create a filter on
   * @param value field value to be filtered
   * @param relationshipDirection {@link RelationshipDirection} relationship direction
   * @return RelationshipFilter
   */
  @Nonnull
  public static RelationshipFilter createRelationshipFilter(@Nonnull String field, @Nonnull String value,
      @Nonnull RelationshipDirection relationshipDirection) {
    return createRelationshipFilter(createFilter(field, value), relationshipDirection);
  }

  /**
   * Create {@link Filter} using field and value
   *
   * @param field field to create a filter on
   * @param value field value to be filtered
   * @return Filter
   */
  @Nonnull
  public static Filter createFilter(@Nonnull String field, @Nonnull String value) {
    return new Filter().setCriteria(new CriterionArray(
        Collections.singletonList(new Criterion().setField(field).setValue(value).setCondition(Condition.EQUAL))));
  }
}
