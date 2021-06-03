package com.linkedin.metadata.entity;

import com.linkedin.metadata.models.EntitySpec;
import com.linkedin.metadata.models.EntitySpecBuilder;
import com.linkedin.metadata.models.registry.EntityRegistry;
import com.linkedin.metadata.snapshot.Snapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;


public class TestEntityRegistry implements EntityRegistry {

  private final Map<String, EntitySpec> entityNameToSpec;

  public TestEntityRegistry() {
    entityNameToSpec = new EntitySpecBuilder(EntitySpecBuilder.AnnotationExtractionMode.IGNORE_ASPECT_FIELDS)
        .buildEntitySpecs(new Snapshot().schema())
        .stream()
        .collect(Collectors.toMap(spec -> spec.getName().toLowerCase(), spec -> spec));
  }

  @Nonnull
  @Override
  public EntitySpec getEntitySpec(@Nonnull final String entityName) {
    String lowercaseEntityName = entityName.toLowerCase();
    if (!entityNameToSpec.containsKey(lowercaseEntityName)) {
      throw new IllegalArgumentException(
          String.format("Failed to find entity with name %s in EntityRegistry", entityName));
    }
    return entityNameToSpec.get(lowercaseEntityName);
  }

  @Nonnull
  @Override
  public List<EntitySpec> getEntitySpecs() {
    return new ArrayList<>(entityNameToSpec.values());
  }
}
