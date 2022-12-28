import json
import logging
from dataclasses import dataclass
from typing import Dict, Iterable, List, Optional, Tuple, Union, cast

import pandas as pd
from snowflake.connector import SnowflakeConnection

from datahub.configuration.pattern_utils import is_schema_allowed
from datahub.emitter.mce_builder import (
    make_container_urn,
    make_data_platform_urn,
    make_dataplatform_instance_urn,
    make_dataset_urn,
    make_dataset_urn_with_platform_instance,
    make_domain_urn,
    make_schema_field_urn,
)
from datahub.emitter.mcp import MetadataChangeProposalWrapper
from datahub.emitter.mcp_builder import (
    DatabaseKey,
    PlatformKey,
    SchemaKey,
    add_dataset_to_container,
    add_domain_to_entity_wu,
    gen_containers,
)
from datahub.ingestion.api.common import PipelineContext, WorkUnit
from datahub.ingestion.api.decorators import (
    SupportStatus,
    capability,
    config_class,
    platform_name,
    support_status,
)
from datahub.ingestion.api.source import (
    CapabilityReport,
    Source,
    SourceCapability,
    SourceReport,
    TestableSource,
    TestConnectionReport,
)
from datahub.ingestion.api.workunit import MetadataWorkUnit
from datahub.ingestion.glossary.classification_mixin import ClassificationMixin
from datahub.ingestion.source.snowflake.constants import (
    GENERIC_PERMISSION_ERROR_KEY,
    SNOWFLAKE_DATABASE,
    SnowflakeEdition,
    SnowflakeObjectDomain,
)
from datahub.ingestion.source.snowflake.snowflake_config import SnowflakeV2Config
from datahub.ingestion.source.snowflake.snowflake_lineage import (
    SnowflakeLineageExtractor,
)
from datahub.ingestion.source.snowflake.snowflake_profiler import SnowflakeProfiler
from datahub.ingestion.source.snowflake.snowflake_report import SnowflakeV2Report
from datahub.ingestion.source.snowflake.snowflake_schema import (
    SnowflakeColumn,
    SnowflakeDatabase,
    SnowflakeDataDictionary,
    SnowflakeFK,
    SnowflakePK,
    SnowflakeQuery,
    SnowflakeSchema,
    SnowflakeTable,
    SnowflakeView,
)
from datahub.ingestion.source.snowflake.snowflake_usage_v2 import (
    SnowflakeUsageExtractor,
)
from datahub.ingestion.source.snowflake.snowflake_utils import (
    SnowflakeCommonMixin,
    SnowflakeConnectionMixin,
    SnowflakePermissionError,
    SnowflakeQueryMixin,
)
from datahub.ingestion.source.sql.sql_common import SqlContainerSubTypes
from datahub.ingestion.source.state.profiling_state_handler import ProfilingHandler
from datahub.ingestion.source.state.redundant_run_skip_handler import (
    RedundantRunSkipHandler,
)
from datahub.ingestion.source.state.sql_common_state import (
    BaseSQLAlchemyCheckpointState,
)
from datahub.ingestion.source.state.stale_entity_removal_handler import (
    StaleEntityRemovalHandler,
)
from datahub.ingestion.source.state.stateful_ingestion_base import (
    StatefulIngestionSourceBase,
)
from datahub.metadata.com.linkedin.pegasus2avro.common import (
    Status,
    SubTypes,
    TimeStamp,
)
from datahub.metadata.com.linkedin.pegasus2avro.dataset import (
    DatasetProperties,
    ViewProperties,
)
from datahub.metadata.com.linkedin.pegasus2avro.schema import (
    ArrayType,
    BooleanType,
    BytesType,
    DateType,
    ForeignKeyConstraint,
    MySqlDDL,
    NullType,
    NumberType,
    RecordType,
    SchemaField,
    SchemaFieldDataType,
    SchemaMetadata,
    StringType,
    TimeType,
)
from datahub.metadata.schema_classes import ChangeTypeClass, DataPlatformInstanceClass
from datahub.utilities.registries.domain_registry import DomainRegistry
from datahub.utilities.time import datetime_to_ts_millis

logger: logging.Logger = logging.getLogger(__name__)

# https://docs.snowflake.com/en/sql-reference/intro-summary-data-types.html
SNOWFLAKE_FIELD_TYPE_MAPPINGS = {
    "DATE": DateType,
    "BIGINT": NumberType,
    "BINARY": BytesType,
    # 'BIT': BIT,
    "BOOLEAN": BooleanType,
    "CHAR": NullType,
    "CHARACTER": NullType,
    "DATETIME": TimeType,
    "DEC": NumberType,
    "DECIMAL": NumberType,
    "DOUBLE": NumberType,
    "FIXED": NumberType,
    "FLOAT": NumberType,
    "INT": NumberType,
    "INTEGER": NumberType,
    "NUMBER": NumberType,
    # 'OBJECT': ?
    "REAL": NumberType,
    "BYTEINT": NumberType,
    "SMALLINT": NumberType,
    "STRING": StringType,
    "TEXT": StringType,
    "TIME": TimeType,
    "TIMESTAMP": TimeType,
    "TIMESTAMP_TZ": TimeType,
    "TIMESTAMP_LTZ": TimeType,
    "TIMESTAMP_NTZ": TimeType,
    "TINYINT": NumberType,
    "VARBINARY": BytesType,
    "VARCHAR": StringType,
    "VARIANT": RecordType,
    "OBJECT": NullType,
    "ARRAY": ArrayType,
    "GEOGRAPHY": NullType,
}


