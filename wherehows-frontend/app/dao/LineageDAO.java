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
package dao;

import java.math.BigInteger;
import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import play.Logger;
import play.libs.Json;
import utils.Lineage;
import wherehows.models.table.ImpactDataset;
import wherehows.models.table.LineageEdge;
import wherehows.models.table.LineageNode;
import wherehows.models.table.LineagePathInfo;
import wherehows.models.table.ScriptInfo;


public class LineageDAO extends AbstractMySQLOpenSourceDAO
{
	private final static String GET_APPLICATION_ID = "SELECT DISTINCT app_id FROM " +
			"job_execution_data_lineage WHERE abstracted_object_name = ?";

	private final static String GET_FLOW_NAME =
			"SELECT flow_name "	+
			"  FROM flow " +
			" WHERE app_id = ? AND flow_id = ?";

	private final static String GET_JOB =
			"SELECT ca.app_id, ca.app_code AS cluster, jedl.job_name, fj.job_path, fj.job_type, " +
			"       jedl.flow_path, jedl.storage_type, jedl.source_target_type, jedl.operation, MAX(jedl.source_srl_no), " +
			"		    MAX(jedl.srl_no), MAX(jedl.job_exec_id) AS job_exec_id " +
			"  FROM job_execution_data_lineage jedl " +
			"  JOIN cfg_application ca" +
			"    ON ca.app_id = jedl.app_id " +
			"  LEFT JOIN job_execution je " +
			"    ON jedl.app_id = je.app_id AND jedl.flow_exec_id = je.flow_exec_id AND jedl.job_exec_id = je.job_exec_id " +
			"  LEFT JOIN flow_job fj " +
			"		 ON je.app_id = fj.app_id AND je.flow_id = fj.flow_id AND je.job_id = fj.job_id " +
			" WHERE abstracted_object_name IN ( :names ) " +
			"   AND jedl.flow_path NOT REGEXP '^(rent-metrics:|tracking-investigation:)' " +
			"   AND FROM_UNIXTIME(job_finished_unixtime) >  CURRENT_DATE - INTERVAL (:days) DAY " +
			" GROUP BY ca.app_id, cluster, jedl.job_name, fj.job_path, fj.job_type, jedl.flow_path, " +
			"       jedl.source_target_type, jedl.storage_type, jedl.operation " +
			" ORDER BY jedl.source_target_type DESC, job_exec_id";

	private final static String GET_UP_LEVEL_JOB =
			"SELECT ca.app_id, ca.app_code AS cluster, jedl.job_name, fj.job_path, fj.job_type, " +
	    "       jedl.flow_path, jedl.storage_type, jedl.source_target_type, jedl.operation, MAX(jedl.source_srl_no), " +
			"       MAX(jedl.srl_no), MAX(jedl.job_exec_id) AS job_exec_id " +
			"  FROM job_execution_data_lineage jedl " +
			"  JOIN cfg_application ca " +
			"		 ON ca.app_id = jedl.app_id " +
			"  LEFT JOIN job_execution je " +
			"    ON jedl.app_id = je.app_id AND jedl.flow_exec_id = je.flow_exec_id AND jedl.job_exec_id = je.job_exec_id " +
			"  LEFT JOIN flow_job fj " +
		  "    ON je.app_id = fj.app_id AND je.flow_id = fj.flow_id AND je.job_id = fj.job_id " +
			" WHERE abstracted_object_name IN ( :names ) " +
			"   AND jedl.source_target_type = 'target' " +
			"   AND jedl.flow_path not REGEXP '^(rent-metrics:|tracking-investigation:)' " +
			"   AND FROM_UNIXTIME(job_finished_unixtime) >  CURRENT_DATE - INTERVAL (:days) DAY " +
			" GROUP BY ca.app_id, cluster, jedl.job_name, fj.job_path, fj.job_type, jedl.flow_path, " +
			"          jedl.source_target_type, jedl.storage_type, jedl.operation " +
			" ORDER BY jedl.source_target_type DESC, job_exec_id";

	private final static String GET_JOB_WITH_SOURCE =
			"SELECT ca.app_id, ca.app_code AS cluster, jedl.job_name, fj.job_path, fj.job_type, " +
			"       jedl.flow_path, jedl.storage_type, jedl.source_target_type, jedl.operation, MAX(jedl.source_srl_no), " +
			"       MAX(jedl.srl_no), MAX(jedl.job_exec_id) AS job_exec_id " +
			"  FROM job_execution_data_lineage jedl " +
			"  JOIN cfg_application ca " +
			"    ON ca.app_id = jedl.app_id " +
			"  LEFT JOIN job_execution je " +
			"    ON jedl.app_id = je.app_id AND jedl.flow_exec_id = je.flow_exec_id AND jedl.job_exec_id = je.job_exec_id " +
			"  LEFT JOIN flow_job fj " +
			"    ON je.app_id = fj.app_id AND je.flow_id = fj.flow_id AND je.job_id = fj.job_id " +
			" WHERE abstracted_object_name IN ( :names ) " +
			"   AND jedl.source_target_type != (:type) " +
			"   AND jedl.flow_path not REGEXP '^(rent-metrics:|tracking-investigation:)' " +
			"   AND FROM_UNIXTIME(job_finished_unixtime) >  CURRENT_DATE - INTERVAL (:days) DAY " +
			" GROUP BY ca.app_id, cluster, jedl.job_name, fj.job_path, fj.job_type, " +
			"          jedl.flow_path, jedl.source_target_type, jedl.storage_type, jedl.operation " +
			" ORDER BY jedl.source_target_type DESC, job_exec_id";

	private final static String GET_DATA =
			"SELECT storage_type, operation, abstracted_object_name, source_target_type, job_start_unixtime, " +
			"       job_finished_unixtime, FROM_UNIXTIME(job_start_unixtime) AS start_time, " +
			"       FROM_UNIXTIME(job_finished_unixtime) AS end_time " +
			"  FROM job_execution_data_lineage " +
			" WHERE app_id = ? " +
			"   AND job_exec_id = ? " +
			"   AND flow_path NOT REGEXP '^(rent-metrics:|tracking-investigation:)' " +
			"		AND COALESCE(source_srl_no, srl_no) = srl_no " +
			" ORDER BY source_target_type DESC";

