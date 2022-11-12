This project is based on https://github.com/MariahSabioni/movesense-data-recorder and modified to fit long jump and triple jump data acquisition.

Description of the original project by https://github.com/MariahSabioni

# Movesense data recorder
This is a on going project with the goal of developing a simple Android app to acquire IMU6 (acc and gyro) data from a Movesense device and save it as experiments. The app allows the user to save subjects and acquire data for multiple movements for a given subject. The data can be exported to .csv and saved on the mobile's shared folders. The app is 'hardcoded' to subscribe to IMU6 @52Hz to avoid possible mistakes in data acquisition.

Simultaneous with the IMU6 data, a video is recorded of the jump, to assist visualization of the jump and detection of important timestamps in the IMU6 curves. The 2 methods are synced and saved together for possible post-processing steps. Future work includes ML algorithms that compare the jump with the 'perfect' technique defined by videos from the top ranking jumpers in the world.

If you would like to use this app for your experiment, make sure to download the DFU and update it to your sensor using Movesense showcase app. If your experiment requires different subscription settings, send me a message requesting the addition of this feature. If it's of use for the sports science community, I'd gladly develop it.

If you use the code or extend it please cite it.
