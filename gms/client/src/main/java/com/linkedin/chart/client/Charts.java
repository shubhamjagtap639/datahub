package com.linkedin.chart.client;

import com.linkedin.BatchGetUtils;
import com.linkedin.chart.ChartsDoAutocompleteRequestBuilder;
import com.linkedin.chart.ChartsDoBrowseRequestBuilder;
import com.linkedin.chart.ChartsDoGetBrowsePathsRequestBuilder;
import com.linkedin.chart.ChartsFindBySearchRequestBuilder;
import com.linkedin.chart.ChartsRequestBuilders;
import com.linkedin.common.urn.ChartUrn;
import com.linkedin.dashboard.Chart;
import com.linkedin.dashboard.ChartKey;
import com.linkedin.data.template.StringArray;
import com.linkedin.metadata.aspect.ChartAspect;
import com.linkedin.metadata.configs.ChartSearchConfig;
import com.linkedin.metadata.dao.ChartActionRequestBuilder;
import com.linkedin.metadata.dao.utils.ModelUtils;
import com.linkedin.metadata.query.AutoCompleteResult;
import com.linkedin.metadata.query.BrowseResult;
import com.linkedin.metadata.query.SortCriterion;
import com.linkedin.metadata.restli.BaseBrowsableClient;
import com.linkedin.metadata.snapshot.ChartSnapshot;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.restli.client.Client;
import com.linkedin.restli.client.GetRequest;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.EmptyRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.linkedin.metadata.dao.utils.QueryUtils.newFilter;


public class Charts extends BaseBrowsableClient<Chart, ChartUrn> {

    private static final ChartsRequestBuilders CHARTS_REQUEST_BUILDERS = new ChartsRequestBuilders();
    private static final ChartActionRequestBuilder CHARTS_ACTION_REQUEST_BUILDERS = new ChartActionRequestBuilder();
    private static final ChartSearchConfig CHARTS_SEARCH_CONFIG = new ChartSearchConfig();

    public Charts(@Nonnull Client restliClient) {
        super(restliClient);
    }

    @Nonnull
    public Chart get(@Nonnull ChartUrn urn)
            throws RemoteInvocationException {
        GetRequest<Chart> getRequest = CHARTS_REQUEST_BUILDERS.get()
                .id(new ComplexResourceKey<>(toChartKey(urn), new EmptyRecord()))
                .build();

        return _client.sendRequest(getRequest).getResponse().getEntity();
    }

    /**
     * Gets browse path(s) given a chart urn
     *
     * @param urn urn for the entity
     * @return list of paths given urn
     * @throws RemoteInvocationException
     */
    @Nonnull
    public StringArray getBrowsePaths(@Nonnull ChartUrn urn) throws RemoteInvocationException {
        ChartsDoGetBrowsePathsRequestBuilder requestBuilder = CHARTS_REQUEST_BUILDERS
            .actionGetBrowsePaths()
            .urnParam(urn);
        return _client.sendRequest(requestBuilder.build()).getResponse().getEntity();
    }

    @Nonnull
    public Map<ChartUrn, Chart> batchGet(@Nonnull Set<ChartUrn> urns)
            throws RemoteInvocationException {
        return BatchGetUtils.batchGet(
                urns,
                (Void v) -> CHARTS_REQUEST_BUILDERS.batchGet(),
                this::getKeyFromUrn,
                this::getUrnFromKey,
                _client
        );
    }

    @Nonnull
    @Override
    public CollectionResponse<Chart> search(@Nonnull String input,
                                                @Nullable StringArray aspectNames,
                                                @Nullable Map<String, String> requestFilters,
                                                @Nullable SortCriterion sortCriterion,
                                                int start,
                                                int count)
            throws RemoteInvocationException {
        final ChartsFindBySearchRequestBuilder requestBuilder = CHARTS_REQUEST_BUILDERS.findBySearch()
                .aspectsParam(aspectNames)
                .inputParam(input)
                .sortParam(sortCriterion)
                .paginate(start, count);
        if (requestFilters != null) {
            requestBuilder.filterParam(newFilter(requestFilters));
        }
        return _client.sendRequest(requestBuilder.build()).getResponse().getEntity();
    }

