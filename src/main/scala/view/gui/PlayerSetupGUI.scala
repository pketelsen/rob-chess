package view.gui

import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import controller.Application
import controller.PlayerInfo
import controller.PlayerType
import controller.PlayerTypeAI
import controller.PlayerTypeHuman
import controller.StartGameEvent
import javax.swing.GroupLayout
import javax.swing.GroupLayout.Alignment
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JFrame
import javax.swing.JLabel
import controller.QuitEvent

class PlayerSetupGUI extends AbstractGUI[PlayerSetupGUIFrame](new PlayerSetupGUIFrame)

class PlayerSetupGUIFrame extends AbstractGUIFrame(" • player setup") {
  {
    val l = new GroupLayout(getContentPane())
    getContentPane().setLayout(l)

    l.setAutoCreateGaps(true)
    l.setAutoCreateContainerGaps(true)

    val choices: Array[PlayerType] = Array(PlayerTypeHuman, PlayerTypeAI)

    val labelWhite = new JLabel("White")
    val labelBlack = new JLabel("Black")
    val choiceWhite = new JComboBox[PlayerType](choices)
    val choiceBlack = new JComboBox[PlayerType](choices)
    choiceBlack.setSelectedIndex(1)

    val startButton = new JButton("Start game")
    startButton.addActionListener(new ActionListener {
      def actionPerformed(e: ActionEvent): Unit = {
        Application.queueEvent(StartGameEvent(
          PlayerInfo(choiceWhite.getSelectedItem.asInstanceOf[PlayerType]),
          PlayerInfo(choiceBlack.getSelectedItem.asInstanceOf[PlayerType])))

        dispose()
      }
    })

    val hGroup = l.createSequentialGroup()

    hGroup.addGroup(l.createParallelGroup().
      addComponent(labelWhite).addComponent(labelBlack))
    hGroup.addGroup(l.createParallelGroup(GroupLayout.Alignment.TRAILING).
      addComponent(choiceWhite).addComponent(choiceBlack).addComponent(startButton))
    l.setHorizontalGroup(hGroup)

    val vGroup = l.createSequentialGroup()

    vGroup.addGroup(l.createParallelGroup(Alignment.BASELINE).
      addComponent(labelWhite).addComponent(choiceWhite))
    vGroup.addGroup(l.createParallelGroup(Alignment.BASELINE).
      addComponent(labelBlack).addComponent(choiceBlack))
    vGroup.addComponent(startButton)
    l.setVerticalGroup(vGroup)

    pack()
    setVisible(true)
  }
}