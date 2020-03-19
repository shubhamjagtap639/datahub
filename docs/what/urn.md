# What is URN?

URN ([Uniform Resource Name](https://en.wikipedia.org/wiki/Uniform_Resource_Name)) is the chosen scheme of [URI](https://en.wikipedia.org/wiki/Uniform_Resource_Identifier) to uniquely define any resource in DataHub. It has the following form:
```
urn:<Namespace>:<Entity Type>:<ID>
```
[Onboarding a new entity](../how/entity-onboarding.md) in [GMA](gma.md) starts with modelling an URN specific to that entity.
You can check URN models for already availabile entities [here](../../li-utils/src/main/java/com/linkedin/common/urn).

## Namespace
All URNs available in DataHub are using `li` as their namespace. 
This can be easily changed to a different namespace for your organization if you fork DataHub.

## Entity Type
Entity type for URN is different than [entity](entity.md) in GMA context. This can be thought of as the object type of
any resource for which you need unique identifier for its each instance. While you can create URNs for GMA entities such as
[DatasetUrn] with entity type `dataset`, you can also define URN for data platforms, [DataPlatformUrn].

## ID
ID is the unique identifier part of an URN. It's unique for a specific entity type within a specific namespace.
ID could contain a single field, or multi fields in the case of complex URNs. A complex URN can even contain other URNs as ID fields. This type of URN is also referred to as nested URN.

Here are some example URNs with a single ID field:

```
urn:li:dataPlatform:kafka
urn:li:corpuser:jdoe
```

[DatasetUrn](../../li-utils/src/main/java/com/linkedin/common/urn/DatasetUrn.java) is an example of a complex nested URN. It contains 3 ID fields: `platform`, `name` & `fabric`, where `platform` is another [URN](../../li-utils/src/main/java/com/linkedin/common/urn/DataPlatformUrn.java).

Below are some examples:
```
urn:li:dataset:(urn:li:dataPlatform:kafka,PageViewEvent,PROD)
urn:li:dataset:(urn:li:dataPlatform:hdfs,PageViewEvent,EI)
```
