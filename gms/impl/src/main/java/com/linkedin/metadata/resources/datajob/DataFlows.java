package com.linkedin.metadata.resources.datajob;

import com.linkedin.common.AuditStamp;

import com.linkedin.common.GlobalTags;
import com.linkedin.common.Status;
import com.linkedin.common.UrnArray;
import com.linkedin.datajob.DataFlowInfo;
import com.linkedin.common.Ownership;
import com.linkedin.common.urn.DataFlowUrn;
import com.linkedin.common.urn.Urn;
import com.linkedin.datajob.DataFlow;
import com.linkedin.datajob.DataFlowKey;
import com.linkedin.data.template.StringArray;
import com.linkedin.entity.Entity;
import com.linkedin.metadata.PegasusUtils;
import com.linkedin.metadata.aspect.DataFlowAspect;
import com.linkedin.metadata.dao.BaseBrowseDAO;
import com.linkedin.metadata.dao.BaseLocalDAO;
import com.linkedin.metadata.dao.BaseSearchDAO;
import com.linkedin.metadata.entity.EntityService;
import com.linkedin.metadata.dao.utils.ModelUtils;
import com.linkedin.metadata.dao.utils.QueryUtils;
import com.linkedin.metadata.query.AutoCompleteResult;
import com.linkedin.metadata.query.BrowseResult;
import com.linkedin.metadata.query.Filter;
import com.linkedin.metadata.query.SearchResult;
import com.linkedin.metadata.query.SearchResultMetadata;
import com.linkedin.metadata.query.SortCriterion;
import com.linkedin.metadata.query.SortOrder;
import com.linkedin.metadata.restli.BackfillResult;
import com.linkedin.metadata.restli.BaseBrowsableEntityResource;
import com.linkedin.metadata.restli.RestliUtils;
import com.linkedin.metadata.search.DataFlowDocument;
import com.linkedin.metadata.search.SearchService;
import com.linkedin.metadata.snapshot.DataFlowSnapshot;
import com.linkedin.metadata.snapshot.Snapshot;
import com.linkedin.parseq.Task;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.server.CollectionResult;
import com.linkedin.restli.server.PagingContext;
import com.linkedin.restli.server.annotations.Action;
import com.linkedin.restli.server.annotations.ActionParam;
import com.linkedin.restli.server.annotations.Finder;
import com.linkedin.restli.server.annotations.Optional;
import com.linkedin.restli.server.annotations.PagingContextParam;
import com.linkedin.restli.server.annotations.QueryParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestMethod;
import java.net.URISyntaxException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import static com.linkedin.metadata.restli.RestliConstants.*;

/**
 * Deprecated! Use {@link EntityResource} instead.
 */