@platform_name("Snowflake", doc_order=1)
@config_class(SnowflakeV2Config)
@support_status(SupportStatus.CERTIFIED)
@capability(SourceCapability.PLATFORM_INSTANCE, "Enabled by default")
@capability(SourceCapability.DOMAINS, "Supported via the `domain` config field")
@capability(SourceCapability.CONTAINERS, "Enabled by default")
@capability(SourceCapability.SCHEMA_METADATA, "Enabled by default")
@capability(
    SourceCapability.DATA_PROFILING,
    "Optionally enabled via configuration `profiling.enabled`",
)
@capability(SourceCapability.DESCRIPTIONS, "Enabled by default")
@capability(
    SourceCapability.LINEAGE_COARSE,
    "Enabled by default, can be disabled via configuration `include_table_lineage` and `include_view_lineage`",
)
@capability(
    SourceCapability.LINEAGE_FINE,
    "Enabled by default, can be disabled via configuration `include_column_lineage`",
)
@capability(
    SourceCapability.USAGE_STATS,
    "Enabled by default, can be disabled via configuration `include_usage_stats",
)
@capability(
    SourceCapability.DELETION_DETECTION,
    "Optionally enabled via `stateful_ingestion.remove_stale_metadata`",
    supported=True,
)
class SnowflakeV2Source(
    ClassificationMixin,
    SnowflakeQueryMixin,
    SnowflakeConnectionMixin,
    SnowflakeCommonMixin,
    StatefulIngestionSourceBase,
    TestableSource,
):
    def __init__(self, ctx: PipelineContext, config: SnowflakeV2Config):
        super().__init__(config, ctx)
        self.config: SnowflakeV2Config = config
        self.report: SnowflakeV2Report = SnowflakeV2Report()
        self.logger = logger
        self.snowsight_base_url: Optional[str] = None
        self.connection: Optional[SnowflakeConnection] = None
        # Create and register the stateful ingestion use-case handlers.
        self.stale_entity_removal_handler = StaleEntityRemovalHandler(
            source=self,
            config=self.config,
            state_type_class=BaseSQLAlchemyCheckpointState,
            pipeline_name=self.ctx.pipeline_name,
            run_id=self.ctx.run_id,
        )

        self.redundant_run_skip_handler = RedundantRunSkipHandler(
            source=self,
            config=self.config,
            pipeline_name=self.ctx.pipeline_name,
            run_id=self.ctx.run_id,
        )

        if self.config.domain:
            self.domain_registry = DomainRegistry(
                cached_domains=[k for k in self.config.domain], graph=self.ctx.graph
            )

        # For database, schema, tables, views, etc
        self.data_dictionary = SnowflakeDataDictionary()

        if config.include_table_lineage:
            # For lineage
            self.lineage_extractor = SnowflakeLineageExtractor(config, self.report)

        if config.include_usage_stats or config.include_operational_stats:
            # For usage stats
            self.usage_extractor = SnowflakeUsageExtractor(config, self.report)

        self.profiling_state_handler: Optional[ProfilingHandler] = None
        if self.config.store_last_profiling_timestamps:
            self.profiling_state_handler = ProfilingHandler(
                source=self,
                config=self.config,
                pipeline_name=self.ctx.pipeline_name,
                run_id=self.ctx.run_id,
            )

        if config.profiling.enabled:
            # For profiling
            self.profiler = SnowflakeProfiler(
                config, self.report, self.profiling_state_handler
            )

        if self.is_classification_enabled():
            self.classifiers = self.get_classifiers()

        # Currently caching using instance variables
        # TODO - rewrite cache for readability or use out of the box solution
        self.db_tables: Dict[str, Optional[Dict[str, List[SnowflakeTable]]]] = {}
        self.db_views: Dict[str, Optional[Dict[str, List[SnowflakeView]]]] = {}

        # For column related queries and constraints, we currently query at schema level
        # TODO: In future, we may consider using queries and caching at database level first
        self.schema_columns: Dict[
            Tuple[str, str], Optional[Dict[str, List[SnowflakeColumn]]]
        ] = {}
        self.schema_pk_constraints: Dict[Tuple[str, str], Dict[str, SnowflakePK]] = {}
        self.schema_fk_constraints: Dict[
            Tuple[str, str], Dict[str, List[SnowflakeFK]]
        ] = {}

    @classmethod
    def create(cls, config_dict: dict, ctx: PipelineContext) -> "Source":
        config = SnowflakeV2Config.parse_obj(config_dict)
        return cls(ctx, config)

    @staticmethod
    def test_connection(config_dict: dict) -> TestConnectionReport:
        test_report = TestConnectionReport()

        try:
            connection_conf = SnowflakeV2Config.parse_obj_allow_extras(config_dict)

            connection: SnowflakeConnection = connection_conf.get_connection()
            assert connection

            test_report.basic_connectivity = CapabilityReport(capable=True)

            test_report.capability_report = SnowflakeV2Source.check_capabilities(
                connection, connection_conf
            )
            connection.close()

        except Exception as e:
            logger.error(f"Failed to test connection due to {e}", exc_info=e)
            if test_report.basic_connectivity is None:
                test_report.basic_connectivity = CapabilityReport(
                    capable=False, failure_reason=f"{e}"
                )
            else:
                test_report.internal_failure = True
                test_report.internal_failure_reason = f"{e}"
        finally:
            return test_report

    @staticmethod
    def check_capabilities(
        conn: SnowflakeConnection, connection_conf: SnowflakeV2Config
    ) -> Dict[Union[SourceCapability, str], CapabilityReport]:
        # Currently only overall capabilities are reported.
        # Resource level variations in capabilities are not considered.

        @dataclass
        class SnowflakePrivilege:
            privilege: str
            object_name: str
            object_type: str

        def query(query):
            logger.info("Query : {}".format(query))
            resp = conn.cursor().execute(query)
            return resp

        _report: Dict[Union[SourceCapability, str], CapabilityReport] = dict()
        privileges: List[SnowflakePrivilege] = []
        capabilities: List[SourceCapability] = [c.capability for c in SnowflakeV2Source.get_capabilities() if c.capability not in (SourceCapability.PLATFORM_INSTANCE, SourceCapability.DOMAINS, SourceCapability.DELETION_DETECTION)]  # type: ignore

        cur = query("select current_role()")
        current_role = [row[0] for row in cur][0]

        cur = query("select current_secondary_roles()")
        secondary_roles_str = json.loads([row[0] for row in cur][0])["roles"]
        secondary_roles = (
            [] if secondary_roles_str == "" else secondary_roles_str.split(",")
        )

        roles = [current_role] + secondary_roles

        # PUBLIC role is automatically granted to every role
        if "PUBLIC" not in roles:
            roles.append("PUBLIC")
        i = 0

        while i < len(roles):
            role = roles[i]
            i = i + 1
            # for some roles, quoting is necessary. for example test-role
            cur = query(f'show grants to role "{role}"')
            for row in cur:
                privilege = SnowflakePrivilege(
                    privilege=row[1], object_type=row[2], object_name=row[3]
                )
                privileges.append(privilege)

                if privilege.object_type in (
                    "DATABASE",
                    "SCHEMA",
                ) and privilege.privilege in ("OWNERSHIP", "USAGE"):
                    _report[SourceCapability.CONTAINERS] = CapabilityReport(
                        capable=True
                    )
                elif privilege.object_type in (
                    "TABLE",
                    "VIEW",
                    "MATERIALIZED_VIEW",
                ):
                    _report[SourceCapability.SCHEMA_METADATA] = CapabilityReport(
                        capable=True
                    )
                    _report[SourceCapability.DESCRIPTIONS] = CapabilityReport(
                        capable=True
                    )

                    # Table level profiling is supported without SELECT access
                    # if privilege.privilege in ("SELECT", "OWNERSHIP"):
                    _report[SourceCapability.DATA_PROFILING] = CapabilityReport(
                        capable=True
                    )

                    if privilege.object_name.startswith("SNOWFLAKE.ACCOUNT_USAGE."):
                        # if access to "snowflake" shared database, access to all account_usage views is automatically granted
                        # Finer access control is not yet supported for shares
                        # https://community.snowflake.com/s/article/Error-Granting-individual-privileges-on-imported-database-is-not-allowed-Use-GRANT-IMPORTED-PRIVILEGES-instead
                        _report[SourceCapability.LINEAGE_COARSE] = CapabilityReport(
                            capable=True
                        )

                        _report[SourceCapability.LINEAGE_FINE] = CapabilityReport(
                            capable=True
                        )

                        _report[SourceCapability.USAGE_STATS] = CapabilityReport(
                            capable=True
                        )
                # If all capabilities supported, no need to continue
                if set(capabilities) == set(_report.keys()):
                    break

                # Due to this, entire role hierarchy is considered
                if (
                    privilege.object_type == "ROLE"
                    and privilege.privilege == "USAGE"
                    and privilege.object_name not in roles
                ):
                    roles.append(privilege.object_name)

        cur = query("select current_warehouse()")
        current_warehouse = [row[0] for row in cur][0]

        default_failure_messages = {
            SourceCapability.SCHEMA_METADATA: "Either no tables exist or current role does not have permissions to access them",
            SourceCapability.DESCRIPTIONS: "Either no tables exist or current role does not have permissions to access them",
            SourceCapability.DATA_PROFILING: "Either no tables exist or current role does not have permissions to access them",
            SourceCapability.CONTAINERS: "Current role does not have permissions to use any database",
            SourceCapability.LINEAGE_COARSE: "Current role does not have permissions to snowflake account usage views",
            SourceCapability.LINEAGE_FINE: "Current role does not have permissions to snowflake account usage views",
            SourceCapability.USAGE_STATS: "Current role does not have permissions to snowflake account usage views",
        }

        for c in capabilities:  # type:ignore
            # These capabilities do not work without active warehouse
            if current_warehouse is None and c in (
                SourceCapability.SCHEMA_METADATA,
                SourceCapability.DESCRIPTIONS,
                SourceCapability.DATA_PROFILING,
                SourceCapability.LINEAGE_COARSE,
                SourceCapability.LINEAGE_FINE,
                SourceCapability.USAGE_STATS,
            ):
                failure_message = (
                    f"Current role {current_role} does not have permissions to use warehouse {connection_conf.warehouse}. Please check the grants associated with this role."
                    if connection_conf.warehouse is not None
                    else "No default warehouse set for user. Either set default warehouse for user or configure warehouse in recipe"
                )
                _report[c] = CapabilityReport(
                    capable=False,
                    failure_reason=failure_message,
                )

            if c in _report.keys():
                continue

            # If some capabilities are missing, then mark them as not capable
            _report[c] = CapabilityReport(
                capable=False,
                failure_reason=default_failure_messages[c],
            )

        return _report

    def get_workunits(self) -> Iterable[WorkUnit]:

        self.connection = self.create_connection()
        if self.connection is None:
            return

        self.add_config_to_report()
        self.inspect_session_metadata()

        if self.config.include_external_url:
            self.snowsight_base_url = self.get_snowsight_base_url()

        if self.report.default_warehouse is None:
            self.report_warehouse_failure()
            return

        self.data_dictionary.set_connection(self.connection)
        databases = self.get_databases()

        if databases is None or len(databases) == 0:
            return

        for snowflake_db in databases:
            try:
                yield from self._process_database(snowflake_db)
            except SnowflakePermissionError as e:
                # FIXME - This may break satetful ingestion if new tables than previous run are emitted above
                # and stateful ingestion is enabled
                self.report_error(GENERIC_PERMISSION_ERROR_KEY, str(e))
                return

        self.connection.close()

        # Emit Stale entity workunits
        yield from self.stale_entity_removal_handler.gen_removed_entity_workunits()

        # TODO: The checkpoint state for stale entity detection can be comitted here.

        if self.config.profiling.enabled and len(databases) != 0:
            yield from self.profiler.get_workunits(databases)

        discovered_tables: List[str] = [
            self.get_dataset_identifier(table.name, schema.name, db.name)
            for db in databases
            for schema in db.schemas
            for table in schema.tables
        ]
        discovered_views: List[str] = [
            self.get_dataset_identifier(table.name, schema.name, db.name)
            for db in databases
            for schema in db.schemas
            for table in schema.views
        ]

        if len(discovered_tables) == 0 and len(discovered_views) == 0:
            self.report_error(
                GENERIC_PERMISSION_ERROR_KEY,
                "No tables/views found. Please check permissions.",
            )
            return

        discovered_datasets = discovered_tables + discovered_views

        if self.config.include_table_lineage:
            yield from self.lineage_extractor.get_workunits(
                discovered_tables, discovered_views
            )

        if self.config.include_usage_stats or self.config.include_operational_stats:
            if (
                self.config.store_last_usage_extraction_timestamp
                and self.redundant_run_skip_handler.should_skip_this_run(
                    cur_start_time_millis=datetime_to_ts_millis(self.config.start_time)
                )
            ):
                # Skip this run
                self.report.report_warning(
                    "usage-extraction",
                    f"Skip this run as there was a run later than the current start time: {self.config.start_time}",
                )
                return

            if self.config.store_last_usage_extraction_timestamp:
                # Update the checkpoint state for this run.
                self.redundant_run_skip_handler.update_state(
                    start_time_millis=datetime_to_ts_millis(self.config.start_time),
                    end_time_millis=datetime_to_ts_millis(self.config.end_time),
                )

            yield from self.usage_extractor.get_workunits(discovered_datasets)

    def report_warehouse_failure(self):
        if self.config.warehouse is not None:
            self.report_error(
                GENERIC_PERMISSION_ERROR_KEY,
                f"Current role does not have permissions to use warehouse {self.config.warehouse}. Please update permissions.",
            )
        else:
            self.report_error(
                "no-active-warehouse",
                "No default warehouse set for user. Either set default warehouse for user or configure warehouse in recipe.",
            )

    def get_databases(self) -> Optional[List[SnowflakeDatabase]]:
        try:
            # `show databases` is required only to get one  of the databases
            # whose information_schema can be queried to start with.
            databases = self.data_dictionary.show_databases()
        except Exception as e:
            logger.debug(f"Failed to list databases due to error {e}", exc_info=e)
            self.report_error(
                "list-databases",
                f"Failed to list databases due to error {e}",
            )
            return None
        else:
            ischema_databases: List[
                SnowflakeDatabase
            ] = self.get_databases_from_ischema(databases)

            if len(ischema_databases) == 0:
                self.report_error(
                    GENERIC_PERMISSION_ERROR_KEY,
                    "No databases found. Please check permissions.",
                )
            return ischema_databases

    def get_databases_from_ischema(self, databases):
        ischema_databases: List[SnowflakeDatabase] = []
        for database in databases:
            try:
                ischema_databases = self.data_dictionary.get_databases(database.name)
                break
            except Exception:
                # query fails if "USAGE" access is not granted for database
                # This is okay, because `show databases` query lists all databases irrespective of permission,
                # if role has `MANAGE GRANTS` privilege. (not advisable)
                logger.debug(
                    f"Failed to list databases {database.name} information_schema"
                )
                # SNOWFLAKE database always shows up even if permissions are missing
                if database == SNOWFLAKE_DATABASE:
                    continue
                logger.info(
                    f"The role {self.report.role} has `MANAGE GRANTS` privilege. This is not advisable and also not required."
                )

        return ischema_databases

    def _process_database(
        self, snowflake_db: SnowflakeDatabase
    ) -> Iterable[MetadataWorkUnit]:

        self.report.report_entity_scanned(snowflake_db.name, "database")
        if not self.config.database_pattern.allowed(snowflake_db.name):
            self.report.report_dropped(f"{snowflake_db.name}.*")
            return

        db_name = snowflake_db.name

        try:
            self.query(SnowflakeQuery.use_database(db_name))
        except Exception as e:
            if isinstance(e, SnowflakePermissionError):
                # This may happen if REFERENCE_USAGE permissions are set
                # We can not run show queries on database in such case.
                # This need not be a failure case.
                self.report_warning(
                    "Insufficient privileges to operate on database, skipping. Please grant USAGE permissions on database to extract its metadata.",
                    db_name,
                )
            else:
                logger.debug(
                    f"Failed to use database {db_name} due to error {e}",
                    exc_info=e,
                )
                self.report_warning(
                    "Failed to get schemas for database",
                    db_name,
                )
            return

        if self.config.include_technical_schema:
            yield from self.gen_database_containers(snowflake_db)

        self.fetch_schemas_for_database(snowflake_db, db_name)

        for snowflake_schema in snowflake_db.schemas:
            yield from self._process_schema(snowflake_schema, db_name)

    def fetch_schemas_for_database(self, snowflake_db, db_name):
        try:
            snowflake_db.schemas = self.data_dictionary.get_schemas_for_database(
                db_name
            )
        except Exception as e:
            if isinstance(e, SnowflakePermissionError):
                error_msg = f"Failed to get schemas for database {db_name}. Please check permissions."
                # Ideal implementation would use PEP 678 – Enriching Exceptions with Notes
                raise SnowflakePermissionError(error_msg) from e.__cause__
            else:
                logger.debug(
                    f"Failed to get schemas for database {db_name} due to error {e}",
                    exc_info=e,
                )
                self.report_warning(
                    "Failed to get schemas for database",
                    db_name,
                )

        if not snowflake_db.schemas:
            self.report_warning(
                "No schemas found in database. If schemas exist, please grant USAGE permissions on them.",
                db_name,
            )

    def _process_schema(
        self, snowflake_schema: SnowflakeSchema, db_name: str
    ) -> Iterable[MetadataWorkUnit]:

        self.report.report_entity_scanned(snowflake_schema.name, "schema")
        if not is_schema_allowed(
            self.config.schema_pattern,
            snowflake_schema.name,
            db_name,
            self.config.match_fully_qualified_names,
        ):
            self.report.report_dropped(f"{db_name}.{snowflake_schema.name}.*")
            return

        schema_name = snowflake_schema.name
        if self.config.include_technical_schema:
            yield from self.gen_schema_containers(snowflake_schema, db_name)

        if self.config.include_tables:
            self.fetch_tables_for_schema(snowflake_schema, db_name, schema_name)

            if self.config.include_technical_schema:
                for table in snowflake_schema.tables:
                    yield from self._process_table(table, schema_name, db_name)

        if self.config.include_views:
            self.fetch_views_for_schema(snowflake_schema, db_name, schema_name)

            if self.config.include_technical_schema:
                for view in snowflake_schema.views:
                    yield from self._process_view(view, schema_name, db_name)

        if not snowflake_schema.views and not snowflake_schema.tables:
            self.report_warning(
                "No tables/views found in schema. If tables exist, please grant REFERENCES or SELECT permissions on them.",
                f"{db_name}.{schema_name}",
            )

    def fetch_views_for_schema(self, snowflake_schema, db_name, schema_name):
        try:
            snowflake_schema.views = self.get_views_for_schema(schema_name, db_name)

        except Exception as e:
            if isinstance(e, SnowflakePermissionError):
                # Ideal implementation would use PEP 678 – Enriching Exceptions with Notes
                error_msg = f"Failed to get views for schema {db_name}.{schema_name}. Please check permissions."

                raise SnowflakePermissionError(error_msg) from e.__cause__
            else:
                logger.debug(
                    f"Failed to get views for schema {db_name}.{schema_name} due to error {e}",
                    exc_info=e,
                )
                self.report_warning(
                    "Failed to get views for schema",
                    f"{db_name}.{schema_name}",
                )

    def fetch_tables_for_schema(self, snowflake_schema, db_name, schema_name):
        try:
            snowflake_schema.tables = self.get_tables_for_schema(schema_name, db_name)
        except Exception as e:
            if isinstance(e, SnowflakePermissionError):
                # Ideal implementation would use PEP 678 – Enriching Exceptions with Notes
                error_msg = f"Failed to get tables for schema {db_name}.{schema_name}. Please check permissions."
                raise SnowflakePermissionError(error_msg) from e.__cause__
            else:
                logger.debug(
                    f"Failed to get tables for schema {db_name}.{schema_name} due to error {e}",
                    exc_info=e,
                )
                self.report_warning(
                    "Failed to get tables for schema",
                    f"{db_name}.{schema_name}",
                )

    def _process_table(
        self,
        table: SnowflakeTable,
        schema_name: str,
        db_name: str,
    ) -> Iterable[MetadataWorkUnit]:
        table_identifier = self.get_dataset_identifier(table.name, schema_name, db_name)

        self.report.report_entity_scanned(table_identifier)

        if not self.config.table_pattern.allowed(table_identifier):
            self.report.report_dropped(table_identifier)
            return

        self.fetch_columns_for_table(table, schema_name, db_name, table_identifier)

        self.fetch_pk_for_table(table, schema_name, db_name, table_identifier)

        self.fetch_foreign_keys_for_table(table, schema_name, db_name, table_identifier)

        dataset_name = self.get_dataset_identifier(table.name, schema_name, db_name)

        self.fetch_sample_data_for_classification(
            table, schema_name, db_name, dataset_name
        )

        yield from self.gen_dataset_workunits(table, schema_name, db_name)

    def fetch_sample_data_for_classification(
        self, table, schema_name, db_name, dataset_name
    ):
        if table.columns and self.is_classification_enabled_for_table(dataset_name):
            try:
                table.sample_data = self.get_sample_values_for_table(
                    table.name, schema_name, db_name
                )
            except Exception as e:
                logger.debug(
                    f"Failed to get sample values for dataset {dataset_name} due to error {e}",
                    exc_info=e,
                )
                if isinstance(e, SnowflakePermissionError):
                    self.report_warning(
                        "Failed to get sample values for dataset. Please grant SELECT permissions on dataset.",
                        dataset_name,
                    )
                else:
                    self.report_warning(
                        "Failed to get sample values for dataset",
                        dataset_name,
                    )

    def fetch_foreign_keys_for_table(
        self, table, schema_name, db_name, table_identifier
    ):
        try:
            table.foreign_keys = self.get_fk_constraints_for_table(
                table.name, schema_name, db_name
            )
        except Exception as e:
            logger.debug(
                f"Failed to get foreign key for table {table_identifier} due to error {e}",
                exc_info=e,
            )
            self.report_warning("Failed to get foreign key for table", table_identifier)

    def fetch_pk_for_table(self, table, schema_name, db_name, table_identifier):
        try:
            table.pk = self.get_pk_constraints_for_table(
                table.name, schema_name, db_name
            )
        except Exception as e:
            logger.debug(
                f"Failed to get primary key for table {table_identifier} due to error {e}",
                exc_info=e,
            )
            self.report_warning("Failed to get primary key for table", table_identifier)

    def fetch_columns_for_table(self, table, schema_name, db_name, table_identifier):
        try:
            table.columns = self.get_columns_for_table(table.name, schema_name, db_name)
        except Exception as e:
            logger.debug(
                f"Failed to get columns for table {table_identifier} due to error {e}",
                exc_info=e,
            )
            self.report_warning("Failed to get columns for table", table_identifier)

    def _process_view(
        self,
        view: SnowflakeView,
        schema_name: str,
        db_name: str,
    ) -> Iterable[MetadataWorkUnit]:
        view_name = self.get_dataset_identifier(view.name, schema_name, db_name)

        self.report.report_entity_scanned(view_name, "view")

        if not self.config.view_pattern.allowed(view_name):
            self.report.report_dropped(view_name)
            return

        try:
            view.columns = self.get_columns_for_table(view.name, schema_name, db_name)
        except Exception as e:
            logger.debug(
                f"Failed to get columns for view {view_name} due to error {e}",
                exc_info=e,
            )
            self.report_warning("Failed to get columns for view", view_name)

        yield from self.gen_dataset_workunits(view, schema_name, db_name)

    def gen_dataset_workunits(
        self,
        table: Union[SnowflakeTable, SnowflakeView],
        schema_name: str,
        db_name: str,
    ) -> Iterable[MetadataWorkUnit]:
        dataset_name = self.get_dataset_identifier(table.name, schema_name, db_name)
        dataset_urn = make_dataset_urn_with_platform_instance(
            self.platform,
            dataset_name,
            self.config.platform_instance,
            self.config.env,
        )

        # Add the entity to the state.
        type = (
            SnowflakeObjectDomain.TABLE
            if isinstance(table, SnowflakeTable)
            else SnowflakeObjectDomain.VIEW
        )
        self.stale_entity_removal_handler.add_entity_to_state(
            type=type, urn=dataset_urn
        )

        status = Status(removed=False)
        yield self.wrap_aspect_as_workunit("dataset", dataset_urn, "status", status)

        schema_metadata = self.get_schema_metadata(table, dataset_name, dataset_urn)
        yield self.wrap_aspect_as_workunit(
            "dataset", dataset_urn, "schemaMetadata", schema_metadata
        )

        dataset_properties = self.get_dataset_properties(
            table, schema_name, db_name, dataset_name
        )
        yield self.wrap_aspect_as_workunit(
            "dataset", dataset_urn, "datasetProperties", dataset_properties
        )

        yield from self.add_table_to_schema_container(
            dataset_urn,
            self.snowflake_identifier(db_name),
            self.snowflake_identifier(schema_name),
        )
        dpi_aspect = self.get_dataplatform_instance_aspect(dataset_urn=dataset_urn)
        if dpi_aspect:
            yield dpi_aspect

        subTypes = SubTypes(
            typeNames=["view"] if isinstance(table, SnowflakeView) else ["table"]
        )
        yield self.wrap_aspect_as_workunit("dataset", dataset_urn, "subTypes", subTypes)

        yield from self._get_domain_wu(
            dataset_name=dataset_name,
            entity_urn=dataset_urn,
            entity_type="dataset",
        )

        if (
            isinstance(table, SnowflakeView)
            and cast(SnowflakeView, table).view_definition is not None
        ):
            view = cast(SnowflakeView, table)
            view_properties_aspect = ViewProperties(
                materialized=False,
                viewLanguage="SQL",
                viewLogic=view.view_definition,
            )
            yield self.wrap_aspect_as_workunit(
                "dataset",
                dataset_urn,
                "viewProperties",
                view_properties_aspect,
            )

    def get_dataset_properties(self, table, schema_name, db_name, dataset_name):
        return DatasetProperties(
            name=table.name,
            created=TimeStamp(time=int(table.created.timestamp() * 1000))
            if table.created is not None
            else None,
            lastModified=TimeStamp(time=int(table.last_altered.timestamp() * 1000))
            if table.last_altered is not None
            else TimeStamp(time=int(table.created.timestamp() * 1000))
            if table.created is not None
            else None,
            description=table.comment,
            qualifiedName=dataset_name,
            customProperties={},
            externalUrl=self.get_external_url_for_table(
                table.name,
                schema_name,
                db_name,
                SnowflakeObjectDomain.TABLE
                if isinstance(table, SnowflakeTable)
                else SnowflakeObjectDomain.VIEW,
            )
            if self.config.include_external_url
            else None,
        )

    def get_schema_metadata(
        self,
        table: Union[SnowflakeTable, SnowflakeView],
        dataset_name: str,
        dataset_urn: str,
    ) -> SchemaMetadata:
        foreign_keys: Optional[List[ForeignKeyConstraint]] = None
        if isinstance(table, SnowflakeTable) and len(table.foreign_keys) > 0:
            foreign_keys = self.build_foreign_keys(table, dataset_urn, foreign_keys)

        schema_metadata = SchemaMetadata(
            schemaName=dataset_name,
            platform=make_data_platform_urn(self.platform),
            version=0,
            hash="",
            platformSchema=MySqlDDL(tableSchema=""),
            fields=[
                SchemaField(
                    fieldPath=self.snowflake_identifier(col.name),
                    type=SchemaFieldDataType(
                        SNOWFLAKE_FIELD_TYPE_MAPPINGS.get(col.data_type, NullType)()
                    ),
                    # NOTE: nativeDataType will not be in sync with older connector
                    nativeDataType=col.get_precise_native_type(),
                    description=col.comment,
                    nullable=col.is_nullable,
                    isPartOfKey=col.name in table.pk.column_names
                    if isinstance(table, SnowflakeTable) and table.pk is not None
                    else None,
                )
                for col in table.columns
            ],
            foreignKeys=foreign_keys,
        )

        # TODO: classification is only run for snowflake tables.
        # Should we run classification for snowflake views as well?
        self.classify_snowflake_table(table, dataset_name, schema_metadata)

        return schema_metadata

    def build_foreign_keys(self, table, dataset_urn, foreign_keys):
        foreign_keys = []
        for fk in table.foreign_keys:
            foreign_dataset = make_dataset_urn(
                self.platform,
                self.get_dataset_identifier(
                    fk.referred_table, fk.referred_schema, fk.referred_database
                ),
                self.config.env,
            )
            foreign_keys.append(
                ForeignKeyConstraint(
                    name=fk.name,
                    foreignDataset=foreign_dataset,
                    foreignFields=[
                        make_schema_field_urn(
                            foreign_dataset,
                            self.snowflake_identifier(col),
                        )
                        for col in fk.referred_column_names
                    ],
                    sourceFields=[
                        make_schema_field_urn(
                            dataset_urn,
                            self.snowflake_identifier(col),
                        )
                        for col in fk.column_names
                    ],
                )
            )
        return foreign_keys

    def classify_snowflake_table(self, table, dataset_name, schema_metadata):
        if isinstance(
            table, SnowflakeTable
        ) and self.is_classification_enabled_for_table(dataset_name):
            if table.sample_data is not None:
                table.sample_data.columns = [
                    self.snowflake_identifier(col) for col in table.sample_data.columns
                ]
            logger.debug(f"Classifying Table {dataset_name}")

            try:
                self.classify_schema_fields(
                    dataset_name,
                    schema_metadata,
                    table.sample_data.to_dict(orient="list")
                    if table.sample_data is not None
                    else {},
                )
            except Exception as e:
                logger.debug(
                    f"Failed to classify table columns for {dataset_name} due to error -> {e}",
                    exc_info=e,
                )
                self.report_warning(
                    "Failed to classify table columns",
                    dataset_name,
                )

    def get_report(self) -> SourceReport:
        return self.report

    def get_dataplatform_instance_aspect(
        self, dataset_urn: str
    ) -> Optional[MetadataWorkUnit]:
        # If we are a platform instance based source, emit the instance aspect
        if self.config.platform_instance:
            mcp = MetadataChangeProposalWrapper(
                entityType="dataset",
                changeType=ChangeTypeClass.UPSERT,
                entityUrn=dataset_urn,
                aspectName="dataPlatformInstance",
                aspect=DataPlatformInstanceClass(
                    platform=make_data_platform_urn(self.platform),
                    instance=make_dataplatform_instance_urn(
                        self.platform, self.config.platform_instance
                    ),
                ),
            )
            wu = MetadataWorkUnit(id=f"{dataset_urn}-dataPlatformInstance", mcp=mcp)
            self.report.report_workunit(wu)
            return wu
        else:
            return None

    def _get_domain_wu(
        self,
        dataset_name: str,
        entity_urn: str,
        entity_type: str,
    ) -> Iterable[MetadataWorkUnit]:
        domain_urn = self._gen_domain_urn(dataset_name)
        if domain_urn:
            wus = add_domain_to_entity_wu(
                entity_type=entity_type,
                entity_urn=entity_urn,
                domain_urn=domain_urn,
            )
            for wu in wus:
                self.report.report_workunit(wu)
                yield wu

    def add_table_to_schema_container(
        self, dataset_urn: str, db_name: str, schema: str
    ) -> Iterable[MetadataWorkUnit]:
        schema_container_key = self.gen_schema_key(db_name, schema)
        container_workunits = add_dataset_to_container(
            container_key=schema_container_key,
            dataset_urn=dataset_urn,
        )

        self.stale_entity_removal_handler.add_entity_to_state(
            type="container",
            urn=make_container_urn(
                guid=schema_container_key.guid(),
            ),
        )

        for wu in container_workunits:
            self.report.report_workunit(wu)
            yield wu

    def gen_schema_key(self, db_name: str, schema: str) -> PlatformKey:
        return SchemaKey(
            database=db_name,
            schema=schema,
            platform=self.platform,
            instance=self.config.platform_instance,
            backcompat_instance_for_guid=self.config.env,
        )

    def gen_database_key(self, database: str) -> PlatformKey:
        return DatabaseKey(
            database=database,
            platform=self.platform,
            instance=self.config.platform_instance,
            backcompat_instance_for_guid=self.config.env,
        )

    def _gen_domain_urn(self, dataset_name: str) -> Optional[str]:
        domain_urn: Optional[str] = None

        for domain, pattern in self.config.domain.items():
            if pattern.allowed(dataset_name):
                domain_urn = make_domain_urn(
                    self.domain_registry.get_domain_urn(domain)
                )

        return domain_urn

    def gen_database_containers(
        self, database: SnowflakeDatabase
    ) -> Iterable[MetadataWorkUnit]:
        domain_urn = self._gen_domain_urn(database.name)

        database_container_key = self.gen_database_key(
            self.snowflake_identifier(database.name)
        )
        container_workunits = gen_containers(
            container_key=database_container_key,
            name=database.name,
            description=database.comment,
            sub_types=[SqlContainerSubTypes.DATABASE],
            domain_urn=domain_urn,
            external_url=self.get_external_url_for_database(database.name)
            if self.config.include_external_url
            else None,
            created=int(database.created.timestamp() * 1000)
            if database.created is not None
            else None,
            last_modified=int(database.last_altered.timestamp() * 1000)
            if database.last_altered is not None
            else int(database.created.timestamp() * 1000)
            if database.created is not None
            else None,
        )

        self.stale_entity_removal_handler.add_entity_to_state(
            type="container",
            urn=make_container_urn(
                guid=database_container_key.guid(),
            ),
        )

        for wu in container_workunits:
            self.report.report_workunit(wu)
            yield wu

    def gen_schema_containers(
        self, schema: SnowflakeSchema, db_name: str
    ) -> Iterable[MetadataWorkUnit]:
        domain_urn = self._gen_domain_urn(f"{db_name}.{schema.name}")

        schema_container_key = self.gen_schema_key(
            self.snowflake_identifier(db_name),
            self.snowflake_identifier(schema.name),
        )

        database_container_key: Optional[PlatformKey] = None
        if db_name is not None:
            database_container_key = self.gen_database_key(
                database=self.snowflake_identifier(db_name)
            )

        container_workunits = gen_containers(
            container_key=schema_container_key,
            name=schema.name,
            description=schema.comment,
            sub_types=[SqlContainerSubTypes.SCHEMA],
            parent_container_key=database_container_key,
            domain_urn=domain_urn,
            external_url=self.get_external_url_for_schema(schema.name, db_name)
            if self.config.include_external_url
            else None,
            created=int(schema.created.timestamp() * 1000)
            if schema.created is not None
            else None,
            last_modified=int(schema.last_altered.timestamp() * 1000)
            if schema.last_altered is not None
            else int(schema.created.timestamp() * 1000)
            if schema.created is not None
            else None,
        )

        for wu in container_workunits:
            self.report.report_workunit(wu)
            yield wu

    def get_tables_for_schema(
        self, schema_name: str, db_name: str
    ) -> List[SnowflakeTable]:
        if db_name not in self.db_tables.keys():
            tables = self.data_dictionary.get_tables_for_database(db_name)
            self.db_tables[db_name] = tables
        else:
            tables = self.db_tables[db_name]

        # get all tables for database failed,
        # falling back to get tables for schema
        if tables is None:
            self.report.num_get_tables_for_schema_queries += 1
            return self.data_dictionary.get_tables_for_schema(schema_name, db_name)

        # Some schema may not have any table
        return tables.get(schema_name, [])

    def get_views_for_schema(
        self, schema_name: str, db_name: str
    ) -> List[SnowflakeView]:
        if db_name not in self.db_views.keys():
            views = self.data_dictionary.get_views_for_database(db_name)
            self.db_views[db_name] = views
        else:
            views = self.db_views[db_name]

        # get all views for database failed,
        # falling back to get views for schema
        if views is None:
            self.report.num_get_views_for_schema_queries += 1
            return self.data_dictionary.get_views_for_schema(schema_name, db_name)

        # Some schema may not have any table
        return views.get(schema_name, [])

    def get_columns_for_table(
        self, table_name: str, schema_name: str, db_name: str
    ) -> List[SnowflakeColumn]:
        if (db_name, schema_name) not in self.schema_columns.keys():
            columns = self.data_dictionary.get_columns_for_schema(schema_name, db_name)
            self.schema_columns[(db_name, schema_name)] = columns
        else:
            columns = self.schema_columns[(db_name, schema_name)]

        # get all columns for schema failed,
        # falling back to get columns for table
        if columns is None:
            self.report.num_get_columns_for_table_queries += 1
            return self.data_dictionary.get_columns_for_table(
                table_name, schema_name, db_name
            )

        # Access to table but none of its columns - is this possible ?
        return columns.get(table_name, [])

    def get_pk_constraints_for_table(
        self, table_name: str, schema_name: str, db_name: str
    ) -> Optional[SnowflakePK]:
        if (db_name, schema_name) not in self.schema_pk_constraints.keys():
            constraints = self.data_dictionary.get_pk_constraints_for_schema(
                schema_name, db_name
            )
            self.schema_pk_constraints[(db_name, schema_name)] = constraints
        else:
            constraints = self.schema_pk_constraints[(db_name, schema_name)]

        # Access to table but none of its constraints - is this possible ?
        return constraints.get(table_name)

    def get_fk_constraints_for_table(
        self, table_name: str, schema_name: str, db_name: str
    ) -> List[SnowflakeFK]:
        if (db_name, schema_name) not in self.schema_fk_constraints.keys():
            constraints = self.data_dictionary.get_fk_constraints_for_schema(
                schema_name, db_name
            )
            self.schema_fk_constraints[(db_name, schema_name)] = constraints
        else:
            constraints = self.schema_fk_constraints[(db_name, schema_name)]

        # Access to table but none of its constraints - is this possible ?
        return constraints.get(table_name, [])

    def add_config_to_report(self):
        self.report.cleaned_account_id = self.config.get_account()
        self.report.ignore_start_time_lineage = self.config.ignore_start_time_lineage
        self.report.upstream_lineage_in_report = self.config.upstream_lineage_in_report
        if not self.report.ignore_start_time_lineage:
            self.report.lineage_start_time = self.config.start_time
        self.report.lineage_end_time = self.config.end_time
        self.report.check_role_grants = self.config.check_role_grants
        self.report.include_technical_schema = self.config.include_technical_schema
        self.report.include_usage_stats = self.config.include_usage_stats
        self.report.include_operational_stats = self.config.include_operational_stats
        self.report.include_column_lineage = self.config.include_column_lineage
        if self.report.include_usage_stats or self.config.include_operational_stats:
            self.report.window_start_time = self.config.start_time
            self.report.window_end_time = self.config.end_time

    def inspect_session_metadata(self) -> None:
        try:
            logger.info("Checking current version")
            for db_row in self.query(SnowflakeQuery.current_version()):
                self.report.saas_version = db_row["CURRENT_VERSION()"]
        except Exception as e:
            self.report_error("version", f"Error: {e}")
        try:
            logger.info("Checking current role")
            for db_row in self.query(SnowflakeQuery.current_role()):
                self.report.role = db_row["CURRENT_ROLE()"]
        except Exception as e:
            self.report_error("version", f"Error: {e}")
        try:
            logger.info("Checking current warehouse")
            for db_row in self.query(SnowflakeQuery.current_warehouse()):
                self.report.default_warehouse = db_row["CURRENT_WAREHOUSE()"]
        except Exception as e:
            self.report_error("current_warehouse", f"Error: {e}")

        try:
            logger.info("Checking current edition")
            if self.is_standard_edition():
                self.report.edition = SnowflakeEdition.STANDARD
            else:
                self.report.edition = SnowflakeEdition.ENTERPRISE
        except Exception:
            self.report.edition = None

    # Stateful Ingestion Overrides.
    def get_platform_instance_id(self) -> str:
        return self.config.get_account()

    # Ideally we do not want null values in sample data for a column.
    # However that would require separate query per column and
    # that would be expensive, hence not done.
    def get_sample_values_for_table(self, table_name, schema_name, db_name):
        # Create a cursor object.
        cur = self.get_connection().cursor()
        NUM_SAMPLED_ROWS = 1000
        # Execute a statement that will generate a result set.
        sql = f'select * from "{db_name}"."{schema_name}"."{table_name}" sample ({NUM_SAMPLED_ROWS} rows);'

        cur.execute(sql)
        # Fetch the result set from the cursor and deliver it as the Pandas DataFrame.

        dat = cur.fetchall()
        df = pd.DataFrame(dat, columns=[col.name for col in cur.description])

        return df

    # domain is either "view" or "table"
    def get_external_url_for_table(
        self, table_name: str, schema_name: str, db_name: str, domain: str
    ) -> Optional[str]:
        if self.snowsight_base_url is not None:
            return f"{self.snowsight_base_url}#/data/databases/{db_name}/schemas/{schema_name}/{domain}/{table_name}/"
        return None

    def get_external_url_for_schema(
        self, schema_name: str, db_name: str
    ) -> Optional[str]:
        if self.snowsight_base_url is not None:
            return f"{self.snowsight_base_url}#/data/databases/{db_name}/schemas/{schema_name}/"
        return None

    def get_external_url_for_database(self, db_name: str) -> Optional[str]:
        if self.snowsight_base_url is not None:
            return f"{self.snowsight_base_url}#/data/databases/{db_name}/"
        return None

    def get_snowsight_base_url(self) -> Optional[str]:
        try:
            # See https://docs.snowflake.com/en/user-guide/admin-account-identifier.html#finding-the-region-and-locator-for-an-account
            for db_row in self.query(SnowflakeQuery.current_account()):
                account_locator = db_row["CURRENT_ACCOUNT()"]

            for db_row in self.query(SnowflakeQuery.current_region()):
                region = db_row["CURRENT_REGION()"]

            self.report.account_locator = account_locator
            self.report.region = region

            # Returned region may be in the form <region_group>.<region>, see https://docs.snowflake.com/en/sql-reference/functions/current_region.html
            region = region.split(".")[-1].lower()
            account_locator = account_locator.lower()

            cloud, cloud_region_id = self.get_cloud_region_from_snowflake_region_id(
                region
            )

            # For privatelink, account identifier ends with .privatelink
            # See https://docs.snowflake.com/en/user-guide/organizations-connect.html#private-connectivity-urls
            return self.create_snowsight_base_url(
                account_locator,
                cloud_region_id,
                cloud,
                self.config.account_id.endswith(".privatelink"),  # type:ignore
            )

        except Exception as e:
            self.warn(
                self.logger,
                "snowsight url",
                f"unable to get snowsight base url due to an error -> {e}",
            )
            return None

    def is_standard_edition(self):
        try:
            self.query(SnowflakeQuery.show_tags())
            return False
        except Exception as e:
            if "Unsupported feature 'TAG'" in str(e):
                return True
            raise

    def close(self) -> None:
        super().close()
        if hasattr(self, "lineage_extractor"):
            self.lineage_extractor.close()
        if hasattr(self, "usage_extractor"):
            self.usage_extractor.close()
