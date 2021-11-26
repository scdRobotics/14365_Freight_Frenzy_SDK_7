package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.external.matrices.OpenGLMatrix;
import org.firstinspires.ftc.robotcore.external.matrices.VectorF;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackable;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackableDefaultListener;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackables;
import org.firstinspires.ftc.robotcore.external.tfod.Recognition;
import org.firstinspires.ftc.robotcore.external.tfod.TFObjectDetector;

import java.util.ArrayList;
import java.util.List;

import static org.firstinspires.ftc.robotcore.external.navigation.AngleUnit.DEGREES;
import static org.firstinspires.ftc.robotcore.external.navigation.AxesOrder.XYZ;
import static org.firstinspires.ftc.robotcore.external.navigation.AxesOrder.XZY;
import static org.firstinspires.ftc.robotcore.external.navigation.AxesReference.EXTRINSIC;


@Autonomous(name="BlueWarehouseWarehouse", group="linearOpMode")
public class BlueWarehouseWarehouse extends AutonomousPrime2021 {

    /*
     ***********************
     *   SETUP TENSORFLOW  *
     ***********************
     */
    private static final String TFOD_MODEL_ASSET = "FreightFrenzy_BCDM.tflite";
    private static final String[] LABELS = {
            "Ball",
            "Cube",
            "Duck",
            "Marker"
    };
    private static final String VUFORIA_KEY = "Aba+gBH/////AAABma/0sYDZakYVhtjb1kH5oBVmYfYsDZXTuEZL9m7EdnFKZN/0v/LvE/Yr0NsXiJo0mJmznKAA5MK6ojvgtV1e1ODodBaMYZpgE1YeoAXYpvvPGEsdGv3xbvgKhvwOvqDToPe3x5w6gsq7a4Ullp76kLxRIoZAqaRpOuf1/tiJJQ7gTBFf8MKgbCDosmMDj7FOZsclk7kos4L46bLkVBcD9E0l7tNR0H0ShiOvxBwq5eDvzvmzsjeGc1aPgx9Br5AbUwN1T+BOvqwvZH2pM2HDbybgcWQJKH1YvXH4O62ENsYhD9ubvktayK8hSuu2CpUd1FVU3YQp91UrCvaKPYMiMFu7zeQCnoc7UOpG1P/kdFKP";
    private static String labelName;
    private static int noLabel;
    private TFObjectDetector tfod;

    double DuckRightPos = -1;
    int DuckPosition = 0;

    /*
     ********************
     *   SETUP VUFORIA  *
     ********************
     */
    private static final float mmPerInch        = 25.4f;
    private static final float mmTargetHeight   = 6 * mmPerInch;          // the height of the center of the target image above the floor
    private static final float halfField        = 72 * mmPerInch;
    private static final float halfTile         = 12 * mmPerInch;
    private static final float oneAndHalfTile   = 36 * mmPerInch;

    // Class Members
    private OpenGLMatrix lastLocation   = null;
    private VuforiaLocalizer vuforia    = null;
    private VuforiaTrackables targets   = null ;
    private WebcamName webcamName       = null;

    private boolean targetVisible       = false;
    List<VuforiaTrackable> allTrackables = new ArrayList<VuforiaTrackable>();

    double VufXPos = 0;
    double VufYPos = 0;
    double VufHeading = 0;


