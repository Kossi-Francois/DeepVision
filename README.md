# DeepVision
A image captioning and OCR app with audio feedback and voice control.
With image captioning, this application can help visually impaired people, or help to learn a new language ...


How to launch the application

This project contains two parts, the application (android) and a server (Flask)


## Flask server
This server is written in Python in a colab notebook (more convenient for the tensorflow environment)

To start the server, go to this [address](https://drive.google.com/drive/folders/1iFL-oawdZFVt3K1xv_dzWim0nOT24KYy?usp=sharing) and download the pre-trained model checkpoint, then put the 3 downloaded files (checkpoint, ckptxxx.data, ckptxxx.index) in a folder named **checkpoint**.
Go to the CaptionModel.py file and set the parameter *<projectPath>* to the path of the created **checkpoints** folder, then execute the *serverEndpoint.ipynb* file
For the moment it works with ngrok, so a web address will be generated, and this address will allow you to configure the android application.
  
This server can also work without the application by calling the function and passing the image in the request form as it is currently done.

  



## Android app

To launch the application, open android studio and run the application, then open the application and go to *options*, enter the web address of the server in the *"Deep Vision API URL"* field, click on *save options*, go back to the main page and click on *camera* and voilà.
You have two buttons: *describe* for the captioning image and *read* for OCR.
  
 in options you have other options like "Glasses camera address" to enter the IP address of a camera and use it instead of the smartphone camera
you can also activate the voice command which currently supports French and English, so instead of clicking on *describe* or *read* you can say *describe* or *read*.


## Demo
Video screenshot of the app running, there are gifs but on the original video we can hear the audio feedback.
 
![alt text](https://github.com/Kossi-Francois/server/blob/main/demo_people_riding_bikes.gif?raw=true)
![alt text](https://github.com/Kossi-Francois/server/blob/main/demo_sheep.gif?raw=true)
![alt text](https://github.com/Kossi-Francois/server/blob/main/demo_supermarket.gif?raw=true)
![alt text](https://github.com/Kossi-Francois/server/blob/main/demo_pizza.gif?raw=true)


