import Component from '@ember/component';
import ComputedProperty, { equal, not } from '@ember/object/computed';
import { getProperties, computed } from '@ember/object';
import { assert } from '@ember/debug';

import { IOwner } from 'wherehows-web/typings/api/datasets/owners';
import { OwnerSource, OwnerType } from 'wherehows-web/utils/api/datasets/owners';
import { action } from '@ember-decorators/object';

/**
 * This component renders a single owner record and also provides functionality for interacting with the component
 * in the ui or performing operations on a single owner record
 * @export
 * @class DatasetAuthor
 * @extends {Component}
 */
export default class DatasetAuthor extends Component {
  tagName = 'tr';

  classNames = ['dataset-author-record'];

  classNameBindings = [
    'isConfirmedSuggestedOwner:dataset-author-record--disabled',
    'isOwnerInActive:dataset-author-record--inactive'
  ];

  /**
   * The owner record being rendered
   * @type {IOwner}
   * @memberof DatasetAuthor
   */
  owner: IOwner;

  /**
   * List of suggested owners that have been confirmed by a user
   * @type {Array<IOwner>}
   * @memberof DatasetAuthor
   */
  commonOwners: Array<IOwner>;

  /**
   * External action to handle owner removal from the confirmed list
   * @param {IOwner} owner the owner to be removed
   * @memberof DatasetAuthor
   */
  removeOwner: (owner: IOwner) => IOwner | void;

  /**
   * External action to handle owner addition to the confirmed list
   * @param {IOwner} owner the suggested owner to be confirmed
   * @return {Array<IOwner> | void} the list of owners or void if unsuccessful
   * @memberof DatasetAuthor
   */
  confirmSuggestedOwner: (owner: IOwner) => Array<IOwner> | void;

  /**
   * External action to handle owner property updates, currently on the confirmed list
   * @param {IOwner} owner the owner to update
   * @param {OwnerType} type the type of the owner
   * @memberof DatasetAuthor
   */
  updateOwnerType: (owner: IOwner, type: OwnerType) => void;

  /**
   * A list of available owner types retrieved from the api
   * @type {Array<string>}
   * @memberof DatasetAuthor
   */
  ownerTypes: Array<string>;

  /**
   * Compares the source attribute on an owner, if it matches the OwnerSource.Ui type
   * @type {ComputedProperty<boolean>}
   * @memberof DatasetAuthor
   */
  isOwnerMutable: ComputedProperty<boolean> = equal('owner.source', OwnerSource.Ui);

  /**
   * Negates the owner attribute flag `isActive`, indicating owner record is considered inactive
   * @type {ComputedProperty<boolean>}
   * @memberOf DatasetAuthor
   */
  isOwnerInActive: ComputedProperty<boolean> = not('owner.isActive');

  /**
   * Determines if the owner record is a system suggested owner and if this record is confirmed by a user
   * @type {ComputedProperty<boolean>}
   * @memberof DatasetAuthor
   */
  isConfirmedSuggestedOwner: ComputedProperty<boolean> = computed('commonOwners', function(
    this: DatasetAuthor
  ): boolean {
    const {
      commonOwners,
      isOwnerMutable,
      owner: { userName }
    } = getProperties(this, ['commonOwners', 'isOwnerMutable', 'owner']);

    return isOwnerMutable ? false : !!commonOwners.findBy('userName', userName);
  });

  constructor() {
    super(...arguments);
    const typeOfRemoveOwner = typeof this.removeOwner;
    const typeOfConfirmSuggestedOwner = typeof this.confirmSuggestedOwner;

    // Checks that the expected external actions are provided
    assert(
      `Expected action removeOwner to be a function (Ember action), got ${typeOfRemoveOwner}`,
      typeOfRemoveOwner === 'function'
    );

    assert(
      `Expected action confirmOwner to be a function (Ember action), got ${typeOfConfirmSuggestedOwner}`,
      typeOfConfirmSuggestedOwner === 'function'
    );
  }

  /**
   * Invokes the external action removeOwner to remove an owner from the confirmed list
   * @return {boolean | void | IOwner}
   */
  @action
  onRemoveOwner(): boolean | void | IOwner {
    const { owner, isOwnerMutable, removeOwner } = getProperties(this, ['owner', 'isOwnerMutable', 'removeOwner']);
    return isOwnerMutable && removeOwner(owner);
  }

  /**
   * Invokes the external action for  confirming the suggested owner
   * @return {Array<IOwner> | void}
   */
  @action
  confirmOwner(): Array<IOwner> | void {
    const { owner, confirmSuggestedOwner } = getProperties(this, ['owner', 'confirmSuggestedOwner']);
    return confirmSuggestedOwner(owner);
  }

  /**
   * Updates the type attribute on the owner record
   * @param {OwnerType} type value to update the type attribute with
   * @return {void}
   */
  @action
  changeOwnerType(type: OwnerType): boolean | void {
    const { owner, isOwnerMutable, updateOwnerType } = getProperties(this, [
      'owner',
      'isOwnerMutable',
      'updateOwnerType'
    ]);

    return isOwnerMutable && updateOwnerType(owner, type);
  }
}
