define([
    'handlebars',
    'backbone',
    'tmpl/templates'
], function (
    Handlebars,
    Backbone,
    templates
) {
    Handlebars.partials = templates;

    var Router = Backbone.Router.extend({
        routes: {
            '': 'mainAction'
        },
        
        mainAction: function (idProject, idContragent, idTransaction) {
            console.log("main action of router");
        }
    });

    return new Router();
});