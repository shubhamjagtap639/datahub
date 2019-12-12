package com.linkedin.dataset.dao.search;

import com.linkedin.metadata.dao.search.BaseSearchConfig;
import com.linkedin.metadata.dao.utils.SearchUtils;
import com.linkedin.metadata.search.DatasetDocument;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;


public class DatasetSearchConfig extends BaseSearchConfig<DatasetDocument> {
  @Override
  @Nonnull
  public Set<String> getFacetFields() {
    return Collections.unmodifiableSet(new HashSet<>(Arrays.asList("origin", "platform")));
  }

  @Nonnull
  public Class getSearchDocument() {
    return DatasetDocument.class;
  }

  @Override
  @Nonnull
  public String getDefaultAutocompleteField() {
    return "name";
  }

  @Override
  @Nonnull
  public String getSearchQueryTemplate() {
    return SearchUtils.readResourceFile(getClass(), "datasetESSearchQueryTemplate.json");
  }

  @Override
  @Nonnull
  public String getAutocompleteQueryTemplate() {
    return SearchUtils.readResourceFile(getClass(), "datasetESAutocompleteQueryTemplate.json");
  }
}
