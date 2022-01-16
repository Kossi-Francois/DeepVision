import tensorflow as tf
import tensorflow_io as tfio
import numpy as np
import os
import time
import json
import matplotlib.pyplot as plt
from PIL import Image



##set the path to the folder which contains the "checkpoints" folder
##or leave it as such if the "checkpoints" folder is in the current folder
projectPath = ""


IMG_HEIGHT, IMG_WIDTH = 575, 575
features_shape = 2048
attention_features_shape = 256
embedding_dim = 512
units = 1024
top_k = 10000
vocab_size = top_k + 1








image_model = tf.keras.applications.InceptionV3(include_top=False, weights='imagenet')

new_input = image_model.input
hidden_layer = image_model.layers[-1].output
hidden_layer = tf.keras.layers.Reshape((-1, hidden_layer.shape[3]))(hidden_layer)
image_features_extract_model = tf.keras.Model(new_input, hidden_layer)





##function to use the application with IP cameras
def load_image_glass1(img):

    print("load glass1")
    img = tfio.experimental.color.rgba_to_rgb(img)



    img = tf.convert_to_tensor(img)


    imgshape = img.shape
    print(imgshape)
    offset_height, offset_width, target_height, target_width = 500, 0, imgshape[0], imgshape[1]
    img = img[offset_height:target_height, offset_width:target_width, :]

    print(img.shape)

    #img = tf.image.adjust_brightness(img, delta=0.1)
    img = tf.expand_dims(img, axis=0)

    img = tf.image.resize(img, (IMG_HEIGHT, IMG_WIDTH), method=tf.image.ResizeMethod.BICUBIC, antialias=True)
    img = tf.keras.applications.inception_v3.preprocess_input(img)


    return img




##function to use the application with the phone camera
def load_image_phoneCam(img):

    print("load phone cam")

    img = tf.convert_to_tensor(img)

    imgshape = img.shape

    img = tf.expand_dims(img, axis=0)

    img = tf.image.resize(img, (IMG_HEIGHT, IMG_WIDTH), method=tf.image.ResizeMethod.BICUBIC, antialias=True)
    img = tf.keras.applications.inception_v3.preprocess_input(img)


    return img






#### model #####

#BahdanauAttention

#CNN_Encoder

#RNN_Decoder



############





encoder = CNN_Encoder(embedding_dim)
decoder = RNN_Decoder(embedding_dim, units, vocab_size)


checkpoint_path = projectPath + "checkpoints"
ckpt = tf.train.Checkpoint(encoder=encoder,
                           decoder=decoder)
ckpt_manager = tf.train.CheckpointManager(ckpt, checkpoint_path, max_to_keep=5)




ckpt.restore(ckpt_manager.latest_checkpoint)




f = open('tokenizer_conf_v3.txt', "r")
tok = f.read()
tokenizer = tf.keras.preprocessing.text.tokenizer_from_json(tok)




max_length = 60

def FullModel():
  inp_img   = tf.keras.layers.Input(shape=[IMG_HEIGHT, IMG_WIDTH,3], dtype=tf.float32)

  img_tensor_features = image_features_extract_model(inp_img) #inceptionV3

  inp_features = encoder(img_tensor_features)



  dec_input  = tf.constant([[3]], dtype=tf.int64)
  inp_hidden = decoder.reset_state(batch_size=1)
  tf_result = tf.constant([3], dtype=tf.int64)



  for i in range(0, max_length):
    y_predictions, inp_hidden, _ = decoder(dec_input, inp_features, inp_hidden, training=False)    # attention + Gru



    #predicted_id = tf.random.categorical(y_predictions, 1)[0]   #or tf.argmax(y_predictions[0]) * tf.ones(1, dtype=tf.int64)
    predicted_id = tf.argmax(y_predictions[0]) * tf.ones(1, dtype=tf.int64)


    tf_result = tf.keras.layers.Concatenate(axis=0)([tf_result, predicted_id])


    dec_input = predicted_id*tf.ones((1,1), dtype=tf.int64)

  #tf_result = tf.cast(tf_result, dtype=tf.float32)
  return tf.keras.Model(inputs =  inp_img, outputs =  tf_result )


fullModel = FullModel()







def executeModel(img, param_process):

  if param_process == "phoneCam":
    img =load_image_phoneCam(img)

  if param_process == "glass1":
    img =load_image_glass1(img)

  result = (fullModel(img, training=False)).numpy()
  result = result[1:]


  r_string = ""
  for i in result:

    if i == 4: #token for the end of the generated text
      break

    else:
      r_string += (" " + tokenizer.index_word[i])


  return r_string
