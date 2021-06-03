import React from 'react';
import { EntityType, FabricType, Owner, GlobalTags, GlossaryTerms } from '../../../../types.generated';
import DefaultPreviewCard from '../../../preview/DefaultPreviewCard';
import { useEntityRegistry } from '../../../useEntityRegistry';
import { capitalizeFirstLetter } from '../../../shared/capitalizeFirstLetter';

export const Preview = ({
    urn,
    name,
    origin,
    description,
    platformName,
    platformLogo,
    owners,
    globalTags,
    snippet,
    glossaryTerms,
}: {
    urn: string;
    name: string;
    origin: FabricType;
    description?: string | null;
    platformName: string;
    platformLogo?: string | null;
    owners?: Array<Owner> | null;
    globalTags?: GlobalTags | null;
    snippet?: React.ReactNode | null;
    glossaryTerms?: GlossaryTerms | null;
}): JSX.Element => {
    const entityRegistry = useEntityRegistry();
    const capitalPlatformName = capitalizeFirstLetter(platformName);
    return (
        <DefaultPreviewCard
            url={entityRegistry.getEntityUrl(EntityType.Dataset, urn)}
            name={name || ''}
            description={description || ''}
            type="Dataset"
            logoUrl={platformLogo || ''}
            platform={capitalPlatformName}
            qualifier={origin}
            tags={globalTags || undefined}
            owners={owners}
            snippet={snippet}
            glossaryTerms={glossaryTerms || undefined}
        />
    );
};
