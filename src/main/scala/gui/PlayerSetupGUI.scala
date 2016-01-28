package gui

import controller.PlayerTypeAI
import controller.PlayerTypeHuman
import javax.swing.GroupLayout
import javax.swing.GroupLayout.Alignment
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JFrame
import javax.swing.JLabel

class PlayerSetupGUI extends JFrame("rob-chess") {
  private val l = new GroupLayout(getContentPane())
  getContentPane().setLayout(l)

  l.setAutoCreateGaps(true)
  l.setAutoCreateContainerGaps(true)

  val choices = Array(PlayerTypeHuman, PlayerTypeAI)

  val labelWhite = new JLabel("White")
  val labelBlack = new JLabel("Black")
  val choiceWhite = new JComboBox(choices)
  val choiceBlack = new JComboBox(choices)
  choiceBlack.setSelectedIndex(1)

  val okButton = new JButton("Start game")

  val hGroup = l.createSequentialGroup();

  hGroup.addGroup(l.createParallelGroup().
    addComponent(labelWhite).addComponent(labelBlack))
  hGroup.addGroup(l.createParallelGroup(GroupLayout.Alignment.TRAILING).
    addComponent(choiceWhite).addComponent(choiceBlack).addComponent(okButton))
  l.setHorizontalGroup(hGroup)

  val vGroup = l.createSequentialGroup();

  vGroup.addGroup(l.createParallelGroup(Alignment.BASELINE).
    addComponent(labelWhite).addComponent(choiceWhite))
  vGroup.addGroup(l.createParallelGroup(Alignment.BASELINE).
    addComponent(labelBlack).addComponent(choiceBlack))
  vGroup.addComponent(okButton)
  l.setVerticalGroup(vGroup)

  pack()
  setVisible(true)
}