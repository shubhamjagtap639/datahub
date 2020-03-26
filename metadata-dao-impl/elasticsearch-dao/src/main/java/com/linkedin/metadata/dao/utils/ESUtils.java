package com.linkedin.metadata.dao.utils;

import com.linkedin.metadata.query.SortCriterion;
import java.util.Arrays;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.ScoreSortBuilder;
import org.elasticsearch.search.sort.SortOrder;


public class ESUtils {

  private static final String DEFAULT_SEARCH_RESULTS_SORT_BY_FIELD = "urn";

  /*
  * TODO: we might need to extend this list if need be, below link has the complete list
  * https://www.elastic.co/guide/en/elasticsearch/reference/current/regexp-syntax.html
  * */
  private static final char[] ELASTICSEARCH_REGEXP_RESERVED_CHARACTERS = {'*'};

  private ESUtils() {

  }

  /**
   * Constructs the filter query given filter map
   *
   * Multiple values can be selected for a filter, and it is currently modeled as string separated by comma
   *
   * @param requestMap the search request map with fields and its values
   * @return built filters
   */
  @Nonnull
  public static BoolQueryBuilder buildFilterQuery(@Nonnull Map<String, String> requestMap) {
    BoolQueryBuilder boolFilter = new BoolQueryBuilder();
    for (Map.Entry<String, String> entry : requestMap.entrySet()) {
      BoolQueryBuilder filters = new BoolQueryBuilder();
      Arrays.stream(entry.getValue().split(","))
          .forEach(elem -> filters.should(QueryBuilders.matchQuery(entry.getKey(), elem)));
      boolFilter.must(filters);
    }
    return boolFilter;
  }

  /**
   * Returns a {@link SearchRequest} given filters to be applied to search query and sort criterion to be applied to search results
   *
   * @param requestMap search request map with fields and values
   * @param sortCriterion {@link SortCriterion} to be applied to the search results
   * @param from index to start the search from
   * @param size the number of search hits to return
   * @return {@link SearchRequest} that contains the filtered query
   */
  @Nonnull
  public static SearchRequest getFilteredSearchQuery(@Nonnull Map<String, String> requestMap, @Nullable SortCriterion sortCriterion, int from, int size) {
    final BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
    for (Map.Entry<String, String> entry : requestMap.entrySet()) {
      if (!entry.getValue().trim().isEmpty()) {
        boolQueryBuilder.filter(QueryBuilders.termsQuery(entry.getKey(), entry.getValue().trim().split("\\s*,\\s*")));
      }
    }
    final SearchRequest searchRequest = new SearchRequest();
    final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
    searchSourceBuilder.query(boolQueryBuilder);
    searchSourceBuilder.from(from).size(size);
    buildSortOrder(searchSourceBuilder, sortCriterion);
    searchRequest.source(searchSourceBuilder);
    return searchRequest;
  }

  /**
   * Populates source field of search query with the sort order as per the criterion provided
   *
   * <p>
   * If no sort criterion is provided then the default sorting criterion is chosen which is descending order of score
   * Furthermore to resolve conflicts, the results are further sorted by ascending order of urn
   * If the input sort criterion is urn itself, then no additional sort criterion is applied as there will be no conflicts.
   * </p>
   *
   * @param searchSourceBuilder {@link SearchSourceBuilder} that needs to be populated with sort order
   * @param sortCriterion {@link SortCriterion} to be applied to the search results
   */
  public static void buildSortOrder(@Nonnull SearchSourceBuilder searchSourceBuilder, @Nullable SortCriterion sortCriterion) {
    if (sortCriterion == null) {
      searchSourceBuilder.sort(new ScoreSortBuilder().order(SortOrder.DESC));
    } else {
      final SortOrder esSortOrder = (sortCriterion.getOrder() == com.linkedin.metadata.query.SortOrder.ASCENDING)
          ? SortOrder.ASC
          : SortOrder.DESC;
      searchSourceBuilder.sort(new FieldSortBuilder(sortCriterion.getField()).order(esSortOrder));
    }
    if (sortCriterion == null || !sortCriterion.getField().equals(DEFAULT_SEARCH_RESULTS_SORT_BY_FIELD)) {
      searchSourceBuilder.sort(new FieldSortBuilder(DEFAULT_SEARCH_RESULTS_SORT_BY_FIELD).order(SortOrder.ASC));
    }
  }

  /**
   * Escapes the Elasticsearch reserved characters in the given input string
   *
   * @param input input string
   * @return input string in which reserved characters are escaped
   */
  @Nonnull
  public static String escapeReservedCharacters(@Nonnull String input) {
    for (char reservedChar : ELASTICSEARCH_REGEXP_RESERVED_CHARACTERS) {
      input = input.replace(String.valueOf(reservedChar), "\\" + reservedChar);
    }
    return input;
  }
}
