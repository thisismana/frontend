/* jscs:disable disallowDanglingUnderscores */
define([
    'bean',
    'bonzo',
    'fastdom',
    'common/utils/$',
    'common/utils/mediator',
    'common/modules/user-prefs'
], function (
    bean,
    bonzo,
    fastdom,
    $,
    mediator,
    userPrefs
) {
    return function (container) {
        var _$container = bonzo(container),
            _$button = bonzo(bonzo.create(
                '<button class="fc-container__toggle" data-link-name="Show">'
                + '<i class="i i-arrow-grey-large"></i>'
                + '<span class="fc-container__toggle__text">Hide</span>'
                + '</button>'
            )),
            buttonText = $('.fc-container__toggle__text', _$button[0]),
            _prefName = 'container-states',
            _toggleText = {
                hidden: 'Show',
                displayed: 'Hide'
            },
            _state = 'displayed',
            _updatePref = function (id, state) {
                // update user prefs
                var prefs = userPrefs.get(_prefName),
                    prefValue = id;
                if (state === 'displayed') {
                    delete prefs[prefValue];
                } else {
                    if (!prefs) {
                        prefs = {};
                    }
                    prefs[prefValue] = 'closed';
                }
                userPrefs.set(_prefName, prefs);
            },
            _readPrefs = function (id) {
                // update user prefs
                var prefs = userPrefs.get(_prefName);
                if (prefs && prefs[id]) {
                    setState('hidden');
                }
            };

        // delete old key
        userPrefs.remove('front-trailblocks');

        function setState(state) {
            var adSlotBadge = $('.ad-slot--paid-for-badge', container);

            _state = state;

            fastdom.write(function () {
                // add/remove rolled class
                _$container[_state === 'displayed' ? 'removeClass' : 'addClass']('fc-container--rolled-up');
                // data-link-name is inverted, as happens before clickstream
                _$button.attr('data-link-name', _toggleText[_state === 'displayed' ? 'hidden' : 'displayed']);
                buttonText.text(_toggleText[_state]);
                // hide/show the badge
                adSlotBadge.css('display', _state === 'hidden' ? 'none' : 'block');
            });
        }

        this.addToggle =  function () {
            // append toggle button
            var id = _$container.attr('data-id'),
                $containerHeader = $('.js-container__header', _$container[0]);

            fastdom.write(function () {
                $containerHeader.append(_$button);
                _$container
                    .removeClass('js-container--toggle')
                    .addClass('fc-container--has-toggle');
                _readPrefs(id);
            });

            mediator.on('module:clickstream:click', function (clickSpec) {
                if (clickSpec.target === _$button[0]) {
                    setState((_state === 'displayed') ? 'hidden' : 'displayed');
                    _updatePref(id, _state);
                }
            });
        };
    };
});
