# DeepVision
A image captioning and OCR app with audio feedback and voice control.
With image captioning, this application can help visually impaired people, or help to learn a new language ...


How to launch the application

This project contains two parts, the application (android) and a server (Flask)


Flask server
This server is written in Python in a colab notebook (simpler for the tensorflow environment)

To start the server, go to this address and download the pre-trained model checkpoint, then put the 3 downloaded files (checkpoint, ckptxxx.data, ckptxxx.index) in a folder named "checkpoint"
Go to the CaptionModel.py file and set the parameter <projectPath> to the path of the created "checkpoints" folder, then execute the serverEndpoint.ipynb file
For the moment it works with ngrok, so a web address will be generated, and this address will allow you to configure the android application

  
adrresse : https://drive.google.com/drive/folders/1iFL-oawdZFVt3K1xv_dzWim0nOT24KYy?usp=sharing



Android app



Demo
a api wich will be hosted on python anywhere
![alt text](https://github.com/Kossi-Francois/server/blob/main/text1.jpg?raw=true)](https://github.com/Kossi-Francois/server/blob/main/Edited_20210628_193748.mp4)

https://github.com/Kossi-Francois/server/blob/main/Edited_20210628_193748.mp4

<iframe width="854" height="480" src="https://github.com/Kossi-Francois/server/blob/main/Edited_20210628_193748.mp4" frameborder="0" allowfullscreen></iframe>
