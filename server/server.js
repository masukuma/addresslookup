var http = require('http'), io = require('socket.io'), users = require('./users'), gcm = require('node-gcm'),ndate=require('date-format-lite');

var app = http.createServer();
app.listen(3000);

// project 1048500612993
console.log('Server running at http://127.0.0.1:3000/');

// Socket.IO server
var io = io.listen(app), addresses = {};
io.set('log level', 1);
Date.masks.default = 'YYYY-MM-DD hh:mm:ss';

io.sockets.on('connection', function(socket) {
	socket.on('user message', function(msg) {
		console.log('Server running at http://127.0.0.1:3000/');
		socket.broadcast.emit('user message', 'successfully connected');
	});

	socket.on('createlogin', function(user,versionName,versionNumber, fn) {
		// console.log('recieved login msg');
		console.log(user);
		var userid = user.id;
		var regID = user.registrationID;
		var now = new Date();
		monikerhead = user.moniker;
		var address = socket.handshake.address;
		console.log("Create login request from " + address.address + ":" + address.port);
		// console.log('email ' + user.email);
		// var arr = user.email.split("@");
		// console.log(JSON.stringify(arr));
		var loginrequest={'user':user,'versionName':versionName,'versionNumber':versionNumber,'address':address.address,'port':address.port, 'datetime':now.format() };
		console.log('recieved login msg ' + userid);
		
		users.findIfMonikerExists(monikerhead, function(qusr) {
			if(qusr == null){
				users.findById(userid,loginrequest, function(usr) {
				if (usr) {
						// fn(true);
						socket.emit('loggedin', usr);
				} else {

						// fn(true);
						var addresses = [ {
							name : 'Home',
							latitude : '-1.288918',
							longitude : '36.822867',
							typeofaddress : '0',
							userid : userid,
							locationname : monikerhead + '@home',
							registrationID: regID
						}, {
							name : 'Work',
							latitude : '-1.288918',
							longitude : '36.822867',
							typeofaddress : '0',
							userid : userid,
							locationname : monikerhead + '@work',
							registrationID: regID
						}, {
							name : 'School',
							latitude : '-1.288918',
							longitude : '36.822867',
							typeofaddress : '0',
							userid : userid,
							locationname : monikerhead + '@school',
							registrationID: regID
						} ];
						user.addresses = addresses;

						users.addUser(user,loginrequest, function(usr1,loginrequest) {
							console.log("returned value");
							console.log(usr1);
							socket.emit('loggedin', usr1);
						});

				}
				});
			} else {
				socket.emit('logincreationfailed', user);
			}			
		});		
	});

	socket.on('login', function(user,versionName,versionNumber, fn) {
		// console.log('recieved login msg');
		console.log(user);
		var userid = user.id;
		var token = user.registrationID;
		var now = new Date();
		var address = socket.handshake.address;
		console.log("New login request from " + address.address + ":" + address.port);
		var loginrequest={'user':user,'versionName':versionName,'versionNumber':versionNumber,'address':address.address,'port':address.port, 'datetime':now.format() };
		// console.log('email ' + user.email);
		// var arr = user.email.split("@");
		// console.log(JSON.stringify(arr));
		console.log('recieved login msg ' + userid);
		users.findById(userid,loginrequest, function(usr) {
			if (usr) {
				users.findReplaceUserTokenWithLatest(userid,token, function(usr1) {
					socket.emit('loggedin', usr1);
				});				
			} else {
				socket.emit('nosuchuser', user);
			}
		});
	});

	socket.on('createaddress', function(user,versionName,versionNumber, fn) {
		// console.log('recieved login msg');
		var address = socket.handshake.address;
		var now = new Date();
		console.log("New create address request from " + address.address + ":" + address.port);
		var createrequest={'user':user,'versionName':versionName,'versionNumber':versionNumber,'address':address.address,'port':address.port, 'datetime':now.format() };
		console.log(user);
		users.updateUser(user,createrequest, function(usr) {
			//notify users that queried this address that it has changed
			
			socket.emit('updated', user);
		});
	});
	
	socket.on('privateaddressrequest', function(address, user,versionName,versionNumber, fn) {
		// console.log('recieved login msg');
		console.log('_________________________________________');
		console.log(JSON.stringify(address));
		console.log('_________________________________________');
		var a = socket.handshake.address;
		var googleApiKey='';//<-- google API Key
		var now = new Date();
		console.log("New private address request from " + a.address + ":" + a.port);
		var createrequest={'user':user,'versionName':versionName,'versionNumber':versionNumber,'address':a.address,'port':a.port, 'datetime':now.format() };
		console.log(user);
		//users.updateUser(user,createrequest, function(usr) {
			var sender = new gcm.Sender(googleApiKey);
			var message = new gcm.Message();
			message.addData('title','Private Address Authorization Request');
			message.addData('message','Request has been made to your "' + address.name  + '" address by ' + user.name + ', Kindly authorize sharing it');
			message.addData('imgurl',user.picture);
			message.addData('token', user.registrationID);
			message.addData('address', JSON.stringify(address));
			message.addData('infotype',2);
			message.delay_while_idle = 3;
			console.log('sending notifications to single id ' + address.registrationID);
			var registrationIds = [];
			registrationIds.push(address.registrationID);
			console.log('sending notifications');
			console.log(registrationIds);
			sender.send(message, registrationIds, 4, function (err, result) {
			console.log(result);
			});								
			//socket.emit('requestnoted', addrs);
		//});
	});

	socket.on('privateaddressauthorization', function(address, token,versionName,versionNumber, fn) {
		console.log('_________________________________________');
		console.log(JSON.stringify(address));
		console.log('_________________________________________');
		var a = socket.handshake.address;
		var googleApiKey=''; //<-- google API Key
		var now = new Date();
		console.log("private address authorization from " + a.address + ":" + a.port);
		//var createrequest={'user':user,'versionName':versionName,'versionNumber':versionNumber,'address':a.address,'port':a.port, 'datetime':now.format() };
		//console.log(user);
		//users.updateUser(user,createrequest, function(usr) {
		var sender = new gcm.Sender(googleApiKey);
		var message = new gcm.Message();
		message.addData('title', 'Authorization for ' + address.locationname);
		message.addData('message','Request has been authorize - click on this to view it');
		message.addData('address', JSON.stringify(address));
		message.addData('infotype',3);
		message.delay_while_idle = 3;
		//message.delayWhileIdle = true;
		//message.timeToLive = 3600;
		console.log('sending notifications to single id ' + token);
		var registrationIds = [];
		registrationIds.push(token);
		console.log('sending notifications');
		console.log(registrationIds);
		sender.send(message, registrationIds, 4, function (err, result) {
		console.log(result);
		});			

		//add to lists of persons of interst
			//socket.emit('requestnoted', addrs);
		//});
	});
	
	socket.on('privateaddressrejection', function(address, token,versionName,versionNumber, fn) {
		// console.log('recieved login msg');
		var a = socket.handshake.address;
		var googleApiKey=''; //<-- Google API Key
		var now = new Date();
		console.log("private address rejection from " + a.address + ":" + a.port);
		//var createrequest={'user':user,'versionName':versionName,'versionNumber':versionNumber,'address':a.address,'port':a.port, 'datetime':now.format() };
		//console.log(user);
		//users.updateUser(user,createrequest, function(usr) {
		var sender = new gcm.Sender(googleApiKey);
		var message = new gcm.Message();
		message.addData('title','Address Request Rejection');
		message.addData('message','The Request made for the private address "' + address.locationname  + '" Has been rejected by the owner');
		message.addData('infotype',1);
		message.delay_while_idle = 3;
		console.log('sending notifications to single id ' + token);
		var registrationIds = [];
		registrationIds.push(token);
		console.log('sending notifications');
		console.log(registrationIds);
		sender.send(message, registrationIds, 4, function (err, result) {
		console.log(result);
		});								
			//socket.emit('requestnoted', addrs);
		//});
	});

	
	socket.on('monikerlookup', function(querystr, currentusr,versionName,versionNumber, fn) {
		console.log(querystr);
		var now = new Date();
		
		var address = socket.handshake.address;
		console.log("New moniker lookup request from " + address.address + ":" + address.port);
		console.log('recieved query msg ' + querystr);
		//var queryrequest={'user':currentusr,'versionName':versionName,'versionNumber':versionNumber };
		users.findIfMonikerExists(querystr, function(addrs) {		
			socket.emit('queried', addrs);
		});	
		
	});
	
	socket.on('query', function(querystr, currentusr,versionName,versionNumber, fn) {
		console.log(querystr);
		var now = new Date();
		var address = socket.handshake.address;
		console.log("New lookup request from " + address.address + ":" + address.port);
		console.log('recieved query msg ' + querystr);
		//var queryrequest={'user':currentusr,'versionName':versionName,'versionNumber':versionNumber };
		users.findLike(querystr, function(addrs) {
			socket.emit('queried', addrs);
		});

		/*
		 * if (addresses[userid]) { fn(true); } else { fn(false);
		 * addresses[userid] = socket.address = address;
		 * socket.broadcast.emit('announcement', { user : address, action :
		 * 'connected' }); io.sockets.emit('addresses', addresses); }
		 */
	});

	socket.on('requestedaddress', function(address, currentusr,versionName,versionNumber, fn) {
		console.log(address);
		var googleApiKey=''; //<-- google api Key
		var now = new Date();
		// console.log('recieved query msg ' + querystr);
		var a = socket.handshake.address;
		console.log("New adderess request from " + a.address + ":" + a.port);
		var requestedaddressrequest={'user':currentusr,'versionName':versionName,'versionNumber':versionNumber,'address':a.address,'port':a.port, 'datetime':now.format() };
		users.updateRequested(address, currentusr,requestedaddressrequest, function(addrs) {
				//var address=addrs[0];
			if(address.notifywhenqueried){
				var sender = new gcm.Sender(googleApiKey);
				var message = new gcm.Message();
				message.addData('title','Address Request Notification');
				message.addData('message','Request has been made to your "' + address.name  + '" address by ' + currentusr.name);
				message.addData('imgurl',currentusr.picture);
				message.addData('infotype',1);
				message.delay_while_idle = 3;
				console.log('sending notifications to single id ' + address.registrationID);
				var registrationIds = [];
				registrationIds.push(address.registrationID);
				console.log('sending notifications');
				console.log(registrationIds);
				sender.send(message, registrationIds, 4, function (err, result) {
					console.log(result);
				});								
				socket.emit('requestnoted', addrs);
			}
		});

		/*
		 * if (addresses[userid]) { fn(true); } else { fn(false);
		 * addresses[userid] = socket.address = address;
		 * socket.broadcast.emit('announcement', { user : address, action :
		 * 'connected' }); io.sockets.emit('addresses', addresses); }
		 */
	});

	socket.on('disconnect', function() {
		console.log('Server running at http://127.0.0.1:3000/');
		// if (!socket.address)
		// return;

		// delete nicknames[socket.userid];
		socket.broadcast.emit('announcement', 'disconnected');
		// socket.broadcast.emit('nicknames', nicknames);
	});
});