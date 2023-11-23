(function(document, $) {
    "use strict";
    window.MsmToolWaitingIndicator = {
        show: function() {
            if (!this.spinner) {
                this.spinner = new Coral.Wait();
                this.spinner.centered = true;
                this.spinner.size = "L";
                document.body.appendChild(this.spinner);
            }
        },

        hide: function(timeoutInMilliseconds) {
            var $this = this;
            if (this.spinner) {
                if (timeoutInMilliseconds) {
                    if (this.timeout) {
                        clearTimeout(this.timeout);
                        this.timeout = null;
                    }
                    this.timeout = setTimeout(function() {
                        $this.hide();
                    }, timeoutInMilliseconds);
                } else {
                    if (this.timeout) {
                        clearTimeout(this.timeout);
                        this.timeout = null;
                    }
                    this.spinner.remove();
                    this.spinner = null;
                }
            }
        }
    };

    // close the loading indicator after the collection has been reloaded.
    // This is needed after a few actions
    $(document).on('foundation-collection-reload', '.foundation-collection', function() {
        MsmToolWaitingIndicator.hide();
    });
})(document, Granite.$);
