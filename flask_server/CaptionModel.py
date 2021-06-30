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










class BahdanauAttention(tf.keras.Model):
  def __init__(self, units):
    super(BahdanauAttention, self).__init__()
    self.W1 = tf.keras.layers.Dense(units) #w encoder
    self.W2 = tf.keras.layers.Dense(units) #w decoder hidden
    self.V = tf.keras.layers.Dense(1) #w alignement

  def call(self, features, hidden):
    hidden_with_time_axis = tf.expand_dims(hidden, 1)

    attention_hidden_layer = (tf.nn.tanh(self.W1(features) + self.W2(hidden_with_time_axis)))

    score = self.V(attention_hidden_layer)

    attention_weights = tf.nn.softmax(score, axis=1)

    context_vector = attention_weights * features
    context_vector = tf.reduce_sum(context_vector, axis=1)

    return context_vector, attention_weights






class CNN_Encoder(tf.keras.Model):

    def __init__(self, embedding_dim):
        super(CNN_Encoder, self).__init__()
        self.fc = tf.keras.layers.Dense(embedding_dim)

    def call(self, x):
        x = self.fc(x)
        x = tf.nn.relu(x)
        return x





class RNN_Decoder(tf.keras.Model):
  def __init__(self, embedding_dim, units, vocab_size):
    super(RNN_Decoder, self).__init__()
    self.units = units

    self.embedding = tf.keras.layers.Embedding(vocab_size, embedding_dim)

    self.gru = tf.keras.layers.LSTM(self.units,
                                   return_sequences=True,
                                   return_state=True,
                                   recurrent_initializer='glorot_uniform')
    #self.gru = tf.keras.layers.Bidirectional(self.lstm, merge_mode='sum')

    self.fc1 = tf.keras.layers.Dense(self.units)
    self.fc2 = tf.keras.layers.Dense(vocab_size)

    self.attention = BahdanauAttention(self.units)
    self.drop =  tf.keras.layers.Dropout(0.35)

  def call(self, inputs, features, hidden, training = True):

    hidden_h,  hidden_c = hidden[0], hidden[1]


    context_vector, attention_weights = self.attention(features, hidden_h)

    x = self.embedding(inputs)


    x = tf.concat([tf.expand_dims(context_vector, 1), x], axis=-1)

    r_hidden_h = tf.zeros((x.shape[0], self.units))


    output, state_h, state_c = self.gru(x, initial_state=[r_hidden_h,  hidden_c])


    x = self.fc1(output)

    x = tf.reshape(x, (-1, x.shape[2]))

    x = self.drop(x, training= training)

    x = self.fc2(x)

    return x, [state_h, state_c], attention_weights


  def reset_state(self, batch_size):
    return [tf.zeros((batch_size, self.units)), tf.zeros((batch_size, self.units))]








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
