import Service from '@ember/service';
import { bannerAnimationSpeed } from '@datahub/utils/constants/notifications';
import { NotificationEvent } from '@datahub/utils/constants/notifications';
import { delay } from 'wherehows-web/utils/promise-delay';
import { computed } from '@ember/object';
import { getConfig } from 'wherehows-web/services/configurator';

/**
 * Expected properties to be found on a basic banner object added to our list
 */
export interface IBanner {
  content: string;
  type: NotificationEvent;
  isExiting: boolean;
  isDismissable: boolean;
  iconName: string;
  usePartial: boolean;
}

/**
 * When creating a new banner, helps to determine whether that banner isDismissable
 */
const isDismissableMap: { [key: string]: boolean } = {
  [NotificationEvent['info']]: true,
  [NotificationEvent['confirm']]: true,
  [NotificationEvent['error']]: false
};

/**
 * When creating a new banner, helps to determine the kind of font awesome icon indicator will be on
 * the left side of the banner
 */
const iconNameMap: { [key: string]: string } = {
  [NotificationEvent['info']]: 'info-circle',
  [NotificationEvent['confirm']]: 'exclamation-circle',
  [NotificationEvent['error']]: 'times-circle'
};

export default class BannerService extends Service {
  /**
   * Our cached list of banner objects to be rendered
   * @type {Array<IBanner>}
   */
  banners: Array<IBanner> = [];

  /**
   * Computes the banners list elements to toggle the isShowingBanners flag, which not only activates the
   * banner component but also triggers the navbar and app-container body to shift up/down to accommodate
   * for the banner component space
   * @type {ComputedProperty<boolean>}
   */
  @computed('banners.@each.isExiting')
  get isShowingBanners(): boolean {
    const { banners } = this;
    // Note: If we have no banners, flag should always be false. If we have more than one banner, flag
    // should always be true, BUT if we only have one banner and it is in an exiting state we can go ahead
    // and trigger this to be false to line up with the animation
    return banners.length > 0 && (banners.length > 1 || !banners[0].isExiting);
  }

  appInitialBanners([showStagingBanner, showLiveDataWarning, showChangeManagement]: Array<boolean>): void {
    if (showStagingBanner) {
      this.addBanner(
        'You are viewing/editing in the staging environment. Changes made here will not reflect in production',
        NotificationEvent['info']
      );
    }

    if (showLiveDataWarning) {
      this.addBanner(
        'You are viewing/editing live data. Changes made here will affect production data',
        NotificationEvent['confirm']
      );
    }

    if (showChangeManagement) {
      const changeManagementLink = getConfig('changeManagementLink', { useDefault: true, default: '' });
      this.addBanner(
        `Our pipeline is currently under maintenance or a new deployment and some features may be temporarily
        unavailable. You can find more information at ${changeManagementLink}`,
        NotificationEvent['confirm']
      );
    }
  }

  /**
   * Method to actually take care of removing the first banner from our queue.
   */
  async dequeue(): Promise<void> {
    // Note: Since dequeuing the banner will remove it from the DOM, we don't want to actually dequeue
    // until the removal animation, which takes 0.6 seconds, is completed.
    const animationOffset = bannerAnimationSpeed + 0.2;
    const dismissDelay = delay(animationOffset);

    await dismissDelay;
    this.banners.shiftObject();
  }

  /**
   * Method to add the banner to our queue. Takes the content and type of banner and creates a
   * standardized interface that our servie can understand
   * @param message - the message to put in the banner's content box
   * @param {NotificationEvent} type - what type of banner notification we are going for (which will
   *  determine the appearance and interaction properties)
   * @param usePartial - in some situations, we want to use a partial component to render in the banner instead
   *  of a simple string. This flag will allow that
   */
  addBanner(message: string, type: NotificationEvent = NotificationEvent['info'], usePartial = false): void {
    this.banners.unshiftObject({
      type,
      usePartial,
      content: message,
      isExiting: false,
      isDismissable: isDismissableMap[type],
      iconName: iconNameMap[type]
    });
  }

  /**
   * Allows us to target a specific banner for removal by giving the content and type and then searching the
   * list for a specific match to remove at the found index. If nothing is matched, function does nothing
   * @param content - banner content to match for removal
   * @param type    = banner type to match for removal
   */
  removeBanner(content: string, type: NotificationEvent): void {
    const { banners } = this;

    const removalIndex = banners.reduce(
      (index, banner, currIndex): number => (banner.content === content && banner.type === type ? currIndex : index),
      -1
    );

    if (removalIndex > -1) {
      banners.removeAt(removalIndex, 1);
    }
  }
}
