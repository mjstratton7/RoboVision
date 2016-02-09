This is code that is meant to be run on an Android device, which will use the Camera and the Qualcomm FastCV library to find and locate a target, send coordinate information over usb OTG to a Arduino device, which will move a Pan and Tilt sytem. 

Once The camera has confirmed that it is on target, fire commands are sent.

The detection process has 5 steps, 
1 - The live preview is stripped of all colors except red, anything red is targeted at this point.
2 - Then the device looks for a red squares
3 - Next it searches for a circle in it (the actual target)
4 - Then it confirms by looking for the while lines through the target (refer to photo of target)
5 - If the target is not centered, it sends coordinate information to the arduino of how to move. H:0 V:0 is origin, and centered on target. (Ex. H:-2 V:8)
5 - The camera confirms that it is on target, and does so by making sure 5 consecutive frames match.

Once that is complete a fire command is sent to the arduino, which causes a dart to be fired from the robot.

This project implements Native C code and uses JNI objects to communicate with a Java layer (the UI which the user sees, and the android ADK for communicating with the Arduino.

