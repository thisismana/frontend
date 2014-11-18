define([
    'modules/search-api',
    'mock-search',
    'utils/mediator'
], function (
    searchApi,
    mockSearch,
    mediator
) {
    /** TODO
     * TEST
     * search an article
     * search a word alone
     * search a word with space
     * search a word with URI components
     * search in draft or in live
     * Exception in done
     * return only filtered values
     */
    describe('Latest articles', function() {
        it('- whole list should filter out articles', function (done) {
            mockSearch.latest([{
                fields: {
                    headline: 'This has an header'
                }
            }, {
                not: 'This doesn\'t, filter out'
            }]);

            searchApi.fetchLatest()
            .then(assert(function (request, list) {
                expect(request.urlParams.page).toBe('1');
                expect(request.urlParams['content-set']).toBe('-web-live');
                expect(request.urlParams['order-by']).toBe('oldest');
                expect(request.urlParams['page-size']).toBe('50');
                expect(!!request.urlParams.q).toBe(false);

                expect(list).toEqual([{
                    fields: {
                        headline: 'This has an header'
                    }
                }]);

                done();
            }));
        });

        function assert (what) {
            var promisedValue, lastRequest;
            var wait = _.after(2, function () {
                what(lastRequest, promisedValue);
            });

            mediator.once('mock:search', function (request) {
                lastRequest = request;
                wait();
            });

            return function (value) {
                promisedValue = value;
                wait();
            };
        }
    });
});
