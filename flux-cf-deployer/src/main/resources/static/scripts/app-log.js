/*global fluxUser fluxHost require console alert $ */
require(['socket', 'jquery'], function (socket) {

	console.log('Lift off!');
	
	console.log('fluxUser = ', fluxUser);
	console.log('fluxHost = ', fluxHost);

	var username = fluxUser; //injected by server
	var app = cfApp; //injected by server
	
	socket.on('connect', function() {
		if (username) {
			socket.emit('connectToChannel', {
				'channel' : username
			}, function(answer) {
				console.log('connectToChannel', answer);
			});
		}
	});
	
	$(".cfAppLog").empty();
	
	function logAppend(stream, msg) {
		var formatted = $('<div/>')
				.addClass(stream)
				.text(msg);

		var appLog = $(".cfAppLog");
		appLog.append(formatted);
		var height = appLog[0].scrollHeight;
		appLog.scrollTop(height);
	}
	
	socket.on('cfAppLog', function (msg) {
		if (app === msg.app) { 
			logAppend(msg.stream, msg.msg);
			console.log('app log :', msg);
		} else {
			console.log('filtered:', msg);
		}
	});
	socket.on('error', function (err) {
		logAppend("SOCKETIO", "Error connecting to websocket at: "+fluxHost);
		logAppend("SOCKETIO", ""+err);
	});
	
});