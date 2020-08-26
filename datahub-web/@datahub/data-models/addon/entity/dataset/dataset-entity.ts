import { BaseEntity, IBaseEntityStatics, statics } from '@datahub/data-models/entity/base-entity';
import {
  saveDatasetCompliance,
  readDatasetExportPolicy,
  readDatasetRetention,
  saveDatasetExportPolicy,
  saveDatasetRetention,
  saveDatasetComplianceSuggestionFeedbackByUrn
} from '@datahub/data-models/api/dataset/compliance';
import DatasetComplianceInfo from '@datahub/data-models/entity/dataset/modules/compliance-info';
import { readDatasetSchema } from '@datahub/data-models/api/dataset/schema';
import DatasetSchema from '@datahub/data-models/entity/dataset/modules/schema';
import { set, setProperties, computed } from '@ember/object';
import DatasetComplianceAnnotation from '@datahub/data-models/entity/dataset/modules/compliance-annotation';
import { IEntityRenderProps } from '@datahub/data-models/types/entity/rendering/entity-render-props';
import { DatasetExportPolicy } from '@datahub/data-models/entity/dataset/modules/export-policy';
import { DatasetPurgePolicy } from '@datahub/data-models/entity/dataset/modules/purge-policy';
import { DatasetPlatform } from '@datahub/metadata-types/constants/entity/dataset/platform';
import { decodeUrn } from '@datahub/utils/validators/urn';
import { IDataPlatform } from '@datahub/metadata-types/types/entity/dataset/platform';
import { DatasetLineage } from '@datahub/data-models/entity/dataset/modules/lineage';
import { readUpstreamDatasets } from '@datahub/data-models/api/dataset/lineage';
import { DatasetLineageList } from '@datahub/metadata-types/types/entity/dataset/lineage';
import { getRenderProps } from '@datahub/data-models/entity/dataset/render-props';
import { NotImplementedError } from '@datahub/data-models/constants/entity/shared';
import { IDatasetSnapshot } from '@datahub/metadata-types/types/metadata/dataset-snapshot';
import { every } from 'lodash';
import DatasetComplianceSuggestion from '@datahub/data-models/entity/dataset/modules/compliance-suggestion';
import { SuggestionIntent } from '@datahub/data-models/constants/entity/dataset/compliance-suggestions';
import { FabricType } from '@datahub/metadata-types/constants/common/fabric-type';
import { getDatasetUrnParts } from '@datahub/data-models/entity/dataset/utils/urn';
import { readDataPlatforms } from '@datahub/data-models/api/dataset/platforms';
import { getDefaultIfNotFoundError } from '@datahub/utils/api/error';
import { Snapshot } from '@datahub/metadata-types/types/metadata/snapshot';
import { oneWay, alias, reads } from '@ember/object/computed';
import { IDatasetEntity } from '@datahub/metadata-types/types/entity/dataset/dataset-entity';
// import { toLegacy } from '@datahub/data-models/entity/dataset/utils/legacy';
import { readDatasetOwnersByUrn } from '@datahub/data-models/api/dataset/ownership';
import { transformOwnersResponseIntoOwners } from '@datahub/data-models/entity/dataset/utils/owner';
import { readInstitutionalMemory, writeInstitutionalMemory } from '@datahub/data-models/entity/institutional-memory';
import { relationship } from '@datahub/data-models/relationships/decorator';
import { PersonEntity } from '@datahub/data-models/entity/person/person-entity';

/**
 * Defines the data model for the Dataset entity.
 */
@statics<IBaseEntityStatics<Com.Linkedin.Dataset.Dataset>>()
export class DatasetEntity extends BaseEntity<Com.Linkedin.Dataset.Dataset> {
  /**
   * The human friendly alias for Dataset entities
   */
  static displayName: 'datasets' = 'datasets';