	private final static String GET_DATA_FILTER_OUT_LASSEN =
			"SELECT j1.storage_type, j1.operation, j1.abstracted_object_name, j1.source_target_type, j1.job_start_unixtime, " +
			"       j1.job_finished_unixtime, FROM_UNIXTIME(j1.job_start_unixtime) AS start_time, " +
			"       FROM_UNIXTIME(j1.job_finished_unixtime) AS end_time " +
			"  FROM job_execution_data_lineage j1 " +
			"  JOIN job_execution_data_lineage j2 " +
			"    ON j1.app_id = j2.app_id AND j1.job_exec_id = j2.job_exec_id " +
			"       AND j2.abstracted_object_name IN (:names) AND j2.source_target_type = 'source' " +
			" WHERE j1.app_id = (:appid) " +
			"   AND j1.job_exec_id = (:execid) " +
			"   AND j1.flow_path NOT REGEXP '^(rent-metrics:|tracking-investigation:)' " +
			"   AND COALESCE(j1.source_srl_no, j1.srl_no) = j2.srl_no " +
			" ORDER BY j1.source_target_type DESC";


	private final static String GET_APP_ID  =
			"SELECT app_id" +
			"  FROM cfg_application" +
			" WHERE LOWER(app_code) = ?";


	private final static String GET_FLOW_JOB =
			"SELECT ca.app_id, ca.app_code, je.flow_id, je.job_id, jedl.job_name, " +
			"       fj.job_path, fj.job_type, jedl.flow_path, jedl.storage_type, jedl.source_target_type, " +
			"       jedl.operation, jedl.job_exec_id, fj.pre_jobs, fj.post_jobs, " +
			"       FROM_UNIXTIME(jedl.job_start_unixtime) AS start_time, " +
			"       FROM_UNIXTIME(jedl.job_finished_unixtime) AS end_time " +
			"  FROM job_execution_data_lineage jedl " +
			"  JOIN cfg_application ca " +
			"    ON ca.app_id = jedl.app_id " +
			"  JOIN job_execution je " +
			"    ON jedl.app_id = je.app_id AND jedl.flow_exec_id = je.flow_exec_id AND jedl.job_exec_id = je.job_exec_id " +
			"  JOIN flow_job fj " +
			"    ON je.app_id = fj.app_id AND je.flow_id = fj.flow_id AND je.job_id = fj.job_id " +
			" WHERE jedl.app_id = ? " +
			"   AND jedl.flow_exec_id = ? " +
			"   AND FROM_UNIXTIME(job_finished_unixtime) >  CURRENT_DATE - INTERVAL ? DAY";

	private final static String GET_LATEST_FLOW_EXEC_ID =
			"SELECT MAX(flow_exec_id) " +
			"  FROM flow_execution " +
			" WHERE flow_exec_status IN ('SUCCEEDED', 'FINISHED') " +
			"   AND app_id = ? " +
			"   AND flow_id = ?";

	private final static String GET_FLOW_DATA_LINEAGE =
			"SELECT ca.app_code, jedl.job_exec_id, jedl.job_name, jedl.storage_type, jedl.abstracted_object_name, " +
			"       jedl.source_target_type, jedl.record_count, jedl.app_id, jedl.partition_type, jedl.operation, " +
			"       jedl.partition_start, jedl.partition_end, jedl.full_object_name, " +
			"       FROM_UNIXTIME(jedl.job_start_unixtime) AS start_time, " +
			"       FROM_UNIXTIME(jedl.job_finished_unixtime) AS end_time " +
			"  FROM job_execution_data_lineage jedl " +
			"  JOIN cfg_application ca " +
			"    ON ca.app_id = jedl.app_id " +
			" WHERE jedl.app_id = ? " +
			"   AND jedl.flow_exec_id = ? " +
			" ORDER BY jedl.partition_end DESC";

	private final static String GET_ONE_LEVEL_IMPACT_DATABASES =
			"SELECT DISTINCT j.storage_type, j.abstracted_object_name, d.id " +
			"  FROM job_execution_data_lineage j " +
			"  LEFT JOIN dict_dataset d "+
			"    ON d.urn = concat(j.storage_type, '://', j.abstracted_object_name) " +
			" WHERE (app_id, job_exec_id) IN " +
			"         ( SELECT app_id, job_exec_id " +
			"             FROM job_execution_data_lineage " +
			"            WHERE abstracted_object_name IN (:pathlist) " +
			"              AND source_target_type = 'source' " +
			"              AND FROM_UNIXTIME(job_finished_unixtime) >  CURRENT_DATE - INTERVAL 60 DAY ) " +
			"   AND abstracted_object_name NOT LIKE '/tmp/%' " +
			"   AND abstracted_object_name NOT LIKE '%tmp' " +
			"   AND source_target_type = 'target' "+
			"   AND FROM_UNIXTIME(job_finished_unixtime) > CURRENT_DATE - INTERVAL 60 DAY";

	private final static String GET_MAPPED_OBJECT_NAME =
			"SELECT mapped_object_name " +
			"  FROM cfg_object_name_map " +
			"	WHERE object_name = ?";

	private final static String GET_OBJECT_NAME_BY_MAPPED_NAME =
			"SELECT object_name " +
			"  FROM cfg_object_name_map" +
			" WHERE mapped_object_name = ?";

	private final static String GET_SCRIPT_INFO_BY_JOB_ID =
			"SELECT script_url, script_name, script_path, script_type " +
			"  FROM job_execution_script " +
			" WHERE app_id = ? " +
			"   AND job_id = ? " +
			" LIMIT 1";


