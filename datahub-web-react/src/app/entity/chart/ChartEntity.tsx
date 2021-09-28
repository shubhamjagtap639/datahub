import { LineChartOutlined } from '@ant-design/icons';
import * as React from 'react';
import { Chart, EntityType, PlatformType, SearchResult } from '../../../types.generated';
import { Direction } from '../../lineage/types';
import getChildren from '../../lineage/utils/getChildren';
import { Entity, IconStyleType, PreviewType } from '../Entity';
import { getLogoFromPlatform } from '../../shared/getLogoFromPlatform';
import { ChartPreview } from './preview/ChartPreview';
import { GetChartQuery, useGetChartQuery, useUpdateChartMutation } from '../../../graphql/chart.generated';
import { DocumentationTab } from '../shared/tabs/Documentation/DocumentationTab';
import { SidebarAboutSection } from '../shared/containers/profile/sidebar/SidebarAboutSection';
import { SidebarTagsSection } from '../shared/containers/profile/sidebar/SidebarTagsSection';
import { SidebarOwnerSection } from '../shared/containers/profile/sidebar/Ownership/SidebarOwnerSection';
import { GenericEntityProperties } from '../shared/types';
import { EntityProfile } from '../shared/containers/profile/EntityProfile';
import { PropertiesTab } from '../shared/tabs/Properties/PropertiesTab';
import { ChartInputsTab } from '../shared/tabs/Entity/ChartInputsTab';
import { ChartDashboardsTab } from '../shared/tabs/Entity/ChartDashboardsTab';

/**
 * Definition of the DataHub Chart entity.
 */
export class ChartEntity implements Entity<Chart> {
    type: EntityType = EntityType.Chart;

    icon = (fontSize: number, styleType: IconStyleType) => {
        if (styleType === IconStyleType.TAB_VIEW) {
            return <LineChartOutlined style={{ fontSize }} />;
        }

        if (styleType === IconStyleType.HIGHLIGHT) {
            return <LineChartOutlined style={{ fontSize, color: 'rgb(144 163 236)' }} />;
        }

        if (styleType === IconStyleType.SVG) {
            return (
                <path d="M888 792H200V168c0-4.4-3.6-8-8-8h-56c-4.4 0-8 3.6-8 8v688c0 4.4 3.6 8 8 8h752c4.4 0 8-3.6 8-8v-56c0-4.4-3.6-8-8-8zM305.8 637.7c3.1 3.1 8.1 3.1 11.3 0l138.3-137.6L583 628.5c3.1 3.1 8.2 3.1 11.3 0l275.4-275.3c3.1-3.1 3.1-8.2 0-11.3l-39.6-39.6a8.03 8.03 0 00-11.3 0l-230 229.9L461.4 404a8.03 8.03 0 00-11.3 0L266.3 586.7a8.03 8.03 0 000 11.3l39.5 39.7z" />
            );
        }

        return (
            <LineChartOutlined
                style={{
                    fontSize,
                    color: '#BFBFBF',
                }}
            />
        );
    };

    isSearchEnabled = () => true;

    isBrowseEnabled = () => true;

    isLineageEnabled = () => true;

    getAutoCompleteFieldName = () => 'title';

    getPathName = () => 'chart';

    getEntityName = () => 'Chart';

    getCollectionName = () => 'Charts';

    renderProfile = (urn: string) => (
        <EntityProfile
            urn={urn}
            entityType={EntityType.Chart}
            useEntityQuery={useGetChartQuery}
            useUpdateQuery={useUpdateChartMutation}
            getOverrideProperties={this.getOverrideProperties}
            tabs={[
                {
                    name: 'Documentation',
                    component: DocumentationTab,
                },
                {
                    name: 'Properties',
                    component: PropertiesTab,
                },
                {
                    name: 'Inputs',
                    component: ChartInputsTab,
                    shouldHide: (_, chart: GetChartQuery) => (chart?.chart?.inputs?.total || 0) === 0,
                },
                {
                    name: 'Dashboards',
                    component: ChartDashboardsTab,
                    shouldHide: (_, chart: GetChartQuery) => (chart?.chart?.dashboards?.total || 0) === 0,
                },
            ]}
            sidebarSections={[
                {
                    component: SidebarAboutSection,
                },
                {
                    component: SidebarTagsSection,
                    properties: {
                        hasTags: true,
                        hasTerms: true,
                    },
                },
                {
                    component: SidebarOwnerSection,
                },
            ]}
        />
    );

    getOverrideProperties = (res: GetChartQuery): GenericEntityProperties => {
        // TODO: Get rid of this once we have correctly formed platform coming back.
        const tool = res.chart?.tool || '';
        const name = res.chart?.info?.name;
        const externalUrl = res.chart?.info?.externalUrl;
        return {
            ...res,
            name,
            externalUrl,
            platform: {
                urn: `urn:li:dataPlatform:(${tool})`,
                type: EntityType.DataPlatform,
                name: tool,
                info: {
                    logoUrl: getLogoFromPlatform(tool),
                    type: PlatformType.Others,
                    datasetNameDelimiter: '.',
                },
            },
        };
    };

    renderPreview = (_: PreviewType, data: Chart) => {
        return (
            <ChartPreview
                urn={data.urn}
                platform={data.tool}
                name={data.info?.name}
                description={data.editableProperties?.description || data.info?.description}
                access={data.info?.access}
                owners={data.ownership?.owners}
                tags={data?.globalTags || undefined}
                glossaryTerms={data?.glossaryTerms}
            />
        );
    };

    renderSearch = (result: SearchResult) => {
        return this.renderPreview(PreviewType.SEARCH, result.entity as Chart);
    };

    getLineageVizConfig = (entity: Chart) => {
        return {
            urn: entity.urn,
            name: entity.info?.name || '',
            type: EntityType.Chart,
            upstreamChildren: getChildren({ entity, type: EntityType.Chart }, Direction.Upstream).map(
                (child) => child.entity.urn,
            ),
            downstreamChildren: getChildren({ entity, type: EntityType.Chart }, Direction.Downstream).map(
                (child) => child.entity.urn,
            ),
            icon: getLogoFromPlatform(entity.tool),
            platform: entity.tool,
        };
    };

    displayName = (data: Chart) => {
        return data.info?.name || data.urn;
    };
}
