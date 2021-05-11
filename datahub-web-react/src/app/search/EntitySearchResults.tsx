import React, { useState, useEffect } from 'react';
import { FilterOutlined } from '@ant-design/icons';
import styled from 'styled-components';
import { Alert, Button, Card, Divider, Drawer, List, Pagination, Row, Typography } from 'antd';
import { ListProps } from 'antd/lib/list';
import { SearchCfg } from '../../conf';
import { useGetSearchResultsQuery } from '../../graphql/search.generated';
import { EntityType, FacetFilterInput, SearchResult } from '../../types.generated';
import { IconStyleType } from '../entity/Entity';
import { Message } from '../shared/Message';
import { useEntityRegistry } from '../useEntityRegistry';
import { SearchFilters } from './SearchFilters';
import { filtersToGraphqlParams } from './utils/filtersToGraphqlParams';
import analytics, { EventType } from '../analytics';

const styles = {
    loading: { marginTop: '10%' },
    resultSummary: { color: 'gray', marginTop: '36px' },
    resultHeaderCardBody: { padding: '16px 24px' },
    resultHeaderCard: { right: '52px', top: '-40px', position: 'absolute' },
    paginationRow: { padding: 40 },
    resultsContainer: { width: '100%', padding: '20px 132px' },
};

const ResultList = styled(List)`
    &&& {
        width: 100%;
        border-color: ${(props) => props.theme.styles['border-color-base']};
        margin-top: 12px;
        padding: 16px 32px;
        box-shadow: ${(props) => props.theme.styles['box-shadow']};
    }
`;

const ApplyButton = styled(Button)`
    && {
        margin: 20px 25px 0 25px;
    }
`;

interface Props {
    type: EntityType;
    query: string;
    page: number;
    filters: Array<FacetFilterInput>;
    onChangeFilters: (filters: Array<FacetFilterInput>) => void;
    onChangePage: (page: number) => void;
}

export const EntitySearchResults = ({ type, query, page, filters, onChangeFilters, onChangePage }: Props) => {
    const [isEditingFilters, setIsEditingFilters] = useState(false);
    const [selectedFilters, setSelectedFilters] = useState(filters);
    useEffect(() => {
        setSelectedFilters(filters);
    }, [filters]);

    const entityRegistry = useEntityRegistry();
    const { loading, error, data } = useGetSearchResultsQuery({
        variables: {
            input: {
                type,
                query,
                start: (page - 1) * SearchCfg.RESULTS_PER_PAGE,
                count: SearchCfg.RESULTS_PER_PAGE,
                filters: filtersToGraphqlParams(filters),
            },
        },
    });

    const results = data?.search?.searchResults || [];
    const pageStart = data?.search?.start || 0;
    const pageSize = data?.search?.count || 0;
    const totalResults = data?.search?.total || 0;
    const lastResultIndex =
        pageStart * pageSize + pageSize > totalResults ? totalResults : pageStart * pageSize + pageSize;

    useEffect(() => {
        if (!loading && !error) {
            analytics.event({
                type: EventType.SearchResultsViewEvent,
                page,
                query,
                entityTypeFilter: type,
                total: totalResults,
            });
        }
    }, [page, query, totalResults, type, loading, error, data]);

    const onFilterSelect = (selected: boolean, field: string, value: string) => {
        const newFilters = selected
            ? [...selectedFilters, { field, value }]
            : selectedFilters.filter((filter) => filter.field !== field || filter.value !== value);
        setSelectedFilters(newFilters);
    };

    const onEditFilters = () => {
        setIsEditingFilters(true);
    };

    const onApplyFilters = () => {
        onChangeFilters(selectedFilters);
        setIsEditingFilters(false);
    };

    const onCloseEditFilters = () => {
        setIsEditingFilters(false);
        setSelectedFilters(filters);
    };

    const onResultClick = (result: SearchResult, index: number) => {
        analytics.event({
            type: EventType.SearchResultClickEvent,
            query,
            entityUrn: result.entity.urn,
            entityType: result.entity.type,
            entityTypeFilter: type,
            index,
            total: totalResults,
        });
    };

    if (error || (!loading && !error && !data)) {
        return <Alert type="error" message={error?.message || 'Entity failed to load'} />;
    }

    return (
        <div style={styles.resultsContainer}>
            {loading && <Message type="loading" content="Loading..." style={styles.loading} />}
            <Button onClick={onEditFilters} data-testid="filters-button">
                <FilterOutlined />
                Filters{' '}
                {filters.length > 0 && (
                    <>
                        {' '}
                        (<b>{filters.length}</b>)
                    </>
                )}
            </Button>
            <Drawer title="Filters" placement="left" closable visible={isEditingFilters} onClose={onCloseEditFilters}>
                <SearchFilters
                    facets={data?.search?.facets || []}
                    selectedFilters={selectedFilters}
                    onFilterSelect={onFilterSelect}
                />
                <ApplyButton onClick={onApplyFilters}>Apply</ApplyButton>
            </Drawer>
            <Typography.Paragraph style={styles.resultSummary}>
                Showing{' '}
                <b>
                    {(page - 1) * pageSize} - {lastResultIndex}
                </b>{' '}
                of <b>{totalResults}</b> results
            </Typography.Paragraph>
            <ResultList<React.FC<ListProps<SearchResult>>>
                header={
                    <Card bodyStyle={styles.resultHeaderCardBody} style={styles.resultHeaderCard as any}>
                        {entityRegistry.getIcon(type, 36, IconStyleType.ACCENT)}
                    </Card>
                }
                dataSource={results}
                split={false}
                renderItem={(searchResult, index) => (
                    <>
                        <List.Item onClick={() => onResultClick(searchResult, index)}>
                            {entityRegistry.renderSearchResult(type, searchResult)}
                        </List.Item>
                        {index < results.length - 1 && <Divider />}
                    </>
                )}
                bordered
            />
            <Row justify="center" style={styles.paginationRow}>
                <Pagination
                    current={page}
                    pageSize={SearchCfg.RESULTS_PER_PAGE}
                    total={totalResults}
                    showLessItems
                    onChange={onChangePage}
                    showSizeChanger={false}
                />
            </Row>
        </div>
    );
};
