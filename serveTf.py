#serving TF
import tensorflow as tf

import numpy as np

from flask import Flask, jsonify, request,make_response
#from flask_cors import CORS

import base64
import json
from io import BytesIO

from tensorflow.keras.preprocessing import image


from PIL import Image




app = Flask(__name__)
#CORS(app)




@app.route('/hello/', methods=['GET', 'POST'])
def hello_world():
    return 'Hello, World!'



@app.route('/imageclassifier/predict/', methods=['POST'])
def image_classifier():
    # Decoding and pre-processing base64 image
    # img = image.img_to_array(image.load_img(BytesIO(base64.b64decode(request.form['b64'])),
    #                                         target_size=(224, 224))) / 255.

    file = request.files['b64']
    print(file)
    # Read the image via file.stream
    img = Image.open(file)




    return jsonify(name = 'kossi')



if __name__ == "__main__":
    app.run(host= '0.0.0.0')
    #app.run(host='0.0.0.0',threaded=True)