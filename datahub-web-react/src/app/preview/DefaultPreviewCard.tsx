import { Divider, Image, Row, Space, Tag, Typography } from 'antd';
import React from 'react';
import { Link } from 'react-router-dom';
import styled from 'styled-components';
import { GlobalTags, Owner } from '../../types.generated';
import { useEntityRegistry } from '../useEntityRegistry';
import AvatarsGroup from '../shared/avatar/AvatarsGroup';
import TagGroup from '../shared/tags/TagGroup';

interface Props {
    name: string;
    logoUrl?: string;
    logoComponent?: JSX.Element;
    url: string;
    description: string;
    type?: string;
    platform?: string;
    qualifier?: string | null;
    tags?: GlobalTags;
    owners?: Array<Owner> | null;
    snippet?: React.ReactNode;
}

const DescriptionParagraph = styled(Typography.Paragraph)`
    &&& {
        margin-bottom: 0px;
        padding-left: 8px;
    }
`;

const PreviewImage = styled(Image)`
    max-height: 48px;
    width: auto;
    object-fit: contain;
`;

const styles = {
    row: { width: '100%', marginBottom: '0px' },
    leftColumn: { maxWidth: '75%' },
    rightColumn: { maxWidth: '25%' },
    name: { fontSize: '18px' },
    typeName: { color: '#585858' },
    platformName: { color: '#585858' },
    ownedBy: { color: '#585858' },
};

export default function DefaultPreviewCard({
    name,
    logoUrl,
    logoComponent,
    url,
    description,
    type,
    platform,
    qualifier,
    tags,
    owners,
    snippet,
}: Props) {
    const entityRegistry = useEntityRegistry();

    return (
        <Row style={styles.row} justify="space-between">
            <Space direction="vertical" align="start" size={28} style={styles.leftColumn}>
                <Link to={url}>
                    <Space direction="horizontal" size={20} align="center">
                        {logoUrl ? <PreviewImage src={logoUrl} preview /> : logoComponent || ''}

                        <Space direction="vertical" size={8}>
                            <Typography.Text strong style={styles.name}>
                                {name}
                            </Typography.Text>
                            {(type || platform || qualifier) && (
                                <Space split={<Divider type="vertical" />} size={16}>
                                    <Typography.Text>{type}</Typography.Text>
                                    <Typography.Text strong>{platform}</Typography.Text>
                                    {qualifier && <Tag>{qualifier}</Tag>}
                                </Space>
                            )}
                        </Space>
                    </Space>
                </Link>
                <div>
                    {description.length === 0 ? (
                        <DescriptionParagraph type="secondary">No description</DescriptionParagraph>
                    ) : (
                        <DescriptionParagraph>{description}</DescriptionParagraph>
                    )}
                    {snippet}
                </div>
            </Space>
            <Space direction="vertical" align="end" size={36} style={styles.rightColumn}>
                <Space direction="vertical" size={12}>
                    <Typography.Text strong>{owners && owners.length > 0 ? 'Owned By' : ''}</Typography.Text>
                    <AvatarsGroup owners={owners} entityRegistry={entityRegistry} maxCount={4} />
                </Space>
                <TagGroup editableTags={tags} maxShow={3} />
            </Space>
        </Row>
    );
}
