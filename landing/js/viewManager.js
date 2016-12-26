define (function() {
    return function ViewManager() {
        this.views = [];

        this.add = function(view) {
            view.on("show", this.hideOthers.bind(this, view));
            this.views.push(view);
        };

        this.hideOthers = function (showedView) {
            _.each(this.views, function (view) {
                if (view != showedView) {
                    view.hide();
                }
            })
        };
    }
});