	public static JsonNode getObjectAdjacnet(String urn, int upLevel, int downLevel, int lookBackTime)
	{
		int level = 0;
		LineagePathInfo pathInfo = utils.Lineage.convertFromURN(urn);
		List<LineageNode> nodes = new ArrayList<LineageNode>();
		List<LineageEdge> edges = new ArrayList<LineageEdge>();
		Map<Long, List<LineageNode>> addedSourceNodes= new HashMap<Long, List<LineageNode>>();
		Map<Long, List<LineageNode>> addedTargetNodes= new HashMap<Long, List<LineageNode>>();
		Map<Long, LineageNode> addedJobNodes = new HashMap<Long, LineageNode>();
		List<LineageNode> allSourceNodes = new ArrayList<LineageNode>();
		List<LineageNode> allTargetNodes = new ArrayList<LineageNode>();
		getObjectAdjacentNode(
				pathInfo,
				level,
				upLevel,
				downLevel,
				null,
				allSourceNodes,
				allTargetNodes,
				addedSourceNodes,
				addedTargetNodes,
				addedJobNodes,
				lookBackTime);

		return renderGraph(
				pathInfo,
				urn,
				upLevel,
				downLevel,
				allSourceNodes,
				allTargetNodes,
				addedSourceNodes,
				addedTargetNodes,
				addedJobNodes,
				nodes,
				edges);
	}

	public static void addSourceNode(
			LineageNode jobNode,
			Map<Long, List<LineageNode>> addedSourceNodes,
			Map<String, List<LineageNode>> addedTargetDataNodes,
			Map<String, LineageNode> addedSourceDataNodes,
			Map<String, List<LineageNode>> toBeConvertedSourceDataNodes,
			List<LineageNode> nodes,
			List<LineageEdge> edges,
			boolean isUpLevel)
	{
		List<LineageNode> sourceNodes = addedSourceNodes.get(jobNode.execId);
		if (sourceNodes != null)
		{
			for(LineageNode node : sourceNodes)
			{
				List<LineageNode> existTargetNodes = addedTargetDataNodes.get(node.abstractedPath);
				LineageNode existNode = null;
				if (existTargetNodes != null)
				{
					Collections.sort(existTargetNodes, new Comparator<LineageNode>(){
						public int compare(LineageNode node1, LineageNode node2){
							if(node1.jobEndUnixTime == node2.jobEndUnixTime)
								return 0;
							return node1.jobEndUnixTime > node2.jobEndUnixTime ? -1 : 1;
						}
					});
					for(LineageNode target : existTargetNodes)
					{
						if (node.jobStartUnixTime >= target.jobEndUnixTime)
						{
							existNode = target;
							break;
						}
					}
				}
				if (existNode == null)
				{
					existNode = addedSourceDataNodes.get(node.abstractedPath);
				}
				if (existNode != null)
				{   node.id = existNode.id;
				}
				else
				{
					node.id = nodes.size();
					nodes.add(node);
					addedSourceDataNodes.put(node.abstractedPath, node);
					if (isUpLevel) {
						List<LineageNode> originalSourceNodes = toBeConvertedSourceDataNodes.get(node.abstractedPath);
						if (originalSourceNodes == null) {
							originalSourceNodes = new ArrayList<LineageNode>();
						}
						originalSourceNodes.add(node);
						toBeConvertedSourceDataNodes.put(node.abstractedPath, originalSourceNodes);
					}

				}
				LineageEdge edge = new LineageEdge();
				edge.id = edges.size();
				edge.source = node.id;
				edge.target = jobNode.id;
				edge.label = node.operation;
				edge.chain = "";
				edges.add(edge);
			}
		}
	}

	public static void addTargetNode(
			LineageNode jobNode,
			Map<Long, List<LineageNode>> addedTargetNodes,
			Map<String, List<LineageNode>> addedTargetDataNodes,
			Map<String, LineageNode> toBeClearedSourceDataNodes,
			List<LineageNode> nodes,
			List<LineageEdge> edges,
			boolean isUpLevel)
	{
		List<LineageNode> targetNodes = addedTargetNodes.get(jobNode.execId);
		if (targetNodes != null)
		{
			for(LineageNode node : targetNodes)
			{
				List<LineageNode> existTargetNodes = addedTargetDataNodes.get(node.abstractedPath);
				LineageNode existNode = null;
				if (existTargetNodes != null)
				{
					for(LineageNode target : existTargetNodes)
					{
						if (target.partitionEnd != null)
						{
							if (target.partitionEnd.equals(node.partitionEnd))
							{
								existNode = target;
								break;
							}
						}
						else if(target.jobEndUnixTime == node.jobEndUnixTime)
						{
							existNode = target;
							break;
						}
					}
				}
				if (isUpLevel) {
					if (existNode == null)
					{
						existNode = toBeClearedSourceDataNodes.get(node.abstractedPath);
						if (existNode != null && existNode.jobStartUnixTime < node.jobEndUnixTime)
						{
							existNode = null;
						}
					}
				}

				if (existNode != null)
				{
					node.id = existNode.id;
				}
				else
				{
					node.id = nodes.size();
					nodes.add(node);
					if (existTargetNodes == null)
					{
						existTargetNodes = new ArrayList<LineageNode>();
					}
					existTargetNodes.add(node);
					addedTargetDataNodes.put(node.abstractedPath, existTargetNodes);
				}
				LineageEdge edge = new LineageEdge();
				edge.id = edges.size();
				edge.source = jobNode.id;
				edge.target = node.id;
				edge.label = node.operation;
				edge.chain = "";
				edges.add(edge);
			}
		}
	}

