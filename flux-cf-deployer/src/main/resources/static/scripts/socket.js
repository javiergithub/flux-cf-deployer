/*global fluxHost fluxUser fluxToken define*/
define(function(require) {

	var io = require('socket.io');

//	var host = window.location.host;
//	var isCloudFoundry = host.indexOf('cfapps.io')>=0;

	//var socket = isCloudFoundry?io.connect('https:'+host+':4443'):io.connect();
	var socket = io.connect(fluxHost, {
		query: "user="+encodeURIComponent(fluxUser) + 
			"&token="+fluxToken //don't need to encode this as it is a hex string
	});

	return socket;
});
