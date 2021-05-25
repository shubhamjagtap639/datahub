import { Modal, Tag } from 'antd';
import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import styled from 'styled-components';

import { BookOutlined } from '@ant-design/icons';
import { useEntityRegistry } from '../../useEntityRegistry';
import { EntityType, GlobalTags, GlobalTagsUpdate, GlossaryTerms } from '../../../types.generated';
import { convertTagsForUpdate } from './utils/convertTagsForUpdate';
import AddTagModal from './AddTagModal';

type Props = {
    uneditableTags?: GlobalTags | null;
    editableTags?: GlobalTags | null;
    glossaryTerms?: GlossaryTerms | null;
    canRemove?: boolean;
    canAdd?: boolean;
    updateTags?: (update: GlobalTagsUpdate) => Promise<any>;
    onOpenModal?: () => void;
    maxShow?: number;
};

const AddNewTag = styled(Tag)`
    cursor: pointer;
`;

export default function TagTermGroup({
    uneditableTags,
    editableTags,
    canRemove,
    canAdd,
    updateTags,
    onOpenModal,
    maxShow,
    glossaryTerms,
}: Props) {
    const entityRegistry = useEntityRegistry();
    const [showAddModal, setShowAddModal] = useState(false);

    const removeTag = (urnToRemove: string) => {
        onOpenModal?.();
        const tagToRemove = editableTags?.tags?.find((tag) => tag.tag.urn === urnToRemove);
        const newTags = editableTags?.tags?.filter((tag) => tag.tag.urn !== urnToRemove);
        Modal.confirm({
            title: `Do you want to remove ${tagToRemove?.tag.name} tag?`,
            content: `Are you sure you want to remove the ${tagToRemove?.tag.name} tag?`,
            onOk() {
                updateTags?.({ tags: convertTagsForUpdate(newTags || []) });
            },
            onCancel() {},
            okText: 'Yes',
            maskClosable: true,
            closable: true,
        });
    };

    let renderedTags = 0;

    return (
        <div>
            {glossaryTerms?.terms?.map((term) => (
                <Link
                    to={`/${entityRegistry.getPathName(EntityType.GlossaryTerm)}/${term.term.urn}`}
                    key={term.term.urn}
                >
                    <Tag color="blue" closable={false}>
                        {term.term.name}
                        <BookOutlined style={{ marginLeft: '2%' }} />
                    </Tag>
                </Link>
            ))}
            {/* uneditable tags are provided by ingestion pipelines exclusively */}
            {uneditableTags?.tags?.map((tag) => {
                renderedTags += 1;
                if (maxShow && renderedTags > maxShow) return null;
                return (
                    <Link to={`/${entityRegistry.getPathName(EntityType.Tag)}/${tag.tag.urn}`} key={tag.tag.urn}>
                        <Tag color="blue" closable={false}>
                            {tag.tag.name}
                        </Tag>
                    </Link>
                );
            })}
            {/* editable tags may be provided by ingestion pipelines or the UI */}
            {editableTags?.tags?.map((tag) => {
                renderedTags += 1;
                if (maxShow && renderedTags > maxShow) return null;
                return (
                    <Link to={`/${entityRegistry.getPathName(EntityType.Tag)}/${tag.tag.urn}`} key={tag.tag.urn}>
                        <Tag
                            color="blue"
                            closable={canRemove}
                            onClose={(e) => {
                                e.preventDefault();
                                removeTag(tag.tag.urn);
                            }}
                        >
                            {tag.tag.name}
                        </Tag>
                    </Link>
                );
            })}
            {canAdd && (uneditableTags?.tags?.length || 0) + (editableTags?.tags?.length || 0) < 10 && (
                <>
                    <AddNewTag color="success" onClick={() => setShowAddModal(true)}>
                        + Add Tag
                    </AddNewTag>
                    {showAddModal && (
                        <AddTagModal
                            globalTags={editableTags}
                            updateTags={updateTags}
                            visible
                            onClose={() => {
                                onOpenModal?.();
                                setShowAddModal(false);
                            }}
                        />
                    )}
                </>
            )}
        </div>
    );
}
