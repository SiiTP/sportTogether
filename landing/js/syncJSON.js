define(['view/notification'], function(Notification) {
    return function(method, model, options) {
        var url = model.url;
        console.log("<--- query : /" + url + " CRUD : " + method);
        switch (method) {
            case "create":
                var data = model.toJSON();
                var ajax = $.ajax({
                    type: "POST",
                    url: url,
                    data: data
                });
                ajax.success(function (obj, textStatus, jqXHR) {
                    console.log("---> SERVER ANSWER : " + obj);
                    var answerJSON = JSON.parse(obj);
                    if (answerJSON.code == 0) {
                        options.success(answerJSON);
                    } else {
                        options.error(answerJSON);
                    }
                });
                ajax.error(function (jqXHR, textStatus, errorThrown) {
                    debugger;
                    Notification.msgServerError(jqXHR.status, errorThrown);
                });
                break;

            case "read":
                var getParameters = model.get("get_parameters") || model.get_parameters;
                if (getParameters != null) {
                    var postfix = "";
                    for (var key in getParameters) {
                        if (getParameters.hasOwnProperty(key)) {
                            if (getParameters[key] !== null && getParameters[key] !== "") {
                                postfix === "" ? postfix += "?" : postfix += "&";
                                postfix += key + "=" + getParameters[key];
                            }
                        }
                    }
                    url = url + postfix;
                }

                var ajax = $.ajax({
                    type: "GET",
                    url: url
                });
                ajax.success(function (obj, textStatus, jqXHR) {
                    console.log("---> SERVER ANSWER : " + obj);

                    var answerJSON = JSON.parse(obj);
                    if (answerJSON.code == 0) {
                        options.success(answerJSON);
                    } else {
                        options.error(answerJSON);
                    }
                });
                ajax.error(function (jqXHR, textStatus, errorThrown) {
                    Notification.msgServerError(jqXHR.status, errorThrown);
                });
                break;

            case "update":
                var ajax = $.ajax({
                    type: "PUT",
                    url: url,
                    data: model.toJSON()
                });

                ajax.success(function (obj, textStatus, jqXHR) {
                    console.log("---> SERVER ANSWER : " + obj);
                    var answerJSON = JSON.parse(obj);
                    if (answerJSON.code == 0) {
                        options.success(answerJSON);
                    } else {
                        options.error(answerJSON);
                    }
                });
                ajax.error(function (jqXHR, textStatus, errorThrown) {
                    Notification.msgServerError(jqXHR.status, errorThrown);
                });
                break;

            case "delete":
                var getParameters = model.get("get_parameters") || model.get_parameters;
                if (getParameters != null) {
                    var postfix = "";
                    for (var key in getParameters) {
                        if (getParameters.hasOwnProperty(key)) {
                            if (getParameters[key] !== null  && getParameters[key] !== "") {
                                postfix === "" ? postfix += "?" : postfix += "&";
                                postfix += key + "=" + getParameters[key];
                            }
                        }
                    }
                    url = url + postfix;
                }

                var ajax = $.ajax({
                    type: "DELETE",
                    url: url
                });
                ajax.success(function (obj, textStatus, jqXHR) {
                    console.log("---> SERVER ANSWER : " + obj);

                    var answerJSON = JSON.parse(obj);
                    if (answerJSON.code == 0) {
                        options.success(answerJSON);
                    } else {
                        options.error(answerJSON);
                    }
                });
                ajax.error(function (jqXHR, textStatus, errorThrown) {
                    Notification.msgServerError(jqXHR.status, errorThrown);
                });
                break;
            default:
                console.log("unresolved operation");
                break;
        }
    };
});