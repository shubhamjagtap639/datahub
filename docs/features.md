# DataHub Features

DataHub is made up of a [generic backend](what/gma.md) and a [React-based UI](../datahub-web-react/README.md).
Original DataHub [blog post](https://engineering.linkedin.com/blog/2019/data-hub) talks about the design extensively and mentions some of the features of DataHub.
Our open sourcing [blog post](https://engineering.linkedin.com/blog/2020/open-sourcing-datahub--linkedins-metadata-search-and-discovery-p) also provides a comparison of some features between LinkedIn production DataHub vs open source DataHub. Below is a list of the latest features that are available in DataHub, as well as ones that will soon become available.

## Data Constructs (Entities)

### Datasets
 - **Search**: full-text & advanced search, search ranking
 - **Browse**: browsing through a configurable hierarchy
 - **Schema**: table & document schema in tabular and JSON format
 - **Coarse grain lineage**: support for lineage at the dataset level, tabular & graphical visualization of downstreams/upstreams
 - **Ownership**: surfacing owners of a dataset, viewing datasets you own
 - **Dataset life-cycle management**: deprecate/undeprecate, surface removed datasets and tag it with "removed"
 - **Institutional knowledge**: support for adding free form doc to any dataset
 - **Fine grain lineage**: support for lineage at the field level [*coming soon*]
 - **Social actions**: likes, follows, bookmarks [*coming soon*]
 - **Compliance management**: field level tag based compliance editing [*coming soon*]
 - **Top users**: frequent users of a dataset [*coming soon*]

### Tags
 - **Globally defined**: Tags provided a standardized set of labels that can be shared across all your entities
 - **Supports entities and schemas**: Tags can be applied at the entity level or for datasets, attached to schema fields.
 - **Searchable** Entities can be searched and filtered by tag
 
### Users
 - **Search**: full-text & advanced search, search ranking
 - **Browse**: browsing through a configurable hierarchy [*coming soon*]
 - **Profile editing**: LinkedIn style professional profile editing such as summary, skills

### Dashboards
- **Search**: full-text & advanced search, search ranking
- **Basic information**: ownership, location. Link to external service for viewing the dashboard.
- **Institutional knowledge**: support for adding free form doc to any dashboards [*coming soon*]

### Schemas [*coming soon*]
 - **Search**: full-text & advanced search, search ranking
 - **Browse**: browsing through a configurable hierarchy
 - **Schema history**: view and diff historic versions of schemas
 - **GraphQL**: visualization of GraphQL schemas

### Jos/flows [*coming soon*]
 - **Search**: full-text & advanced search, search ranking
 - **Browse**: browsing through a configurable hierarchy
 - **Basic information**: 
 - **Execution history**: Executions and their status. Link to external service for viewing full info.

### Metrics [*coming soon*]
 - **Search**: full-text & advanced search, search ranking
 - **Browse**: browsing through a configurable hierarchy
 - **Basic information**: ownershp, dimensions, formula, input & output datasets, dashboards
 - **Institutional knowledge**: support for adding free form doc to any metric

## Metadata Sources
There's a basic, Java-oriented overview of [metadata ingestion](architecture/metadata-ingestion.md).

We also have a [Python-based ingestion framework](../metadata-ingestion/README.md) which supports the following sources:
 - Hive
 - Kafka
 - RDBMS (MySQL, Oracle, Postgres, MS SQL, etc)
 - Data warehouse (Snowflake, BigQuery, etc)
 - LDAP

That ingestion framework is extensible, so you can easily create new sources of metadata. You just need to transform the metadata into our standard [MCE](what/mxe.md) format, and the framework will help ingest metadata to DataHub.