@Deprecated
@RestLiCollection(name = "dataFlows", namespace = "com.linkedin.dataflow", keyName = "key")
public class DataFlows extends BaseBrowsableEntityResource<
    // @formatter:off
    ComplexResourceKey<DataFlowKey, EmptyRecord>,
    DataFlow,
    DataFlowUrn,
    DataFlowSnapshot,
    DataFlowAspect,
    DataFlowDocument> {
  // @formatter:on

  private static final String DEFAULT_ACTOR = "urn:li:principal:UNKNOWN";
  private final Clock _clock = Clock.systemUTC();

  public DataFlows() {
    super(DataFlowSnapshot.class, DataFlowAspect.class, DataFlowUrn.class);
  }

  @Inject
  @Named("entityService")
  private EntityService _entityService;

  @Inject
  @Named("searchService")
  private SearchService _searchService;

  @Nonnull
  @Override
  protected BaseSearchDAO<DataFlowDocument> getSearchDAO() {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  protected BaseLocalDAO<DataFlowAspect, DataFlowUrn> getLocalDAO() {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  protected BaseBrowseDAO getBrowseDAO() {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  protected DataFlowUrn createUrnFromString(@Nonnull String urnString) throws Exception {
    return DataFlowUrn.createFromString(urnString);
  }

  @Nonnull
  @Override
  protected DataFlowUrn toUrn(@Nonnull ComplexResourceKey<DataFlowKey, EmptyRecord> key) {
    return new DataFlowUrn(key.getKey().getOrchestrator(), key.getKey().getFlowId(), key.getKey().getCluster());
  }

  @Nonnull
  @Override
  protected ComplexResourceKey<DataFlowKey, EmptyRecord> toKey(@Nonnull DataFlowUrn urn) {
    return new ComplexResourceKey<>(
        new DataFlowKey()
            .setOrchestrator(urn.getOrchestratorEntity())
            .setFlowId(urn.getFlowIdEntity())
            .setCluster(urn.getClusterEntity()),
        new EmptyRecord());
  }

  @Nonnull
  @Override
  protected DataFlow toValue(@Nonnull DataFlowSnapshot snapshot) {
    final DataFlow value = new DataFlow()
        .setUrn(snapshot.getUrn())
        .setOrchestrator(snapshot.getUrn().getOrchestratorEntity())
        .setFlowId(snapshot.getUrn().getFlowIdEntity())
        .setCluster(snapshot.getUrn().getClusterEntity());
    ModelUtils.getAspectsFromSnapshot(snapshot).forEach(aspect -> {
      if (aspect instanceof DataFlowInfo) {
        DataFlowInfo info = DataFlowInfo.class.cast(aspect);
        value.setInfo(info);
      } else if (aspect instanceof Ownership) {
        Ownership ownership = Ownership.class.cast(aspect);
        value.setOwnership(ownership);
      } else if (aspect instanceof Status) {
        Status status = Status.class.cast(aspect);
        value.setStatus(status);
      } else if (aspect instanceof GlobalTags) {
        value.setGlobalTags(GlobalTags.class.cast(aspect));
      }
    });

    return value;
  }

  @Nonnull
  @Override
  protected DataFlowSnapshot toSnapshot(@Nonnull DataFlow dataFlow, @Nonnull DataFlowUrn urn) {
    final List<DataFlowAspect> aspects = new ArrayList<>();
    if (dataFlow.hasInfo()) {
      aspects.add(ModelUtils.newAspectUnion(DataFlowAspect.class, dataFlow.getInfo()));
    }
    if (dataFlow.hasOwnership()) {
      aspects.add(ModelUtils.newAspectUnion(DataFlowAspect.class, dataFlow.getOwnership()));
    }
    if (dataFlow.hasStatus()) {
      aspects.add(ModelUtils.newAspectUnion(DataFlowAspect.class, dataFlow.getStatus()));
    }
    if (dataFlow.hasGlobalTags()) {
      aspects.add(ModelUtils.newAspectUnion(DataFlowAspect.class, dataFlow.getGlobalTags()));
    }
    return ModelUtils.newSnapshot(DataFlowSnapshot.class, urn, aspects);
  }

  @RestMethod.Get
  @Override
  @Nonnull
  public Task<DataFlow> get(@Nonnull ComplexResourceKey<DataFlowKey, EmptyRecord> key,
      @QueryParam(PARAM_ASPECTS) @Optional @Nullable String[] aspectNames) {
    final Set<String> projectedAspects = aspectNames == null ? Collections.emptySet() : new HashSet<>(
        Arrays.asList(aspectNames).stream().map(PegasusUtils::getAspectNameFromFullyQualifiedName)
            .collect(Collectors.toList()));
    return RestliUtils.toTask(() -> {
      final Entity entity = _entityService.getEntity(new DataFlowUrn(
          key.getKey().getOrchestrator(),
          key.getKey().getFlowId(),
          key.getKey().getCluster()), projectedAspects);
      if (entity != null) {
        return toValue(entity.getValue().getDataFlowSnapshot());
      }
      throw RestliUtils.resourceNotFoundException();
    });
  }

  @RestMethod.BatchGet
  @Override
  @Nonnull
  public Task<Map<ComplexResourceKey<DataFlowKey, EmptyRecord>, DataFlow>> batchGet(
      @Nonnull Set<ComplexResourceKey<DataFlowKey, EmptyRecord>> keys,
      @QueryParam(PARAM_ASPECTS) @Optional @Nullable String[] aspectNames) {
    final Set<String> projectedAspects = aspectNames == null ? Collections.emptySet() : new HashSet<>(
        Arrays.asList(aspectNames).stream().map(PegasusUtils::getAspectNameFromFullyQualifiedName)
            .collect(Collectors.toList()));
    return RestliUtils.toTask(() -> {

      final Map<ComplexResourceKey<DataFlowKey, EmptyRecord>, DataFlow> entities = new HashMap<>();
      for (final ComplexResourceKey<DataFlowKey, EmptyRecord> key : keys) {
        final Entity entity = _entityService.getEntity(
            new DataFlowUrn(key.getKey().getOrchestrator(), key.getKey().getFlowId(), key.getKey().getCluster()),
            projectedAspects);

        if (entity != null) {
          entities.put(key, toValue(entity.getValue().getDataFlowSnapshot()));
        }
      }
      return entities;
    });
  }

  @RestMethod.GetAll
  @Nonnull
  public Task<List<DataFlow>> getAll(@PagingContextParam @Nonnull PagingContext pagingContext,
      @QueryParam(PARAM_ASPECTS) @Optional @Nullable String[] aspectNames,
      @QueryParam(PARAM_FILTER) @Optional @Nullable Filter filter,
      @QueryParam(PARAM_SORT) @Optional @Nullable SortCriterion sortCriterion) {
    final Set<String> projectedAspects = aspectNames == null ? Collections.emptySet() : new HashSet<>(
        Arrays.asList(aspectNames).stream().map(PegasusUtils::getAspectNameFromFullyQualifiedName)
            .collect(Collectors.toList()));
    return RestliUtils.toTask(() -> {

      final Filter searchFilter = filter != null ? filter : QueryUtils.EMPTY_FILTER;
      final SortCriterion searchSortCriterion = sortCriterion != null ? sortCriterion
          : new SortCriterion().setField("urn").setOrder(SortOrder.ASCENDING);
      final SearchResult filterResults = _searchService.filter(
          "dataFlow",
          searchFilter,
          searchSortCriterion,
          pagingContext.getStart(),
          pagingContext.getCount());

      final Set<Urn> urns = new HashSet<>(filterResults.getEntities());
      final Map<Urn, Entity> entity = _entityService.getEntities(urns, projectedAspects);

      return new CollectionResult<>(
          entity.keySet().stream().map(urn -> toValue(entity.get(urn).getValue().getDataFlowSnapshot())).collect(
              Collectors.toList()),
          filterResults.getNumEntities(),
          filterResults.getMetadata().setUrns(new UrnArray(urns))
      ).getElements();
    });
  }

  @Finder(FINDER_SEARCH)
  @Override
  @Nonnull
  public Task<CollectionResult<DataFlow, SearchResultMetadata>> search(@QueryParam(PARAM_INPUT) @Nonnull String input,
      @QueryParam(PARAM_ASPECTS) @Optional @Nullable String[] aspectNames,
      @QueryParam(PARAM_FILTER) @Optional @Nullable Filter filter,
      @QueryParam(PARAM_SORT) @Optional @Nullable SortCriterion sortCriterion,
      @PagingContextParam @Nonnull PagingContext pagingContext) {
    final Set<String> projectedAspects = aspectNames == null ? Collections.emptySet() : new HashSet<>(
        Arrays.asList(aspectNames).stream().map(PegasusUtils::getAspectNameFromFullyQualifiedName)
            .collect(Collectors.toList()));
    return RestliUtils.toTask(() -> {

      final SearchResult searchResult = _searchService.search(
          "dataFlow",
          input,
          filter,
          sortCriterion,
          pagingContext.getStart(),
          pagingContext.getCount());

      final Set<Urn> urns = new HashSet<>(searchResult.getEntities());
      final Map<Urn, Entity> entity = _entityService.getEntities(urns, projectedAspects);

      return new CollectionResult<>(
          entity.keySet().stream().map(urn -> toValue(entity.get(urn).getValue().getDataFlowSnapshot())).collect(
              Collectors.toList()),
          searchResult.getNumEntities(),
          searchResult.getMetadata().setUrns(new UrnArray(urns))
      );
    });
  }

  @Action(name = ACTION_AUTOCOMPLETE)
  @Override
  @Nonnull
  public Task<AutoCompleteResult> autocomplete(@ActionParam(PARAM_QUERY) @Nonnull String query,
      @ActionParam(PARAM_FIELD) @Nullable String field, @ActionParam(PARAM_FILTER) @Nullable Filter filter,
      @ActionParam(PARAM_LIMIT) int limit) {
    return RestliUtils.toTask(() ->
        _searchService.autoComplete(
            "dataFlow",
            query,
            field,
            filter,
            limit)
    );  }

  @Action(name = ACTION_BROWSE)
  @Override
  @Nonnull
  public Task<BrowseResult> browse(@ActionParam(PARAM_PATH) @Nonnull String path,
      @ActionParam(PARAM_FILTER) @Optional @Nullable Filter filter, @ActionParam(PARAM_START) int start,
      @ActionParam(PARAM_LIMIT) int limit) {
    return RestliUtils.toTask(() ->
        _searchService.browse(
            "dataFlow",
            path,
            filter,
            start,
            limit)
    );  }

  @Action(name = ACTION_GET_BROWSE_PATHS)
  @Override
  @Nonnull
  public Task<StringArray> getBrowsePaths(
      @ActionParam(value = "urn", typeref = com.linkedin.common.Urn.class) @Nonnull Urn urn) {
    return RestliUtils.toTask(() ->
        new StringArray(_searchService.getBrowsePaths(
            "dataFlow",
            urn))
    );  }

  @Action(name = ACTION_INGEST)
  @Override
  @Nonnull
  public Task<Void> ingest(@ActionParam(PARAM_SNAPSHOT) @Nonnull DataFlowSnapshot snapshot) {
    return RestliUtils.toTask(() -> {
      try {
        final AuditStamp auditStamp =
            new AuditStamp().setTime(_clock.millis()).setActor(Urn.createFromString(DEFAULT_ACTOR));
        _entityService.ingestEntity(new Entity().setValue(Snapshot.create(snapshot)), auditStamp);
      } catch (URISyntaxException e) {
        throw new RuntimeException("Failed to create Audit Urn");
      }
      return null;
    });
  }

  @Action(name = ACTION_GET_SNAPSHOT)
  @Override
  @Nonnull
  public Task<DataFlowSnapshot> getSnapshot(@ActionParam(PARAM_URN) @Nonnull String urnString,
      @ActionParam(PARAM_ASPECTS) @Optional @Nullable String[] aspectNames) {
    final Set<String> projectedAspects = aspectNames == null ? Collections.emptySet() : new HashSet<>(
        Arrays.asList(aspectNames).stream().map(PegasusUtils::getAspectNameFromFullyQualifiedName)
            .collect(Collectors.toList()));
    return RestliUtils.toTask(() -> {
      final Entity entity;
      try {
        entity = _entityService.getEntity(
            Urn.createFromString(urnString), projectedAspects);

        if (entity != null) {
          return entity.getValue().getDataFlowSnapshot();
        }
        throw RestliUtils.resourceNotFoundException();
      } catch (URISyntaxException e) {
        throw new RuntimeException(String.format("Failed to convert urnString %s into an Urn", urnString));
      }
    });
  }

  @Action(name = ACTION_BACKFILL_WITH_URNS)
  @Override
  @Nonnull
  public Task<BackfillResult> backfill(@ActionParam(PARAM_URNS) @Nonnull String[] urns,
      @ActionParam(PARAM_ASPECTS) @Optional @Nullable String[] aspectNames) {
    return super.backfill(urns, aspectNames);
  }
}