    @Override
    public void runOpMode(){
        webcamName = hardwareMap.get(WebcamName.class, "Webcam");
        initVuforia();
        initTfod();
        tfod.activate();
        tfodTrack();

        mapObjects();
        waitForStart();



        tfodTrack();

        //Middle Pos
        if(DuckRightPos>=164&&DuckRightPos<=364) {
            DuckPosition=1;
            telemetry.addData("Duck Position: ", DuckPosition);
            telemetry.update();
        }
        //Right Pos
        else if(DuckRightPos>=463&&DuckRightPos<=663){
            DuckPosition=2;
            telemetry.addData("Duck Position: ", DuckPosition);
            telemetry.update();
        }
        //Left Pos
        else{
            telemetry.addData("Duck Position: ", DuckPosition);
            telemetry.update();
        }

        strafeRightEncoder(70, 0.5);
        zeroBotEncoder(0.5);
        pause(0.5);
        forwardEncoder(110, 0.5);

        pause(0.5);
        strafeRightEncoder(50, 0.5);
        pause(0.5);
        reverseTime(1.5, 0.15);
//Initialize odometry for inside warehouse
        leftEncoder(60, 0.5);
        forwardEncoder(50, 0.5);
        pause(0.5);
        rightEncoder(45, 0.5);
        pause(0.5);
        strafeRightEncoder(15, 0.5);
        reverseEncoder(130, 0.5);
        pause(0.5);
        rightEncoder(180, 0.5);
        zeroBotEncoderOffset(180,0.5);


        pause(1); //Was 0.5


        //call vuforia function vuforiaTrack
        vuforiaTrack();
        telemetry.addData("Vuforia X Pos: ", VufXPos);
        telemetry.addData("Vuforia Y Pos: ", VufYPos);
        telemetry.addData("Vuforia Heading: ", VufHeading);
        int idealXPos = 0; //Change This Value
        int idealYPos = 0; //Change This Value
        int idealHeading = 0; //Change This Value

        /*if(targetVisible) {                       //NEEDS TESTING TO SEE HOW TO SET THE OFFSET OR IF ITS EVEN NECESSARY AT ALL
            if(VufHeading>=0&&VufHeading<180){
                rightEncoder(VufHeading, 0.15);
            }
            else if (VufHeading>=180&&VufHeading<360){
                leftEncoder((360-VufHeading), 0.15);
            }

        }*/

        vuforiaTrack();
        while(!targetVisible) {
            forwardEncoder(5, 0.5);
            vuforiaTrack();
        }
        if(targetVisible) {
            vuforiaTrack();
            forwardEncoder(VufXPos - idealXPos, 0.25); //Was forward encoder, but I think it would be going backwards, not forwards, I haven't tested this though
            strafeLeftEncoder(VufYPos - idealYPos, 0.25);
        }




        //forwardEncoder(70, 0.5);


        //drop off cube


    }

    private void initVuforia() {

        int cameraMonitorViewId = hardwareMap.appContext.getResources().getIdentifier("cameraMonitorViewId", "id", hardwareMap.appContext.getPackageName());
        VuforiaLocalizer.Parameters parameters = new VuforiaLocalizer.Parameters(cameraMonitorViewId);

        parameters.vuforiaLicenseKey = VUFORIA_KEY;
        parameters.cameraName = webcamName;
        parameters.useExtendedTracking = false;


        //  Instantiate the Vuforia engine
        vuforia = ClassFactory.getInstance().createVuforia(parameters);

        // Loading trackables is not necessary for the TensorFlow Object Detection engine; but it is now!
        targets = this.vuforia.loadTrackablesFromAsset("FreightFrenzy");
        allTrackables.addAll(targets);

        identifyTarget(0, "Blue Storage",       -halfField,  oneAndHalfTile, mmTargetHeight, 90, 0, 90);
        identifyTarget(1, "Blue Alliance Wall",  halfTile,   halfField,      mmTargetHeight, 90, 0, 0);
        identifyTarget(2, "Red Storage",        -halfField, -oneAndHalfTile, mmTargetHeight, 90, 0, 90);
        identifyTarget(3, "Red Alliance Wall",   halfTile,  -halfField,      mmTargetHeight, 90, 0, 180);

        final float CAMERA_FORWARD_DISPLACEMENT  = 0.0f * mmPerInch;   // eg: Enter the forward distance from the center of the robot to the camera lens
        final float CAMERA_VERTICAL_DISPLACEMENT = 8.9f * mmPerInch;   // eg: Camera is 6 Inches above ground
        final float CAMERA_LEFT_DISPLACEMENT     = 0.0f * mmPerInch;   // eg: Enter the left distance from the center of the robot to the camera lens

        OpenGLMatrix cameraLocationOnRobot = OpenGLMatrix
                .translation(CAMERA_FORWARD_DISPLACEMENT, CAMERA_LEFT_DISPLACEMENT, CAMERA_VERTICAL_DISPLACEMENT)
                .multiplied(Orientation.getRotationMatrix(EXTRINSIC, XZY, DEGREES, 90, 90, 0));

        for (VuforiaTrackable trackable : allTrackables) {
            ((VuforiaTrackableDefaultListener) trackable.getListener()).setCameraLocationOnRobot(parameters.cameraName, cameraLocationOnRobot);
        }

        targets.activate();


    }

