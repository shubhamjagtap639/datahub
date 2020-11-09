# @dh-tools/eslint-plugin

Custom ES-Lint rules for datahub.

## Installation

You should not worry about this, since the scope of this addon is aimed only for @datahub and should be already installed. But just in case you wonder:

You'll first need to install [ESLint](http://eslint.org):

```sh
npm i eslint --save-dev
```

Next, install `@dh-tools/eslint-plugin`:

```sh
npm install @dh-tools/eslint-plugin --save-dev
```

**Note:** If you installed ESLint globally (using the `-g` flag) then you must also install `@dh-tools/eslint-plugin` globally.

## Usage

Add `@datahub` to the plugins section of your `.eslintrc` configuration file. You can omit the `eslint-plugin` part:

```json
{
  "plugins": ["@datahub"]
}
```

Then configure the rules you want to use under the rules section.

```json
{
  "rules": {
    "@dh-tools/no-addon-import": "error"
  }
}
```

## Supported Rules

- Fill in provided rules here

["@dh-tools/no-addon-import"](docs/rules/no-addon-import.md)

["@dh-tools/no-linkedin-import"](docs/rules/no-linkedin-import.md)
