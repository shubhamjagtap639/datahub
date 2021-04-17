import { GetDatasetDocument, UpdateDatasetDocument } from './graphql/dataset.generated';
import { GetBrowsePathsDocument, GetBrowseResultsDocument } from './graphql/browse.generated';
import {
    GetAutoCompleteResultsDocument,
    GetSearchResultsDocument,
    GetSearchResultsQuery,
} from './graphql/search.generated';
import { GetUserDocument } from './graphql/user.generated';
import { Dataset, DatasetLineageType, EntityType, PlatformType, RelatedDataset } from './types.generated';
import { GetTagDocument } from './graphql/tag.generated';

const user1 = {
    username: 'sdas',
    urn: 'urn:li:corpuser:1',
    type: EntityType.CorpUser,
    info: {
        email: 'sdas@domain.com',
        active: true,
        displayName: 'sdas',
        title: 'Software Engineer',
        firstName: 'Shirshanka',
        lastName: 'Das',
        fullName: 'Shirshanka Das',
    },
    editableInfo: {
        pictureLink: 'https://crunchconf.com/img/2019/speakers/1559291783-ShirshankaDas.png',
    },
};

const user2 = {
    username: 'john',
    urn: 'urn:li:corpuser:3',
    type: EntityType.CorpUser,
    info: {
        email: 'john@domain.com',
        active: true,
        displayName: 'john',
        title: 'Eng',
        firstName: 'John',
        lastName: 'Joyce',
        fullName: 'John Joyce',
    },
    editableInfo: {
        pictureLink: null,
    },
};

const dataset1 = {
    urn: 'urn:li:dataset:1',
    type: EntityType.Dataset,
    platform: {
        urn: 'urn:li:dataPlatform:hdfs',
        name: 'HDFS',
        type: EntityType.DataPlatform,
        info: {
            type: PlatformType.FileSystem,
            datasetNameDelimiter: '.',
            logoUrl: '',
        },
    },
    platformNativeType: 'TABLE',
    name: 'The Great Test Dataset',
    origin: 'PROD',
    tags: ['Private', 'PII'],
    description: 'This is the greatest dataset in the world, youre gonna love it!',
    uri: 'www.google.com',
    properties: [
        {
            key: 'TestProperty',
            value: 'My property value.',
        },
        {
            key: 'AnotherTestProperty',
            value: 'My other property value.',
        },
    ],
    created: {
        time: 0,
    },
    lastModified: {
        time: 0,
    },
    ownership: {
        owners: [
            {
                owner: {
                    ...user1,
                },
                type: 'DATAOWNER',
            },
            {
                owner: {
                    ...user2,
                },
                type: 'DELEGATE',
            },
        ],
        lastModified: {
            time: 0,
        },
    },
    institutionalMemory: {
        elements: [
            {
                url: 'https://www.google.com',
                description: 'This only points to Google',
                created: {
                    actor: 'urn:li:corpuser:1',
                    time: 1612396473001,
                },
            },
        ],
    },
};

const dataset2 = {
    urn: 'urn:li:dataset:2',
    type: EntityType.Dataset,
    platform: {
        urn: 'urn:li:dataPlatform:mysql',
        name: 'MySQL',
        info: {
            type: PlatformType.RelationalDb,
            datasetNameDelimiter: '.',
            logoUrl: '',
        },
        type: EntityType.DataPlatform,
    },
    platformNativeType: 'TABLE',
    name: 'Some Other Dataset',
    origin: 'PROD',
    tags: ['Outdated'],
    description: 'This is some other dataset, so who cares!',
    uri: 'www.google.com',
    properties: [],
    created: {
        time: 0,
    },
    lastModified: {
        time: 0,
    },
    ownership: {
        owners: [
            {
                owner: {
                    ...user1,
                },
                type: 'DATAOWNER',
            },
            {
                owner: {
                    ...user2,
                },
                type: 'DELEGATE',
            },
        ],
        lastModified: {
            time: 0,
        },
    },
};

