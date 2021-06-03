package com.linkedin.datahub.graphql.types.datajob.mappers;

import com.linkedin.common.GlobalTags;

import com.linkedin.common.Ownership;
import com.linkedin.common.Status;
import com.linkedin.datahub.graphql.generated.DataFlow;
import com.linkedin.datahub.graphql.generated.DataJob;
import com.linkedin.datahub.graphql.generated.DataJobInfo;
import com.linkedin.datahub.graphql.generated.DataJobInputOutput;
import com.linkedin.datahub.graphql.generated.Dataset;
import com.linkedin.datahub.graphql.generated.EntityType;
import com.linkedin.datahub.graphql.types.common.mappers.OwnershipMapper;
import com.linkedin.datahub.graphql.types.common.mappers.StatusMapper;
import com.linkedin.datahub.graphql.types.common.mappers.StringMapMapper;
import com.linkedin.datahub.graphql.types.mappers.ModelMapper;
import com.linkedin.datahub.graphql.types.tag.mappers.GlobalTagsMapper;
import com.linkedin.metadata.dao.utils.ModelUtils;
import com.linkedin.metadata.snapshot.DataJobSnapshot;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;


public class DataJobSnapshotMapper implements ModelMapper<DataJobSnapshot, DataJob> {

    public static final DataJobSnapshotMapper INSTANCE = new DataJobSnapshotMapper();

    public static DataJob map(@Nonnull final DataJobSnapshot dataJob) {
        return INSTANCE.apply(dataJob);
    }

    @Override
    public DataJob apply(@Nonnull final DataJobSnapshot dataJob) {
        final DataJob result = new DataJob();
        result.setUrn(dataJob.getUrn().toString());
        result.setType(EntityType.DATA_JOB);
        result.setDataFlow(new DataFlow.Builder().setUrn(dataJob.getUrn().getFlowEntity().toString()).build());
        result.setJobId(dataJob.getUrn().getJobIdEntity());

        ModelUtils.getAspectsFromSnapshot(dataJob).forEach(aspect -> {
            if (aspect instanceof com.linkedin.datajob.DataJobInfo) {
                com.linkedin.datajob.DataJobInfo info = com.linkedin.datajob.DataJobInfo.class.cast(aspect);
                result.setInfo(mapDataJobInfo(info));
            } else if (aspect instanceof com.linkedin.datajob.DataJobInputOutput) {
                com.linkedin.datajob.DataJobInputOutput inputOutput = com.linkedin.datajob.DataJobInputOutput.class.cast(aspect);
                result.setInputOutput(mapDataJobInputOutput(inputOutput));
            } else if (aspect instanceof Ownership) {
                Ownership ownership = Ownership.class.cast(aspect);
                result.setOwnership(OwnershipMapper.map(ownership));
            } else if (aspect instanceof Status) {
                Status status = Status.class.cast(aspect);
                result.setStatus(StatusMapper.map(status));
            } else if (aspect instanceof GlobalTags) {
                result.setGlobalTags(GlobalTagsMapper.map(GlobalTags.class.cast(aspect)));
            }
        });

        return result;
    }

    private DataJobInfo mapDataJobInfo(final com.linkedin.datajob.DataJobInfo info) {
        final DataJobInfo result = new DataJobInfo();
        result.setName(info.getName());
        result.setDescription(info.getDescription());
        if (info.hasExternalUrl()) {
            result.setExternalUrl(info.getExternalUrl().toString());
        }
        if (info.hasCustomProperties()) {
            result.setCustomProperties(StringMapMapper.map(info.getCustomProperties()));
        }
        return result;
    }

    private DataJobInputOutput mapDataJobInputOutput(final com.linkedin.datajob.DataJobInputOutput inputOutput) {
        final DataJobInputOutput result = new DataJobInputOutput();
        result.setInputDatasets(inputOutput.getInputDatasets().stream().map(urn -> {
            final Dataset dataset = new Dataset();
            dataset.setUrn(urn.toString());
            return dataset;
        }).collect(Collectors.toList()));
        result.setOutputDatasets(inputOutput.getOutputDatasets().stream().map(urn -> {
            final Dataset dataset = new Dataset();
            dataset.setUrn(urn.toString());
            return dataset;
        }).collect(Collectors.toList()));

        return result;
    }
}