  // TODO: [META-9120] This logic should not exist on the UI and is a temporary fix that will hopefully one day
  // get changed and read from API level instead. Also it won't matter once we fully migrate to new compliance
  /**
   * Calculates whether or not to show this dataset compliance as inherited from parents based on the nature
   * of the upstream releationships
   * @param upstreams - representation of upstream lineage information. Typing can change if used in data-portal
   */
  static hasInheritedCompliance<T extends { type: string }>(upstreams: Array<T> = []): boolean {
    if (!upstreams.length) {
      return false;
    }
    // Rule 1: If all upstreams are transformed, then we don't want to show this as inherited compliance
    const upstreamsAreAllTransformed: boolean = every(
      upstreams,
      (upstream): boolean => upstream.type === 'TRANSFORMED'
    );
    return !upstreamsAreAllTransformed;
  }

  /**
   * There is no snapshot for datasets, returning undefined but
   * implementing since, framework may call it
   */
  get readSnapshot(): Promise<undefined> | Promise<Snapshot> {
    return Promise.resolve(undefined);
  }

  /**
   * Additional hook to fetch the platforms needed for some operation in datasets
   */
  async onAfterCreate(): Promise<void> {
    const dataPlatforms: Array<IDataPlatform> = await readDataPlatforms();
    const currentDataPlatform = dataPlatforms.find((platform): boolean => platform.name === this.platform);
    // Reads dataset owner information from the resolved ownership endpoint
    const ownersResponse = await readDatasetOwnersByUrn(this.urn);
    // Transforms the owners response into a more generic owners response and sets it on the entity
    const owners = transformOwnersResponseIntoOwners(ownersResponse);
    setProperties(this, { owners, currentDataPlatform });
  }

  get displayName(): 'datasets' {
    return DatasetEntity.displayName;
  }

  /**
   * Discriminant when included in a tagged Entity union
   */
  static kind = 'DatasetEntity';

  get kind(): string {
    return DatasetEntity.kind;
  }

  /**
   * Shows the urn as an unencoded value
   */
  get rawUrn(): string {
    return decodeUrn(this.urn);
  }

  /**
   * Returns the fabric/environment for this particular dataset entity
   */
  get fabric(): FabricType | undefined {
    throw new Error(NotImplementedError);
  }

  /**
   * Reads the snapshots for a list of Dataset urns
   * @static
   */
  static readSnapshots(_urns: Array<string>): Promise<Array<IDatasetSnapshot>> {
    throw new Error(NotImplementedError);
  }

  /**
   * Holds the classified data retrieved from the API layer for the compliance information as a compliance
   * info class
   * @type {DatasetComplianceInfo}
   */
  compliance?: DatasetComplianceInfo;

  /**
   * Holds the classified data retrieved from the API layer for the schema information as a DatasetSchema
   * class
   * @type {DatasetSchema}
   */
  schema?: DatasetSchema;

  /**
   * Holds the classified data retrieved from the API layer for the export policy as a DatasetExportPolicy
   * class
   * @type {DatasetExportPolicy}
   */
  exportPolicy?: DatasetExportPolicy;

  /**
   * Holds the classified data retrieved from the API layer for the purge policy as a DatasetPurgePOlicy
   * class
   * @type {DatasetPurgePolicy}
   */
  purgePolicy?: DatasetPurgePolicy;

  /**
   * Holds the classified data retrieved from the API layer for the upstream datasets list as an array of
   * DatasetLineage classes
   * @type {Array<DatasetLineage>}
   */
  upstreams?: Array<DatasetLineage>;

  /**
   * Whether or not the dataset has been deprecated
   */
  @alias('entity.dataset.deprecated')
  deprecated?: boolean;

  /**
   * Note attached to the deprecation process for this dataset
   */
  @alias('entity.dataset.deprecationNote')
  deprecationNote?: string;

  /**
   * Timestamp for when the dataset was deprecated
   */
  @alias('entity.dataset.decommissionTime')
  decommissionTime?: number;

  /**
   * Last timestamp for the modification of this dataset
   */
  @oneWay('entity.dataset.modifiedTime')
  modifiedTime?: number;

  /**
   * Whether or not the entity has been removed.
   * Note: This overrides the BaseEntity implementation since DatasetEntity has a different behavior than
   * other entities
   */
  @oneWay('entity.dataset.removed')
  removed!: boolean;

