import tensorflow as tf

import numpy as np

from PIL import Image




#***************parametres
#img
IMG_WIDTH = 299
IMG_HEIGHT = 299


#***********************

inception_model = tf.keras.applications.InceptionV3(weights='imagenet', input_shape=(IMG_HEIGHT, IMG_WIDTH, 3))

model = tf.keras.Model(inception_model.input, inception_model.output)




imagenetLabel_path = "imagenetLabels.txt"
imagenet_labels = np.array(open(imagenetLabel_path).read().splitlines())

######" functions


def normalize(input_image):
  input_image = (input_image / 127.5) -1
  return input_image



def resize(input_image, height, width):
  input_image = tf.image.resize(input_image, [height, width], method=tf.image.ResizeMethod.BICUBIC, antialias=True)
  return input_image





def processImage(file):
    img = Image.open(file)

    img = np.array(img)
    img = tf.cast(img, tf.float32)
    img = normalize(img)
    img = resize(img, IMG_HEIGHT, IMG_WIDTH)

    img = tf.expand_dims(img, axis = 0)

    return img



def executeModel(file):
    img = processImage(file)

    probs = model(img)

    class_idx = np.argmax(probs[0])



    return imagenet_labels[class_idx + 1]



##############











