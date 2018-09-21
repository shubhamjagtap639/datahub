/**
 * Copyright 2015 LinkedIn Corp. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */
package wherehows.common.schemas;

import java.util.List;


/**
 * Data record model for Gobblin tracking event compaction
 *
 */
public class GobblinTrackingCompactionRecord extends AbstractRecord {

  String cluster;
  String dataset;
  String partitionType;
  String partitionName;
  long recordCount;
  long lateRecordCount;
  String dedupeStatus;
  String jobContext;
  String projectName;
  String flowName;
  String jobName;
  int flowExecId;
  long logEventTime;

  public GobblinTrackingCompactionRecord() {
  }

  public GobblinTrackingCompactionRecord(long timestamp, String jobContext, String cluster, String projectName,
      String flowId, String jobId, int execId) {
    this.logEventTime = timestamp;
    this.jobContext = jobContext;
    this.cluster = cluster;
    this.projectName = projectName;
    this.flowName = flowId;
    this.jobName = jobId;
    this.flowExecId = execId;
  }

  @Override
  public String[] getDbColumnNames() {
    final String[] columnNames =
        {"cluster", "dataset", "partition_type", "partition_name", "record_count", "late_record_count", "dedupe_status",
            "job_context", "project_name", "flow_name", "job_name", "flow_exec_id", "log_event_time"};
    return columnNames;
  }

  @Override
  public List<Object> fillAllFields() {
    return null;
  }

  public void setDatasetUrn(String dataset, String partitionType, String partitionName) {
    this.dataset = dataset;
    this.partitionType = partitionType;
    this.partitionName = partitionName;
  }

  public String getCluster() {
    return cluster;
  }

  public void setCluster(String cluster) {
    this.cluster = cluster;
  }

  public String getDataset() {
    return dataset;
  }

  public void setDataset(String dataset) {
    this.dataset = dataset;
  }

  public String getPartitionType() {
    return partitionType;
  }

  public void setPartitionType(String partitionType) {
    this.partitionType = partitionType;
  }

  public String getPartitionName() {
    return partitionName;
  }

  public void setPartitionName(String partitionName) {
    this.partitionName = partitionName;
  }

  public long getRecordCount() {
    return recordCount;
  }

  public void setRecordCount(long recordCount) {
    this.recordCount = recordCount;
  }

  public long getLateRecordCount() {
    return lateRecordCount;
  }

  public void setLateRecordCount(long lateRecordCount) {
    this.lateRecordCount = lateRecordCount;
  }

  public String getDedupeStatus() {
    return dedupeStatus;
  }

  public void setDedupeStatus(String dedupeStatus) {
    this.dedupeStatus = dedupeStatus;
  }

  public String getJobContext() {
    return jobContext;
  }

  public void setJobContext(String jobContext) {
    this.jobContext = jobContext;
  }

  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public String getFlowName() {
    return flowName;
  }

  public void setFlowName(String flowName) {
    this.flowName = flowName;
  }

  public String getJobName() {
    return jobName;
  }

  public void setJobName(String jobName) {
    this.jobName = jobName;
  }

  public int getFlowExecId() {
    return flowExecId;
  }

  public void setFlowExecId(int flowExecId) {
    this.flowExecId = flowExecId;
  }

  public long getLogEventTime() {
    return logEventTime;
  }

  public void setLogEventTime(long logEventTime) {
    this.logEventTime = logEventTime;
  }
}
