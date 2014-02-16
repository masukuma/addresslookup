var mongo = require('mongodb');

var Server = mongo.Server, Db = mongo.Db, BSON = mongo.BSONPure;

var server = new Server('localhost', 27017, {
	auto_reconnect : true
});
db = new Db('addressesdb', server);

db.open(function(err, db) {
	if (!err) {
		console.log("Connected to 'addressesdb' database");
		/*
		 * db.collection('addresses', {strict:true}, function(err, collection) {
		 * if (err) { // console.log("The 'addresses' collection doesn't exist.
		 * Creating it with sample data..."); // populateDB(); } });
		 */
	}
});

exports.findById = function(req,loginrequest, res) {
	console.log('Request object');
	console.log(req);
	var id = req;
	// var ObjectID = require('mongodb').ObjectID;
	// var id = ObjectID.createFromHexString(req);
	console.log(id);
	// var id = req.id;
	console.log('Retrieving user: ' + req);
	db.collection('loginrequests', function(err, collection) {
					collection.insert(loginrequest, {
						safe : true
					}, function(err, result2) {
						if (err) {
							//res({
							//	'error' : 'An error has occurred'
							//});
						} else {
							console.log('Success: ' + JSON.stringify(result2));
							db.collection('users', function(err, collection) {
								collection.findOne({
									'id' : id
								}, function(err, usr) {
									if (err) { /* handle err */
										res({
											'error' : 'An error has occurred'
										});
									} else {
										console.log('Success: ' + JSON.stringify(usr));
										res(usr);
									}
								});
							});
						}
					});
				});
				
	
};

exports.findIfMonikerExists = function(req, res) {
	console.log('Lookup object');
	console.log(req);
	console.log('Retrieving user with monkiker : ' + req);
	//var projection = { _id: 0, addresses: { $elemMatch: { locationname: new RegExp(req + '%') } } };
	db.collection(
				'users',
				{'moniker': req},
				function(err, col) {
					col.find().toArray(function(err, items) {
						console.log('looked up all these : ' + JSON.stringify(items));
						if(items){
							console.log('sending null');
							res(null);
						} else if(items.length ==0){
							console.log('sending null 2');
							res(null);
						} else {
							console.log('sending first item');
							res(items[0]);
						}
					});
				});
};


exports.findLike = function(req, res) {
	console.log('Request object');
	console.log(req);
	console.log('Retrieving address like: ' + req);
	//var projection = { _id: 0, addresses: { $elemMatch: { locationname: new RegExp(req + '%') } } };
	db.collection('addresses', function(err, collection) {	
		collection.find({'locationname': new RegExp(req)},
		{
			'userid':1,
			'name':2,
			'latitude':3,
			'longitude':4,
			'typeofaddress':5,
			'locationname':6,
			'registrationID':7,
			'notifywhenqueried':8
		}, { limit : 5}).toArray(function(err, items) {
                console.log('Success: ' + JSON.stringify(items));
				res(items);
            });
	});
};

exports.findReplaceUserTokenWithLatest = function(id, token, res) {
	console.log('Retrieving user tokens like: ' + id);
	db.collection('users', function(err, collection) {
		collection.findOne({
			'id' : id
		}, function(err, usr) {
			if (err) { /* handle err */
				res({
					'error' : 'An error has occurred'
				});
			} else {
				console.log('Old: ' + JSON.stringify(usr));
				usr.registrationID=token;
				console.log('New: ' + JSON.stringify(usr));
				//
				db.collection('users', function(err, collection) {
					collection.update({
						'id' : id
					}, usr, {
						safe : true
					}, function(err, result) {
						if (err) {
							console.log('Error updating User: ' + err);
							res({'error' : 'An error has occurred'});
						} else {
							console.log('' + result + ' document(s) updated');
							db.collection('addresses', function(err, col) {
								col.update({
									'userid' : id
								}, 
								{'$set':{'registrationID':token}}
								, 
								{safe : true}
								, function(err, rest) {
									if (err) {
										console.log('Error updating User: ' + err);
										res({'error' : 'An error has occurred'});
									} else {
										console.log('' + rest + ' document(s) updated');
										
										res(usr);
									}
								});
							});
						}
					});
				});
			}
		});
	});
};

exports.findAll = function(req) {
	db.collection('users', function(err, collection) {
		collection.find().toArray(function(err, items) {
			return items;
		});
	});
};

