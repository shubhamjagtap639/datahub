import React, { useState } from 'react';

import { Button, Table, Typography } from 'antd';
import { AlignType } from 'rc-table/lib/interface';
import styled from 'styled-components';
import { FetchResult } from '@apollo/client';

import TypeIcon from './TypeIcon';
import {
    Schema,
    SchemaFieldDataType,
    GlobalTags,
    EditableSchemaMetadata,
    SchemaField,
    EditableSchemaMetadataUpdate,
    GlobalTagsUpdate,
    EditableSchemaFieldInfo,
    EditableSchemaFieldInfoUpdate,
} from '../../../../../types.generated';
import TagGroup from '../../../../shared/tags/TagGroup';
import { UpdateDatasetMutation } from '../../../../../graphql/dataset.generated';
import { convertTagsForUpdate } from '../../../../shared/tags/utils/convertTagsForUpdate';
import DescriptionField from './SchemaDescriptionField';

const ViewRawButtonContainer = styled.div`
    display: flex;
    justify-content: flex-end;
    padding-bottom: 16px;
`;

export type Props = {
    schema?: Schema | null;
    editableSchemaMetadata?: EditableSchemaMetadata | null;
    updateEditableSchema: (
        update: EditableSchemaMetadataUpdate,
    ) => Promise<FetchResult<UpdateDatasetMutation, Record<string, any>, Record<string, any>>>;
};

const defaultColumns = [
    {
        width: 96,
        title: 'Type',
        dataIndex: 'type',
        key: 'type',
        align: 'center' as AlignType,
        render: (type: SchemaFieldDataType) => {
            return <TypeIcon type={type} />;
        },
    },
    {
        title: 'Field',
        dataIndex: 'fieldPath',
        key: 'fieldPath',
        width: 192,
        render: (fieldPath: string) => {
            return <Typography.Text strong>{fieldPath}</Typography.Text>;
        },
    },
];

function convertEditableSchemaMetadataForUpdate(
    editableSchemaMetadata: EditableSchemaMetadata | null | undefined,
): EditableSchemaMetadataUpdate {
    return {
        editableSchemaFieldInfo:
            editableSchemaMetadata?.editableSchemaFieldInfo.map((editableSchemaFieldInfo) => ({
                fieldPath: editableSchemaFieldInfo?.fieldPath,
                description: editableSchemaFieldInfo?.description,
                globalTags: { tags: convertTagsForUpdate(editableSchemaFieldInfo?.globalTags?.tags || []) },
            })) || [],
    };
}