	public static JsonNode renderGraph(LineagePathInfo pathInfo,
									   String urn,
									   int upLevel,
									   int downLevel,
									   List<LineageNode> allSourceNodes,
									   List<LineageNode> allTargetNodes,
									   Map<Long, List<LineageNode>> addedSourceNodes,
									   Map<Long, List<LineageNode>> addedTargetNodes,
									   Map<Long, LineageNode> addedJobNodes,
									   List<LineageNode> nodes,
									   List<LineageEdge> edges)
	{
		ObjectNode resultNode = Json.newObject();
		String message = null;
		Map<String, LineageNode> addedSourceDataNodes = new HashMap<String, LineageNode>();
		Map<String, LineageNode> toBeClearedSourceDataNodes = new HashMap<String, LineageNode>();
		Map<String, List<LineageNode>> addedTargetDataNodes = new HashMap<String, List<LineageNode>>();
		Map<String, List<LineageNode>> toBeConvertedSourceDataNodes = new HashMap<String, List<LineageNode>>();
		message = "No lineage information found for this dataset";
		if (allSourceNodes.size() == 0 && allTargetNodes.size() == 0)
		{
			LineageNode node = new LineageNode();
			node.id = nodes.size();
			node.sortList = new ArrayList<String>();
			node.nodeType = "data";
			node.abstractedPath = pathInfo.filePath;
			node.storageType = pathInfo.storageType;
			node.sortList.add("abstracted_path");
			node.sortList.add("storage_type");
			node.sortList.add("urn");
			node.urn = urn;
			nodes.add(node);
		}
		else
		{
			message = "Found lineage information";
			for(int i = 0; i < Math.max(upLevel, downLevel); i++)
			{
				if (i < upLevel)
				{
					if (toBeConvertedSourceDataNodes.size() > 0)
					{
						for(Map.Entry<String, List<LineageNode> > mapEntry : toBeConvertedSourceDataNodes.entrySet())
						{
							List<LineageNode> hashedNodes = addedTargetDataNodes.get(mapEntry.getKey());
							List<LineageNode> list = mapEntry.getValue();
							if (list != null && list.size() > 0)
							{
								if (hashedNodes == null)
								{
									hashedNodes = new ArrayList<LineageNode>();
								}
								hashedNodes.addAll(list);
								addedTargetDataNodes.put(mapEntry.getKey(), hashedNodes);
							}

						}
						toBeConvertedSourceDataNodes.clear();
					}
					if (addedSourceDataNodes.size() > 0)
					{
						toBeClearedSourceDataNodes.putAll(addedSourceDataNodes);
						addedSourceDataNodes.clear();
					}

					for(LineageNode job : addedJobNodes.values())
					{
						if (job.level != i)
						{
							continue;
						}
						job.id = nodes.size();
						nodes.add(job);
						addTargetNode(job,
								addedTargetNodes,
								addedTargetDataNodes,
								toBeClearedSourceDataNodes,
								nodes,
								edges,
								true);

						addSourceNode(job,
								addedSourceNodes,
								addedTargetDataNodes,
								addedSourceDataNodes,
								toBeConvertedSourceDataNodes,
								nodes,
								edges,
								true);
					}
				}
				if ((i > 0) && (i < downLevel))
				{
					for(LineageNode job : addedJobNodes.values())
					{
						if (job.level != -i)
						{
							continue;
						}
						job.id = nodes.size();
						nodes.add(job);
						addTargetNode(job,
								addedTargetNodes,
								addedTargetDataNodes,
								toBeClearedSourceDataNodes,
								nodes,
								edges,
								false);

						addSourceNode(job,
								addedSourceNodes,
								addedTargetDataNodes,
								addedSourceDataNodes,
								toBeConvertedSourceDataNodes,
								nodes,
								edges,
								false);
					}
				}
			}
		}
		resultNode.set("nodes", Json.toJson(nodes));
		resultNode.set("links", Json.toJson(edges));
		resultNode.put("urn", urn);
		resultNode.put("message", message);
		return resultNode;

	}

	public static List<String> getLiasDatasetNames(String abstractedObjectName)
	{
		if (StringUtils.isBlank(abstractedObjectName))
			return null;
		List<String> totalNames = new ArrayList<String>();
		totalNames.add(abstractedObjectName);
		List<String> mappedNames = getJdbcTemplate().queryForList(GET_MAPPED_OBJECT_NAME, String.class, abstractedObjectName);
		if (mappedNames != null && mappedNames.size() > 0)
		{
			totalNames.addAll(mappedNames);
			for (String name : mappedNames)
			{
				List<String> objNames = getJdbcTemplate().queryForList(
						GET_OBJECT_NAME_BY_MAPPED_NAME, String.class, name);
				{
					if (objNames != null)
					{
						totalNames.addAll(objNames);
					}
				}

			}
		}
		else
		{
			List<String> objNames = getJdbcTemplate().queryForList(
					GET_OBJECT_NAME_BY_MAPPED_NAME, String.class, abstractedObjectName);
			{
				if (objNames != null)
				{
					totalNames.addAll(objNames);
				}
			}

		}
		List<String> results = new ArrayList<String>();
		Set<String> sets = new HashSet<String>();
		sets.addAll(totalNames);
		results.addAll(sets);
		return results;
	}

	public static ScriptInfo getScriptInfo(int appId, Long jobId)
	{
		List<Map<String, Object>> rows = null;
		ScriptInfo scriptInfo = new ScriptInfo();
		rows = getJdbcTemplate().queryForList(GET_SCRIPT_INFO_BY_JOB_ID, appId, jobId);
		if (rows != null)
		{
			for (Map row : rows)
			{
				scriptInfo.gitPath = (String)row.get("script_url");
				scriptInfo.scriptName = (String)row.get("script_name");
				scriptInfo.scriptPath = (String)row.get("script_path");
				scriptInfo.scriptType = (String)row.get("script_type");
				break;
			}
		}
		return scriptInfo;
	}

