from flask import Flask, request, Response, jsonify
import cv2
import os
import base64
import numpy as np

app = Flask(__name__)
# Port number is required to fetch from env variable
# http://docs.cloudfoundry.org/devguide/deploy-apps/environment-variable.html#PORT

cf_port = os.getenv("PORT")

# Only get method by default
@app.route('/', methods=['GET'])
def hello():
    return 'Blur Detection Microservice.'

# post method
@app.route('/', methods=['POST'])
def blur_detection():
    request_json = request.get_json()
    imgbase64 = request_json.get('img')
    imgdata = base64.b64decode(str(imgbase64).replace('data:image/jpeg;base64','').replace('data:image/png;base64',''))
    nparr = np.fromstring(imgdata, dtype='uint8')
    img = cv2.imdecode(nparr, cv2.COLOR_BGR2RGB)
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    variance = round(cv2.Laplacian(gray, cv2.CV_64F).var(), 2)
    
    return str(variance)

if __name__ == '__main__':
	if cf_port is None:
		app.run(host='0.0.0.0', port=5000, debug=True)
	else:
		app.run(host='0.0.0.0', port=int(cf_port), debug=True)
