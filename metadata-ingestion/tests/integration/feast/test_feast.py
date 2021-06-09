import pytest

from datahub.ingestion.run.pipeline import Pipeline
from tests.test_helpers import mce_helpers

# from datahub.ingestion.run.pipeline import Pipeline
# from tests.test_helpers import mce_helpers
from tests.test_helpers.docker_helpers import wait_for_port


# make sure that mock_time is excluded here because it messes with feast
@pytest.mark.slow
def test_feast_ingest(docker_compose_runner, pytestconfig, tmp_path):
    test_resources_dir = pytestconfig.rootpath / "tests/integration/feast"

    with docker_compose_runner(
        test_resources_dir / "docker-compose.yml", "feast"
    ) as docker_services:
        wait_for_port(docker_services, "testfeast", 6565)

        # container listens to this port once test cases have been setup
        wait_for_port(docker_services, "testfeast_setup", 6789)

        # Run the metadata ingestion pipeline.
        pipeline = Pipeline.create(
            {
                "run_id": "feast-test",
                "source": {
                    "type": "feast",
                    "config": {
                        "core_url": "localhost:6565",
                        "use_local_build": True,
                    },
                },
                "sink": {
                    "type": "file",
                    "config": {
                        "filename": f"{tmp_path}/feast_mces.json",
                    },
                },
            }
        )
        pipeline.run()
        pipeline.raise_from_status()

        # Verify the output.
        output = mce_helpers.load_json_file(str(tmp_path / "feast_mces.json"))
        golden = mce_helpers.load_json_file(
            str(test_resources_dir / "feast_mce_golden.json")
        )
        mce_helpers.assert_mces_equal(output, golden)