  /**
   * References the value of the healthScore from the underlying IDatasetEntity object
   * used in search results where the Health Score is shown as an attribute
   */
  @reads('entity.health.score')
  healthScore?: number;

  /**
   * gets the dataorigin field (needed from search)
   * from the urn
   */
  @computed('urn')
  get dataorigin(): FabricType | undefined {
    return getDatasetUrnParts(this.urn).fabric;
  }

  /**
   * Reference to the data entity, is the data platform to which the dataset belongs
   */
  @computed('entity.dataset.platform', 'urn')
  get platform(): DatasetPlatform | undefined {
    const { urn, entity } = this;
    const parts = getDatasetUrnParts(urn);
    const platform = (entity && entity.platform) || parts.platform;
    // New API return platform with urn:li:dataPlatform:, in order to
    // make it compatible, we manually remove that part.
    const platformName = platform?.replace('urn:li:dataPlatform:', '');

    return platformName as DatasetPlatform | undefined;
  }

  /**
   * Reference to the data entity's native name, should not be something that is editable but gives us a
   * more human readable form for the dataset vs the urn
   */
  get name(): string {
    const { entity } = this;
    const name = (entity && entity.name) || '';
    return name || getDatasetUrnParts(this.urn).prefix || this.urn;
  }

  // TODO: [META-9120] This logic should not exist on the UI and is a temporary fix that will hopefully one day
  // get changed and read from API level instead. Also it won't matter once we fully migrate to new compliance
  /**
   * Calculates whether or not to show this dataset compliance as inherited from parents based on the nature
   * of the upstream relationships
   * @type {boolean}
   */
  @computed('upstreams', 'compliance')
  get hasInheritedCompliance(): boolean | void {
    const compliance = this.compliance;

    if (compliance) {
      return DatasetEntity.hasInheritedCompliance(this.upstreams) && compliance.resolvedFrom !== this.rawUrn;
    }
  }

  /**
   * Reference to the constructed data platform once it is fetched from api
   */
  currentDataPlatform?: IDataPlatform;

  /**
   * Class properties common across instances
   * Dictates how visual ui components should be rendered
   * Implemented as a getter to ensure that reads are idempotent
   * @readonly
   * @static
   * @type {IEntityRenderProps}
   */
  static get renderProps(): IEntityRenderProps {
    return getRenderProps();
  }

  /**
   * Is a promise that retrieves the compliance information (if it doesn't exist yet) and returns whether or not
   * the dataset contains personally identifiable information
   */
  readPiiStatus(): Promise<boolean> {
    return Promise.resolve().then(() => false);
  }

  /**
   * Is a promise that retrieves the schema information and returns and sets the schema information in a
   * thennable format
   */
  async readSchema(): Promise<DatasetSchema> {
    const schema = await readDatasetSchema(this.urn);
    const datasetSchema = new DatasetSchema(schema);
    set(this, 'schema', datasetSchema);
    return datasetSchema;
  }

  /**
   * Is a promise that retrieves the dataset export policy information and sets the policy information to the class
   * as a property
   */
  async readExportPolicy(): Promise<DatasetExportPolicy> {
    const exportPolicy = await readDatasetExportPolicy(this.urn).catch(
      // Handling for an expected possibility of receiving a 404 error for this export policy, which would require
      // us to actually use a factory generator for the export policy
      getDefaultIfNotFoundError(undefined)
    );
    const datasetExportPolicy = new DatasetExportPolicy(exportPolicy);
    set(this, 'exportPolicy', datasetExportPolicy);
    return datasetExportPolicy;
  }

  /**
   * Is a promise that retrieves the purge policy information and sets this to the class as a property
   */
  async readPurgePolicy(): Promise<DatasetPurgePolicy> {
    const purgePolicy = await readDatasetRetention(this.urn).catch(
      // Handling for an expected possibility of receiving a 404 error for this purge policy, which would require
      // us to actually use a factory generator for the purge policy
      getDefaultIfNotFoundError(undefined)
    );

    const datasetPurgePolicy = new DatasetPurgePolicy(purgePolicy);
    set(this, 'purgePolicy', datasetPurgePolicy);
    return datasetPurgePolicy;
  }

