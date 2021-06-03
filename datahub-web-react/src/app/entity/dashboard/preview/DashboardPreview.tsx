import React from 'react';
import { AccessLevel, EntityType, GlobalTags, Owner } from '../../../../types.generated';
import DefaultPreviewCard from '../../../preview/DefaultPreviewCard';
import { useEntityRegistry } from '../../../useEntityRegistry';
import { getLogoFromPlatform } from '../../../shared/getLogoFromPlatform';
import { capitalizeFirstLetter } from '../../../shared/capitalizeFirstLetter';

export const DashboardPreview = ({
    urn,
    name,
    description,
    platform,
    access,
    owners,
    tags,
}: {
    urn: string;
    platform: string;
    name?: string;
    description?: string | null;
    access?: AccessLevel | null;
    owners?: Array<Owner> | null;
    tags?: GlobalTags;
}): JSX.Element => {
    const entityRegistry = useEntityRegistry();
    const capitalizedPlatform = capitalizeFirstLetter(platform);

    return (
        <DefaultPreviewCard
            url={entityRegistry.getEntityUrl(EntityType.Dashboard, urn)}
            name={name || ''}
            description={description || ''}
            type="Dashboard"
            logoUrl={getLogoFromPlatform(platform) || ''}
            platform={capitalizedPlatform}
            qualifier={access}
            owners={owners}
            tags={tags}
        />
    );
};
