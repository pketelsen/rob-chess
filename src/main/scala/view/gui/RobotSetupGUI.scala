package view.gui

import java.awt.Dialog.ModalityType
import java.awt.FlowLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener

import controller.Application
import controller.PlayerInfo
import controller.PlayerType
import controller.PlayerTypeAI
import controller.PlayerTypeHuman
import controller.RobotSetupEvent
import controller.StartGameEvent
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.border.EtchedBorder


class RobotSetupGUI(owner: JFrame) extends AbstractGUI[RobotSetupGUIDialog](new RobotSetupGUIDialog(owner))

class RobotSetupGUIDialog(owner: JFrame) extends JDialog(owner, "Robot setup") with AbstractGUIWindow {
  {
    val loweredEtched = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED)
    
    val labelRobotIp = new JLabel("IP")
    val inputRobotIp = new JTextField(16)
    val labelRobotPort = new JLabel("Port")
    val inputRobotPort = new JTextField(6)
    val robotConnectButton = new JButton("Connect")
    val robotPanel = new JPanel()
    robotPanel.setBorder(BorderFactory.createTitledBorder( loweredEtched, "Robot Server")) 
    robotPanel.add(labelRobotIp)
    robotPanel.add(inputRobotIp)
    robotPanel.add(labelRobotPort)
    robotPanel.add(inputRobotPort)
    robotPanel.add(robotConnectButton)
    
    val labelTrackingIp = new JLabel("IP")
    val inputTrackingIp = new JTextField(16)
    val labelTrackingPort = new JLabel("Port")
    val inputTrackingPort = new JTextField(6)
    val robotTrackingButton = new JButton("Connect")
    val trackingPanel = new JPanel()
    trackingPanel.setBorder(BorderFactory.createTitledBorder(loweredEtched, "Tracking Server"))
    trackingPanel.add(labelTrackingIp)
    trackingPanel.add(inputTrackingIp)
    trackingPanel.add(labelTrackingPort)
    trackingPanel.add(inputTrackingPort)
    trackingPanel.add(robotTrackingButton)
    robotTrackingButton.addActionListener(new ActionListener {
      def actionPerformed(e: ActionEvent): Unit = {
        // TODO
      }
    })
    
    val calibrateButton = new JButton("Calibrate Now")
    val calibrateLoadButton = new JButton("Load From File")
    val calibrateSaveButton = new JButton("Save To File")
    val calibrationPanel = new JPanel()
    calibrationPanel.setBorder(BorderFactory.createTitledBorder(loweredEtched, "Calibration")) 
    calibrationPanel.add(calibrateButton)
    calibrationPanel.add(calibrateLoadButton)
    calibrationPanel.add(calibrateSaveButton)
    calibrateButton.addActionListener(new ActionListener {
      def actionPerformed(e: ActionEvent): Unit = {
        // TODO
      }
    })
    calibrateLoadButton.addActionListener(new ActionListener {
      def actionPerformed(e: ActionEvent): Unit = {
        // TODO
      }
    })
    calibrateSaveButton.addActionListener(new ActionListener {
      def actionPerformed(e: ActionEvent): Unit = {
        // TODO
      }
    })
    
    val skipButton = new JButton("Skip Robot Setup")
    val useButton = new JButton("Use This Robot")
    val buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT))
    buttonPanel.add(skipButton);
    buttonPanel.add(useButton);
    skipButton.addActionListener(new ActionListener {
      def actionPerformed(e: ActionEvent): Unit = {
        Application.queueEvent(RobotSetupEvent(None))
        dispose()
      }
    })
    useButton.addActionListener(new ActionListener {
      def actionPerformed(e: ActionEvent): Unit = {
        // TODO
        Application.queueEvent(RobotSetupEvent(None))
        dispose()
      }
    })
    
    val container = new JPanel()
    container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
    container.add(robotPanel)
    container.add(trackingPanel)
    container.add(calibrationPanel)
    container.add(buttonPanel);
    getContentPane().add(container)
    
    pack()
    setModalityType(ModalityType.APPLICATION_MODAL)
    setLocationRelativeTo(owner)
    setVisible(true)
  }
}