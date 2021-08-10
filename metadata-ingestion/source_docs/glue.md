# Glue

For context on getting started with ingestion, check out our [metadata ingestion guide](../README.md).

## Setup

To install this plugin, run `pip install 'acryl-datahub[glue]'`.

Note: if you also have files in S3 that you'd like to ingest, we recommend you use Glue's built-in data catalog. See [here](../s3-ingestion.md) for a quick guide on how to set up a crawler on Glue and ingest the outputs with DataHub.

## Capabilities

This plugin extracts the following:

- Tables in the Glue catalog
- Column types associated with each table
- Table metadata, such as owner, description and parameters
- Jobs and their component transformations, data sources, and data sinks

## Quickstart recipe

Check out the following recipe to get started with ingestion! See [below](#config-details) for full configuration options.

For general pointers on writing and running a recipe, see our [main recipe guide](../README.md#recipes).

```yml
source:
  type: glue
  config:
    # Coordinates
    aws_region: "my-aws-region"

sink:
  # sink configs
```

## Config details

Note that a `.` is used to denote nested fields in the YAML recipe.

| Field                    | Required | Default                     | Description                                                                        |
| ------------------------ | -------- | --------------------------- | ---------------------------------------------------------------------------------- |
| `aws_region`             | ✅       |                             | AWS region code.                                                                   |
| `env`                    |          | `"PROD"`                    | Environment to use in namespace when constructing URNs.                            |
| `aws_access_key_id`      |          | Autodetected                | See https://boto3.amazonaws.com/v1/documentation/api/latest/guide/credentials.html |
| `aws_secret_access_key`  |          | Autodetected                | See https://boto3.amazonaws.com/v1/documentation/api/latest/guide/credentials.html |
| `aws_session_token`      |          | Autodetected                | See https://boto3.amazonaws.com/v1/documentation/api/latest/guide/credentials.html |
| `aws_role`               |          | Autodetected                | See https://boto3.amazonaws.com/v1/documentation/api/latest/guide/credentials.html |
| `extract_transforms`     |          | `True`                      | Whether to extract Glue transform jobs.                                            |
| `database_pattern.allow` |          |                             | Regex pattern for databases to include in ingestion.                               |
| `database_pattern.deny`  |          |                             | Regex pattern for databases to exclude from ingestion.                             |
| `table_pattern.allow`    |          |                             | Regex pattern for tables to include in ingestion.                                  |
| `table_pattern.deny`     |          |                             | Regex pattern for tables to exclude from ingestion.                                |
| `underlying_platform`    |          | `glue`                      | Override for platform name. Allowed values - `glue`, `athena`                      |

## Compatibility

Coming soon!

## Questions

If you've got any questions on configuring this source, feel free to ping us on [our Slack](https://slack.datahubproject.io/)!