  /**
   * Asynchronously retrieves the upstream datasets for this dataset and assigns to the upstreams property
   */
  async readUpstreams(): Promise<Array<DatasetLineage>> {
    const lineageList = await readUpstreamDatasets(this.urn).catch(
      // Handling for an expected possibility of receiving a 404 error for this upstream dataset response, which
      // would require us to replace error with just an empty list
      getDefaultIfNotFoundError([] as DatasetLineageList)
    );

    const upstreams = lineageList.map((lineage): DatasetLineage => new DatasetLineage(lineage));
    set(this, 'upstreams', upstreams);
    return upstreams;
  }

  /**
   * Processes a request to save compliance data by row or overall compliance. This allows us to save compliance
   * information for a single row or as a bulk edit. We know already that bulk edits affect the working copy in
   * the compliance object directly, but row edits have their own working copy for the specific field. In the case
   * of the latter, we have optional parameters to signify that we should be applying a row edit
   * @param {string} fieldName - optional, signifies the field to edit
   * @param {Array<DatasetComplianceAnnotation>} fieldDiff - optional, overwriting diffs for the field
   * @returns {boolean} whether or not the application was successful
   */
  async saveCompliance(fieldName?: string, fieldDiff?: Array<DatasetComplianceAnnotation>): Promise<void> {
    const { compliance, schema, urn } = this;

    if (compliance) {
      if (fieldName && fieldDiff) {
        compliance.applyAnnotationsByField(fieldName, fieldDiff);
      }

      const workingCopy = compliance.readWorkingCopy({
        withoutNullFields: true,
        schema
      });
      await saveDatasetCompliance(urn, workingCopy);
    }
  }

  /**
   * Processes a request to save the export policy working copy. Returns this in a thennable format so that any
   * responses or issues can be handled on the component side
   */
  async saveExportPolicy(): Promise<void> {
    const { exportPolicy, urn } = this;

    if (exportPolicy) {
      const workingCopy = exportPolicy.readWorkingCopy();
      await saveDatasetExportPolicy(urn, workingCopy);
    }
  }

  /**
   * Processes a request to save the purge policy working copy. Returns this in a thennable format so that any
   * responses or issues can be handled on the component side
   */
  async savePurgePolicy(): Promise<void> {
    const { purgePolicy, urn } = this;

    if (purgePolicy) {
      const workingCopy = purgePolicy.readWorkingCopy();
      await saveDatasetRetention(urn, {
        ...workingCopy,
        datasetUrn: decodeUrn(urn),
        datasetId: null
      });
    }
  }

  /**
   * Processes a save request to save the suggestion feedback given by the user, or inferred from the user action.
   * Since this is a background task, we do not await for a response from the server
   * @param suggestion - suggestion for which we are saving the feedback
   * @param feedback - whether the user has accepted or rejected the suggestion as accurate
   */
  saveSuggestionFeedback(suggestion: DatasetComplianceSuggestion, feedback: SuggestionIntent): void {
    const { uid } = suggestion;
    saveDatasetComplianceSuggestionFeedbackByUrn(this.urn, uid, feedback);
  }

  /**
   * TODO META-11674
   * Interim method until IDatasetEntity is removed
   */
  get legacyDataset(): IDatasetEntity | void {
    if (this.entity) {
      return (this.entity as unknown) as IDatasetEntity;
    }
  }

  // TODO META-12149 this should be part of an Aspect. This fns can't live under BaseEntity as
  // then we would have a circular dependency:
  // BaseEntity -> InstitutionalMemory -> PersonEntity -> BaseEntity
  readInstitutionalMemory = readInstitutionalMemory;
  writeInstitutionalMemory = writeInstitutionalMemory;

  constructor(readonly urn: string, entityData?: Com.Linkedin.Dataset.Dataset) {
    super(urn);
    // Sometimes we do not need readEntity to get this information as it was already provided by another entity
    // and we can just instantiate the class with it
    if (entityData) {
      set(this, 'entity', entityData);
    }
  }

  @relationship('people', 'ownerUrns')
  datasetOwners?: Array<PersonEntity>;
}
