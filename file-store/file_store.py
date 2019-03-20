from flask import Flask, request, Response, jsonify
import os
import base64
import requests

app = Flask(__name__)
# Port number is required to fetch from env variable
# http://docs.cloudfoundry.org/devguide/deploy-apps/environment-variable.html#PORT

cf_port = os.getenv("PORT")

# Only get method by default
@app.route('/', methods=['GET'])
def hello():
    return 'File Store Microservice.'

# post method
@app.route('/', methods=['POST'])
def img_to_pdf():
    request_json = request.get_json()
    base64content = request_json.get('content')
    filename = request_json.get('filename')
    folder = request_json.get('folder')
    api = request_json.get('object_store_api')
    upload_object_path = '/api/uploadObject?path=' + folder
    if folder != '':
        upload_object_path += '/'
    
    pdfdata = base64.b64decode(str(base64content))
    
    file = open(filename, 'wb')
    file.write(pdfdata)

    file = open(filename, 'rb')
    files = {'file': file}
    res = requests.post(api + upload_object_path, files=files)
    os.remove(filename)
    return res.text

if __name__ == '__main__':
	if cf_port is None:
		app.run(host='0.0.0.0', port=5000, debug=True)
	else:
		app.run(host='0.0.0.0', port=int(cf_port), debug=True)
