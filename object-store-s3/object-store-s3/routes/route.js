var express = require('express');
var bodyParser = require('body-parser');
var router = express.Router();
var xsenv = require('@sap/xsenv');
var xssec = require('@sap/xssec');
var S3 = require('aws-sdk/clients/s3');
var multerS3 = require('multer-s3');
var multer = require('multer');
var vcap_services = JSON.parse(process.env.VCAP_SERVICES);
var urlBase = '/api';

// Set credentials and region
var s3 = new S3({
	apiVersion: '2006-03-01',
	region: vcap_services.objectstore[0].credentials.region,
	endpoint: vcap_services.objectstore[0].credentials.host,
	credentials: {
		accessKeyId: vcap_services.objectstore[0].credentials.access_key_id,
		secretAccessKey: vcap_services.objectstore[0].credentials.secret_access_key
	}
});

router.get('/', function (req, res, next) {
	res.send("Application to test object store");
});

router.get(`${urlBase}/getObjects`, function (req, res, next) {
	console.log("inside getall objects");
	var params = {
		Bucket: vcap_services.objectstore[0].credentials.bucket,
		MaxKeys: 10
	};
	s3.listObjects(params, function (err, data) {
		if (err) console.log(err, err.stack); // an error occurred
		else res.send(data); // successful response
	});
});

router.get(`${urlBase}/getObject`, function (req, res, next) {
	console.log("inside get object id = " + req.query.id);
	var params = {
		Bucket: vcap_services.objectstore[0].credentials.bucket,
		Key: req.query.id
	};
	s3.getObject(params, function (err, data) {
		if (err){
			console.log("Error getting object " + err, err.stack);
		} else {
			res.attachment(req.query.id);
			res.send(data.Body);
		}
	});

});

router.delete(`${urlBase}/deleteObject`, function(req, res, next) {
	console.log("inside delete object id = " + req.query.id);
	var params = {
		Bucket: vcap_services.objectstore[0].credentials.bucket,
             	Key: req.query.id
	};
        s3.deleteObject(params, function (err, data) {
        	if (err){
                          console.log("Error deleting object " + err, err.stack);
                } else {
                          res.send("File has been deleted successfully.");
                }
       	});

});

var upload = multer({
	storage: multerS3({
		s3: s3,
		bucket: vcap_services.objectstore[0].credentials.bucket,
		acl: 'public-read',
		metadata: function (req, file, cb) {
			cb(null, {
				fieldName: file.fieldname
			});
		},
		key: function (req, file, cb) {
			console.log("Inside upload object: " + req.query.path + file.originalname);
			cb(null, req.query.path + file.originalname);
		}
	})
});

router.post(`${urlBase}/uploadObject`, upload.any(), function (req, res, next) {
	res.send('Successfully uploaded ' + JSON.stringify(req.files));
});

module.exports = router;
