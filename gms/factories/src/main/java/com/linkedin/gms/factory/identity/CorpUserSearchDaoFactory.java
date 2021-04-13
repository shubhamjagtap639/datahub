package com.linkedin.gms.factory.identity;

import com.linkedin.gms.factory.common.IndexConventionFactory;
import com.linkedin.metadata.configs.CorpUserSearchConfig;
import com.linkedin.metadata.dao.search.ESSearchDAO;
import com.linkedin.metadata.search.CorpUserInfoDocument;
import com.linkedin.metadata.utils.elasticsearch.IndexConvention;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import javax.annotation.Nonnull;


@Configuration
public class CorpUserSearchDaoFactory {
  @Autowired
  ApplicationContext applicationContext;

  @Bean(name = "corpUserSearchDAO")
  @DependsOn({"elasticSearchRestHighLevelClient", IndexConventionFactory.INDEX_CONVENTION_BEAN})
  @Nonnull
  protected ESSearchDAO createInstance() {
    return new ESSearchDAO(applicationContext.getBean(RestHighLevelClient.class), CorpUserInfoDocument.class,
        new CorpUserSearchConfig(applicationContext.getBean(IndexConvention.class)));
  }
}