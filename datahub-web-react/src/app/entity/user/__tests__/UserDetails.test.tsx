import React from 'react';
import { render } from '@testing-library/react';
import UserDetails from '../UserDetails';
import { EntityType, PlatformNativeType } from '../../../../types.generated';
import TestPageContainer from '../../../../utils/test-utils/TestPageContainer';
import { Subview } from '../Subview';

const ownerships = {
    [EntityType.Dataset]: [
        {
            entity: {
                name: 'HiveDataset',
                origin: 'PROD',
                description: 'this is a dataset',
                platformNativeType: PlatformNativeType.Table,
                platform: {
                    name: 'hive',
                },
                tags: [],
            },
        },
        {
            entity: {
                name: 'KafkaDataset',
                origin: 'PROD',
                description: 'this is also a dataset',
                platformNativeType: PlatformNativeType.Table,
                platform: {
                    name: 'kafka',
                },
                tags: [],
            },
        },
    ],
};

describe('UserDetails', () => {
    it('renders a menu with the ownership submenu and datasets option', () => {
        const { getByText } = render(
            <TestPageContainer>
                <UserDetails urn="some:urn" ownerships={ownerships} />;
            </TestPageContainer>,
        );
        expect(getByText('Ownership')).toBeInTheDocument();
        expect(getByText('Datasets')).toBeInTheDocument();
    });

    it('will not the show the ownership details by default', () => {
        const { queryByText } = render(
            <TestPageContainer>
                <UserDetails urn="some:urn" ownerships={ownerships} />;
            </TestPageContainer>,
        );
        expect(queryByText('Datasets owned')).not.toBeInTheDocument();
    });

    it('will the show the ownership details when selected', () => {
        const { getByText } = render(
            <TestPageContainer>
                <UserDetails urn="some:urn" ownerships={ownerships} subview={Subview.Ownership} item="dataset" />;
            </TestPageContainer>,
        );
        expect(getByText('Datasets owned')).toBeInTheDocument();
    });
});