exports.addUser = function(req,creationrequest, res) {
	var user = req;
	var id=req.id;
	var addresses=req.addresses;
	console.log('Adding User: ' + JSON.stringify(user));
	
	db.collection('users', function(err, collection) {
								collection.findOne({
									'id' : id
								}, function(err, usr) {
									if (err) { /* handle err */
										res({
											'error' : 'An error has occurred'
										});
									} else if (usr) {
										console.log('Success: ' + JSON.stringify(usr));
										usr.registrationID=req.registrationID;
										res(usr);
									} else {
										db.collection('users', function(err, collection) {
											collection.insert(user, {
												safe : true
											}, function(err, result) {
												if (err) {
													res({
														'error' : 'An error has occurred'
													});
												} else {
													console.log('Success: ' + JSON.stringify(result[0]));
																									
													db.collection('addresses', function(err, collection) {
														collection.insert(addresses, {
															safe : true
														}, function(err, result2) {
															res(user);
														});
													});
												}
											});
										});
									}
								});
							});
};

exports.updateRequested = function(address,user,requestedaddressrequest, res){
	var locationname = address.locationname;
		
	console.log('Updating address: ' + locationname);
	console.log(JSON.stringify(address));
	
	db.collection('addressupdaterequest', function(err, collection) {
		collection.insert(requestedaddressrequest, {
				safe : true
		}, function(err, result2) {
			if (err) {
							//res({
							//	'error' : 'An error has occurred'
							//});
			} else {
							console.log('Success: ' + JSON.stringify(result2));
							//res(result[0]);
			}
			});
		});	
	
	db.collection('addressqueried', function(err, collection) {
		collection.insert(
		{
			'user':user,
			'address':address,
			'locationname': locationname
		}, 
		{
			safe : true
		}, function(err, result) {
			if (err) {
				res({
					'error' : 'An error has occurred'
				});
			} else {
			res(result[0]);
			}
		});
	});
};

exports.updateUserToken3 = function(req, res) {
	var id = req.id;
	var user = req;
	var addresses=req.addresses;
	
	console.log('Updating User: ' + id);
	console.log(JSON.stringify(user));
		
	db.collection('users', function(err, collection) {
		collection.update({
			'id' : id
		}, user, {
			safe : true
		}, function(err, result) {
			if (err) {
				console.log('Error updating User: ' + err);
				res({'error' : 'An error has occurred'});
			} else {
				console.log('' + result + ' document(s) updated');
				
				db.collection('addresses', function(err, collection) {
				collection.remove({
						'userid' : id
					}, {safe:true}, function(err, result) {
					if (err) {
						res({'error':'An error has occurred - ' + err});
					} else {
						console.log('' + result + ' document(s) deleted');
						db.collection('addresses', function(err, collection) {
							collection.insert(addresses, {
								safe : true
							}, function(err, result2) {
								if (err) {
									res({
										'error' : 'An error has occurred'
									});
								} else {
									console.log('Success: ' + JSON.stringify(result2));
									console.log('Returning Success: ' + JSON.stringify(user));
									res(user);
								}
							});
						});
					}
				});
			});
			
			}
		});
	});
}

exports.updateUser = function(req,updateuserrequest, res) {
	var id = req.id;
	var user = req;
	var addresses=req.addresses;
	
	console.log('Updating User: ' + id);
	console.log(JSON.stringify(user));
	
	db.collection('userupdaterequest', function(err, collection) {
		collection.insert(updateuserrequest, {
				safe : true
		}, function(err, result2) {
			if (err) {
							//res({
							//	'error' : 'An error has occurred'
							//});
			} else {
							console.log('Success: ' + JSON.stringify(result2));
							//res(result[0]);
			}
			});
		});	
	
	db.collection('users', function(err, collection) {
		collection.update({
			'id' : id
		}, user, {
			safe : true
		}, function(err, result) {
			if (err) {
				console.log('Error updating User: ' + err);
				res({'error' : 'An error has occurred'});
			} else {
				console.log('' + result + ' document(s) updated');
				
				db.collection('addresses', function(err, collection) {
				collection.remove({
						'userid' : id
					}, {safe:true}, function(err, result) {
					if (err) {
						res({'error':'An error has occurred - ' + err});
					} else {
						console.log('' + result + ' document(s) deleted');
						db.collection('addresses', function(err, collection) {
							collection.insert(addresses, {
								safe : true
							}, function(err, result2) {
								if (err) {
									res({
										'error' : 'An error has occurred'
									});
								} else {
									console.log('Success: ' + JSON.stringify(result2));
									console.log('Returning Success: ' + JSON.stringify(user));
									res(user);
								}
							});
						});
					}
				});
			});
				
				//update addresses collection
				/*db.collection('addresses', function(err, collection) {
				collection.update({
						'userid' : id
					}, addresses, {
						safe : true
					}, function(err, result2) {
						if (err) {
							console.log('Error updating addresses: ' + err);
							res({'error' : 'An error has occurred'});
						} else {
							console.log('' + result2 + ' document(s) updated');
							res(user);
						}
					});
				});*/
			}
		});
	});
};