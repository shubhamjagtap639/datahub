import { Typography, message, Tag } from 'antd';
import { EditOutlined } from '@ant-design/icons';
import React, { useState } from 'react';
import styled from 'styled-components';
import { FetchResult } from '@apollo/client';

import { UpdateDatasetMutation } from '../../../../../graphql/dataset.generated';
import UpdateDescriptionModal from '../../../shared/DescriptionModal';
import MarkdownViewer from '../../../shared/MarkdownViewer';

const EditIcon = styled(EditOutlined)`
    cursor: pointer;
    padding: 2px;
    margin-top: 4px;
    margin-left: 8px;
    display: none;
`;

const AddNewDescription = styled(Tag)`
    cursor: pointer;
    display: none;
`;

const DescriptionContainer = styled.div`
    position: relative;
    display: flex;
    flex-direction: row;
    width: 100%;
    height: 100%;
    min-height: 22px;
    &:hover ${EditIcon} {
        display: block;
    }

    &:hover ${AddNewDescription} {
        display: block;
    }
`;

const DescriptionText = styled(MarkdownViewer)`
    padding-right: 8px;
`;

const EditedLabel = styled(Typography.Text)`
    position: absolute;
    right: -10px;
    top: -15px;
    color: rgba(150, 150, 150, 0.5);
    font-style: italic;
`;

type Props = {
    description: string;
    updatedDescription?: string | null;
    onUpdate: (
        description: string,
    ) => Promise<FetchResult<UpdateDatasetMutation, Record<string, any>, Record<string, any>> | void>;
};

export default function DescriptionField({ description, updatedDescription, onUpdate }: Props) {
    const [showAddModal, setShowAddModal] = useState(false);

    const onCloseModal = () => setShowAddModal(false);
    const currentDesc: string = updatedDescription || description;

    const onUpdateModal = async (desc: string | null) => {
        message.loading({ content: 'Updating...' });
        try {
            await onUpdate(desc || '');
            message.destroy();
            message.success({ content: 'Updated!', duration: 2 });
        } catch (e) {
            message.destroy();
            message.error({ content: `Update Failed! \n ${e.message || ''}`, duration: 2 });
        }
        onCloseModal();
    };

    return (
        <DescriptionContainer
            onClick={(e) => {
                e.preventDefault();
                e.stopPropagation();
            }}
        >
            <DescriptionText source={currentDesc} />
            {currentDesc && <EditIcon twoToneColor="#52c41a" onClick={() => setShowAddModal(true)} />}
            {updatedDescription && <EditedLabel>(edited)</EditedLabel>}
            {showAddModal && (
                <div>
                    <UpdateDescriptionModal
                        title={currentDesc ? 'Update description' : 'Add description'}
                        description={currentDesc}
                        original={description}
                        onClose={onCloseModal}
                        onSubmit={onUpdateModal}
                        isAddDesc={!currentDesc}
                    />
                </div>
            )}
            {!currentDesc && (
                <AddNewDescription color="success" onClick={() => setShowAddModal(true)}>
                    + Add Description
                </AddNewDescription>
            )}
        </DescriptionContainer>
    );
}