    @Nonnull
    public CollectionResponse<Chart> search(@Nonnull String input, int start, int count)
            throws RemoteInvocationException {
        return search(input, null, null, start, count);
    }

    @Nonnull
    @Override
    public AutoCompleteResult autocomplete(@Nonnull String query, @Nullable String field, @Nonnull Map<String, String> requestFilters, int limit)
            throws RemoteInvocationException {
        final String autocompleteField = (field != null) ? field : CHARTS_SEARCH_CONFIG.getDefaultAutocompleteField();
        ChartsDoAutocompleteRequestBuilder requestBuilder = CHARTS_REQUEST_BUILDERS
                .actionAutocomplete()
                .queryParam(query)
                .fieldParam(autocompleteField)
                .filterParam(newFilter(requestFilters))
                .limitParam(limit);

        return _client.sendRequest(requestBuilder.build()).getResponse().getEntity();
    }

    /**
     * Gets browse snapshot of a given path
     *
     * @param path path being browsed
     * @param requestFilters browse filters
     * @param start start offset of first dataset
     * @param limit max number of datasets
     * @throws RemoteInvocationException
     */
    @Nonnull
    @Override
    public BrowseResult browse(@Nonnull String path, @Nullable Map<String, String> requestFilters,
        int start, int limit) throws RemoteInvocationException {
        ChartsDoBrowseRequestBuilder requestBuilder = CHARTS_REQUEST_BUILDERS
            .actionBrowse()
            .pathParam(path)
            .startParam(start)
            .limitParam(limit);
        if (requestFilters != null) {
            requestBuilder.filterParam(newFilter(requestFilters));
        }
        return _client.sendRequest(requestBuilder.build()).getResponse().getEntity();
    }

    /**
     * Update an existing Chart
     */
    public void update(@Nonnull final ChartUrn urn, @Nonnull final Chart chart) throws RemoteInvocationException {
        Request request = CHARTS_ACTION_REQUEST_BUILDERS.createRequest(urn, toSnapshot(chart, urn));
        _client.sendRequest(request).getResponse();
    }

    static ChartSnapshot toSnapshot(@Nonnull Chart chart, @Nonnull ChartUrn urn) {
        final List<ChartAspect> aspects = new ArrayList<>();
        if (chart.hasInfo()) {
            aspects.add(ModelUtils.newAspectUnion(ChartAspect.class, chart.getInfo()));
        }
        if (chart.hasQuery()) {
            aspects.add(ModelUtils.newAspectUnion(ChartAspect.class, chart.getQuery()));
        }
        if (chart.hasOwnership()) {
            aspects.add(ModelUtils.newAspectUnion(ChartAspect.class, chart.getOwnership()));
        }
        if (chart.hasStatus()) {
            aspects.add(ModelUtils.newAspectUnion(ChartAspect.class, chart.getStatus()));
        }
        if (chart.hasGlobalTags()) {
            aspects.add(ModelUtils.newAspectUnion(ChartAspect.class, chart.getGlobalTags()));
        }
        return ModelUtils.newSnapshot(ChartSnapshot.class, urn, aspects);
    }

    @Nonnull
    private ComplexResourceKey<ChartKey, EmptyRecord> getKeyFromUrn(@Nonnull ChartUrn urn) {
        return new ComplexResourceKey<>(toChartKey(urn), new EmptyRecord());
    }

    @Nonnull
    private ChartUrn getUrnFromKey(@Nonnull ComplexResourceKey<ChartKey, EmptyRecord> key) {
        return toChartUrn(key.getKey());
    }

    @Nonnull
    private ChartKey toChartKey(@Nonnull ChartUrn urn) {
        return new ChartKey().setTool(urn.getDashboardToolEntity()).setChartId(urn.getChartIdEntity());
    }

    @Nonnull
    private ChartUrn toChartUrn(@Nonnull ChartKey key) {
        return new ChartUrn(key.getTool(), key.getChartId());
    }
}
