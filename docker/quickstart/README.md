# DataHub Quickstart
To start all Docker containers at once, please run below command:
```
cd docker/quickstart && docker-compose pull && docker-compose up --build
```
At this point, all containers are ready and DataHub can be considered up and running. Check specific containers guide
for details:
* [Elasticsearch & Kibana](../elasticsearch)
* [DataHub Frontend](../frontend)
* [DataHub GMS](../gms)
* [Kafka, Schema Registry & Zookeeper](../kafka)
* [DataHub MAE Consumer](../mae-consumer)
* [DataHub MCE Consumer](../mce-consumer)
* [MySQL](../mysql) 

From this point on, if you want to be able to sign in to DataHub and see some sample data, please see 
[Metadata Ingestion Guide](../../metadata-ingestion) for `bootstrapping DataHub`.

## Debugging Containers
If you want to debug containers, you can check container logs:
```
docker logs <<container_name>>
```
Also, you can connect to container shell for further debugging:
```
docker exec -it <<container_name>> bash
```
