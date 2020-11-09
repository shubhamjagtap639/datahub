'use strict';

const EmberApp = require('ember-cli/lib/broccoli/ember-app');
const buildCliOptionsFor = require('../../configs/ember-cli-build-options');

module.exports = function(defaults) {
  const { options } = buildCliOptionsFor(EmberApp.env());
  const app = new EmberApp(defaults, options);

  return app.toTree();
};