	public static void getNodes(
			LineagePathInfo pathInfo,
			int level,
			int upLevel,
			int downLevel,
			LineageNode currentNode,
			List<LineageNode> allSourceNodes,
			List<LineageNode> allTargetNodes,
			Map<Long, List<LineageNode>>addedSourceNodes,
			Map<Long, List<LineageNode>>addedTargetNodes,
			Map<Long, LineageNode> addedJobNodes,
			int lookBackTime)
	{
		if (upLevel < 1 && downLevel < 1)
		{
			return;
		}
		if (currentNode != null)
		{
			if ( StringUtils.isBlank(currentNode.sourceTargetType))
			{
				Logger.error("Source target type is not available");
				Logger.error(currentNode.abstractedPath);
				return;
			}
			else if (currentNode.sourceTargetType.equalsIgnoreCase("target") && downLevel <= 0)
			{
				Logger.warn("Dataset " + currentNode.abstractedPath + " downLevel = " + Integer.toString(downLevel));
				return;
			}
			else if (currentNode.sourceTargetType.equalsIgnoreCase("source") && upLevel <= 0)
			{
				Logger.warn("Dataset " + currentNode.abstractedPath + " upLevel = " + Integer.toString(upLevel));
				return;
			}
		}
		List<String> nameList = getLiasDatasetNames(pathInfo.filePath);
		List<Map<String, Object>> rows = null;
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		parameters.addValue("names", nameList);
		NamedParameterJdbcTemplate namedParameterJdbcTemplate = new
				NamedParameterJdbcTemplate(getJdbcTemplate().getDataSource());
		parameters.addValue("days", lookBackTime);

		if (currentNode != null)
		{
			if (currentNode.sourceTargetType.equalsIgnoreCase("source"))
			{
				rows = namedParameterJdbcTemplate.queryForList(
						GET_UP_LEVEL_JOB,
						parameters);
			}
			else
			{
				parameters.addValue("type", currentNode.sourceTargetType);
				rows = namedParameterJdbcTemplate.queryForList(
						GET_JOB_WITH_SOURCE,
						parameters);
			}

		}
		else
		{
			rows = namedParameterJdbcTemplate.queryForList(
					GET_JOB,
					parameters);
		}

		if (rows != null) {
			for (Map row : rows) {
				LineageNode node = new LineageNode();
				Object jobExecIdObject = row.get("job_exec_id");
				if (jobExecIdObject == null) {
					continue;
				}
				Long jobExecId = ((BigInteger) jobExecIdObject).longValue();
				if (addedJobNodes.get(jobExecId) != null) {
					continue;
				}
				node.sortList = new ArrayList<String>();
				node.nodeType = "script";
				node.jobType = (String) row.get("job_type");
				node.cluster = (String) row.get("cluster");
				node.jobName = (String) row.get("job_name");
				node.jobPath = (String) row.get("flow_path") + "/" + node.jobName;
				node.execId = jobExecId;
				node.applicationId = (Integer)row.get("app_id");
				node.jobId = (Long)row.get("job_id");
				node.operation = (String) row.get("operation");
				ScriptInfo scriptInfo = getScriptInfo(node.applicationId, node.jobId);
				node.gitLocation = scriptInfo.gitPath;
				node.scriptName = scriptInfo.scriptName;
				node.scriptPath = scriptInfo.scriptPath;
				node.scriptType = scriptInfo.scriptType;
				node.sourceTargetType = (String) row.get("source_target_type");
				node.level = level;
				node.sortList.add("cluster");
				node.sortList.add("job_path");
				node.sortList.add("job_name");
				node.sortList.add("job_type");
				node.sortList.add("job_start_time");
				node.sortList.add("job_end_time");
				node.sortList.add("script_name");
				node.sortList.add("script_path");
				node.sortList.add("script_type");
				node.sortList.add("exec_id");
				node.sortList.add("job_id");
				addedJobNodes.put(jobExecId, node);
				List<LineageNode> sourceNodeList = new ArrayList<LineageNode>();
				List<LineageNode> targetNodeList = new ArrayList<LineageNode>();
				int applicationID = (int)row.get("app_id");
				Long jobId = ((BigInteger)row.get("job_exec_id")).longValue();
				List<Map<String, Object>> relatedDataRows = null;

				if (node.sourceTargetType.equalsIgnoreCase("source") && node.operation.equalsIgnoreCase("JDBC Read"))
				{
					MapSqlParameterSource lassenParams = new MapSqlParameterSource();
					lassenParams.addValue("names", nameList);
					lassenParams.addValue("appid", applicationID);
					lassenParams.addValue("execid", jobId);
					relatedDataRows = namedParameterJdbcTemplate.queryForList(
							GET_DATA_FILTER_OUT_LASSEN,
							lassenParams);
				}
				else
				{
					relatedDataRows = getJdbcTemplate().queryForList(
							GET_DATA,
							applicationID,
							jobId);
				}

				if (relatedDataRows != null) {
					for (Map relatedDataRow : relatedDataRows)
					{
						String abstractedObjectName = (String)relatedDataRow.get("abstracted_object_name");
						if (abstractedObjectName.startsWith("/tmp/"))
						{
							continue;
						}
						String relatedSourceType = (String)relatedDataRow.get("source_target_type");
						LineageNode relatedNode = new LineageNode();
						relatedNode.sortList = new ArrayList<String>();
						relatedNode.nodeType = "data";
						relatedNode.level = level;
						relatedNode.sourceTargetType = relatedSourceType;
						relatedNode.abstractedPath = (String)relatedDataRow.get("abstracted_object_name");
						relatedNode.storageType = ((String)relatedDataRow.get("storage_type")).toLowerCase();
						relatedNode.jobStartUnixTime = (Long)relatedDataRow.get("job_start_unixtime");

						relatedNode.jobStartTime = relatedDataRow.get("start_time").toString();
						relatedNode.jobEndTime = relatedDataRow.get("end_time").toString();
						relatedNode.jobEndUnixTime = (Long)relatedDataRow.get("job_finished_unixtime");
						node.jobStartUnixTime = relatedNode.jobStartUnixTime;
						node.jobEndUnixTime = relatedNode.jobEndUnixTime;
						node.jobStartTime = relatedNode.jobStartTime;
						node.jobEndTime = relatedNode.jobEndTime;
						relatedNode.operation = (String)relatedDataRow.get("operation");
						LineagePathInfo info = new LineagePathInfo();
						info.filePath = relatedNode.abstractedPath;
						info.storageType = relatedNode.storageType;
						relatedNode.urn = utils.Lineage.convertToURN(info);
						relatedNode.sortList.add("abstracted_path");
						relatedNode.sortList.add("storage_type");
						relatedNode.sortList.add("urn");
						if (relatedSourceType.equalsIgnoreCase("source"))
						{
							if (node.sourceTargetType.equalsIgnoreCase("target") ||
									utils.Lineage.isInList(nameList, relatedNode.abstractedPath))
							{
								sourceNodeList.add(relatedNode);
								allSourceNodes.add(relatedNode);
							}
						}
						else if (relatedSourceType.equalsIgnoreCase("target"))
						{
							if (node.sourceTargetType.equalsIgnoreCase("source") ||
									utils.Lineage.isInList(nameList, relatedNode.abstractedPath))
							{
								targetNodeList.add(relatedNode);
								allTargetNodes.add(relatedNode);
							}
						}
					}
					if (sourceNodeList.size() > 0)
					{
						addedSourceNodes.put(jobExecId, sourceNodeList);
					}
					if (targetNodeList.size() > 0)
					{
						addedTargetNodes.put(jobExecId, targetNodeList);
					}
				}
			}
		}
		if ((allSourceNodes != null ) && (allSourceNodes.size() > 0) && (upLevel > 1))
		{
			List<LineageNode> currentSourceNodes = new ArrayList<LineageNode>();
			currentSourceNodes.addAll(allSourceNodes);
			for(LineageNode sourceNode : currentSourceNodes)
			{
				LineagePathInfo subPath = new LineagePathInfo();
				subPath.storageType = sourceNode.storageType;
				subPath.filePath = sourceNode.abstractedPath;
				if (sourceNode.level == level)
				{
					getObjectAdjacentNode(
							subPath,
							level+1,
							upLevel - 1,
							0,
							sourceNode,
							allSourceNodes,
							allTargetNodes,
							addedSourceNodes,
							addedTargetNodes,
							addedJobNodes,
							lookBackTime);
				}
			}
		}
		if ((allTargetNodes != null ) && (allTargetNodes.size() > 0) && (downLevel > 1))
		{
			List<LineageNode> currentTargetNodes = new ArrayList<LineageNode>();
			currentTargetNodes.addAll(allTargetNodes);
			for(LineageNode targetNode : currentTargetNodes)
			{
				LineagePathInfo subPath = new LineagePathInfo();
				subPath.storageType = targetNode.storageType;
				subPath.filePath = targetNode.abstractedPath;
				if (targetNode.level == level)
				{
					getObjectAdjacentNode(
							subPath,
							level-1,
							0,
							downLevel - 1,
							targetNode,
							allSourceNodes,
							allTargetNodes,
							addedSourceNodes,
							addedTargetNodes,
							addedJobNodes,
							lookBackTime);
				}
			}
		}

	}

