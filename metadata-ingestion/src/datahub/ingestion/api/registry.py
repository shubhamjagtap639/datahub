import importlib
import inspect
from typing import Dict, Generic, Type, TypeVar, Union

from datahub.configuration.common import ConfigurationError

T = TypeVar("T")


class Registry(Generic[T]):
    def __init__(self):
        self._mapping: Dict[str, Union[Type[T], Exception]] = {}

    def _register(self, key: str, tp: Union[Type[T], Exception]) -> None:
        if key in self._mapping:
            raise KeyError(f"key already in use - {key}")
        if key.find(".") >= 0:
            raise KeyError(f"key cannot contain '.' - {key}")
        self._mapping[key] = tp

    def register(self, key: str, cls: Type[T]) -> None:
        if inspect.isabstract(cls):
            raise ValueError("cannot register an abstract type in the registry")
        self._register(key, cls)

    def register_disabled(self, key: str, reason: Exception) -> None:
        self._register(key, reason)

    @property
    def mapping(self):
        return self._mapping

    def get(self, key: str) -> Type[T]:
        if key.find(".") >= 0:
            # If the key contains a dot, we treat it as a import path and attempt
            # to load it dynamically.
            module_name, class_name = key.rsplit(".", 1)
            MyClass = getattr(importlib.import_module(module_name), class_name)
            return MyClass

        if key not in self._mapping:
            raise KeyError(f"Did not find a registered class for {key}")
        tp = self._mapping[key]
        if isinstance(tp, Exception):
            raise ConfigurationError(f"{key} is disabled") from tp
        else:
            # If it's not an exception, then it's a registered type.
            return tp
