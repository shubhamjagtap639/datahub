package com.linkedin.identity.factory;

import com.linkedin.identity.dao.search.CorpUserSearchConfig;
import com.linkedin.metadata.dao.search.ESSearchDAO;
import com.linkedin.metadata.search.CorpUserInfoDocument;
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

    @Bean(name = "corpUserSearchDao")
    @DependsOn({"elasticSearchRestHighLevelClient"})
    @Nonnull
    protected ESSearchDAO createInstance() {
        return new ESSearchDAO(applicationContext.getBean(RestHighLevelClient.class), CorpUserInfoDocument.class,
                new CorpUserSearchConfig());
    }
}