#serving TF





from flask import Flask, jsonify, request,make_response
#from flask_cors import CORS


from saveModel import*


#import json

#import tensorflow as tf
#import numpy as np
#from PIL import Image


#
# import base64
#
# from io import BytesIO
#






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

    file = request.files['image']


    class_name = executeModel(file)



    return jsonify(result_class = class_name)



if __name__ == "__main__":
    app.run(host= '0.0.0.0')
    #app.run(host='0.0.0.0',threaded=True)







