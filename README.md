# Movesense data recorder
This is a on going project with the goal of developing a simple Android app to acquire IMU6 (acc and gyro) data from a Movesense device and save it as experiments. The app allows the user to save subjects and acquire data for multiple movements for a given subject. The data can be exported to .csv and saved on the mobile's shared folders. The app is 'hardcoded' to subscribe to IMU6 @52Hz to avoid possible mistakes in data acquisition.

Data will later on be used on HAR. The goal is to train a ML model with data acquired for multiple CrossFit movements to identify the movement being performed. The feature will be used for CrossFit to allow automatic computation of round split times, moving times and rest times for analysis of pacing and performance.

Many pieces of code were provided by Anders Lindström at KTH mobile applications course in 2021.

If you would like to use this app for your experiment, make sure to download the DFU and update it to your sensor using Movesense showcase app. If your experiment requires different subscription settings, send me a message requesting the addition of this feature. If it's of use for the sports science community, I'd gladly develop it.

 >> If you use the code or extend it please cite it <<
