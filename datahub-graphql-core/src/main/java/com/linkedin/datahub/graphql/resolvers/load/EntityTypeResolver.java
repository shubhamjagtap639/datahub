package com.linkedin.datahub.graphql.resolvers.load;

import com.google.common.collect.Iterables;
import com.linkedin.datahub.graphql.generated.Entity;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.dataloader.DataLoader;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * GraphQL resolver responsible for
 *
 *    1. Retrieving a single input urn.
 *    2. Resolving a single Entity
 *
 *
 */
public class EntityTypeResolver implements DataFetcher<CompletableFuture<Entity>> {

    private final List<com.linkedin.datahub.graphql.types.EntityType<?>> _entityTypes;
    private final Function<DataFetchingEnvironment, Entity> _entityProvider;

    public EntityTypeResolver(
        final List<com.linkedin.datahub.graphql.types.EntityType<?>> entityTypes,
        final Function<DataFetchingEnvironment, Entity> entity
    ) {
        _entityTypes = entityTypes;
        _entityProvider = entity;
    }

    @Override
    public CompletableFuture get(DataFetchingEnvironment environment) {
        final String urn = _entityProvider.apply(environment).getUrn();
        final Object javaObject = _entityProvider.apply(environment);
        final com.linkedin.datahub.graphql.types.EntityType<?> filteredEntity = Iterables.getOnlyElement(_entityTypes.stream()
                .filter(entity -> javaObject.getClass().isAssignableFrom(entity.objectClass()))
                .collect(Collectors.toList()));
        final DataLoader<String, Entity> loader = environment.getDataLoaderRegistry().getDataLoader(filteredEntity.name());
        return loader.load(urn);
    }
}
