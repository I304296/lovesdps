from flask import Flask, request, Response, jsonify
import cv2
import os
import base64
import numpy as np
import json

app = Flask(__name__)
# Port number is required to fetch from env variable
# http://docs.cloudfoundry.org/devguide/deploy-apps/environment-variable.html#PORT


cf_port = os.getenv("PORT")

# Only get method by default
@app.route('/', methods=['GET'])
def hello():
    return 'Contrast Enhancement Microservice.'

# post method
@app.route('/', methods=['POST'])
def enhance_contrast():
    request_json = request.get_json()
    imgbase64 = request_json.get('img')
    contrast_threshold = request_json.get('contrast_threshold')
    imgdata = base64.b64decode(str(imgbase64).replace('data:image/jpeg;base64','').replace('data:image/png;base64',''))
    nparr = np.fromstring(imgdata, dtype='uint8')
    img = cv2.imdecode(nparr, cv2.COLOR_BGR2LAB)

    lab = cv2.cvtColor(img,cv2.COLOR_BGR2LAB)

    l, a, b = cv2.split(lab)

    clahe = cv2.createCLAHE(clipLimit=contrast_threshold, tileGridSize=(8, 8))
    cl = clahe.apply(l)
    
    limg = cv2.merge((cl, a, b))

    new_img = cv2.cvtColor(limg, cv2.COLOR_LAB2BGR)

    # Conver image back to JPG
    retval, buffer = cv2.imencode('.jpg', new_img)
    new_imgbase64  = base64.b64encode(buffer)
    
    return new_imgbase64

if __name__ == '__main__':
	if cf_port is None:
		app.run(host='0.0.0.0', port=5000, debug=True)
	else:
		app.run(host='0.0.0.0', port=int(cf_port), debug=True)
