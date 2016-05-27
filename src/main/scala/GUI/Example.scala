//package GUI
//
//import java.awt.Color
//import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter
//import javax.swing.text.{DefaultHighlighter, StyledDocument, EditorKit}
//import javax.swing.{JTextPane, BorderFactory, ImageIcon}
//import javax.swing.table.{DefaultTableModel, AbstractTableModel}
//import scala.swing._
//
//class TextPane() extends TextComponent {
//  override lazy val peer: JTextPane = new JTextPane() with SuperMixin
//  def contentType: String = peer.getContentType
//  def contentType_=(t: String) = peer.setContentType(t)
//  def editorKit: EditorKit = peer.getEditorKit
//  def editorKit_=(k: EditorKit) = peer.setEditorKit(k)
//  def styledDocument: StyledDocument = peer.getStyledDocument
//  def getText() = peer.getText
//  def attributes = peer.getInputAttributes
//
//  def addHighlight(end: Int, label: Int) = {
//    val painter = label match {
//      case 0 => new DefaultHighlighter.DefaultHighlightPainter(Color.red)
//      case 1 => new DefaultHighlighter.DefaultHighlightPainter(Color.yellow)
//      case 2 => new DefaultHighlighter.DefaultHighlightPainter(Color.green)
//    }
//    peer.getHighlighter.addHighlight(1, end, painter)
//  }
//}
//
//object GUI extends App {
//
//  val main = new MainFrame {
//    bounds = new Rectangle(400, 400, 400, 400)
//
//    val pane = new TextPane() {
//      bounds = new Rectangle(100, 100, 100, 100)
//    }
//
//    dis("HelloHelloHelloHelloHelloHelloHelloHelloHelloHelloHelloHelloHelloHelloHelloHelloHelloHelloHelloHelloHelloHelloHelloHelloHelloHelloHelloHelloHelloHelloHelloHelloHelloHello", 0, pane)
//    dis("world", 1, pane)
//    contents = pane
//  }
//
//  main.visible = true
//
//  def dis(s: String, count: Int, pane: TextPane) {
//    val _length = pane.getText().length()
//    val doc = pane.styledDocument
//    val atr = pane.attributes
//    doc.insertString(0, s, atr)
//    pane.addHighlight(s.length - 1, 0)
//    doc.insertString(0, "\n", atr)
//  }
//}