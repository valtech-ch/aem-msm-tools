(function(document, $) {
    "use strict";
    var FOUNDATION_ACTION = 'foundation.collection.action.action';
    var FOUNDATION_ACTION_CONDITION = 'foundation.collection.action.activecondition';
    var DEFAULT_SPINNER_WAIT_TIMEOUT = 30000;

    var foundationRegistry = $(window).adaptTo("foundation-registry");

    // ==================================================================
    // ==================================================================
    // Actions:
    // ==================================================================
    // ==================================================================

    foundationRegistry.register(FOUNDATION_ACTION, {
        name: "valtech.msmtools.liveCopyManager.update",
        handler: function(name, el, config, collection, selections) {
            MsmToolWaitingIndicator.show();
            var paths = [];
            for (var i = 0; i < selections.length; i++) {
                paths.push($(selections[i]).data('target-path'));
            }
            var liveCopyRoot = $(selections[0]).data("live-copy-root-path");
            $.ajax({
                type: "POST",
                url: "/bin/valtech/msm-tools/synchronize",
                data: {
                    liveCopyRoot: liveCopyRoot,
                    paths: paths
                },
                success: function () {
                    /*$('.foundation-collection').adaptTo('foundation-selections').clear();
                    $('.foundation-collection').adaptTo('foundation-collection').reload();
                    MsmToolWaitingIndicator.hide(DEFAULT_SPINNER_WAIT_TIMEOUT);*/
                    window.location.reload();
                },
                error: function (response) {
                    MsmToolWaitingIndicator.hide();
                    MsmToolInfoDialog.show(response.responseText, true);
                }
            });
        }
    });

    foundationRegistry.register(FOUNDATION_ACTION, {
        name: "valtech.msmtools.liveCopyManager.markAsDone",
        handler: function(name, el, config, collection, selections) {
            var paths = [];
            for (var i = 0; i < selections.length; i++) {
                var item = $(selections[i]);
                if (item.data('livecopy-status') === 'upToDateNotReviewed') {
                    paths.push(item.data('target-path'));
                }
            }
            MsmToolWaitingIndicator.show();
            $.ajax({
                type: "POST",
                url: "/bin/valtech/msm-tools/mark-as-done",
                data: { paths: paths },
                success: function () {
                    /*$('.foundation-collection').adaptTo('foundation-selections').clear();
                    $('.foundation-collection').adaptTo('foundation-collection').reload();
                    MsmToolWaitingIndicator.hide(DEFAULT_SPINNER_WAIT_TIMEOUT);*/
                    window.location.reload();
                },
                error: function (response) {
                    MsmToolWaitingIndicator.hide();
                    MsmToolInfoDialog.show(response.responseText, true);
                }
            });
        }
    });

    foundationRegistry.register(FOUNDATION_ACTION, {
        name: "valtech.msmtools.liveCopyManager.compare",
        handler: function(name, el, config, collection, selections) {
            var item = $(selections[0]);
            var trigger = $('<span>')
                .addClass('cq-common-admin-timeline-compare-button')
                .data('path', item.data('source-path'))
                .data('vid', item.data('last-rolled-out-version'));
            $('body').append(trigger);
            trigger.trigger('click');
        }
    });

    // ==================================================================
    // ==================================================================
    // Action conditions:
    // ==================================================================
    // ==================================================================

    foundationRegistry.register(FOUNDATION_ACTION_CONDITION, {
        name: "valtech.msmtools.liveCopyManager.canMarkAsDone",
        handler: function(name, el, config, collection, selections) {
            if (selections.length === 0) {
                return false;
            }
            for (var i = 0; i < selections.length; i++) {
                if ($(selections[i]).data('livecopy-status') !== 'upToDateNotReviewed') {
                    return false;
                }
            }
            return true;
        }
    });

    foundationRegistry.register(FOUNDATION_ACTION_CONDITION, {
        name: "valtech.msmtools.liveCopyManager.canCompare",
        handler: function(name, el, config, collection, selections) {
            if (selections.length !== 1) {
                return false;
            }
            var item = $(selections[0]);
            return !!(item.data('source-path') && item.data('last-rolled-out-version'));
        }
    });

    foundationRegistry.register(FOUNDATION_ACTION_CONDITION, {
        name: "valtech.msmtools.liveCopyManager.canEdit",
        handler: function(name, el, config, collection, selections) {
            if (selections.length !== 1) {
                return false;
            }
            var item = $(selections[0]);
            var allowedStates = ['upToDate', 'outdated', 'localOnly', 'moved', 'movedOutdated', 'conflicting', 'deleted', 'upToDateNotReviewed'];
            return allowedStates.indexOf(item.data('livecopy-status')) > -1;
        }
    });

    foundationRegistry.register(FOUNDATION_ACTION_CONDITION, {
        name: "valtech.msmtools.liveCopyManager.canUpdate",
        handler: function(name, el, config, collection, selections) {
            if (selections.length === 0) {
                return false;
            }
            var allowedStates = ['outdated', 'newPage', 'moved', 'movedOutdated', 'conflicting', 'deleted'];
            for (var i = 0; i < selections.length; i++) {
                if (allowedStates.indexOf($(selections[i]).data('livecopy-status')) < 0) {
                    return false;
                }
            }
            return true;
        }
    });

})(document, Granite.$);
