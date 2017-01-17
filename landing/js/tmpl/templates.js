//todo render by grunt
define(
    [
        "handlebars",
        "tmpl/hbs_templates"
    ],
    function (
        Handlebars,
        hbsTemplates
    ) {
        var now = Date.now();
        var templates = {};
        _.each(hbsTemplates, function (template, key) {
            var result = /[^/]*$/.exec(key)[0];
            templates[result] = Handlebars.compile(template);
        });
        console.log(Date.now() - now + " ms render templates");
        return templates;
    });