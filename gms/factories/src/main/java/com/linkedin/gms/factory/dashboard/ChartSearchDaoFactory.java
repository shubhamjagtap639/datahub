package com.linkedin.gms.factory.dashboard;

import com.linkedin.gms.factory.common.IndexConventionFactory;
import com.linkedin.metadata.configs.ChartSearchConfig;
import com.linkedin.metadata.dao.search.ESSearchDAO;
import com.linkedin.metadata.search.ChartDocument;
import com.linkedin.metadata.utils.elasticsearch.IndexConvention;
import javax.annotation.Nonnull;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;


@Configuration
public class ChartSearchDaoFactory {
  @Autowired
  ApplicationContext applicationContext;

  @Bean(name = "chartSearchDAO")
  @DependsOn({"elasticSearchRestHighLevelClient", IndexConventionFactory.INDEX_CONVENTION_BEAN})
  @Nonnull
  protected ESSearchDAO createInstance() {
    return new ESSearchDAO(applicationContext.getBean(RestHighLevelClient.class), ChartDocument.class,
        new ChartSearchConfig(applicationContext.getBean(IndexConvention.class)));
  }
}