export const dataset3 = {
    __typename: 'Dataset',
    urn: 'urn:li:dataset:3',
    type: EntityType.Dataset,
    platform: {
        urn: 'urn:li:dataPlatform:kafka',
        name: 'Kafka',
        info: {
            type: PlatformType.MessageBroker,
            datasetNameDelimiter: '.',
            logoUrl: '',
        },
        type: EntityType.DataPlatform,
    },
    platformNativeType: 'STREAM',
    name: 'Yet Another Dataset',
    origin: 'PROD',
    tags: ['Trusted'],
    description: 'This and here we have yet another Dataset (YAN). Are there more?',
    uri: 'www.google.com',
    properties: [],
    created: {
        time: 0,
    },
    lastModified: {
        time: 0,
    },
    ownership: {
        owners: [
            {
                owner: {
                    ...user1,
                },
                type: 'DATAOWNER',
            },
            {
                owner: {
                    ...user2,
                },
                type: 'DELEGATE',
            },
        ],
        lastModified: {
            time: 0,
        },
    },
    globalTags: {
        tags: [
            {
                tag: {
                    type: EntityType.Tag,
                    urn: 'urn:li:tag:abc-sample-tag',
                    name: 'abc-sample-tag',
                    description: 'sample tag',
                },
            },
        ],
    },
    upstreamLineage: null,
    downstreamLineage: null,
    institutionalMemory: {
        elements: [
            {
                url: 'https://www.google.com',
                author: 'datahub',
                description: 'This only points to Google',
                created: {
                    actor: 'urn:li:corpuser:1',
                    time: 1612396473001,
                },
            },
        ],
    },
    schema: null,
    editableSchemaMetadata: null,
    deprecation: null,
} as Dataset;

export const dataset4 = {
    ...dataset3,
    name: 'Fourth Test Dataset',
    urn: 'urn:li:dataset:4',
};

export const dataset5 = {
    ...dataset3,
    name: 'Fifth Test Dataset',
    urn: 'urn:li:dataset:5',
};

export const dataset6 = {
    ...dataset3,
    name: 'Sixth Test Dataset',
    urn: 'urn:li:dataset:6',
};

export const dataset7 = {
    ...dataset3,
    name: 'Seventh Test Dataset',
    urn: 'urn:li:dataset:7',
};

export const dataset3WithLineage = {
    ...dataset3,
    upstreamLineage: {
        upstreams: [
            {
                created: {
                    time: 0,
                },
                lastModified: {
                    time: 0,
                },
                type: DatasetLineageType.Transformed,
                dataset: dataset7,
            },
            {
                created: {
                    time: 0,
                },
                lastModified: {
                    time: 0,
                },
                type: DatasetLineageType.Transformed,
                dataset: dataset4,
            },
        ],
    },
    downstreamLineage: {
        downstreams: [],
    },
};

export const dataset4WithLineage = {
    ...dataset4,
    upstreamLineage: {
        upstreams: [
            {
                created: {
                    time: 0,
                },
                lastModified: {
                    time: 0,
                },
                type: DatasetLineageType.Transformed,
                dataset: dataset6,
            },
            {
                created: {
                    time: 0,
                },
                lastModified: {
                    time: 0,
                },
                type: DatasetLineageType.Transformed,
                dataset: dataset5,
            },
        ],
    },
    downstreamLineage: {
        downstreams: [
            {
                created: {
                    time: 0,
                },
                lastModified: {
                    time: 0,
                },
                type: DatasetLineageType.Transformed,
                dataset: dataset3,
            },
        ],
    },
};

export const dataset5WithCyclicalLineage = {
    ...dataset5,
    upstreamLineage: {
        upstreams: [
            {
                created: {
                    time: 0,
                },
                lastModified: {
                    time: 0,
                },
                type: DatasetLineageType.Transformed,
                dataset: dataset3,
            },
        ],
    },
    downstreamLineage: {
        downstreams: [
            {
                created: {
                    time: 0,
                },
                lastModified: {
                    time: 0,
                },
                type: DatasetLineageType.Transformed,
                dataset: dataset7,
            },
            {
                created: {
                    time: 0,
                },
                lastModified: {
                    time: 0,
                },
                type: DatasetLineageType.Transformed,
                dataset: dataset6,
            },
            {
                created: {
                    time: 0,
                },
                lastModified: {
                    time: 0,
                },
                type: DatasetLineageType.Transformed,
                dataset: dataset4,
            },
        ],
    },
};

export const dataset5WithLineage = {
    ...dataset5,
    upstreamLineage: {
        upstreams: [] as RelatedDataset[],
    },
    downstreamLineage: {
        downstreams: [
            {
                created: {
                    time: 0,
                },
                lastModified: {
                    time: 0,
                },
                type: DatasetLineageType.Transformed,
                dataset: dataset7,
            },
            {
                created: {
                    time: 0,
                },
                lastModified: {
                    time: 0,
                },
                type: DatasetLineageType.Transformed,
                dataset: dataset6,
            },
            {
                created: {
                    time: 0,
                },
                lastModified: {
                    time: 0,
                },
                type: DatasetLineageType.Transformed,
                dataset: dataset4,
            },
        ],
    },
};

