define([
    'common/utils/cookies',
    'common/utils/detect',
    'common/utils/storage',
    'common/utils/template',
    'common/modules/user-prefs',
    'common/modules/ui/message'
], function (
    cookies,
    detect,
    storage,
    template,
    userPrefs,
    Message
) {

    /**
     * Rules:
     *
     * 4 visits within the last month
     * 3 impressions
     * Persist close state
     */

    var COOKIE_IMPRESSION_KEY = 'GU_SMARTAPPBANNER',
        DATA = {
            IOS: {
                LOGO: 'http://assets.guim.co.uk/images/apps/ios-logo.png',
                SCREENSHOTS: 'http://assets.guim.co.uk/images/apps/ios-screenshots.jpg',
                LINK: 'http://ad-x.co.uk/API/click/guardian789057jo/web3537df56ab1f7e',
                STORE: 'on the App Store'
            },
            ANDROID: {
                LOGO: 'http://assets.guim.co.uk/images/apps/android-logo-2x.png',
                SCREENSHOTS: 'http://assets.guim.co.uk/images/apps/ios-screenshots.jpg',
                LINK: 'http://ad-x.co.uk/API/click/guardian789057jo/web3537df5992e76b',
                STORE: 'in Google Play'
            }
        },
        cookieVal = cookies.get(COOKIE_IMPRESSION_KEY),
        impressions = cookieVal && !isNaN(cookieVal) ? parseInt(cookieVal, 10) : 0,
        tmp = '<img src="{{LOGO}}" class="app__logo" alt="Guardian App logo" /><div class="app__cta"><h4 class="app__heading">The Guardian app</h4>' +
            '<p class="app__copy">Instant alerts. Offline reading.<br/>Tailored to you.</p>' +
            '<p class="app__copy"><strong>FREE</strong> – {{STORE}}</p></div><a href="{{LINK}}" class="app__link">View</a>',
        tablet = '<img src="{{SCREENSHOTS}}" class="app__screenshots" alt="screenshots" />';

    function isDevice() {
        return ((detect.isIOS() || detect.isAndroid()) && !detect.isFireFoxOSApp());
    }

    function canShow() {
        return impressions < 4;
    }

    function showMessage() {
        var platform = (detect.isIOS()) ? 'ios' : 'android',
            msg = new Message(platform),
            fullTemplate = tmp + (detect.getBreakpoint() === 'mobile' ? '' : tablet);

        msg.show(template(fullTemplate, DATA[platform.toUpperCase()]));
        cookies.add(COOKIE_IMPRESSION_KEY, impressions + 1);
    }

    function init() {
        if (isDevice() && canShow()) {
            showMessage();
        }
    }

    return {
        init: init
    };

});
