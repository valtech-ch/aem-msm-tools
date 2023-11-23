(function(document, $) {
    "use strict";
    window.MsmToolInfoDialog = {
        show: function(content, isError) {
            if (!this.dialog) {
                this.dialog = new Coral.Dialog().set({
                    id: 'msmInfoDialog',
                    footer: {
                        innerHTML: '<button id="cancelButton" is="coral-button" variant="default" coral-close>Close</button>'
                    }
                });
                document.body.appendChild(this.dialog);
            }
            if (isError) {
                this.dialog.variant = Coral.Dialog.variant.ERROR;
                this.dialog.header.innerHTML = 'Error';
            } else {
                this.dialog.variant = Coral.Dialog.variant.DEFAULT;
                this.dialog.header.innerHTML = 'Info';
            }
            $(this.dialog).find('coral-dialog-content').empty();
            var content = $('<p>').html(content);
            $(this.dialog).find('coral-dialog-content').append(content);
            this.dialog.show();
        }
    };
})(document, Granite.$);
