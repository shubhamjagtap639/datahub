package com.linkedin.datahub.graphql.types.mlmodel;

import com.linkedin.common.urn.Urn;

import com.linkedin.datahub.graphql.types.mappers.UrnSearchResultsMapper;
import com.linkedin.datahub.graphql.types.mlmodel.mappers.MLModelSnapshotMapper;
import com.linkedin.entity.client.EntityClient;
import com.linkedin.entity.Entity;
import com.linkedin.metadata.query.SearchResult;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.ImmutableSet;
import com.linkedin.common.urn.MLModelUrn;
import com.linkedin.datahub.graphql.QueryContext;
import com.linkedin.datahub.graphql.generated.AutoCompleteResults;
import com.linkedin.datahub.graphql.generated.EntityType;
import com.linkedin.datahub.graphql.generated.FacetFilterInput;
import com.linkedin.datahub.graphql.generated.MLModel;
import com.linkedin.datahub.graphql.generated.SearchResults;
import com.linkedin.datahub.graphql.resolvers.ResolverUtils;
import com.linkedin.datahub.graphql.types.SearchableEntityType;
import com.linkedin.datahub.graphql.types.mappers.AutoCompleteResultsMapper;
import com.linkedin.metadata.query.AutoCompleteResult;

public class MLModelType implements SearchableEntityType<MLModel> {

    private static final Set<String> FACET_FIELDS = ImmutableSet.of("origin", "platform");
    private final EntityClient _mlModelsClient;

    public MLModelType(final EntityClient mlModelsClient) {
        _mlModelsClient = mlModelsClient;
    }

    @Override
    public EntityType type() {
        return EntityType.MLMODEL;
    }

    @Override
    public Class<MLModel> objectClass() {
        return MLModel.class;
    }

    @Override
    public List<MLModel> batchLoad(final List<String> urns, final QueryContext context) throws Exception {
        final List<MLModelUrn> mlModelUrns = urns.stream()
            .map(MLModelUtils::getMLModelUrn)
            .collect(Collectors.toList());

        try {
            final Map<Urn, Entity> mlModelMap = _mlModelsClient.batchGet(mlModelUrns
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));

            final List<Entity> gmsResults = mlModelUrns.stream()
                .map(modelUrn -> mlModelMap.getOrDefault(modelUrn, null)).collect(Collectors.toList());

            return gmsResults.stream()
                .map(gmsMlModel -> gmsMlModel == null ? null : MLModelSnapshotMapper.map(
                    gmsMlModel.getValue().getMLModelSnapshot()))
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to batch load MLModels", e);
        }
    }

    @Override
    public SearchResults search(@Nonnull String query,
                                @Nullable List<FacetFilterInput> filters,
                                int start,
                                int count,
                                @Nonnull final QueryContext context) throws Exception {
        final Map<String, String> facetFilters = ResolverUtils.buildFacetFilters(filters, FACET_FIELDS);
        final SearchResult searchResult = _mlModelsClient.search("mlModel", query, facetFilters, start, count);
        return UrnSearchResultsMapper.map(searchResult);
    }

    @Override
    public AutoCompleteResults autoComplete(@Nonnull String query,
                                            @Nullable String field,
                                            @Nullable List<FacetFilterInput> filters,
                                            int limit,
                                            @Nonnull final QueryContext context) throws Exception {
        final Map<String, String> facetFilters = ResolverUtils.buildFacetFilters(filters, FACET_FIELDS);
        final AutoCompleteResult result = _mlModelsClient.autoComplete("mlModel", query, facetFilters, limit);
        return AutoCompleteResultsMapper.map(result);
    }
}