	public static void getObjectAdjacentNode(
			LineagePathInfo pathInfo,
			int level,
			int upLevel,
			int downLevel,
			LineageNode currentNode,
			List<LineageNode> allSourceNodes,
			List<LineageNode> allTargetNodes,
			Map<Long, List<LineageNode>> addedSourceNodes,
			Map<Long, List<LineageNode>> addedTargetNodes,
			Map<Long, LineageNode> addedJobNodes,
			int lookBackTime)
	{
		if (upLevel < 1 && downLevel < 1)
		{
			return;
		}

		if (pathInfo == null || StringUtils.isBlank(pathInfo.filePath))
		{
			return;
		}
		getNodes(
				pathInfo,
				level,
				upLevel,
				downLevel,
				currentNode,
				allSourceNodes,
				allTargetNodes,
				addedSourceNodes,
				addedTargetNodes,
				addedJobNodes,
				lookBackTime);
	}

	public static ObjectNode getFlowLineage(String application, String project, Long flowId)
	{
		ObjectNode resultNode = Json.newObject();
		List<LineageNode> nodes = new ArrayList<LineageNode>();
		List<LineageEdge> edges = new ArrayList<LineageEdge>();
		String flowName = null;

		Map<Long, Integer> addedJobNodes = new HashMap<Long, Integer>();
		Map<Pair, Integer> addedDataNodes = new HashMap<Pair, Integer>();

		if (StringUtils.isBlank(application) || StringUtils.isBlank(project) || (flowId <= 0))
		{
			resultNode.set("nodes", Json.toJson(nodes));
			resultNode.set("links", Json.toJson(edges));
			return resultNode;
		}

		String applicationName = application.replace(".", " ");

		int appID = 0;
		try
		{
			appID = getJdbcTemplate().queryForObject(
					GET_APP_ID,
					new Object[] {applicationName},
					Integer.class);
		}
		catch(EmptyResultDataAccessException e)
		{
			Logger.error("getFlowLineage get application id failed, application name = " + application);
			Logger.error("Exception = " + e.getMessage());
		}

		Map<Long, List<LineageNode>> nodeHash = new HashMap<Long, List<LineageNode>>();
		Map<String, List<LineageNode>> partitionedNodeHash = new HashMap<String, List<LineageNode>>();

		if (appID != 0)
		{
			try
			{
				flowName = getJdbcTemplate().queryForObject(
						GET_FLOW_NAME,
						new Object[] {appID, flowId},
						String.class);
			}
			catch(EmptyResultDataAccessException e)
			{
				Logger.error("getFlowLineage get flow name failed, application name = " + application +
						" flowId " + Long.toString(flowId));
				Logger.error("Exception = " + e.getMessage());
			}

			Long flowExecId = 0L;
			try
			{
				flowExecId = getJdbcTemplate().queryForObject(
						GET_LATEST_FLOW_EXEC_ID,
						new Object[] {appID, flowId},
						Long.class);
			}
			catch(EmptyResultDataAccessException e)
			{
				Logger.error("getFlowLineage get flow execution id failed, application name = " + application +
						" flowId " + Long.toString(flowExecId));
				Logger.error("Exception = " + e.getMessage());
			}
			List<Map<String, Object>> rows = null;
			rows = getJdbcTemplate().queryForList(
					GET_FLOW_DATA_LINEAGE,
					appID,
					flowExecId);
			if (rows != null)
			{
				for (Map row : rows) {
					Long jobExecId = ((BigInteger)row.get("job_exec_id")).longValue();
					LineageNode node = new LineageNode();
					node.abstractedPath = (String)row.get("abstracted_object_name");
					node.sourceTargetType = (String)row.get("source_target_type");
					node.execId = jobExecId;
					Object recordCountObject = row.get("record_count");
					if (recordCountObject != null)
					{
						node.recordCount = ((BigInteger)recordCountObject).longValue();
					}

					node.applicationId = (int)row.get("app_id");
					node.cluster = (String)row.get("app_code");
					node.partitionType = (String)row.get("partition_type");
					node.operation = (String)row.get("operation");
					node.partitionStart = (String)row.get("partition_start");
					node.partitionEnd = (String)row.get("partition_end");
					node.fullObjectName = (String)row.get("full_object_name");
					node.jobStartTime = row.get("start_time").toString();
					node.jobEndTime = row.get("end_time").toString();
					node.storageType = ((String)row.get("storage_type")).toLowerCase();
					node.nodeType = "data";
					node.sortList = new ArrayList<String>();
					node.sortList.add("cluster");
					node.sortList.add("abstracted_path");
					node.sortList.add("storage_type");
					node.sortList.add("partition_type");
					node.sortList.add("partition_start");
					node.sortList.add("partition_end");
					node.sortList.add("source_target_type");
					List<LineageNode> nodeList = nodeHash.get(jobExecId);
					if (nodeList != null)
					{
						nodeList.add(node);
					}
					else
					{
						nodeList = new ArrayList<LineageNode>();
						nodeList.add(node);
						nodeHash.put(jobExecId, nodeList);
					}
				}
			}

			List<LineageNode> jobNodes = new ArrayList<LineageNode>();
			List<Map<String, Object>> jobRows = null;
			jobRows = getJdbcTemplate().queryForList(
					GET_FLOW_JOB,
					appID,
					flowExecId,
					30);
			int index = 0;
			int edgeIndex = 0;
			Map<Long, LineageNode> jobNodeMap = new HashMap<Long, LineageNode>();
			List<Pair> addedEdges = new ArrayList<Pair>();
			if (rows != null)
			{
				for (Map row : jobRows) {
					Long jobExecId = ((BigInteger)row.get("job_exec_id")).longValue();
					LineageNode node = new LineageNode();
					node.sortList = new ArrayList<String>();
					node.nodeType = "script";
					node.jobType = (String)row.get("job_type");
					node.cluster = (String)row.get("app_code");
					node.jobPath = (String)row.get("job_path");
					node.jobName = (String)row.get("job_name");
					node.preJobs = (String)row.get("pre_jobs");
					node.postJobs = (String)row.get("post_jobs");
					node.jobId = (Long)row.get("job_id");
					ScriptInfo scriptInfo = getScriptInfo(node.applicationId, node.jobId);
					node.gitLocation = scriptInfo.gitPath;
					node.scriptName = scriptInfo.scriptName;
					node.scriptPath = scriptInfo.scriptPath;
					node.scriptType = scriptInfo.scriptType;
					node.jobStartTime = row.get("start_time").toString();
					node.jobEndTime = row.get("end_time").toString();
					node.execId = jobExecId;
					node.sortList.add("cluster");
					node.sortList.add("job_path");
					node.sortList.add("job_name");
					node.sortList.add("job_type");
					node.sortList.add("job_id");
					node.sortList.add("job_start_time");
					node.sortList.add("job_end_time");
					node.sortList.add("script_name");
					node.sortList.add("script_path");
					node.sortList.add("script_type");
					Integer id = addedJobNodes.get(jobExecId);
					if (id == null)
					{
						node.id = index++;
						nodes.add(node);
						jobNodeMap.put(node.jobId, node);
						jobNodes.add(node);
						addedJobNodes.put(jobExecId, node.id);
					}
					else
					{
						node.id = id;
					}

					String sourceType = (String)row.get("source_target_type");
					if (sourceType.equalsIgnoreCase("target"))
					{
						List<LineageNode> sourceNodeList = nodeHash.get(jobExecId);
						if (sourceNodeList != null && sourceNodeList.size() > 0)
						{
							for(LineageNode sourceNode : sourceNodeList)
							{
								if (sourceNode.sourceTargetType.equalsIgnoreCase("source"))
								{
									Pair matchedSourcePair = new ImmutablePair<>(
											sourceNode.abstractedPath,
											sourceNode.partitionEnd);
									Integer nodeId = addedDataNodes.get(matchedSourcePair);
									if (nodeId == null)
									{
										List<LineageNode> nodeList = partitionedNodeHash.get(sourceNode.abstractedPath);
										if (StringUtils.isBlank(sourceNode.partitionEnd))
										{
											Boolean bFound = false;
											if (nodeList != null)
											{
												for(LineageNode n : nodeList)
												{
													if (StringUtils.isNotBlank(n.partitionEnd) &&
															n.partitionEnd.compareTo(sourceNode.jobStartTime) < 0)
													{
														sourceNode.id = n.id;
														bFound = true;
														break;
													}
												}
											}
											if (!bFound)
											{
												sourceNode.id = index++;
												nodes.add(sourceNode);
												Pair sourcePair = new ImmutablePair<>(
														sourceNode.abstractedPath,
														sourceNode.partitionEnd);
												addedDataNodes.put(sourcePair, sourceNode.id);
											}
										}
										else
										{
											if (nodeList == null)
											{
												nodeList = new ArrayList<LineageNode>();
											}
											nodeList.add(sourceNode);
											partitionedNodeHash.put(sourceNode.abstractedPath, nodeList);
											sourceNode.id = index++;
											nodes.add(sourceNode);
											Pair sourcePair = new ImmutablePair<>(
													sourceNode.abstractedPath,
													sourceNode.partitionEnd);
											addedDataNodes.put(sourcePair, sourceNode.id);
										}
									}
									else
									{
										sourceNode.id = nodeId;
									}
									LineageEdge edge = new LineageEdge();
									edge.id = edgeIndex++;
									edge.source = sourceNode.id;
									edge.target = node.id;
									if (StringUtils.isNotBlank(sourceNode.operation))
									{
										edge.label = sourceNode.operation;
									}
									else
									{
										edge.label = "load";
									}
									edge.chain = "data";
									edges.add(edge);
								}
							}
						}
					}
					else if (sourceType.equalsIgnoreCase("source"))
					{

						List<LineageNode> targetNodeList = nodeHash.get(jobExecId);
						if (targetNodeList != null && targetNodeList.size() > 0)
						{
							for(LineageNode targetNode : targetNodeList)
							{
								if (targetNode.sourceTargetType.equalsIgnoreCase("target"))
								{
									Pair matchedTargetPair = new ImmutablePair<>(
											targetNode.abstractedPath,
											targetNode.partitionEnd);
									Integer nodeId = addedDataNodes.get(matchedTargetPair);
									if (nodeId == null)
									{
										List<LineageNode> nodeList = partitionedNodeHash.get(targetNode.abstractedPath);
										if (StringUtils.isBlank(targetNode.partitionEnd))
										{
											Boolean bFound = false;
											if (nodeList != null)
											{
												for(LineageNode n : nodeList)
												{
													if (StringUtils.isNotBlank(n.partitionEnd) &&
															n.partitionEnd.compareTo(targetNode.jobStartTime) < 0)
													{
														targetNode.id = n.id;
														bFound = true;
														break;
													}
												}
											}
											if (!bFound)
											{
												targetNode.id = index++;
												nodes.add(targetNode);
												Pair targetPair = new ImmutablePair<>(
														targetNode.abstractedPath,
														targetNode.partitionEnd);
												addedDataNodes.put(targetPair, targetNode.id);
											}
										}
										else
										{
											if (nodeList == null)
											{
												nodeList = new ArrayList<LineageNode>();
											}
											nodeList.add(targetNode);
											partitionedNodeHash.put(targetNode.abstractedPath, nodeList);
											targetNode.id = index++;
											nodes.add(targetNode);
											Pair targetPair = new ImmutablePair<>(
													targetNode.abstractedPath,
													targetNode.partitionEnd);
											addedDataNodes.put(targetPair, targetNode.id);
										}
									}
									else
									{
										targetNode.id = nodeId;
									}
									LineageEdge edge = new LineageEdge();
									edge.id = edgeIndex++;
									edge.source = node.id;
									edge.target = targetNode.id;
									if (StringUtils.isNotBlank(targetNode.operation))
									{
										edge.label = targetNode.operation;
									}
									else
									{
										edge.label = "load";
									}
									edge.chain = "data";
									edges.add(edge);
								}
							}
						}
					}
				}
				for (LineageNode node : jobNodes)
				{
					Long jobId = node.jobId;
					if (StringUtils.isNotBlank(node.preJobs))
					{
						String [] prevJobIds = node.preJobs.split(",");
						if (prevJobIds != null)
						{
							for(String jobIdString: prevJobIds)
							{
								if(StringUtils.isNotBlank(jobIdString))
								{
									Long id = Long.parseLong(jobIdString);
									LineageNode sourceNode = jobNodeMap.get(id);
									if (sourceNode != null)
									{
										Pair pair = new ImmutablePair<>(sourceNode.id, node.id);
										if (!addedEdges.contains(pair))
										{
											LineageEdge edge = new LineageEdge();
											edge.id = edgeIndex++;
											edge.source = sourceNode.id;
											edge.target = node.id;
											edge.label = "";
											edge.type = "job";
											edges.add(edge);
											addedEdges.add(pair);
										}
									}
								}
							}
						}
					}

					if (StringUtils.isNotBlank(node.postJobs))
					{
						String [] postJobIds = node.postJobs.split(",");
						if (postJobIds != null)
						{
							for(String jobIdString: postJobIds)
							{
								if(StringUtils.isNotBlank(jobIdString))
								{
									Long id = Long.parseLong(jobIdString);
									LineageNode targetNode = jobNodeMap.get(id);
									if (targetNode != null)
									{
										Pair pair = new ImmutablePair<>(node.id, targetNode.id);
										if (!addedEdges.contains(pair))
										{
											LineageEdge edge = new LineageEdge();
											edge.id = edgeIndex++;
											edge.source = node.id;
											edge.target = targetNode.id;
											edge.label = "";
											edge.type = "job";
											edges.add(edge);
											addedEdges.add(pair);
										}
									}
								}
							}
						}
					}
				}
			}
		}
		resultNode.set("nodes", Json.toJson(nodes));
		resultNode.set("links", Json.toJson(edges));
		resultNode.put("flowName", flowName);
		return resultNode;
	}

