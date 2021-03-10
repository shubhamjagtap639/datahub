import React from 'react';
import { Layout } from 'antd';
import { useHistory } from 'react-router';
import { useTheme } from 'styled-components';

import { SearchHeader } from './SearchHeader';
import { useEntityRegistry } from '../useEntityRegistry';
import { useGetAutoCompleteResultsLazyQuery } from '../../graphql/search.generated';
import { navigateToSearchUrl } from './utils/navigateToSearchUrl';
import { useGetAuthenticatedUser } from '../useGetAuthenticatedUser';

const styles = {
    children: { marginTop: 80 },
};

interface Props extends React.PropsWithChildren<any> {
    initialQuery?: string;
    onSearch?: (query: string) => void;
    onAutoComplete?: (query: string) => void;
}

const defaultProps = {
    initialQuery: '',
    onSearch: undefined,
    onAutoComplete: undefined,
};

/**
 * A page that includes a sticky search header (nav bar)
 */
export const SearchablePage = ({ initialQuery, onSearch, onAutoComplete, children }: Props) => {
    const history = useHistory();
    const entityRegistry = useEntityRegistry();
    const themeConfig = useTheme();

    const { data: userData } = useGetAuthenticatedUser();
    const [getAutoCompleteResults, { data: suggestionsData }] = useGetAutoCompleteResultsLazyQuery();

    const search = (query: string) => {
        navigateToSearchUrl({
            query,
            history,
            entityRegistry,
        });
    };

    const autoComplete = (query: string) => {
        getAutoCompleteResults({
            variables: {
                input: {
                    type: entityRegistry.getDefaultSearchEntityType(),
                    query,
                },
            },
        });
    };

    return (
        <Layout>
            <SearchHeader
                initialQuery={initialQuery as string}
                placeholderText={themeConfig.content.search.searchbarMessage}
                suggestions={
                    (suggestionsData && suggestionsData?.autoComplete && suggestionsData.autoComplete.suggestions) || []
                }
                onSearch={onSearch || search}
                onQueryChange={onAutoComplete || autoComplete}
                authenticatedUserUrn={userData?.corpUser?.urn || ''}
                authenticatedUserPictureLink={userData?.corpUser?.editableInfo?.pictureLink}
            />
            <div style={styles.children}>{children}</div>
        </Layout>
    );
};

SearchablePage.defaultProps = defaultProps;