export default function SchemaView({ schema, editableSchemaMetadata, updateEditableSchema }: Props) {
    const [tagHoveredIndex, setTagHoveredIndex] = useState<number | undefined>(undefined);
    const [descHoveredIndex, setDescHoveredIndex] = useState<number | undefined>(undefined);
    const [showRaw, setShowRaw] = useState(false);

    const updateSchema = (newFieldInfo: EditableSchemaFieldInfoUpdate, record?: EditableSchemaFieldInfo) => {
        let existingMetadataAsUpdate = convertEditableSchemaMetadataForUpdate(editableSchemaMetadata);

        if (existingMetadataAsUpdate.editableSchemaFieldInfo.some((field) => field.fieldPath === record?.fieldPath)) {
            // if we already have a record for this field, update the record
            existingMetadataAsUpdate = {
                editableSchemaFieldInfo: existingMetadataAsUpdate.editableSchemaFieldInfo.map((fieldUpdate) => {
                    if (fieldUpdate.fieldPath === record?.fieldPath) {
                        return newFieldInfo;
                    }
                    return fieldUpdate;
                }),
            };
        } else {
            // otherwise add a new record
            existingMetadataAsUpdate.editableSchemaFieldInfo.push(newFieldInfo);
        }
        return updateEditableSchema(existingMetadataAsUpdate);
    };

    const onUpdateTags = (update: GlobalTagsUpdate, record?: EditableSchemaFieldInfo) => {
        if (!record) return Promise.resolve();
        const newFieldInfo: EditableSchemaFieldInfoUpdate = {
            fieldPath: record?.fieldPath,
            description: record?.description,
            globalTags: update,
        };

        return updateSchema(newFieldInfo, record);
    };

    const onUpdateDescription = (updatedDescription: string, record?: EditableSchemaFieldInfo) => {
        if (!record) return Promise.resolve();
        const newFieldInfo: EditableSchemaFieldInfoUpdate = {
            fieldPath: record?.fieldPath,
            description: updatedDescription,
            globalTags: { tags: convertTagsForUpdate(record?.globalTags?.tags || []) },
        };
        return updateSchema(newFieldInfo, record);
    };

    const descriptionRender = (description: string, record: SchemaField, rowIndex: number | undefined) => {
        const relevantEditableFieldInfo = editableSchemaMetadata?.editableSchemaFieldInfo.find(
            (candidateEditableFieldInfo) => candidateEditableFieldInfo.fieldPath === record.fieldPath,
        ) || { fieldPath: record.fieldPath };
        return (
            <DescriptionField
                description={description}
                updatedDescription={relevantEditableFieldInfo.description}
                onHover={descHoveredIndex !== undefined && rowIndex === descHoveredIndex}
                onUpdate={(update) => onUpdateDescription(update, relevantEditableFieldInfo)}
            />
        );
    };

    const tagGroupRender = (tags: GlobalTags, record: SchemaField, rowIndex: number | undefined) => {
        const relevantEditableFieldInfo = editableSchemaMetadata?.editableSchemaFieldInfo.find(
            (candidateEditableFieldInfo) => candidateEditableFieldInfo.fieldPath === record.fieldPath,
        );
        return (
            <TagGroup
                uneditableTags={tags}
                editableTags={relevantEditableFieldInfo?.globalTags}
                canRemove
                canAdd={tagHoveredIndex === rowIndex}
                onOpenModal={() => setTagHoveredIndex(undefined)}
                updateTags={(update) =>
                    onUpdateTags(update, relevantEditableFieldInfo || { fieldPath: record.fieldPath })
                }
            />
        );
    };

    const descriptionColumn = {
        title: 'Description',
        dataIndex: 'description',
        key: 'description',
        render: descriptionRender,
        width: 900,
        onCell: (record: SchemaField, rowIndex: number | undefined) => ({
            onMouseEnter: () => {
                setDescHoveredIndex(rowIndex);
            },
            onMouseLeave: () => {
                setDescHoveredIndex(undefined);
            },
        }),
    };

    const tagColumn = {
        width: 450,
        title: 'Tags',
        dataIndex: 'globalTags',
        key: 'tag',
        render: tagGroupRender,
        onCell: (record: SchemaField, rowIndex: number | undefined) => ({
            onMouseEnter: () => {
                setTagHoveredIndex(rowIndex);
            },
            onMouseLeave: () => {
                setTagHoveredIndex(undefined);
            },
        }),
    };

    return (
        <>
            {schema?.platformSchema?.__typename === 'TableSchema' && schema?.platformSchema?.schema?.length > 0 && (
                <ViewRawButtonContainer>
                    <Button onClick={() => setShowRaw(!showRaw)}>{showRaw ? 'Tabular' : 'Raw'}</Button>
                </ViewRawButtonContainer>
            )}
            {showRaw ? (
                <Typography.Text data-testid="schema-raw-view">
                    <pre>
                        <code>
                            {schema?.platformSchema?.__typename === 'TableSchema' &&
                                JSON.stringify(JSON.parse(schema.platformSchema.schema), null, 2)}
                        </code>
                    </pre>
                </Typography.Text>
            ) : (
                <Table
                    pagination={false}
                    dataSource={schema?.fields}
                    columns={[...defaultColumns, descriptionColumn, tagColumn]}
                    rowKey="fieldPath"
                />
            )}
        </>
    );
}
