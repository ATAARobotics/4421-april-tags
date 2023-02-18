package frc.robot.commands;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.controller.ProfiledPIDController;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.trajectory.Trajectory;
import edu.wpi.first.math.trajectory.TrapezoidProfile;
import edu.wpi.first.math.trajectory.Trajectory.State;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.CommandBase;
import frc.robot.AutoCommand;
import frc.robot.Constants;
import frc.robot.RobotContainer;
import frc.robot.subsystems.SwerveDriveSubsystem;

public class AutoDriveCommand extends CommandBase {
    private AutoCommand autoCommand;
    private SwerveDriveSubsystem m_swerveDriveSubsystem;
    private Timer timer = new Timer();
    private State desiredState;
    private Pose2d desiredPose;
    private double xVelocity;
    private double yVelocity;
    private double rotationVelocity;
    private boolean FirstAuto;
    private boolean FirstRun;
    private double currentAngle;
    private double desiredAngle;
    private PIDController rotationController = new PIDController(0.9, 0, 0);
    public AutoDriveCommand(SwerveDriveSubsystem swerveDriveSubsystem, AutoCommand autoCommand, boolean firstauto) {
        this.autoCommand = autoCommand;
        rotationController.enableContinuousInput(-Math.PI, Math.PI);
        xVelocity = 0;
        yVelocity = 0;
        rotationVelocity = 0;
        m_swerveDriveSubsystem = swerveDriveSubsystem;
        FirstAuto = firstauto;
        addRequirements(swerveDriveSubsystem);
    }

    @Override
    public void initialize() {
        FirstRun = true;
        // Configure the rotation PID to take the shortest route to the setpoint
        Pose2d currentPose = m_swerveDriveSubsystem.getPose();

        // Get the current angle of the robot
        currentAngle = currentPose.getRotation().getRadians();
        desiredAngle = autoCommand.getTargetAngle();
        System.out.println("First Desired Angle" + desiredAngle);
        rotationController.setSetpoint(desiredAngle);
    }

    @Override
    public void execute() {
        if(FirstAuto && FirstRun){
            timer.reset();
            timer.start();
            m_swerveDriveSubsystem.setBrakes(true);
            m_swerveDriveSubsystem.setFieldOriented(true, autoCommand.getRotationOffset());
            m_swerveDriveSubsystem.setInitialPose(new Pose2d(
                autoCommand.getState(0).poseMeters.getTranslation(), 
                new Rotation2d(autoCommand.getRotationOffset()))
            );
            FirstRun = false;
        }else if (FirstRun){
            timer.reset();
            timer.start(); 
            FirstRun = false;
        }
        desiredState = autoCommand.getState(timer.get());
        // Get the current position of the robot
        Pose2d currentPose = m_swerveDriveSubsystem.getPose();

        // Get the position we want to be at
        desiredPose = desiredState.poseMeters;

        // Get the current angle of the robot
        currentAngle = m_swerveDriveSubsystem.getHeading();
        desiredAngle = autoCommand.getTargetAngle();

        // Get the total speed the robot should be travelling (not accounting for
        // deviations)
        double totalVelocity = desiredState.velocityMetersPerSecond;

        // Get the velocity in the X and Y direction based on the heading and total
        // speed
        xVelocity = totalVelocity * desiredPose.getRotation().getCos();
        yVelocity = totalVelocity * desiredPose.getRotation().getSin();

        // Get the current rotational velocity from the rotation PID based on the
        // desired angle
        
        rotationVelocity = -rotationController.calculate(currentAngle);
        System.out.println("current angle: " + currentAngle);
        System.out.println("rotation velocity: " + rotationVelocity);
        m_swerveDriveSubsystem.setSwerveDrive(
                xVelocity,
                -yVelocity,
                rotationVelocity,
                true);
    }

    @Override
    public boolean isFinished() {
        SmartDashboard.putNumber("Current Angle", currentAngle);
        desiredAngle = autoCommand.getTargetAngle();
        double error =  currentAngle-desiredAngle;
        return timer.get() >= autoCommand.getLastState().timeSeconds && Math.abs(error) <= 0.1;
    }
}