export const dataset6WithLineage = {
    ...dataset6,
    upstreamLineage: {
        upstreams: [
            {
                created: {
                    time: 0,
                },
                lastModified: {
                    time: 0,
                },
                type: DatasetLineageType.Transformed,
                dataset: dataset5,
            },
        ],
    },
    downstreamLineage: {
        downstreams: [
            {
                created: {
                    time: 0,
                },
                lastModified: {
                    time: 0,
                },
                type: DatasetLineageType.Transformed,
                dataset: dataset4,
            },
        ],
    },
};

export const dataset7WithLineage = {
    ...dataset7,
    upstreamLineage: {
        upstreams: [
            {
                created: {
                    time: 0,
                },
                lastModified: {
                    time: 0,
                },
                type: DatasetLineageType.Transformed,
                dataset: dataset5,
            },
        ],
    },
    downstreamLineage: {
        downstreams: [
            {
                created: {
                    time: 0,
                },
                lastModified: {
                    time: 0,
                },
                type: DatasetLineageType.Transformed,
                dataset: dataset3,
            },
        ],
    },
};

export const dataset7WithSelfReferentialLineage = {
    ...dataset7,
    upstreamLineage: {
        upstreams: [
            {
                created: {
                    time: 0,
                },
                lastModified: {
                    time: 0,
                },
                type: DatasetLineageType.Transformed,
                dataset: dataset5,
            },
            {
                created: {
                    time: 0,
                },
                lastModified: {
                    time: 0,
                },
                type: DatasetLineageType.Transformed,
                dataset: dataset7,
            },
        ],
    },
    downstreamLineage: {
        downstreams: [
            {
                created: {
                    time: 0,
                },
                lastModified: {
                    time: 0,
                },
                type: DatasetLineageType.Transformed,
                dataset: dataset3,
            },
            {
                created: {
                    time: 0,
                },
                lastModified: {
                    time: 0,
                },
                type: DatasetLineageType.Transformed,
                dataset: dataset7,
            },
        ],
    },
};

const sampleTag = {
    urn: 'urn:li:tag:abc-sample-tag',
    name: 'abc-sample-tag',
    description: 'sample tag description',
    ownership: {
        owners: [
            {
                owner: {
                    ...user1,
                },
                type: 'DATAOWNER',
            },
            {
                owner: {
                    ...user2,
                },
                type: 'DELEGATE',
            },
        ],
        lastModified: {
            time: 0,
        },
    },
};

