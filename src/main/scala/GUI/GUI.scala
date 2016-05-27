package GUI

import java.awt
import java.awt.Cursor
import java.io.File
import java.util.EventObject
import javax.swing.border.TitledBorder
import javax.swing.event.CellEditorListener
import javax.swing.{JTable, JScrollPane, ImageIcon}
import javax.swing.table._
import scala.io.Source
import scala.swing._
import scala.swing.event.ButtonClicked
import Sentiment.Citation
import Sentiment.Paper
import javax.swing.JOptionPane.showMessageDialog

import swing._
import event._

object Gui extends App {

  val main = new MainFrame {
    var paper = None: Option[Paper]
    title = "Citation analyse"

    val m = this

    val emptyLabel = new Label { text = "" }

    val paperLabel = new Label { text = "Target paper:" }

    val paperTextField = new TextField

    val fileNameLabel = new Label { text = "no opened file" }

    val openFileButton = new Button {
      text = "Open source paper"
      reactions += {
        case ButtonClicked(_) => {

          val file = choosePlainFile("Choose paper")
          file match {
            case Some(f) => {
              val paperText = Source.fromFile(f).getLines().mkString(" ")
              fileNameLabel.text = f.getName
              paper = Some(new Paper(paperText))
            }
            case None =>
          }
        }
      }
    }
    val sentimentButton = new Button {
      text = "Find citations"
      reactions += {
        case ButtonClicked(_) => {
          paper match {
            case None => showMessageDialog(null, "Error")
            case Some(p) => {
              val id = paperTextField.text
              val values = p.findCitations(id) map { c => citationToRow(c) }
              if (values.isEmpty) {
                showMessageDialog(null, "Error, incorrect target paper")
              }
              else {
                val tableFrame = new SentimentTable(values, id)
              }
            }
          }
        }
      }
    }

    //paperLabel, paperTextField, sentimentButton
    val significanceButton = new Button {
      text = "Significance"
      reactions += {
        case ButtonClicked(_) => {
          paper match {
            case None => showMessageDialog(null, "Error")
            case Some(p) => {
              val id = paperTextField.text
              try {
                val split = id.split(",")
                val year = split(1).toInt
                if (p.isPaperSignificance(split(0), year)) {
                  showMessageDialog(null, "Paper " + id + " is significance for source paper!")
                }
                else {
                  showMessageDialog(null, "Paper " + id + " is NOT significance for source paper!")
                }
              }
              catch {
                case e => showMessageDialog(null, "Wrong paper id!")
              }
            }
          }
        }
      }
    }
    val menu = new GridPanel(4, 1) {
      contents.append(paperLabel, paperTextField, sentimentButton, significanceButton)
      border = Swing.EmptyBorder(30, 30, 30, 30)
    }
    val fileMenu = new GridPanel(2, 1) {
      contents.append(fileNameLabel, openFileButton)
      border = Swing.EmptyBorder(30, 30, 30, 30)
    }
    contents = new GridPanel(1,1) {
      contents.append(fileMenu, menu)
    }
  }

  main.visible = true

  def choosePlainFile(title: String = ""): Option[File] = {
    val chooser = new FileChooser(new File("."))
    chooser.title = title
    val result = chooser.showOpenDialog(null)
    if (result == FileChooser.Result.Approve) {
      println("Approve -- " + chooser.selectedFile)
      Some(chooser.selectedFile)
    } else None
  }

  def citationToRow(cit: Citation) = {
    Array[AnyRef](cit.label.toString, cit.infos.mkString("\n"), cit.getFullText)
  }
}

