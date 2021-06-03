package com.linkedin.metadata.resources.identity;

import com.linkedin.common.urn.CorpuserUrn;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.metadata.aspect.CorpUserAspect;
import com.linkedin.metadata.dao.BaseLocalDAO;
import com.linkedin.metadata.entity.EntityService;
import com.linkedin.metadata.key.CorpUserKey;
import com.linkedin.metadata.restli.BaseVersionedAspectResource;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.server.PathKeys;
import com.linkedin.restli.server.annotations.PathKeysParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;

public class BaseCorpUsersAspectResource<ASPECT extends RecordTemplate>
    extends BaseVersionedAspectResource<CorpuserUrn, CorpUserAspect, ASPECT> {

  private static final String CORPUSER_KEY = CorpUsers.class.getAnnotation(RestLiCollection.class).keyName();

  public BaseCorpUsersAspectResource(Class<ASPECT> aspectClass) {
    super(CorpUserAspect.class, aspectClass);
  }

  @Inject
  @Named("entityService")
  private EntityService _entityService;

  @Nonnull
  @Override
  protected BaseLocalDAO<CorpUserAspect, CorpuserUrn> getLocalDAO() {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  protected CorpuserUrn getUrn(@PathKeysParam @Nonnull PathKeys keys) {
    return new CorpuserUrn(keys.<ComplexResourceKey<CorpUserKey, EmptyRecord>>get(CORPUSER_KEY).getKey().getUsername());
  }

  @Nonnull
  protected EntityService getEntityService() {
    return _entityService;
  }
}