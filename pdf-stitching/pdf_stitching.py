from flask import Flask, request, Response, jsonify
import os
import json
from PyPDF2 import PdfFileMerger
import requests
import gc

app = Flask(__name__)
# Port number is required to fetch from env variable
# http://docs.cloudfoundry.org/devguide/deploy-apps/environment-variable.html#PORT

cf_port = os.getenv("PORT")

# Only get method by default
@app.route('/', methods=['GET'])
def hello():
    return 'PDF Stitching Microservice.'

# post method
@app.route('/', methods=['POST'])
def pdf_stitching():
    request_json = request.get_json()
    pdffilenames = request_json.get('files')
    outputname = request_json.get('outputname')
    api = request_json.get('object_store_api')
    get_object_path = '/api/getObject?id='
    folder_path = request_json.get('folder')
    upload_object_path = '/api/uploadObject?path=' + folder_path
    if folder_path != '':
        upload_object_path += '/'

    for filename in pdffilenames:
        name = filename['name']
        r = requests.get(api + get_object_path + name, allow_redirects=True)
        print('status code: ' + str(r.status_code))
        if r.status_code == 200:
            print('writing file: ' + name.split('/')[-1])
            open(name.split('/')[-1], 'wb').write(r.content)
        else:
            return Response("failed to retrieve PDF files from the object store", status = r.status_code)

    merger = PdfFileMerger()

    for pdf in pdffilenames:
        merger.append(open(pdf['name'].split('/')[-1], 'rb'))

    with open(outputname, 'wb') as fout:
        merger.write(fout)

    file = open(outputname, 'rb')
    files = {'file': file}
    res = requests.post(api + upload_object_path, files = files)
    for pdf in pdffilenames:
        print('Deleting file: ' + pdf['name'].split('/')[-1])
        os.remove(pdf['name'].split('/')[-1])
    os.remove(outputname) 
    print('Deleted file: ' + outputname)
    gc.collect()
    return res.text

if __name__ == '__main__':
	if cf_port is None:
		app.run(host='0.0.0.0', port=5000, debug=True)
	else:
		app.run(host='0.0.0.0', port=int(cf_port), debug=True)
