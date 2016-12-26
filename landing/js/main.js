/**
 * Created by root on 31.08.16.
 */

require.config({
    urlArgs: "_=" + (new Date()).getTime(),
    baseUrl: "js",
    paths: {
        jquery: "lib/jquery-2.1.4",
        underscore: "lib/underscore-min",
        backbone: "lib/backbone",
        handlebars: "lib/handlebars-v4.0.5",
        materialize: "lib/materialize",
        wavesjs: "lib/waves",
        hammerjs: "lib/hammer.min",
        velocity: "lib/velocity.min"
    },
    shim: {
        'wavesjs': {
            exports: 'Waves'
        },
        'backbone': {
            deps: ['underscore', 'jquery'],
            exports: 'Backbone'
        },
        'underscore': {
            exports: '_'
        },
        'handlebars': {
            exports: 'Handlebars'
        },
        'materialize': {
            deps: ['jquery', 'hammerjs', 'velocity'],
            exports: 'Materialize'
        }
    }
});

define([
    'backbone',
    'handlebars',
    'handlebarsHelpers',
    'router'
], function(
    Backbone,
    Handlebars,
    handlebarsHelpers,

    router
){
    console.log("in main!");
    Backbone.history.start({
        root: "/"
    });

});