	public static void getImpactDatasets(
			List<String> searchUrnList,
			int level,
			List<ImpactDataset> impactDatasets)
	{
		if (searchUrnList != null && searchUrnList.size() > 0) {

			if (impactDatasets == null)
			{
				impactDatasets = new ArrayList<ImpactDataset>();
			}

			List<String> pathList = new ArrayList<String>();
			List<String> nextSearchList = new ArrayList<String>();

			for (String urn : searchUrnList)
			{
				LineagePathInfo pathInfo = Lineage.convertFromURN(urn);
				if (pathInfo != null && StringUtils.isNotBlank(pathInfo.filePath))
				{
					if (!pathList.contains(pathInfo.filePath))
					{
						pathList.add(pathInfo.filePath);
					}
				}
			}

			if (pathList != null && pathList.size() > 0)
			{
				Map<String, List> param = Collections.singletonMap("pathlist", pathList);
				NamedParameterJdbcTemplate namedParameterJdbcTemplate = new
						NamedParameterJdbcTemplate(getJdbcTemplate().getDataSource());
				List<ImpactDataset> impactDatasetList = namedParameterJdbcTemplate.query(
						GET_ONE_LEVEL_IMPACT_DATABASES,
						param,
						new ImpactDatasetRowMapper());

				if (impactDatasetList != null) {
					for (ImpactDataset dataset : impactDatasetList) {
						dataset.level = level;
						if (impactDatasets.stream().filter(o -> o.urn.equals(dataset.urn)).findFirst().isPresent())
						{
							continue;
						}
						impactDatasets.add(dataset);
						nextSearchList.add(dataset.urn);
					}
				}
			}

			if (nextSearchList.size() > 0)
			{
				getImpactDatasets(nextSearchList, level + 1, impactDatasets);
			}
		}
	}

	public static List<ImpactDataset> getImpactDatasetsByUrn(String urn)
	{
		List<ImpactDataset> impactDatasetList = new ArrayList<ImpactDataset>();

		if (StringUtils.isNotBlank(urn))
		{
			List<String> searchUrnList = new ArrayList<String>();
			searchUrnList.add(urn);
			getImpactDatasets(searchUrnList, 1, impactDatasetList);
		}

		return impactDatasetList;
	}

}