/*
    Define mock data to be returned by Apollo MockProvider. 
*/
export const mocks = [
    {
        request: {
            query: GetDatasetDocument,
            variables: {
                urn: 'urn:li:dataset:3',
            },
        },
        result: {
            data: {
                dataset: {
                    ...dataset3,
                },
            },
        },
    },
    {
        request: {
            query: GetUserDocument,
            variables: {
                urn: 'urn:li:corpuser:1',
            },
        },
        result: {
            data: {
                corpUser: {
                    ...user1,
                },
            },
        },
    },
    {
        request: {
            query: GetBrowsePathsDocument,
            variables: {
                input: {
                    type: 'DATASET',
                    urn: 'urn:li:dataset:1',
                },
            },
        },
        result: {
            data: {
                browsePaths: [['prod', 'hdfs', 'datasets']],
            },
        },
    },
    {
        request: {
            query: GetBrowseResultsDocument,
            variables: {
                input: {
                    type: 'DATASET',
                    path: [],
                    start: 0,
                    count: 20,
                    filters: null,
                },
            },
        },
        result: {
            data: {
                browse: {
                    entities: [],
                    start: 0,
                    count: 0,
                    total: 0,
                    metadata: {
                        path: [],
                        groups: [
                            {
                                name: 'prod',
                                count: 1,
                            },
                        ],
                        totalNumEntities: 1,
                    },
                },
            },
        },
    },
    {
        request: {
            query: GetBrowseResultsDocument,
            variables: {
                input: {
                    type: 'DATASET',
                    path: ['prod', 'hdfs'],
                    start: 0,
                    count: 20,
                    filters: null,
                },
            },
        },
        result: {
            data: {
                browse: {
                    entities: [
                        {
                            __typename: 'Dataset',
                            ...dataset1,
                        },
                    ],
                    start: 0,
                    count: 1,
                    total: 1,
                    metadata: {
                        path: ['prod', 'hdfs'],
                        groups: [],
                        totalNumEntities: 0,
                    },
                },
            },
        },
    },
    {
        request: {
            query: GetBrowseResultsDocument,
            variables: {
                input: {
                    type: 'DATASET',
                    path: ['prod'],
                    start: 0,
                    count: 20,
                    filters: null,
                },
            },
        },
        result: {
            data: {
                browse: {
                    entities: [],
                    start: 0,
                    count: 0,
                    total: 0,
                    metadata: {
                        path: ['prod'],
                        groups: [
                            {
                                name: 'hdfs',
                                count: 1,
                            },
                        ],
                        totalNumEntities: 1,
                    },
                },
            },
        },
    },
    {
        request: {
            query: GetAutoCompleteResultsDocument,
            variables: {
                input: {
                    type: 'DATASET',
                    query: 't',
                },
            },
        },
        result: {
            data: {
                autoComplete: {
                    query: 't',
                    suggestions: ['The Great Test Dataset', 'Some other test'],
                },
            },
        },
    },
    {
        request: {
            query: GetAutoCompleteResultsDocument,
            variables: {
                input: {
                    type: 'USER',
                    query: 'j',
                },
            },
        },
        result: {
            data: {
                autoComplete: {
                    query: 'j',
                    suggestions: ['jjoyce'],
                },
            },
        },
    },
    {
        request: {
            query: GetSearchResultsDocument,
            variables: {
                input: {
                    type: 'DATASET',
                    query: 'test',
                    start: 0,
                    count: 10,
                    filters: [],
                },
            },
        },
        result: {
            data: {
                search: {
                    start: 0,
                    count: 3,
                    total: 3,
                    searchResults: [
                        {
                            entity: {
                                __typename: 'Dataset',
                                ...dataset1,
                            },
                            matchedFields: [
                                {
                                    name: 'fieldName',
                                    value: 'fieldValue',
                                },
                            ],
                        },
                        {
                            entity: {
                                __typename: 'Dataset',
                                ...dataset2,
                            },
                        },
                        {
                            entity: {
                                __typename: 'Dataset',
                                ...dataset3,
                            },
                        },
                    ],
                    facets: [
                        {
                            field: 'origin',
                            aggregations: [{ value: 'PROD', count: 3 }],
                        },
                        {
                            field: 'platform',
                            aggregations: [
                                { value: 'HDFS', count: 1 },
                                { value: 'MySQL', count: 1 },
                                { value: 'Kafka', count: 1 },
                            ],
                        },
                    ],
                },
            } as GetSearchResultsQuery,
        },
    },
    {
        request: {
            query: GetSearchResultsDocument,
            variables: {
                input: {
                    type: 'DATASET',
                    query: 'test',
                    start: 0,
                    count: 10,
                    filters: [
                        {
                            field: 'platform',
                            value: 'kafka',
                        },
                    ],
                },
            },
        },
        result: {
            data: {
                __typename: 'Query',
                search: {
                    __typename: 'SearchResults',
                    start: 0,
                    count: 1,
                    total: 1,
                    searchResults: [
                        {
                            entity: {
                                __typename: 'Dataset',
                                ...dataset3,
                            },
                            matchedFields: [],
                        },
                    ],
                    facets: [
                        {
                            field: 'origin',
                            aggregations: [
                                {
                                    value: 'PROD',
                                    count: 3,
                                },
                            ],
                        },
                        {
                            field: 'platform',
                            aggregations: [
                                { value: 'hdfs', count: 1 },
                                { value: 'mysql', count: 1 },
                                { value: 'kafka', count: 1 },
                            ],
                        },
                    ],
                },
            } as GetSearchResultsQuery,
        },
    },
    {
        request: {
            query: GetSearchResultsDocument,
            variables: {
                input: {
                    type: 'DATASET',
                    query: 'test',
                    start: 0,
                    count: 10,
                    filters: [
                        {
                            field: 'platform',
                            value: 'kafka,hdfs',
                        },
                    ],
                },
            },
        },
        result: {
            data: {
                __typename: 'Query',
                search: {
                    __typename: 'SearchResults',
                    start: 0,
                    count: 1,
                    total: 1,
                    searchResults: [
                        {
                            entity: {
                                __typename: 'Dataset',
                                ...dataset3,
                            },
                            matchedFields: [],
                        },
                    ],
                    facets: [
                        {
                            field: 'origin',
                            aggregations: [
                                {
                                    value: 'PROD',
                                    count: 3,
                                },
                            ],
                        },
                        {
                            field: 'platform',
                            aggregations: [
                                { value: 'hdfs', count: 1 },
                                { value: 'mysql', count: 1 },
                                { value: 'kafka', count: 1 },
                            ],
                        },
                    ],
                },
            } as GetSearchResultsQuery,
        },
    },
    {
        request: {
            query: GetSearchResultsDocument,
            variables: {
                input: {
                    type: 'USER',
                    query: 'Test',
                    start: 0,
                    count: 10,
                },
            },
        },
        result: {
            data: {
                search: {
                    start: 0,
                    count: 2,
                    total: 2,
                    searchResult: [
                        {
                            entity: {
                                ...user1,
                            },
                            matchedFields: [],
                        },
                    ],
                },
            },
        },
    },
    {
        request: {
            query: UpdateDatasetDocument,
            variables: {
                input: {
                    urn: 'urn:li:dataset:1',
                    ownership: {
                        owners: [
                            {
                                owner: 'urn:li:corpuser:1',
                                type: 'DATAOWNER',
                            },
                        ],
                    },
                },
            },
        },
        result: {
            data: {
                dataset: {
                    urn: 'urn:li:corpuser:1',
                    ownership: {
                        owners: [
                            {
                                owner: {
                                    ...user1,
                                },
                                type: 'DATAOWNER',
                            },
                        ],
                        lastModified: {
                            time: 0,
                        },
                    },
                },
            },
        },
    },
    {
        request: {
            query: GetSearchResultsDocument,
            variables: {
                input: {
                    type: 'CORP_USER',
                    query: 'tags:abc-sample-tag',
                    start: 0,
                    count: 1,
                    filters: [],
                },
            },
        },
        result: {
            data: {
                __typename: 'Query',
                search: {
                    __typename: 'SearchResults',
                    start: 0,
                    count: 0,
                    total: 2,
                    searchResults: [],
                    facets: [],
                },
            },
        },
    },
    {
        request: {
            query: GetSearchResultsDocument,
            variables: {
                input: {
                    type: 'DATASET',
                    query: 'tags:abc-sample-tag',
                    start: 0,
                    count: 1,
                    filters: [],
                },
            },
        },
        result: {
            data: {
                __typename: 'Query',
                search: {
                    __typename: 'SearchResults',
                    start: 0,
                    count: 1,
                    total: 1,
                    searchResults: [
                        {
                            entity: {
                                __typename: 'Dataset',
                                ...dataset3,
                            },
                            matchedFields: [],
                        },
                    ],
                    facets: [
                        {
                            field: 'origin',
                            aggregations: [
                                {
                                    value: 'PROD',
                                    count: 3,
                                },
                            ],
                        },
                        {
                            field: 'platform',
                            aggregations: [
                                { value: 'hdfs', count: 1 },
                                { value: 'mysql', count: 1 },
                                { value: 'kafka', count: 1 },
                            ],
                        },
                    ],
                },
            } as GetSearchResultsQuery,
        },
    },
    {
        request: {
            query: GetSearchResultsDocument,
            variables: {
                input: {
                    type: 'DATASET',
                    query: '*',
                    start: 0,
                    count: 20,
                    filters: [],
                },
            },
        },
        result: {
            data: {
                __typename: 'Query',
                search: {
                    __typename: 'SearchResults',
                    start: 0,
                    count: 1,
                    total: 1,
                    searchResults: [
                        {
                            entity: {
                                __typename: 'Dataset',
                                ...dataset3,
                            },
                            matchedFields: [],
                        },
                        {
                            entity: {
                                __typename: 'Dataset',
                                ...dataset4,
                            },
                            matchedFields: [],
                        },
                    ],
                    facets: [
                        {
                            field: 'origin',
                            aggregations: [
                                {
                                    value: 'PROD',
                                    count: 3,
                                },
                            ],
                        },
                        {
                            field: 'platform',
                            aggregations: [
                                { value: 'hdfs', count: 1 },
                                { value: 'mysql', count: 1 },
                                { value: 'kafka', count: 1 },
                            ],
                        },
                    ],
                },
            } as GetSearchResultsQuery,
        },
    },
    {
        request: {
            query: GetTagDocument,
            variables: {
                urn: 'urn:li:tag:abc-sample-tag',
            },
        },
        result: {
            data: {
                tag: { ...sampleTag },
            },
        },
    },
];
