import Component from '@ember/component';
import { get, set } from '@ember/object';
import ComputedProperty, { gte } from '@ember/object/computed';
import { TaskInstance, TaskProperty } from 'ember-concurrency';
import { action } from '@ember-decorators/object';
import {
  IAccessControlAccessTypeOption,
  IRequestAccessControlEntry
} from 'wherehows-web/typings/api/datasets/aclaccess';
import { getDefaultRequestAccessControlEntry } from 'wherehows-web/utils/datasets/acl-access';

/**
 * Returns the number of days in milliseconds, default is 1 day
 * @param {number} [day=1] the number of days to scale
 */
const millisecondDays = (day = 1) => day * 24 * 60 * 60 * 1000;

/**
 * Date object with the minimum selectable date for acl request expiration,
 * at least 1 day from now
 * @type {Date}
 */
const minSelectableExpirationDate = new Date(Date.now() + millisecondDays());

/**
 * Date object with the maximum selectable date for acl request expiration,
 * up to 7 days from now
 * @type {Date}
 */
const maxSelectableExpirationDate = new Date(Date.now() + millisecondDays(7));

export default class DatasetAclAccess extends Component {
  /**
   * The currently logged in user is listed on the related datasets acl
   * @type {boolean}
   * @memberof DatasetAclAccess
   */
  readonly userHasAclAccess: boolean;

  /**
   * Currently selected date
   * @type {Date}
   * @memberof DatasetAclAccess
   */
  selectedDate: Date = new Date();

  /**
   * @type {IRequestAccessControlEntry | void}
   */
  userAclRequest: IRequestAccessControlEntry | void;

  /**
   * Date around which the calendar is centered
   * @type {Date}
   * @memberof DatasetAclAccess
   */
  centeredDate: Date = this.selectedDate;

  /**
   * The earliest date a user can select as an expiration date
   * @type {Date}
   * @memberof DatasetAclAccess
   */
  minSelectableExpirationDate: Date = minSelectableExpirationDate;

  /**
   * The furthest date a user can select as an expiration date
   * @type {Date}
   * @memberof DatasetAclAccess
   */
  maxSelectableExpirationDate: Date = maxSelectableExpirationDate;

  /**
   * External action invoked on change to access request access type
   * @type {(option: IAccessControlAccessTypeOption) => void}
   */
  accessTypeDidChange: (option: IAccessControlAccessTypeOption) => void;

  /**
   * External action invoked on change to expiration date
   * @type {(date: Date) => void}
   */
  expiresAtDidChange: (date: Date) => void;

  /**
   * Checks if the expiration date is greater than the min allowed
   * @type {ComputedProperty<boolean>}
   * @memberof DatasetAclAccess
   */
  hasValidExpiration: ComputedProperty<boolean> = gte('selectedDate', minSelectableExpirationDate.getTime());

  /**
   * External task to remove the logged in user from the related dataset's acl
   * @memberof DatasetAclAccess
   */
  removeAccessTask: TaskProperty<Promise<void>> & { perform: (a?: {} | undefined) => TaskInstance<Promise<void>> };

  /**
   * Action to reset the request form
   * @memberof DatasetAclAccess
   */
  resetForm() {
    set(this, 'userAclRequest', getDefaultRequestAccessControlEntry());
  }

  /**
   * Invokes external action when the accessType to be requested is modified
   * @param {IAccessControlAccessTypeOption} arg
   * @memberof DatasetAclAccess
   */
  @action
  onAccessTypeChange(arg: IAccessControlAccessTypeOption): void {
    get(this, 'accessTypeDidChange')(arg);
  }

  /**
   * Sets the selectedDate property on this and invokes the external action to set the expiration date
   * @param {Date} date
   * @memberof DatasetAclAccess
   */
  @action
  onExpirationDateChange(date: Date) {
    set(this, 'selectedDate', date);
    get(this, 'expiresAtDidChange')(date);
  }
}
