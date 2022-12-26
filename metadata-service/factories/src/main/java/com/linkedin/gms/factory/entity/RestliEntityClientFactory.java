package com.linkedin.gms.factory.entity;

import com.linkedin.entity.client.RestliEntityClient;
import com.linkedin.gms.factory.spring.YamlPropertySourceFactory;
import com.linkedin.metadata.restli.DefaultRestliClientFactory;
import com.linkedin.parseq.retry.backoff.ExponentialBackoff;
import com.linkedin.restli.client.Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.net.URI;


@Configuration
@PropertySource(value = "classpath:/application.yml", factory = YamlPropertySourceFactory.class)
public class RestliEntityClientFactory {

  @Value("${datahub.gms.host}")
  private String gmsHost;

  @Value("${datahub.gms.port}")
  private int gmsPort;

  @Value("${datahub.gms.useSSL}")
  private boolean gmsUseSSL;

  @Value("${datahub.gms.uri}")
  private String gmsUri;

  @Value("${datahub.gms.sslContext.protocol}")
  private String gmsSslProtocol;

  @Value("${entityClient.retryInterval:2}")
  private int retryInterval;

  @Value("${entityClient.numRetries:3}")
  private int numRetries;

  @Bean("restliEntityClient")
  public RestliEntityClient getRestliEntityClient() {
    final Client restClient;
    if (gmsUri != null) {
      restClient = DefaultRestliClientFactory.getRestLiClient(URI.create(gmsUri), gmsSslProtocol);
    } else {
      restClient = DefaultRestliClientFactory.getRestLiClient(gmsHost, gmsPort, gmsUseSSL, gmsSslProtocol);
    }
    return new RestliEntityClient(restClient, new ExponentialBackoff(retryInterval), numRetries);
  }
}
