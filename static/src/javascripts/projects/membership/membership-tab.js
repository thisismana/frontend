define([
    'bean',
    'common/utils/$',
    'common/utils/ajax',
    'common/utils/config',
    'membership/payment-form'
], function (bean, $, ajax, config, PaymentForm) {

    var PAYMENT_FORM = '.js-mem-stripe-form',
        CARD_DETAILS = '.js-mem-card-details',
        CHANGE_CARD = '.js-mem-change-card',
        CARD_DETAILS_FORM_CONTAINER = '.js-mem-card-details-form-container',
        CARD_CHANGE_SUCCESS_MSG = '.js-mem-card-change-success-msg',
        CARD_LAST4 = '.js-mem-card-last4',
        CARD_TYPE = '.js-mem-card-type',
        PACKAGE_COST = '.js-mem-package-cost',
        PACKAGE_CURRENT_PERIOD_END = '.js-mem-current-period-end',
        PACKAGE_CURRENT_PERIOD_START = '.js-mem-current-period-start',
        PACKAGE_INTERVAL = '.js-mem-plan-interval',
        DETAILS_MEMBERSHIP_TIER_ICON_CURRENT = '.js-mem-icon-current',
        DETAILS_JOIN_DATE = '.js-mem-join-date',
        DETAILS_MEMBER_NUM_TEXT = '.js-mem-number',
        NOTIFICATION_CANCEL = '.js-mem-cancel-tier',
        NOTIFICATION_CHANGE = '.js-mem-change-tier',
        MEMBER_DETAILS = '.js-mem-details',
        DETAILS_MEMBER_NUMBER_CONTAINER = '.js-mem-number-container',
        MEMBERSHIP_TAB = '.js-mem-tab',
        MEMBERSHIP_TIER = '.js-mem-tier',
        UP_SELL = '.js-mem-up-sell',
        MEMBER_INFO = '.js-mem-info',
        LOADER = '.js-mem-loader',
        CLOSED_CLASSNAME = 'is-closed',
        IS_HIDDEN_CLASSNAME = 'is-hidden',
        CTA_DISABLED_CLASSNAME = 'membership-cta--disabled';

    function formatAmount(amount) {
        return amount ? '£' + (amount / 100).toFixed(2) : 'FREE';
    }

    function formatDate(timestamp) {
        var date = new Date(timestamp),
            months = [
                'January',
                'February',
                'March',
                'April',
                'May',
                'June',
                'July',
                'August',
                'September',
                'October',
                'November',
                'December'
            ],
            day = date.getDate(),
            month = months[date.getMonth()],
            year = date.getFullYear();

        return [day, month, year].join(' ');
    }

    function fetchUserDetails() {
        ajax({
            url: config.page.membershipUrl + '/user/me/details',
            crossOrigin: true,
            withCredentials: true,
            method: 'get'
        }).then(function (resp) {
            if (resp && resp.subscription) {
                hideLoader();
                populateUserDetails(resp);
                addSpriteCss();
                setupPaymentForm();
                addToggleFormListener();
            } else {
                hideLoader();
                displayMembershipUpSell();
            }
        });
    }

    function hideLoader() {
        $(LOADER).addClass(IS_HIDDEN_CLASSNAME);
    }

    function setupPaymentForm() {
        (new PaymentForm()).init($(PAYMENT_FORM)[0], function (newCard) {
            toggleForm(false);
            updateCard(newCard);
            $(CARD_CHANGE_SUCCESS_MSG).removeClass(IS_HIDDEN_CLASSNAME);
        });
    }

    function addToggleFormListener() {
        bean.on($(CHANGE_CARD)[0], 'click', function () {
            toggleForm();
            $(CARD_CHANGE_SUCCESS_MSG).addClass(IS_HIDDEN_CLASSNAME);
        });
    }

    function populateUserDetails(userDetails) {
        var intervalText = userDetails.subscription.plan.interval === 'month' ? 'Monthly' : 'Annual',
            notificationTypeSelector;

        $(MEMBERSHIP_TIER).text(userDetails.tier);
        $(PACKAGE_COST).text(formatAmount(userDetails.subscription.plan.amount));
        $(DETAILS_JOIN_DATE).text(formatDate(userDetails.joinDate));
        $(PACKAGE_INTERVAL).text(intervalText);
        $(PACKAGE_CURRENT_PERIOD_START).text(formatDate(userDetails.subscription.start));
        $(PACKAGE_CURRENT_PERIOD_END).text(formatDate(userDetails.subscription.end));

        // display membership number
        if (userDetails.regNumber) {
            $(DETAILS_MEMBER_NUMBER_CONTAINER).removeClass(IS_HIDDEN_CLASSNAME);
            $(DETAILS_MEMBER_NUM_TEXT).text(userDetails.regNumber);
        }

        // update card details
        if (userDetails.subscription.card) {
            updateCard(userDetails.subscription.card);
        }

        // user has cancelled
        if (userDetails.subscription.cancelledAt) {
            // is this a tier change or a cancellation
            notificationTypeSelector = userDetails.optIn ? NOTIFICATION_CHANGE : NOTIFICATION_CANCEL;
            $(notificationTypeSelector).removeClass(IS_HIDDEN_CLASSNAME);
            $(MEMBER_DETAILS).addClass(IS_HIDDEN_CLASSNAME);
            $(DETAILS_MEMBERSHIP_TIER_ICON_CURRENT).addClass('i-g-' + userDetails.tier.toLowerCase());
        } else if (userDetails.subscription.card) {
            // only show card details if user hasn't changed their subscription and has a payment method
            $(CARD_DETAILS).removeClass(IS_HIDDEN_CLASSNAME);
        }

        $(MEMBER_INFO).removeClass(IS_HIDDEN_CLASSNAME);
    }

    function displayMembershipUpSell() {
        $(UP_SELL).removeClass(IS_HIDDEN_CLASSNAME);
    }

    function addSpriteCss() {
        var spriteSheetUrl = $(MEMBERSHIP_TAB).data('sprite-url'),
            $head = $('head'),
            link = document.createElement('link');

        link.id = 'membership-sprite';
        link.rel = 'stylesheet';
        link.type = 'text/css';
        link.href = spriteSheetUrl;
        link.media = 'all';
        $head.append(link);
    }

    function toggleForm(show) {
        var $cont = $(CARD_DETAILS_FORM_CONTAINER),
            $button = $(CHANGE_CARD);

        show = show !== undefined ? show : $cont.hasClass(CLOSED_CLASSNAME);

        if (show) {
            $cont.removeClass(CLOSED_CLASSNAME);
            $button.addClass(CTA_DISABLED_CLASSNAME).text('Cancel');
        } else {
            $cont.addClass(CLOSED_CLASSNAME);
            $button.removeClass(CTA_DISABLED_CLASSNAME).text('Change card');
        }
    }

    function updateCard(card) {
        var cardTypeClassName,
            $cardTypeElem;

        cardTypeClassName = card.type.toLowerCase().replace(' ', '-');
        $cardTypeElem = $(CARD_TYPE);
        $(CARD_LAST4).text(card.last4);
        $cardTypeElem[0].className = $cardTypeElem[0].className.replace(/\bi-\S+/g, '');
        $cardTypeElem.addClass('i-' + cardTypeClassName);
    }

    function init() {
        fetchUserDetails();
    }

    return {
        init: init
    };
});
