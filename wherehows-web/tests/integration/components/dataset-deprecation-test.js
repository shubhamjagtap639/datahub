import { module, test } from 'qunit';
import { setupRenderingTest } from 'ember-qunit';
import { render, find, findAll, click, fillIn } from '@ember/test-helpers';
import notificationsStub from 'wherehows-web/tests/stubs/services/notifications';
import hbs from 'htmlbars-inline-precompile';
import { run } from '@ember/runloop';

module('Integration | Component | dataset deprecation', function(hooks) {
  setupRenderingTest(hooks);

  test('it renders', async function(assert) {
    assert.expect(4);

    await render(hbs`{{dataset-deprecation}}`);

    assert.ok(
      document.querySelector('.dataset-deprecation-toggle__toggle-header__label'),
      'it shows the dataset is deprecation label element'
    );
    assert.equal(findAll('#dataset-is-deprecated').length, 1, 'has one input checkbox with known selector');
    assert.equal(
      find('#dataset-is-deprecated').getAttribute('type'),
      'checkbox',
      'has an input checkbox to toggle deprecation'
    );
    assert.equal(findAll('.dataset-deprecation-toggle__actions').length, 1, 'has an actions container');
  });

  test('setting the deprecated property should toggle the checkbox', async function(assert) {
    assert.expect(2);

    this.set('deprecated', true);

    await render(hbs`{{dataset-deprecation deprecated=deprecated}}`);

    assert.ok(this.$('#dataset-is-deprecated').is(':checked'), 'checkbox is checked when property is set true');

    this.set('deprecated', false);
    assert.notOk(this.$('#dataset-is-deprecated').is(':checked'), 'checkbox is unchecked when property is set false');
  });

  test('decommissionTime', async function(assert) {
    let isDisabled;
    assert.expect(2);

    this.set('decommissionTime', void 0);
    this.set('deprecated', true);

    await render(hbs`{{dataset-deprecation deprecated=deprecated decommissionTime=decommissionTime}}`);
    isDisabled = this.$('.dataset-deprecation-toggle__actions [type=submit]').is(':disabled');
    assert.ok(isDisabled, 'submit button is disabled');

    this.setProperties({ decommissionTime: new Date(), isDirty: true });
    await render(hbs`{{dataset-deprecation deprecated=deprecated decommissionTime=decommissionTime}}`);
    await fillIn('.comment-new__content', 'text');

    isDisabled = this.$('.dataset-deprecation-toggle__actions [type=submit]').is(':disabled');
    assert.notOk(isDisabled, 'submit button is not disabled');
  });

  test('triggers the onUpdateDeprecation action when submitted', async function(assert) {
    let submitActionCallCount = 0;

    this.set('submit', function(deprecated, note) {
      submitActionCallCount++;
      assert.equal(deprecated, true, 'action is called with deprecation value of true');
      assert.equal(note, '', 'action is called with an empty deprecation note');
    });
    this.set('decommissionTime', new Date());

    await render(hbs`{{dataset-deprecation onUpdateDeprecation=(action submit) decommissionTime=decommissionTime}}`);

    assert.equal(submitActionCallCount, 0, 'action is not called on render');
    assert.equal(this.$('#dataset-is-deprecated').is(':checked'), false, 'deprecation checkbox is unchecked');

    await click('#dataset-is-deprecated');

    assert.equal(this.$('#dataset-is-deprecated').is(':checked'), true, 'deprecation checkbox is checked');
    await click('.dataset-deprecation-toggle__actions [type=submit]');

    assert.equal(submitActionCallCount, 1, 'action is called once');
  });
});