    private void initTfod() {
        int tfodMonitorViewId = hardwareMap.appContext.getResources().getIdentifier(
                "tfodMonitorViewId", "id", hardwareMap.appContext.getPackageName());
        TFObjectDetector.Parameters tfodParameters = new TFObjectDetector.Parameters(tfodMonitorViewId);
        tfodParameters.minResultConfidence = 0.85f;
        tfodParameters.isModelTensorFlow2 = true;
        tfodParameters.inputSize = 320;
        tfod = ClassFactory.getInstance().createTFObjectDetector(tfodParameters, vuforia);
        tfod.loadModelFromAsset(TFOD_MODEL_ASSET, LABELS);
    }

    void    identifyTarget(int targetIndex, String targetName, float dx, float dy, float dz, float rx, float ry, float rz) {
        VuforiaTrackable aTarget = targets.get(targetIndex);
        aTarget.setName(targetName);
        aTarget.setLocation(OpenGLMatrix.translation(dx, dy, dz)
                .multiplied(Orientation.getRotationMatrix(EXTRINSIC, XYZ, DEGREES, rx, ry, rz)));
    }

    private void vuforiaTrack(){
        targetVisible = false;
        for (VuforiaTrackable trackable : allTrackables) {
            if (((VuforiaTrackableDefaultListener)trackable.getListener()).isVisible()) {
                telemetry.addData("Visible Target", trackable.getName());
                targetVisible = true;

                // getUpdatedRobotLocation() will return null if no new information is available since
                // the last time that call was made, or if the trackable is not currently visible.
                OpenGLMatrix robotLocationTransform = ((VuforiaTrackableDefaultListener)trackable.getListener()).getUpdatedRobotLocation();
                if (robotLocationTransform != null) {
                    lastLocation = robotLocationTransform;
                }
                break;
            }
        }

        // Provide feedback as to where the robot is located (if we know).
        if (targetVisible) {
            // express position (translation) of robot in inches.
            VectorF translation = lastLocation.getTranslation();
            telemetry.addData("Pos (inches)", "{X, Y, Z} = %.1f, %.1f, %.1f",
                    translation.get(0) / mmPerInch, translation.get(1) / mmPerInch, translation.get(2) / mmPerInch);
            VufXPos=translation.get(0) / mmPerInch;
            VufYPos=translation.get(2) / mmPerInch;

            // express the rotation of the robot in degrees.
            Orientation rotation = Orientation.getOrientation(lastLocation, EXTRINSIC, XYZ, DEGREES);
            telemetry.addData("Rot (deg)", "{Roll, Pitch, Heading} = %.0f, %.0f, %.0f", rotation.firstAngle, rotation.secondAngle, rotation.thirdAngle + 180);
            VufHeading = rotation.thirdAngle + 180;
        }
        else {
            telemetry.addData("Visible Target", "none");
        }
    }

    private void tfodTrack(){
        if (tfod != null) {
            List<Recognition> updatedRecognitions = tfod.getUpdatedRecognitions();
            if (updatedRecognitions != null) {
                telemetry.addData("# Object Detected", updatedRecognitions.size());
                noLabel  = updatedRecognitions.size();
                int i = 0;
                for (Recognition recognition : updatedRecognitions) {
                    telemetry.addData(String.format("label (%d)", i), recognition.getLabel());
                    telemetry.addData(String.format("  left,top (%d)", i), "%.03f , %.03f",
                            recognition.getLeft(), recognition.getTop());
                    telemetry.addData(String.format("  right,bottom (%d)", i), "%.03f , %.03f",
                            recognition.getRight(), recognition.getBottom());
                    labelName = recognition.getLabel();
                    if(labelName.equals("Duck")){
                        DuckRightPos=recognition.getRight();
                    }

                }


            }
        }
    }


}
