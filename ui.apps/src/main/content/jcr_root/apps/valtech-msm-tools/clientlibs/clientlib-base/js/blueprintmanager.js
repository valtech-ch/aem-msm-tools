(function(document, $) {
    "use strict";
    var FOUNDATION_ACTION = 'foundation.collection.action.action';

    var foundationRegistry = $(window).adaptTo("foundation-registry");

    var CountriesNotificationDialog = {
        show: function(liveCopies) {
            this.liveCopies = liveCopies;
            this.dialog = new Coral.Dialog().set({
                id: 'MsmToolsNotifyRegions',
                header: {
                    innerHTML: "E-mail Notification"
                },
                footer: {
                    innerHTML: '<button id="cancelButton" is="coral-button" variant="default" coral-close>Cancel</button><button id="sendNotificationButton" is="coral-button" variant="primary">Send</button>'
                }
            });
            this.dialog.show();
            this.prepareDialogContent();
            this.textContent.empty();
            this.textContent.append(this.getContent(liveCopies));
        },

        prepareDialogContent: function() {
            this.dialog.on('click', '#sendNotificationButton', this.onButtonClicked.bind(this));
            document.body.appendChild(this.dialog);
            this.textContent = $('<div>');
            var dialogContent = $(this.dialog).find('coral-dialog-content');
            dialogContent.append($('<p>').text('Leave blank to use the default subject and text, or define your own:'));
            this.customSubjectTextfield = $(new Coral.Textfield()).width(400).attr('placeholder', 'Subject');
            dialogContent.append($('<div>').append(this.customSubjectTextfield));
            dialogContent.append($('<br>'));
            this.customMessageTextarea = $(new Coral.Textarea()).width(400).height(150).attr('placeholder', 'Text');
            dialogContent.append($('<div>').append(this.customMessageTextarea));
            dialogContent.append($('<p>').text('Available placeholder: {liveCopyManagerLink}'));
            dialogContent.append(this.textContent);
        },

        getContent: function(liveCopies) {
            var content = $('<div>');
            content.append($('<p>').text("The following regions will be notified:"));
            var list = $('<ul>')
            for(var i = 0; i < liveCopies.length; i++) {
                list.append($('<li>').text(liveCopies[i].region + ' - ' + liveCopies[i].language));
            }
            content.append(list);
            return content;
        },

        onButtonClicked: function() {
            this.dialog.hide();
            MsmToolWaitingIndicator.show();
            var $this = this;
            $.ajax({
                type: "POST",
                url: "/bin/valtech/msm-tools/notify-regions",
                data: {
                    liveCopyPaths: this.getLiveCopyPaths(),
                    subject: this.customSubjectTextfield.val(),
                    emailContent: this.customMessageTextarea.val()
                },
                success: function () {
                    $('.foundation-collection').adaptTo('foundation-selections').clear();
                    MsmToolWaitingIndicator.hide();
                    MsmToolInfoDialog.show('Notification e-mails successfully sent.', false);
                },
                error: function (response) {
                    MsmToolWaitingIndicator.hide();
                    var text = response.responseText;
                    for(var i = 0; i < $this.liveCopies.length; i++) {
                        text = text.replace(
                            $this.liveCopies[i].liveCopyPath,
                            '<b>' + $this.liveCopies[i].region + ' - ' + $this.liveCopies[i].language + '</b>');
                    }
                    MsmToolInfoDialog.show(text, true);
                }
            });
        },

        getLiveCopyPaths: function() {
            var liveCopyPaths = [];
            for(var i = 0; i< this.liveCopies.length; i++) {
                liveCopyPaths.push(this.liveCopies[i].liveCopyPath);
            }
            return liveCopyPaths;
        }
    };

    function updateLiveCopyStates(liveCopyRows) {
        var item = liveCopyRows.shift();
        if (!item) {
            return;
        }
        var stateCell = item.querySelector('.col-status');
        var lastUpdatedCell = item.querySelector('.col-last-modified');
        stateCell.appendChild(new Coral.Wait());
        lastUpdatedCell.appendChild(new Coral.Wait());
        $.ajax({
            type: "GET",
            url: "/bin/valtech/msm-tools/check-status",
            dataType: "json",
            data: {
                path: item.dataset['livecopyPath']
            },
            success: function (data) {
                stateCell.innerHTML = data.state;
                lastUpdatedCell.innerHTML = '<foundation-time  value="' + data.lastUpdated + '"></foundation-time>';
                updateLiveCopyStates(liveCopyRows);
            },
            error: function (response) {
                console.error('Something went wrong', response);
                stateCell.innerHTML = 'Error';
                lastUpdatedCell.innerHTML = 'Error';
                updateLiveCopyStates(liveCopyRows);
            }
        });
    };

    $(document).on("foundation-contentloaded", function(event) {
        Coral.commons.ready(event.target, function() {
            var container = event.target;
            var liveCopyRows = [];
            container.querySelectorAll('.live-copy-row').forEach(function(row) {
                liveCopyRows.push(row);
            });
            updateLiveCopyStates(liveCopyRows);
        });
    });

    foundationRegistry.register(FOUNDATION_ACTION, {
        name: "valtech.msmtools.blueprintManager.notify",
        handler: function(name, el, config, collection, selections) {
            if(selections.length === 0) {
                return;
            }
            var liveCopies = [];
            for (var i = 0; i < selections.length; i++) {
                var item = $(selections[i]);
                liveCopies.push({
                    region: item.data('region'),
                    language: item.data('language'),
                    liveCopyPath: item.data('livecopy-path')
                });
            }
            CountriesNotificationDialog.show(liveCopies);
        }
    });

})(document, Granite.$);
