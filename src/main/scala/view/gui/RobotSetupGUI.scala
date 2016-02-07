package view.gui

import java.awt.Dialog.ModalityType
import java.awt.FlowLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import controller.Application
import controller.CalibrateRobotEvent
import controller.Host
import controller.MeasureBoardEvent
import controller.RobotConnectEvent
import controller.RobotConnectedEvent
import controller.RobotSetupEvent
import controller.TrackingConnectEvent
import controller.TrackingConnectedEvent
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.border.EmptyBorder
import javax.swing.border.EtchedBorder
import javax.swing.filechooser.FileNameExtensionFilter
import robot.Robot
import robot.RobotController
import robot.Tracking
import view.RobotView

class RobotSetupGUI(owner: JFrame) extends AbstractGUI[RobotSetupGUIDialog](new RobotSetupGUIDialog(owner)) {
  def connectRobot(host: Host): Unit = Future {
    val robot = try {
      Some(new Robot(host))
    } catch {
      case _: Exception =>
        None
    }

    Application.queueEvent(RobotConnectedEvent(robot))
  }

  def connectTracking(host: Host): Unit = Future {
    val markerEffector = "Gripper_21012016"
    val markerChessboard = "Chessboard"

    val tracking = try {
      Some((Tracking(host, markerEffector), Tracking(host, markerChessboard)))
    } catch {
      case _: Exception =>
        None
    }

    Application.queueEvent(TrackingConnectedEvent(tracking))
  }

  def robotConnected(robot: Option[Robot]) {
    run { window =>
      window.robot = robot

      if (robot.isEmpty) {
        window.inputRobotIp.setEnabled(true)
        window.inputRobotPort.setEnabled(true)
        window.robotConnectButton.setText("Connect")
        window.robotConnectButton.setEnabled(true)
      } else {
        window.robotConnectButton.setText("Connected")
      }

      window.updateConnected()
    }
  }

  def trackingConnected(tracking: Option[(Tracking, Tracking)]) {
    run { window =>
      window.tracking = tracking

      if (tracking.isEmpty) {
        window.inputTrackingIp.setEnabled(true)
        window.inputTrackingPort.setEnabled(true)
        window.trackingConnectButton.setText("Connect")
        window.trackingConnectButton.setEnabled(true)
      } else {
        window.trackingConnectButton.setText("Connected")
      }

      window.updateConnected()
    }
  }

  def calibrateRobot(): Unit = Future {
    val control = run(_.control.get)
    control.calibrateRobot()
    run(_.updateCalibrationButtons())
  }

  def measureBoard(): Unit = Future {
    val control = run(_.control.get)
    control.measureBoard()
    run(_.updateMeasureButton())
  }
}

class RobotSetupGUIDialog(owner: JFrame) extends JDialog(owner, "Robot setup") with AbstractGUIWindow {
  var robot: Option[Robot] = None
  var tracking: Option[(Tracking, Tracking)] = None
  var control: Option[RobotController] = None

