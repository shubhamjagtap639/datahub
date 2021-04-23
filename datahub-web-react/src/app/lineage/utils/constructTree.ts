import EntityRegistry from '../../entity/EntityRegistry';
import { Direction, EntityAndType, FetchedEntities, NodeData } from '../types';
import constructFetchedNode from './constructFetchedNode';
import getChildren from './getChildren';

export default function constructTree(
    entityAndType: EntityAndType | null | undefined,
    fetchedEntities: FetchedEntities,
    direction: Direction,
    entityRegistry: EntityRegistry,
): NodeData {
    if (!entityAndType?.entity) return { name: 'loading...', children: [] };
    const constructedNodes = {};

    const fetchedEntity = entityRegistry.getLineageVizConfig(entityAndType.type, entityAndType.entity);

    const root: NodeData = {
        name: fetchedEntity?.name || '',
        urn: fetchedEntity?.urn,
        type: fetchedEntity?.type,
        icon: fetchedEntity?.icon,
        platform: fetchedEntity?.platform,
        unexploredChildren: 0,
    };
    root.children = getChildren(entityAndType, direction)
        .map((child) => {
            if (child.entity.urn === root.urn) {
                return null;
            }
            return constructFetchedNode(child.entity.urn, fetchedEntities, direction, constructedNodes, [
                root.urn || '',
            ]);
        })
        ?.filter(Boolean) as Array<NodeData>;
    return root;
}
