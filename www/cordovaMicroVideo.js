var exec = require('cordova/exec');

exports.startRecorder = function(arg0, success, error) {
    exec(success, error, "_CordovaPlugin", "startRecorder", [arg0]);
};
