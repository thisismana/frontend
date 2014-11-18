define([
    'modules/vars',
    'modules/authed-ajax',
    'utils/url-abs-path',
], function (
    vars,
    authedAjax,
    urlAbsPath
) {
    // If term contains slashes, assume it's an article id (and first convert it to a path)
    var isSearchTermAnItem = /\//;

    function dateYyyymmdd() {
        var d = new Date();
        return [d.getFullYear(), d.getMonth() + 1, d.getDate()].map(function(p) { return p < 10 ? '0' + p : p; }).join('-');
    }

    function fetchLatest (options) {
        var url = vars.CONST.apiSearchBase + '/',
            propName, term;

        options = _.extend(options || {}, {
            term: '',
            filter: '',
            filterType: '',
            page: 1,
            pageSize: vars.CONST.searchPageSize || 25,
            isDraft: true
        });
        term = options.term;

        if (isSearchTermAnItem.test(term)) {
            term = urlAbsPath(term);
            // TODO I've removed this
            // self.term(term);
            propName = 'content';
            url += term + '?' + vars.CONST.apiSearchParams;
        } else {
            term = encodeURIComponent(term.trim().replace(/ +/g,' AND '));
            propName = 'results';
            url += 'search?' + vars.CONST.apiSearchParams;
            url += options.isDraft ?
                '&content-set=-web-live&order-by=oldest&use-date=scheduled-publication&from-date=' + dateYyyymmdd() :
                '&content-set=web-live&order-by=newest';
            url += '&page-size=' + options.pageSize;
            url += '&page=' + options.page;
            url += term ? '&q=' + filter : '';
            url += options.filter ? '&' + options.filterType + '=' + encodeURIComponent(options.filter) : '';
        }

        return new Promise(function (resolve, reject) {
            authedAjax.request({
                url: url
            }).done(function(data) {
                var rawArticles = data.response && data.response[propName] ? [].concat(data.response[propName]) : [];

                if (!term && !rawArticles.length) {
                    reject(new Error('Sorry, the Content API is not currently returning content'));
                }

                resolve(_.filter(rawArticles, function(opts) {
                    return opts.fields && opts.fields.headline;
                }));
            }, function (xhr) {
                reject(new Error('Content API error (' + xhr.status + '). Content is currently unavailable'));
            });

        });

    }

    return {
        fetchLatest: fetchLatest
    };
});
