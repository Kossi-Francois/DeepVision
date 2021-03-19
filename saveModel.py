import tensorflow as tf

import numpy as np
from  import InceptionV3

inception_model = tf.keras.applications.InceptionV3(weights='imagenet', input_shape=(299, 299, 3))



model = tf.keras.Model(inception_model.input, inception_model.output)