/*global fluxUser fluxHost require console alert $ */
require(['socket', 'jquery'], function (socket) {

	console.log('Lift off!');
	
	console.log('fluxUser = ', fluxUser);
	console.log('fluxHost = ', fluxHost);

	var username = fluxUser;
	
	socket.on('connect', function() {
		if (username) {
			socket.emit('connectToChannel', {
				'channel' : username
			}, function(answer) {
				console.log('connectToChannel', answer);
				if (answer.connectedToChannel) {
					return socket.emit('getProjectsRequest', {
						username: username,
						callback_id: 0
					});
				} else {
					if (answer.error) {
						alert("Flux connection couldn't be established. \n"+answer.error);
					}
				}
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
//		console.log('cfAppLog', msg);
		logAppend(msg.stream, msg.msg);
//		logAppend("STDOUT", "Changed ["+msg.timestamp+"] "+msg.project+"/"+msg.resource);
	});
	socket.on('error', function (err) {
		logAppend("SOCKETIO", "Error connecting to websocket at: "+fluxHost);
		logAppend("SOCKETIO", ""+err);
	});
	
});