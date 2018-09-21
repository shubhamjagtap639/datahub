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

import java.util.ArrayList;
import java.util.List;


/**
 * Created by zechen on 9/16/15.
 */
public class AppworxFlowRecord extends AbstractRecord {
  Integer appId;
  Long flowId;
  String flowName;
  String flowGroup;
  String flowPath;
  Integer flowLevel;
  Long sourceModifiedTime;
  Integer sourceVersion;
  Character isActive;
  Long whExecId;

  public AppworxFlowRecord(Integer appId, Long flowId, String flowName, String flowGroup, String flowPath,
      Integer flowLevel, Long sourceModifiedTime, Integer sourceVersion, Character isActive, Long whExecId) {
    this.appId = appId;
    this.flowId = flowId;
    this.flowName = flowName;
    this.flowGroup = flowGroup;
    this.flowPath = flowPath;
    this.flowLevel = flowLevel;
    this.sourceModifiedTime = sourceModifiedTime;
    this.sourceVersion = sourceVersion;
    this.isActive = isActive;
    this.whExecId = whExecId;
  }

  @Override
  public List<Object> fillAllFields() {
    List<Object> allFields = new ArrayList<>();
    allFields.add(appId);
    allFields.add(flowId);
    allFields.add(flowName);
    allFields.add(flowGroup);
    allFields.add(flowPath);
    allFields.add(flowLevel);
    allFields.add(sourceModifiedTime);
    allFields.add(sourceVersion);
    allFields.add(isActive);
    allFields.add(whExecId);
    return allFields;
  }
}
