import { Direction, EntityAndType } from '../types';
import { EntityType } from '../../../types.generated';

export default function getChildren(entityAndType: EntityAndType, direction: Direction | null): Array<EntityAndType> {
    if (direction === Direction.Upstream) {
        if (
            entityAndType.type === EntityType.Mlfeature ||
            entityAndType.type === EntityType.MlprimaryKey ||
            entityAndType.type === EntityType.MlfeatureTable
        ) {
            return [];
        }

        return (
            entityAndType.entity.upstreamLineage?.entities?.map(
                (entity) =>
                    ({
                        type: entity?.entity?.type,
                        entity: entity?.entity,
                    } as EntityAndType),
            ) || []
        );
    }
    if (direction === Direction.Downstream) {
        if (entityAndType.type === EntityType.MlfeatureTable) {
            const entities = [
                ...(entityAndType.entity.featureTableProperties?.mlFeatures || []),
                ...(entityAndType.entity.featureTableProperties?.mlPrimaryKeys || []),
            ];
            return (
                entities.map(
                    (entity) =>
                        ({
                            type: entity?.type,
                            entity,
                        } as EntityAndType),
                ) || []
            );
        }
        if (entityAndType.type === EntityType.Mlfeature) {
            return (
                (entityAndType.entity.featureProperties?.sources || []).map(
                    (entity) =>
                        ({
                            type: entity?.type,
                            entity,
                        } as EntityAndType),
                ) || []
            );
        }
        if (entityAndType.type === EntityType.MlprimaryKey) {
            return (
                (entityAndType.entity.primaryKeyProperties?.sources || []).map(
                    (entity) =>
                        ({
                            type: entity?.type,
                            entity,
                        } as EntityAndType),
                ) || []
            );
        }
        return (
            entityAndType.entity.downstreamLineage?.entities?.map(
                (entity) =>
                    ({
                        type: entity?.entity?.type,
                        entity: entity?.entity,
                    } as EntityAndType),
            ) || []
        );
    }

    return [];
}
