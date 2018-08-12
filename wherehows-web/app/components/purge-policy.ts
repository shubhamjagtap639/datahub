import Component from '@ember/component';
import { set } from '@ember/object';
import { run, next } from '@ember/runloop';
import DatasetCompliance from 'wherehows-web/components/dataset-compliance';
import {
  baseCommentEditorOptions,
  DatasetPlatform,
  exemptPolicy,
  isExempt,
  PurgePolicy,
  purgePolicyProps
} from 'wherehows-web/constants';
import { IComplianceInfo } from 'wherehows-web/typings/api/datasets/compliance';
import { action } from '@ember-decorators/object';

export default class PurgePolicyComponent extends Component {
  /**
   * Reference to the purge exempt policy
   * @type {PurgePolicy}
   */
  exemptPolicy = exemptPolicy;

  /**
   * Reference to the informational text if the dataset does not have a saved purge policy
   * @type {string}
   */
  missingPolicyText: string;

  /**
   * Reference to client options for each purge policy
   * @type {PurgePolicyProperties}
   */
  purgePolicyProps = purgePolicyProps;

  /**
   * The list of supported purge policies for the related platform
   * @type {Array<PurgePolicy>}
   * @memberof PurgePolicyComponent
   */
  supportedPurgePolicies: DatasetCompliance['supportedPurgePolicies'];

  /**
   * The dataset's  platform
   * @type {DatasetPlatform}
   * @memberof PurgePolicyComponent
   */
  platform: DatasetPlatform;

  /**
   * The currently save policy for the dataset purge
   * @type {PurgePolicy}
   * @memberof PurgePolicyComponent
   */
  purgePolicy: PurgePolicy;

  /**
   * The default selected Purge Policy if one does not exist for this dataset
   * @type {PurgePolicy}
   * @memberof PurgePolicyComponent
   */
  defaultPurgePolicy: PurgePolicy;

  /**
   * Flag indication that policy has a request exemption reason
   * @type {boolean}
   */
  requestExemptionReason: boolean;

  /**
   * An options hash for the purge exempt reason text editor
   * @type {}
   * @memberof PurgePolicyComponent
   */
  editorOptions = {
    ...baseCommentEditorOptions,
    placeholder: {
      text: 'Please provide an explanation for why this dataset is marked "Purge Exempt" status',
      hideOnClick: false
    }
  };

  /**
   * Action to handle policy change, by default a no-op function
   * @type {(purgePolicy: PurgePolicy) => IComplianceInfo['complianceType'] | null}
   * @memberof PurgePolicyComponent
   */
  onPolicyChange: (purgePolicy: PurgePolicy) => IComplianceInfo['complianceType'] | null;

  constructor() {
    super(...arguments);

    this.requestExemptionReason || (this.requestExemptionReason = false);
  }

  didReceiveAttrs() {
    this._super(...arguments);
    this.checkExemption(this.purgePolicy);
  }

  /**
   * Checks that the selected purge policy is exempt, if so, set the
   * flag to request the exemption to true
   * @param {PurgePolicy} purgePolicy
   */
  checkExemption(this: PurgePolicyComponent, purgePolicy: PurgePolicy) {
    const exemptionReasonRequested = isExempt(purgePolicy);
    set(this, 'requestExemptionReason', exemptionReasonRequested);

    if (exemptionReasonRequested) {
      // schedule for a future queue, 'likely' post render
      // this allows us to ensure that editor it visible after the set above has been performed
      run(() => next(this, 'focusEditor'));
    }
  }

  /**
   * Applies cursor / document focus to the purge note text editor
   */
  focusEditor(this: PurgePolicyComponent) {
    const { element } = this;
    const exemptionReasonElement: HTMLElement | null = element && element.querySelector('.comment-new__content');

    if (exemptionReasonElement) {
      exemptionReasonElement.focus();
    }
  }

  /**
   * Handles the change to the currently selected purge policy
   * @param {PurgePolicy} purgePolicy the selected purge policy
   */
  @action
  onChange(purgePolicy: PurgePolicy) {
    return this.onPolicyChange(purgePolicy);
  }
}
