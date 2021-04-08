import logging
from typing import Any, List

import avro.schema

from datahub.metadata.com.linkedin.pegasus2avro.schema import (
    ArrayTypeClass,
    BooleanTypeClass,
    BytesTypeClass,
    EnumTypeClass,
    FixedTypeClass,
    MapTypeClass,
    NullTypeClass,
    NumberTypeClass,
    RecordTypeClass,
    SchemaField,
    SchemaFieldDataType,
    StringTypeClass,
    UnionTypeClass,
)

"""A helper file for Avro schema -> MCE schema transformations"""

logger = logging.getLogger(__name__)

_field_type_mapping = {
    "null": NullTypeClass,
    "bool": BooleanTypeClass,
    "boolean": BooleanTypeClass,
    "int": NumberTypeClass,
    "long": NumberTypeClass,
    "float": NumberTypeClass,
    "double": NumberTypeClass,
    "bytes": BytesTypeClass,
    "string": StringTypeClass,
    "record": RecordTypeClass,
    "map": MapTypeClass,
    "enum": EnumTypeClass,
    "array": ArrayTypeClass,
    "union": UnionTypeClass,
    "fixed": FixedTypeClass,
}


def _get_column_type(field_type) -> SchemaFieldDataType:
    tp = field_type
    if hasattr(tp, "type"):
        tp = tp.type
    tp = str(tp)
    TypeClass: Any = _field_type_mapping.get(tp)
    # Note: we could populate the nestedTypes field for unions and similar fields
    # for the other types as well. However, since we already populate the nativeDataType
    # field below, it is mostly ok to leave this as not fully initialized.
    dt = SchemaFieldDataType(type=TypeClass())
    return dt


def _is_nullable(field):
    if isinstance(field.type, avro.schema.UnionSchema):
        if any(schema.name == "null" for schema in field.type.schemas):
            return True
        else:
            return False
    elif isinstance(field.type, avro.schema.PrimitiveSchema):
        return field.type.name == "null"
    else:
        return False


def avro_schema_to_mce_fields(avro_schema_string: str) -> List[SchemaField]:
    """Converts an avro schema into a schema compatible with MCE"""

    # Handle some library compatability issues.
    if hasattr(avro.schema, "parse"):
        schema_parse_fn = avro.schema.parse
    else:
        schema_parse_fn = avro.schema.Parse

    parsed_schema: avro.schema.RecordSchema = schema_parse_fn(avro_schema_string)

    fields: List[SchemaField] = []
    for parsed_field in parsed_schema.fields:
        field = SchemaField(
            fieldPath=parsed_field.name,
            nativeDataType=str(parsed_field.type),
            type=_get_column_type(parsed_field.type),
            description=parsed_field.props.get("doc", None),
            recursive=False,
            nullable=_is_nullable(parsed_field),
        )

        fields.append(field)

    return fields