  private val loweredEtched = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED)

  private val robotPanel = new JPanel()
  robotPanel.setBorder(BorderFactory.createTitledBorder(loweredEtched, "Robot Server"))

  {
    val labelRobotIp = new JLabel("IP")
    robotPanel.add(labelRobotIp)

    val labelRobotPort = new JLabel("Port")
    robotPanel.add(labelRobotPort)
  }

  val inputRobotIp = new JTextField(16)
  inputRobotIp.setText("127.0.0.1")
  robotPanel.add(inputRobotIp)

  val inputRobotPort = new JTextField(6)
  inputRobotPort.setText("5005")
  robotPanel.add(inputRobotPort)

  val robotConnectButton = new JButton("Connect")
  robotPanel.add(robotConnectButton)

  robotConnectButton.addActionListener(new ActionListener {
    def actionPerformed(e: ActionEvent): Unit = {
      try {
        Application.queueEvent(RobotConnectEvent(
          Host(inputRobotIp.getText, inputRobotPort.getText.toInt)))

        inputRobotIp.setEnabled(false)
        inputRobotPort.setEnabled(false)
        robotConnectButton.setText("Connecting...")
        robotConnectButton.setEnabled(false)
      } catch {
        case _: Exception =>
      }
    }
  })

  private val trackingPanel = new JPanel()
  trackingPanel.setBorder(BorderFactory.createTitledBorder(loweredEtched, "Tracking Server"))

  {
    val labelTrackingIp = new JLabel("IP")
    trackingPanel.add(labelTrackingIp)

    val labelTrackingPort = new JLabel("Port")
    trackingPanel.add(labelTrackingPort)
  }

  val inputTrackingIp = new JTextField(16)
  inputTrackingIp.setText("141.83.19.44")
  trackingPanel.add(inputTrackingIp)

  val inputTrackingPort = new JTextField(6)
  inputTrackingPort.setText("5000")
  trackingPanel.add(inputTrackingPort)

  val trackingConnectButton = new JButton("Connect")
  trackingPanel.add(trackingConnectButton)

  trackingConnectButton.addActionListener(new ActionListener {
    def actionPerformed(e: ActionEvent): Unit = {
      try {
        Application.queueEvent(TrackingConnectEvent(
          Host(inputTrackingIp.getText, inputTrackingPort.getText.toInt)))

        inputTrackingIp.setEnabled(false)
        inputTrackingPort.setEnabled(false)
        trackingConnectButton.setText("Connecting...")
        trackingConnectButton.setEnabled(false)
      } catch {
        case _: Exception =>
      }
    }
  })

  private val calibrationPanel = new JPanel()
  calibrationPanel.setBorder(BorderFactory.createTitledBorder(loweredEtched, "Calibration"))

  val calibrateButton = new JButton("Calibrate Now")
  calibrateButton.setEnabled(false)
  calibrationPanel.add(calibrateButton)

  val calibrateLoadButton = new JButton("Load From File")
  calibrateLoadButton.setEnabled(false)
  calibrationPanel.add(calibrateLoadButton)

  val calibrateSaveButton = new JButton("Save To File")
  calibrateSaveButton.setEnabled(false)
  calibrationPanel.add(calibrateSaveButton)

  val measureButton = new JButton("Measure chessboard")
  measureButton.setEnabled(false)
  calibrationPanel.add(measureButton)

  calibrateButton.addActionListener(new ActionListener {
    def actionPerformed(e: ActionEvent): Unit = {
      calibrateButton.setEnabled(false)
      calibrateLoadButton.setEnabled(false)
      calibrateSaveButton.setEnabled(false)
      updateUseButton()

      Application.queueEvent(CalibrateRobotEvent)
    }
  })
  calibrateLoadButton.addActionListener(new ActionListener {
    def actionPerformed(e: ActionEvent): Unit = {
      val chooser = new JFileChooser
      val filter = new FileNameExtensionFilter("Calibration files (.cal)", "cal")
      chooser.setFileFilter(filter)

      if (chooser.showOpenDialog(RobotSetupGUIDialog.this) == JFileChooser.APPROVE_OPTION) {
        control.get.calibration = RobotController.loadCalibration(chooser.getSelectedFile)
        updateCalibrationButtons()
      }
    }
  })
  calibrateSaveButton.addActionListener(new ActionListener {
    def actionPerformed(e: ActionEvent): Unit = {
      val chooser = new JFileChooser
      val filter = new FileNameExtensionFilter("Calibration files (.cal)", "cal")
      chooser.setFileFilter(filter)

      if (chooser.showSaveDialog(RobotSetupGUIDialog.this) == JFileChooser.APPROVE_OPTION) {
        RobotController.saveCalibration(chooser.getSelectedFile, control.get.calibration.get)
      }
    }
  })
  measureButton.addActionListener(new ActionListener {
    def actionPerformed(e: ActionEvent): Unit = {
      measureButton.setEnabled(false)
      updateUseButton()

      Application.queueEvent(MeasureBoardEvent)
    }
  })

  private val statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT))
  statusPanel.setBorder(new EmptyBorder(5, 0, 5, 0))

  private val statusLabel = new JLabel("Please connect to both the robot and the tracking server to enable the robot.")
  statusPanel.add(statusLabel)

  val buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT))

  val skipButton = new JButton("Skip Robot Setup")
  buttonPanel.add(skipButton)

  val useButton = new JButton("Use This Robot")
  useButton.setEnabled(false)
  buttonPanel.add(useButton)

  skipButton.addActionListener(new ActionListener {
    def actionPerformed(e: ActionEvent): Unit = {
      Application.queueEvent(RobotSetupEvent(None))
      dispose()
    }
  })
  useButton.addActionListener(new ActionListener {
    def actionPerformed(e: ActionEvent): Unit = {
      Application.queueEvent(RobotSetupEvent(control.map(new RobotView(_))))
      dispose()
    }
  })

  {
    setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
    add(robotPanel)
    add(trackingPanel)
    add(calibrationPanel)
    add(statusPanel)
    add(buttonPanel)
  }

  pack()
  setModalityType(ModalityType.APPLICATION_MODAL)
  setLocationRelativeTo(owner)

  def updateUseButton() {
    useButton.setEnabled(
      control.get.calibration.isDefined &&
        control.get.t_Track_Board.isDefined &&
        calibrateButton.isEnabled() &&
        measureButton.isEnabled())

    statusLabel.setText(
      if (!calibrateButton.isEnabled() || !measureButton.isEnabled())
        "Please wait..."
      else if (control.get.calibration.isEmpty)
        "Please calibrate the robot or load calibration data from a file to enable it."
      else if (control.get.t_Track_Board.isEmpty)
        "Please measure the chessboard to enable the robot."
      else
        "The robot is ready to use.")

  }

  def updateCalibrationButtons() {
    calibrateButton.setEnabled(true)
    calibrateLoadButton.setEnabled(true)
    calibrateSaveButton.setEnabled(control.get.calibration.isDefined)
    updateUseButton()
  }

  def updateMeasureButton() {
    measureButton.setEnabled(true)
    updateUseButton()
  }

  def updateConnected() {
    (robot, tracking) match {
      case (Some(r), Some((tEff, tChess))) =>
        control = Some(new RobotController(r, tEff, tChess))
        updateCalibrationButtons()
        updateMeasureButton()

      case _ =>
    }
  }
}